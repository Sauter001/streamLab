package tools.watcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import context.GameContext;
import domain.validation.factory.ViolationFactory;
import tools.grader.TutorialGrader;
import tools.grader.TutorialTestData;
import tools.validator.CompositeValidator;
import tools.validator.NoVariableDeclarationValidator;
import tools.validator.SingleStatementValidator;
import ui.view.grading.GradingView;
import util.ErrorLogger;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;

public class TutorialGraderObserver extends AbstractGraderObserver {
    private static final String TUTORIAL_CLASS = "solutions.tutorial.Tutorial";
    private static final String TEST_DATA_PATH = "data/test-data/tutorial/tutorial.json";
    private static final int TUTORIAL_MAX_SCORE = 5;
    public static final String TUTORIAL_JAVA_FILE = "Tutorial.java";

    private final TutorialTestData testData;
    private final CompositeValidator validator;
    private final GradingView view;
    private final GameContext gameContext;

    public TutorialGraderObserver(GradingView view, GameContext gameContext) {
        super(TUTORIAL_JAVA_FILE, "GraderScheduler");
        this.view = view;
        this.gameContext = gameContext;
        try {
            ObjectMapper mapper = new ObjectMapper();
            this.testData = mapper.readValue(new File(TEST_DATA_PATH), TutorialTestData.class);

            this.validator = ViolationFactory.createStrictestValidator()
                    .add(new SingleStatementValidator())
                    .add(new NoVariableDeclarationValidator());
        } catch (Exception e) {
            throw new RuntimeException("TutorialGraderObserver 초기화 실패", e);
        }
    }

    @Override
    protected void onFileChangeDetected() {
        view.displayFileChangeDetected();
    }

    @Override
    protected void executeGrading(Path filePath) {
        view.displayGradingStart(filePath.getFileName().toString());

        if (!compileSource()) {
            ErrorLogger.user("컴파일 실패. 문법 오류를 확인하세요.");
            return;
        }

        int totalMethods = testData.getMethods().size();
        int completedMethods = 0;

        for (TutorialTestData.MethodTest methodTest : testData.getMethods()) {
            if (gradeMethod(methodTest, filePath.toFile())) {
                completedMethods++;
            }
        }

        if (completedMethods == totalMethods) {
            gameContext.setTutorialCompleted(true);
        }
    }

    private boolean gradeMethod(TutorialTestData.MethodTest methodTest, File sourceFile) {
        view.displayMethodHeader(methodTest.getName(), methodTest.getDescription());

        try {
            TutorialGrader grader = new TutorialGrader(validator, TUTORIAL_MAX_SCORE);

            TutorialGrader.GradeResult gradeResult = grader.gradeMethod(sourceFile, methodTest.getName());

            if (!gradeResult.isValid()) {
                view.displayFinalResult(gradeResult, 0, methodTest.getTestCases().size());
                return false;
            }

            int passedTests = runTestCases(methodTest);
            int totalTests = methodTest.getTestCases().size();

            view.displayFinalResult(gradeResult, passedTests, totalTests);

            return passedTests == totalTests;

        } catch (Exception e) {
            ErrorLogger.user("메서드 채점 중 오류 발생 " + methodTest.getName() + ": " + e.getMessage());
            return false;
        }
    }

    private int runTestCases(TutorialTestData.MethodTest methodTest) {
        int passed = 0;
        int testNum = 1;

        try {
            Class<?> tutorialClass = loadClass(TUTORIAL_CLASS);
            Method method = tutorialClass.getMethod(methodTest.getName(), List.class);

            view.displayTestStart();

            for (TutorialTestData.TestCase testCase : methodTest.getTestCases()) {
                try {
                    @SuppressWarnings("unchecked")
                    List<Integer> actual = (List<Integer>) method.invoke(null, testCase.getInput());

                    boolean testPassed = actual.equals(testCase.getExpected());
                    view.displayTestCaseResult(testNum, testPassed, testCase.getInput(), testCase.getExpected(), actual);

                    if (testPassed) {
                        passed++;
                    }
                } catch (Exception e) {
                    String errorMessage = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
                    view.displayTestCaseError(testNum, errorMessage);
                }
                testNum++;
            }

            System.out.println();
        } catch (ClassNotFoundException e) {
            ErrorLogger.user("Tutorial 클래스를 찾을 수 없습니다. 프로젝트가 컴파일되었는지 확인하세요.");
        } catch (Exception e) {
            ErrorLogger.user("테스트 케이스 실행 중 오류 발생: " + e.getMessage());
        }

        return passed;
    }
}
