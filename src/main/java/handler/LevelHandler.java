package handler;

import constants.LevelConstants;
import context.GameContext;
import domain.GameState;
import domain.Profile;
import domain.level.LevelInfo;
import repository.LevelDataRepository;
import repository.ProfileRepository;
import ui.view.level.LevelView;
import ui.view.level.LevelView.ProblemSummary;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

public class LevelHandler extends BaseGameLevelHandler {
    private final LevelView view;
    private final LevelDataRepository levelDataRepository;

    private int currentLevel;
    private LevelInfo levelInfo;

    public LevelHandler(LevelView view, LevelDataRepository levelDataRepository, ProfileRepository profileRepository) {
        super(profileRepository);
        this.view = view;
        this.levelDataRepository = levelDataRepository;
    }

    @Override
    protected boolean canEnter(Profile profile) {
        return profile.getCurrentLevel() <= LevelConstants.MAX_LEVEL;
    }

    @Override
    protected void showAccessDeniedMessage() {
        System.out.println("모든 레벨을 완료했습니다! 메인 화면으로 돌아갑니다.");
        System.out.println();
    }

    @Override
    protected int getLevel(Profile profile) {
        this.currentLevel = profile.getCurrentLevel();
        this.levelInfo = LevelInfo.ofLevel(currentLevel);
        return currentLevel;
    }

    @Override
    protected void setupContext(int level) {
        GameContext context = GameState.getContext();
        if (context != null) {
            context.setCurrentLevel(level);
            context.setupLevelObserver();
        }
    }

    @Override
    protected void showIntro(Profile profile) {
        String levelKey = "level" + currentLevel;
        boolean showDialogue = profile.isLevelDialogueShown(levelKey);

        view.showLevelIntro(levelInfo, showDialogue);

        if (!showDialogue) {
            profile.markLevelDialogueShown(levelKey);
            profileRepository.save(profile);
        }
    }

    @Override
    protected void showProblemList() {
        List<ProblemSummary> problems = levelDataRepository.loadProblemSummaries(currentLevel);
        view.showProblemList(problems);
    }

    @Override
    protected void showPrompt() {
        view.showLevelPrompt(currentLevel);
    }

    @Override
    protected GameState processCommand(String command) {
        return switch (command) {
            case "o", "open" -> {
                view.showLearningObjectives(levelInfo);
                yield null;
            }
            case "m", "main" -> GameState.MAIN;
            default -> null;
        };
    }

    @Override
    protected GameState handleComplete(int completedLevel, BufferedReader reader) {
        view.showLevelCompleteOptions(completedLevel);

        while (true) {
            GameState result = processCompleteCommand(readCommand(reader), completedLevel);
            if (result != null) {
                return result;
            }
        }
    }

    private GameState processCompleteCommand(String cmd, int completedLevel) {
        if (cmd == null) {
            return null;
        }
        if (isNextLevelCommand(cmd) && completedLevel < LevelConstants.MAX_LEVEL) {
            return GameState.LEVEL;
        }
        if (isMainCommand(cmd)) {
            return GameState.MAIN;
        }
        return null;
    }

    private String readCommand(BufferedReader reader) {
        try {
            String input = reader.readLine();
            return input != null ? input.trim().toLowerCase() : null;
        } catch (IOException e) {
            return null;
        }
    }

    private boolean isNextLevelCommand(String cmd) {
        return cmd.equals("1") || cmd.equals("n") || cmd.equals("next");
    }

    private boolean isMainCommand(String cmd) {
        return cmd.equals("2") || cmd.equals("m") || cmd.equals("main");
    }
}
