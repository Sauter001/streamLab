package tools.grader.level;

import domain.validation.ValidationResult;
import tools.validator.Validator;

import java.io.File;
import java.lang.reflect.Method;

/**
 * Level 문제 채점기
 * - CodeValidator로 코드 구조 검증
 * - TestExecutor로 메소드 실행
 * - ResultComparator로 결과 비교
 */
public class LevelGrader {
    private final CodeValidator codeValidator;
    private final InputPreparer inputPreparer;
    private final TestExecutor testExecutor;
    private final ResultComparator resultComparator;

    public LevelGrader(LevelTestData levelTestData) {
        this.codeValidator = new CodeValidator(levelTestData.getLevel());
        this.inputPreparer = new InputPreparer(levelTestData);
        this.testExecutor = new TestExecutor();
        this.resultComparator = new ResultComparator();
    }

    public GradeResult gradeProblem(LevelTestData.Problem problem, File sourceFile, Class<?> solutionClass) {
        Validator validator = codeValidator.createValidator(problem.getValidationOrDefault());
        ValidationResult validationResult = codeValidator.validateMethod(sourceFile, problem.getMethodName(), validator);

        if (!(validationResult instanceof ValidationResult.Ok)) {
            return new GradeResult(problem.getId(), validationResult, 0, problem.getTestCases().size());
        }

        int passedTests = runTestCases(problem, solutionClass);
        return new GradeResult(problem.getId(), validationResult, passedTests, problem.getTestCases().size());
    }

    private int runTestCases(LevelTestData.Problem problem, Class<?> solutionClass) {
        int passed = 0;
        Long timeLimitMs = problem.getValidationOrDefault().getTimeLimitMs();

        for (LevelTestData.TestCase testCase : problem.getTestCases()) {
            if (runSingleTestCase(problem, solutionClass, testCase, timeLimitMs)) {
                passed++;
            }
        }
        return passed;
    }

    private boolean runSingleTestCase(
            LevelTestData.Problem problem,
            Class<?> solutionClass,
            LevelTestData.TestCase testCase,
            Long timeLimitMs
    ) {
        try {
            Object input = inputPreparer.prepareInput(problem.getInputType(), testCase);
            Object expected = resultComparator.convertExpected(problem.getOutputType(), testCase.getExpected());
            Method method = testExecutor.findMethod(solutionClass, problem.getMethodName());

            long startTime = System.nanoTime();
            Object actual = testExecutor.invokeWithToolbox(method, input, problem.getRequiresToolbox());
            long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;

            if (isTimeExceeded(timeLimitMs, elapsedMs)) {
                System.out.println("  ⏱️ 시간 초과 (" + elapsedMs + "ms > " + timeLimitMs + "ms)");
                return false;
            }

            return resultComparator.compareResults(expected, actual);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTimeExceeded(Long timeLimitMs, long elapsedMs) {
        return timeLimitMs != null && elapsedMs > timeLimitMs;
    }

    public record GradeResult(
            String problemId,
            ValidationResult validationResult,
            int passedTests,
            int totalTests
    ) {
        public boolean isValid() {
            return validationResult instanceof ValidationResult.Ok;
        }

        public boolean allTestsPassed() {
            return passedTests == totalTests;
        }

        public boolean isComplete() {
            return isValid() && allTestsPassed();
        }
    }
}
