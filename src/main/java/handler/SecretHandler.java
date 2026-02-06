package handler;

import constants.LevelConstants;
import context.GameContext;
import domain.GameState;
import domain.Profile;
import repository.LevelDataRepository;
import repository.ProfileRepository;
import ui.view.secret.SecretView;
import ui.view.secret.SecretView.ProblemSummary;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

public class SecretHandler extends BaseGameLevelHandler {
    private final SecretView view;
    private final LevelDataRepository levelDataRepository;

    public SecretHandler(SecretView view, LevelDataRepository levelDataRepository, ProfileRepository profileRepository) {
        super(profileRepository);
        this.view = view;
        this.levelDataRepository = levelDataRepository;
    }

    @Override
    protected boolean canEnter(Profile profile) {
        return profile.isSecretUnlocked();
    }

    @Override
    protected void showAccessDeniedMessage() {
        System.out.println("Secret Phase에 접근할 수 없습니다.");
    }

    @Override
    protected int getLevel(Profile profile) {
        return LevelConstants.SECRET_LEVEL;
    }

    @Override
    protected void setupContext(int level) {
        GameContext context = GameState.getContext();
        if (context != null) {
            context.setCurrentLevel(LevelConstants.SECRET_LEVEL);
            context.setupLevelObserver();
        }
    }

    @Override
    protected void showIntro(Profile profile) {
        String secretKey = "secret";
        boolean showDialogue = profile.isLevelDialogueShown(secretKey);

        view.showSecretIntro(showDialogue);

        if (showDialogue) {
            profile.markLevelDialogueShown(secretKey);
            profileRepository.save(profile);
        }
    }

    @Override
    protected void showProblemList() {
        List<ProblemSummary> problems = loadSecretProblemSummaries();
        view.showProblemList(problems);
    }

    @Override
    protected void showPrompt() {
        view.showSecretPrompt();
    }

    @Override
    protected GameState processCommand(String command) {
        return switch (command) {
            case "m", "main" -> GameState.MAIN;
            default -> null;
        };
    }

    @Override
    protected GameState handleComplete(int completedLevel, BufferedReader reader) {
        view.showSecretCompleteOptions();

        while (true) {
            if (isMainCommand(readCommand(reader))) {
                return GameState.MAIN;
            }
        }
    }

    private boolean isMainCommand(String cmd) {
        return cmd != null && (cmd.equals("m") || cmd.equals("main"));
    }

    private List<ProblemSummary> loadSecretProblemSummaries() {
        var summaries = levelDataRepository.loadProblemSummaries(LevelConstants.SECRET_LEVEL);
        return summaries.stream()
                .map(s -> new ProblemSummary(s.id(), s.name(), s.description(), s.solved()))
                .toList();
    }

    private String readCommand(BufferedReader reader) {
        try {
            String input = reader.readLine();
            return input != null ? input.trim().toLowerCase() : null;
        } catch (IOException e) {
            return null;
        }
    }
}
