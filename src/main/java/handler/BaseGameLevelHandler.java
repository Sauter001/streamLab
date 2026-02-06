package handler;

import context.GameContext;
import domain.GameState;
import domain.Profile;
import repository.ProfileRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * LevelHandler와 SecretHandler의 공통 로직을 담은 추상 클래스
 * Template Method 패턴 적용
 */
public abstract class BaseGameLevelHandler implements StateHandler {
    protected final ProfileRepository profileRepository;

    protected BaseGameLevelHandler(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @Override
    public final GameState handle() {
        Profile profile = profileRepository.load().orElseThrow();

        if (!canEnter(profile)) {
            showAccessDeniedMessage();
            return GameState.MAIN;
        }

        int level = getLevel(profile);
        setupContext(level);
        showIntro(profile);
        showProblemList();
        showPrompt();

        return waitForCommandOrCompletion(level);
    }

    protected abstract boolean canEnter(Profile profile);

    protected abstract void showAccessDeniedMessage();

    protected abstract int getLevel(Profile profile);

    protected abstract void setupContext(int level);

    protected abstract void showIntro(Profile profile);

    protected abstract void showProblemList();

    protected abstract void showPrompt();

    protected abstract GameState processCommand(String command);

    protected abstract GameState handleComplete(int completedLevel, BufferedReader reader);

    protected final GameState waitForCommandOrCompletion(int level) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        GameContext context = GameState.getContext();

        do {
            if (context != null && context.isLevelCompleted(level)) {
                return handleLevelComplete(level, reader);
            }

            GameState nextState = tryReadCommand(reader);
            if (nextState != null) {
                return nextState;
            }

        } while (!shouldBreak());
        return GameState.MAIN;
    }

    private GameState tryReadCommand(BufferedReader reader) {
        try {
            if (!reader.ready()) {
                return null;
            }
            String input = reader.readLine();
            if (input == null) {
                return null;
            }
            return processCommand(input.trim().toLowerCase());
        } catch (IOException e) {
            return null;
        }
    }

    private boolean shouldBreak() {
        try {
            Thread.sleep(100);
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return true;
        }
    }

    protected final GameState handleLevelComplete(int completedLevel, BufferedReader reader) {
        Profile profile = profileRepository.load().orElseThrow();
        profile.passLevel(completedLevel);
        profileRepository.save(profile);

        return handleComplete(completedLevel, reader);
    }
}
