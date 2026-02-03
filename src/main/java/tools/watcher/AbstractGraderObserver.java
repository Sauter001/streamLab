package tools.watcher;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractGraderObserver implements GraderObserver {
    private static final long DEBOUNCE_DELAY_SECONDS = 2;

    private final String expectedFileName;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean isGrading = new AtomicBoolean(false);
    private ScheduledFuture<?> pendingGrade;

    protected AbstractGraderObserver(String expectedFileName, String schedulerName) {
        this.expectedFileName = expectedFileName;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, schedulerName);
            thread.setDaemon(true);
            return thread;
        });
    }

    @Override
    public final void onFileChanged(Path filePath) {
        String fileName = filePath.getFileName().toString();

        if (!fileName.equals(expectedFileName)) {
            return;
        }

        if (pendingGrade != null && !pendingGrade.isDone()) {
            pendingGrade.cancel(false);
        }

        onFileChangeDetected();

        pendingGrade = scheduler.schedule(
                () -> safeExecuteGrading(filePath),
                DEBOUNCE_DELAY_SECONDS, TimeUnit.SECONDS
        );
    }

    private void safeExecuteGrading(Path filePath) {
        if (!isGrading.compareAndSet(false, true)) {
            return;
        }
        try {
            executeGrading(filePath);
        } finally {
            isGrading.set(false);
        }
    }

    protected abstract void onFileChangeDetected();

    protected abstract void executeGrading(Path filePath);

    protected boolean compileSource() {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            String gradleCommand = os.contains("win") ? "gradlew.bat" : "./gradlew";

            ProcessBuilder pb = new ProcessBuilder(gradleCommand, "compileJava", "-q");
            pb.redirectErrorStream(true);
            pb.directory(new File(System.getProperty("user.dir")));

            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                System.err.println(output.toString().trim());
                return false;
            }

            return true;
        } catch (Exception e) {
            System.err.println("❌ 컴파일 프로세스 실행 실패: " + e.getMessage());
            return false;
        }
    }

    protected Class<?> loadClass(String className) throws Exception {
        String classFilePath = className.replace('.', '/') + ".class";
        Path classesDir = Paths.get("build/classes/java/main").toAbsolutePath();
        Path classFile = classesDir.resolve(classFilePath);

        byte[] classBytes = Files.readAllBytes(classFile);
        return new ByteArrayClassLoader(getClass().getClassLoader(), className, classBytes)
                .loadClass(className);
    }

    @Override
    public void shutdown() {
        if (pendingGrade != null && !pendingGrade.isDone()) {
            pendingGrade.cancel(false);
        }
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
