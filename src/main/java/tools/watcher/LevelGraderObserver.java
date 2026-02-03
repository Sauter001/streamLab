package tools.watcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import constants.LevelConstants;
import context.GameContext;
import domain.Profile;
import repository.ProfileRepository;
import tools.grader.level.LevelGrader;
import tools.grader.level.LevelTestData;
import ui.view.grading.LevelGradingView;
import ui.view.grading.LevelGradingView.ProblemGradeResult;
import util.ErrorLogger;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class LevelGraderObserver extends AbstractGraderObserver {
    private static final String SOLUTION_CLASS_PATTERN = "solutions.level%d.Level%d";
    private static final String TEST_DATA_PATTERN = "data/test-data/level%d/level%d.json";
    private static final String LEVEL_FILE_PATTERN = "Level%d.java";
    private static final List<Integer> FIBONACCI_7 = List.of(1, 1, 2, 3, 5, 8, 13);

    // Secret Phase (Level 6) 전용 경로
    private static final String SECRET_SOLUTION_CLASS = "solutions.secret.LevelSecret";
    private static final String SECRET_TEST_DATA_PATH = "data/test-data/secret/secret.json";
    private static final String SECRET_FILE_NAME = "LevelSecret.java";

    private final int level;
    private final LevelTestData testData;
    private final LevelGradingView view;
    private final GameContext gameContext;
    private final ProfileRepository profileRepository;

    public LevelGraderObserver(int level, LevelGradingView view, GameContext gameContext, ProfileRepository profileRepository) {
        super(
                level == LevelConstants.SECRET_LEVEL ? SECRET_FILE_NAME : String.format(LEVEL_FILE_PATTERN, level),
                "LevelGraderScheduler-" + level
        );
        this.level = level;
        this.view = view;
        this.gameContext = gameContext;
        this.profileRepository = profileRepository;

        try {
            ObjectMapper mapper = new ObjectMapper();
            String testDataPath = level == LevelConstants.SECRET_LEVEL
                    ? SECRET_TEST_DATA_PATH
                    : String.format(TEST_DATA_PATTERN, level, level);
            this.testData = mapper.readValue(new File(testDataPath), LevelTestData.class);
        } catch (Exception e) {
            throw new RuntimeException("LevelGraderObserver 초기화 실패 (Level " + level + ")", e);
        }
    }

    @Override
    protected void onFileChangeDetected() {
        view.displayFileChangeDetected();
    }

    @Override
    protected void executeGrading(Path filePath) {
        try {
            view.displayGradingStart(filePath.getFileName().toString());

            if (!compileSource()) {
                ErrorLogger.user("컴파일 실패. 문법 오류를 확인하세요.");
                return;
            }

            String className = level == LevelConstants.SECRET_LEVEL
                    ? SECRET_SOLUTION_CLASS
                    : String.format(SOLUTION_CLASS_PATTERN, level, level);
            Class<?> solutionClass = loadClass(className);
            File sourceFile = filePath.toFile();

            LevelGrader grader = new LevelGrader(testData);

            List<ProblemGradeResult> results = new ArrayList<>();

            for (LevelTestData.Problem problem : testData.getProblems()) {
                LevelGrader.GradeResult gradeResult = grader.gradeProblem(problem, sourceFile, solutionClass);
                results.add(new ProblemGradeResult(problem, gradeResult));
            }

            view.displayCompactResults(level, results);

            long passedCount = results.stream().filter(ProblemGradeResult::isComplete).count();
            if (passedCount == results.size()) {
                gameContext.setLevelCompleted(level, true);
            }

            // Level 5는 항상 Secret Phase 해금 체크 (나중에 구현해도 해금 가능)
            if (level == LevelConstants.MAX_LEVEL) {
                checkSecretPhaseUnlock(solutionClass);
            }

        } catch (ClassNotFoundException e) {
            ErrorLogger.user("Level" + level + " 클래스를 찾을 수 없습니다. 프로젝트가 컴파일되었는지 확인하세요.");
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("컴파일")) {
                ErrorLogger.user(msg);
            } else {
                ErrorLogger.user("채점 중 오류 발생: " + msg);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void checkSecretPhaseUnlock(Class<?> solutionClass) {
        try {
            Method doNotTouchMethod = solutionClass.getMethod("doNotTouch");
            Object result = doNotTouchMethod.invoke(null);

            if (result instanceof List<?> list && list.equals(FIBONACCI_7)) {
                Profile profile = profileRepository.load().orElse(null);
                if (profile != null && !profile.isSecretUnlocked()) {
                    profile.unlockSecret();
                    profileRepository.save(profile);

                    // 해금 연출
                    System.out.println();
                    System.out.println("██████████████████████████████████████");
                    System.out.println("█                                    █");
                    System.out.println("█   ░░░ HIDDEN SEQUENCE DETECTED ░░░ █");
                    System.out.println("█                                    █");
                    System.out.println("██████████████████████████████████████");
                    System.out.println();
                    System.out.println("🔓 Secret Phase 해금!");
                    System.out.println();
                }
            }
        } catch (Exception e) {
            // doNotTouch 메서드 호출 실패 시 무시
        }
    }
}
