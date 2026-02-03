package context;

import constants.LevelConstants;
import domain.GameState;
import lombok.Getter;
import lombok.Setter;
import repository.ProfileRepository;
import tools.watcher.FileWatcher;
import tools.watcher.GraderObserver;
import tools.watcher.LevelGraderObserver;
import tools.watcher.TutorialGraderObserver;
import ui.view.grading.GradingConsoleView;
import ui.view.grading.GradingView;
import ui.view.grading.LevelGradingConsoleView;
import ui.view.grading.LevelGradingView;

import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GameContext {
    private final FileWatcher fileWatcher;
    private final GradingView gradingView;
    private final LevelGradingView levelGradingView;
    private final ProfileRepository profileRepository;
    private GraderObserver currentObserver;
    @Setter
    @Getter
    private volatile boolean tutorialCompleted = false;
    private final Map<Integer, Boolean> levelCompletedMap = new ConcurrentHashMap<>();
    @Setter
    @Getter
    private int currentLevel = LevelConstants.INITIAL_LEVEL;

    public GameContext() {
        this.fileWatcher = new FileWatcher(Paths.get("src/main/java/solutions"));
        this.gradingView = new GradingConsoleView();
        this.levelGradingView = new LevelGradingConsoleView();
        this.profileRepository = new ProfileRepository();
    }

    public void setLevelCompleted(int level, boolean completed) {
        levelCompletedMap.put(level, completed);
    }

    public boolean isLevelCompleted(int level) {
        return levelCompletedMap.getOrDefault(level, false);
    }

    public void startWatcher() {
        Thread watcherThread = new Thread(fileWatcher, "FileWatcherThread");
        watcherThread.setDaemon(true);
        watcherThread.start();
    }

    public void stopWatcher() {
        fileWatcher.stop();
    }

    public void switchObserver(GameState state) {
        clearCurrentObserver();

        switch (state) {
            case TUTORIAL -> {
                currentObserver = new TutorialGraderObserver(gradingView, this);
                fileWatcher.addObserver(currentObserver);
            }
            case LEVEL -> {
                // LEVEL은 LevelHandler에서 setCurrentLevel 후 setupLevelObserver() 호출
            }
            case MAIN -> {
                // Main menu에서는 Observer가 필요하지 않음
            }
            case INTRO, PROFILE, EXIT -> {
                // Observer가 필요하지 않음
            }
        }
    }

    public void setupLevelObserver() {
        clearCurrentObserver();
        currentObserver = new LevelGraderObserver(currentLevel, levelGradingView, this, profileRepository);
        fileWatcher.addObserver(currentObserver);
    }

    private void clearCurrentObserver() {
        if (currentObserver != null) {
            fileWatcher.removeObserver(currentObserver);
            currentObserver.shutdown();
            currentObserver = null;
        }
    }
}
