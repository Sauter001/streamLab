package handler;

import constants.LevelConstants;
import context.GameContext;
import domain.GameState;
import domain.Profile;
import lombok.RequiredArgsConstructor;
import repository.LevelDataRepository;
import repository.ProfileRepository;
import ui.view.secret.SecretView;
import ui.view.secret.SecretView.ProblemSummary;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

@RequiredArgsConstructor
public class SecretHandler implements StateHandler {
    private final SecretView view;
    private final LevelDataRepository levelDataRepository;
    private final ProfileRepository profileRepository;

    @Override
    public GameState handle() {
        Profile profile = profileRepository.load().orElseThrow();

        // Secret Phase 해금 여부 확인
        if (!profile.isSecretUnlocked()) {
            System.out.println("Secret Phase에 접근할 수 없습니다.");
            return GameState.MAIN;
        }

        // GameContext에 Secret Level 설정 후 Observer 등록
        GameContext context = GameState.getContext();
        if (context != null) {
            context.setCurrentLevel(LevelConstants.SECRET_LEVEL);
            context.setupLevelObserver();
        }

        String secretKey = "secret";
        boolean showDialogue = profile.isLevelDialogueShown(secretKey);

        view.showSecretIntro(showDialogue);

        if (showDialogue) {
            profile.markLevelDialogueShown(secretKey);
            profileRepository.save(profile);
        }

        List<ProblemSummary> problems = loadSecretProblemSummaries();
        view.showProblemList(problems);
        view.showSecretPrompt();

        return waitForCommandOrCompletion();
    }

    private List<ProblemSummary> loadSecretProblemSummaries() {
        // LevelDataRepository를 활용하여 secret.json에서 로드
        // level 6으로 처리
        var summaries = levelDataRepository.loadProblemSummaries(LevelConstants.SECRET_LEVEL);
        return summaries.stream()
                .map(s -> new ProblemSummary(s.id(), s.name(), s.description(), s.solved()))
                .toList();
    }

    private GameState waitForCommandOrCompletion() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        GameContext context = GameState.getContext();

        while (true) {
            // Secret Phase 완료 체크
            if (context != null && context.isLevelCompleted(LevelConstants.SECRET_LEVEL)) {
                return handleSecretComplete(reader);
            }

            try {
                if (reader.ready()) {
                    String input = reader.readLine();
                    if (input != null) {
                        GameState nextState = processCommand(input.trim().toLowerCase());
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

    private GameState handleSecretComplete(BufferedReader reader) {
        // Profile 업데이트 (Secret 완료 표시)
        Profile profile = profileRepository.load().orElseThrow();
        profile.passLevel(LevelConstants.SECRET_LEVEL);
        profileRepository.save(profile);

        view.showSecretCompleteOptions();

        while (true) {
            try {
                String input = reader.readLine();
                if (input != null) {
                    String cmd = input.trim().toLowerCase();
                    if (cmd.equals("m") || cmd.equals("main")) {
                        return GameState.MAIN;
                    }
                }
            } catch (IOException e) {
                // ignore
            }
        }
    }

    private GameState processCommand(String command) {
        return switch (command) {
            case "m", "main" -> GameState.MAIN;
            default -> null;
        };
    }
}
