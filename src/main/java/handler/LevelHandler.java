package handler;

import constants.LevelConstants;
import context.GameContext;
import domain.GameState;
import domain.Profile;
import domain.level.LevelInfo;
import lombok.RequiredArgsConstructor;
import repository.LevelDataRepository;
import repository.ProfileRepository;
import ui.view.level.LevelView;
import ui.view.level.LevelView.ProblemSummary;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

@RequiredArgsConstructor
public class LevelHandler implements StateHandler {
    private final LevelView view;
    private final LevelDataRepository levelDataRepository;
    private final ProfileRepository profileRepository;

    @Override
    public GameState handle() {
        Profile profile = profileRepository.load().orElseThrow();
        int currentLevel = profile.getCurrentLevel();

        // 마지막 레벨 완료 후 LEVEL 진입 방지
        if (currentLevel > LevelConstants.MAX_LEVEL) {
            System.out.println("모든 레벨을 완료했습니다! 메인 화면으로 돌아갑니다.");
            System.out.println();
            return GameState.MAIN;
        }

        // GameContext에 현재 레벨 설정 후 Observer 등록
        GameContext context = GameState.getContext();
        if (context != null) {
            context.setCurrentLevel(currentLevel);
            context.setupLevelObserver();
        }

        LevelInfo levelInfo = LevelInfo.ofLevel(currentLevel);
        String levelKey = "level" + currentLevel;

        boolean showDialogue = profile.isLevelDialogueShown(levelKey);

        view.showLevelIntro(levelInfo, showDialogue);

        if (!showDialogue) {
            profile.markLevelDialogueShown(levelKey);
            profileRepository.save(profile);
        }

        List<ProblemSummary> problems = levelDataRepository.loadProblemSummaries(currentLevel);
        view.showProblemList(problems);
        view.showLevelPrompt(currentLevel);

        return waitForCommandOrCompletion(currentLevel, levelInfo);
    }

    private GameState waitForCommandOrCompletion(int currentLevel, LevelInfo levelInfo) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        GameContext context = GameState.getContext();

        while (true) {
            // 레벨 완료 체크
            if (context != null && context.isLevelCompleted(currentLevel)) {
                return handleLevelComplete(currentLevel, reader);
            }

            try {
                if (reader.ready()) {
                    String input = reader.readLine();
                    if (input != null) {
                        GameState nextState = processCommand(input.trim().toLowerCase(), levelInfo);
                        if (nextState != null) {
                            return nextState;
                        }
                    }
                }
            } catch (IOException e) {
                // ignore
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        return GameState.MAIN;
    }

    private GameState handleLevelComplete(int completedLevel, BufferedReader reader) {
        // Profile 업데이트
        Profile profile = profileRepository.load().orElseThrow();
        profile.passLevel(completedLevel);
        profileRepository.save(profile);

        // 선택지 표시
        view.showLevelCompleteOptions(completedLevel);

        while (true) {
            try {
                String input = reader.readLine();
                if (input != null) {
                    String cmd = input.trim().toLowerCase();
                    if (cmd.equals("1") || cmd.equals("n") || cmd.equals("next")) {
                        if (completedLevel < LevelConstants.MAX_LEVEL) {
                            return GameState.LEVEL;  // 다음 레벨로 (Profile.currentLevel이 이미 증가됨)
                        }
                        // 마지막 레벨이면 무시
                    } else if (cmd.equals("2") || cmd.equals("m") || cmd.equals("main")) {
                        return GameState.MAIN;
                    }
                }
            } catch (IOException e) {
                // ignore
            }
        }
    }

    private GameState processCommand(String command, LevelInfo levelInfo) {
        return switch (command) {
            case "o", "open" -> {
                view.showLearningObjectives(levelInfo);
                yield null;
            }
            case "m", "main" -> GameState.MAIN;
            default -> null;
        };
    }
}
