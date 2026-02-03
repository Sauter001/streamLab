package tools.watcher;

import util.ErrorLogger;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FileWatcher implements Runnable {
    private static final long DUPLICATE_EVENT_THRESHOLD_MS = 500;

    private final Path directoryToWatch;
    private final List<GraderObserver> observers;
    private final Map<Path, Long> lastEventTimeMap = new ConcurrentHashMap<>();
    private volatile boolean running = true;

    public FileWatcher(Path directoryToWatch) {
        this.directoryToWatch = directoryToWatch;
        this.observers = new ArrayList<>();
    }

    public void addObserver(GraderObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(GraderObserver observer) {
        observers.remove(observer);
    }

    public void stop() {
        running = false;
    }

    @Override
    public void run() {
        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            // 디렉토리와 하위 디렉토리 등록
            registerRecursive(directoryToWatch, watchService);

            while (running) {
                WatchKey key;
                try {
                    key = watchService.take();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }

                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                    Path fileName = pathEvent.context();

                    // .java 파일만 감시
                    if (fileName.toString().endsWith(".java")) {
                        Path watchedPath = (Path) key.watchable();
                        Path fullPath = watchedPath.resolve(fileName);

                        // Observer들에게 알림
                        notifyObservers(fullPath);
                    }
                }

                boolean valid = key.reset();
                if (!valid) {
                    break;
                }
            }
        } catch (IOException e) {
            ErrorLogger.system("파일 감시 시작 중 오류 발생", e);
        }
    }

    private void registerRecursive(Path start, WatchService watchService) throws IOException {
        Files.walk(start)
                .filter(Files::isDirectory)
                .forEach(path -> {
                    try {
                        // ENTRY_MODIFY만 감지하여 중복 이벤트 방지
                        path.register(
                                watchService,
                                StandardWatchEventKinds.ENTRY_MODIFY
                        );
                    } catch (IOException e) {
                        ErrorLogger.system("디렉토리 등록 중 오류 발생: " + path);
                    }
                });
    }

    private void notifyObservers(Path filePath) {
        if (isDuplicateEvent(filePath)) {
            return;
        }

        for (GraderObserver observer : observers) {
            observer.onFileChanged(filePath);
        }
    }

    private boolean isDuplicateEvent(Path filePath) {
        long now = System.currentTimeMillis();
        Long lastTime = lastEventTimeMap.get(filePath);

        if (lastTime != null && (now - lastTime) < DUPLICATE_EVENT_THRESHOLD_MS) {
            return true;
        }

        lastEventTimeMap.put(filePath, now);
        return false;
    }
}
