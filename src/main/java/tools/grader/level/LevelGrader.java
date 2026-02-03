package tools.grader.level;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import constants.LevelConstants;
import domain.AnalysisContext;
import domain.tools.GoldbachToolbox;
import domain.tools.GoldbachToolboxImpl;
import domain.tools.PermutationToolbox;
import domain.tools.PermutationToolboxImpl;
import domain.tools.SetToolbox;
import domain.tools.SetToolboxImpl;
import domain.validation.ValidationResult;
import domain.validation.factory.ViolationFactory;
import tools.validator.CompositeValidator;
import tools.validator.MaxVariableDeclarationValidator;
import tools.validator.Validator;

import java.io.File;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Level 문제 채점기
 * - Validator로 코드 구조 검증
 * - 리플렉션으로 메소드 실행 후 expected와 비교
 */
public class LevelGrader {
    private static final double EPSILON = 1e-6;
    private final LevelTestData levelTestData;
    private final TestDataAssembler assembler;
    private final int level;

    public LevelGrader(LevelTestData levelTestData) {
        this.levelTestData = levelTestData;
        this.assembler = new TestDataAssembler();
        this.level = levelTestData.getLevel();
    }

    /**
     * 특정 문제 채점
     */
    public GradeResult gradeProblem(LevelTestData.Problem problem, File sourceFile, Class<?> solutionClass) {
        // 1. Validator 구성
        Validator validator = createValidator(problem);

        // 2. 코드 구조 검증 (해당 메서드만)
        ValidationResult validationResult = validateMethod(sourceFile, problem.getMethodName(), validator);
        if (!(validationResult instanceof ValidationResult.Ok)) {
            return new GradeResult(problem.getId(), validationResult, 0, problem.getTestCases().size());
        }

        // 3. 테스트 케이스 실행
        int passedTests = runTestCases(problem, solutionClass);
        int totalTests = problem.getTestCases().size();

        return new GradeResult(problem.getId(), validationResult, passedTests, totalTests);
    }

    private Validator createValidator(LevelTestData.Problem problem) {
        LevelTestData.ValidationConfig config = problem.getValidationOrDefault();

        CompositeValidator validator = getValidatorByLevel();

        // maxVariables 설정에 따른 Validator 추가
        validator.add(new MaxVariableDeclarationValidator(config.getMaxVariables()));

        return validator;
    }

    private CompositeValidator getValidatorByLevel() {
        if (this.level <= LevelConstants.STRICT_VALIDATOR_MAX_LEVEL) {
            return ViolationFactory.createStrictestValidator();
        }

        if (this.level <= LevelConstants.MAX_LEVEL) {
            return ViolationFactory.createBlockLambdaAllowedValidator();
        }

        return ViolationFactory.createSecretPhaseValidator();
    }

    private ValidationResult validateMethod(File sourceFile, String methodName, Validator validator) {
        try {
            JavaParser parser = new JavaParser();
            CompilationUnit cu = parser.parse(sourceFile).getResult().orElseThrow();
            AnalysisContext context = AnalysisContext.forMethod(cu, methodName);
            return validator.validate(context);
        } catch (Exception e) {
            return ValidationResult.error("파일 파싱 실패: " + e.getMessage());
        }
    }

    private int runTestCases(LevelTestData.Problem problem, Class<?> solutionClass) {
        int passed = 0;
        Long timeLimitMs = problem.getValidationOrDefault().getTimeLimitMs();

        for (LevelTestData.TestCase testCase : problem.getTestCases()) {
            try {
                Object input = prepareInput(problem.getInputType(), testCase);
                Object expected = convertExpected(problem.getOutputType(), testCase.getExpected());

                Method method = findMethod(solutionClass, problem.getMethodName());

                long startTime = System.nanoTime();
                Object actual = invokeWithToolbox(method, input, problem.getRequiresToolbox());
                long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;

                // 시간 제한 체크
                if (timeLimitMs != null && elapsedMs > timeLimitMs) {
                    System.out.println("  ⏱️ 시간 초과 (" + elapsedMs + "ms > " + timeLimitMs + "ms)");
                    continue;
                }

                if (compareResults(expected, actual)) {
                    passed++;
                }
            } catch (Exception e) {
                // 테스트 실패
            }
        }

        return passed;
    }

    private Object invokeWithToolbox(Method method, Object input, String toolboxType) throws Exception {
        if (toolboxType == null) {
            return method.invoke(null, input);
        }

        return switch (toolboxType) {
            case "set" -> {
                SetToolbox<?> toolbox = new SetToolboxImpl<>();
                yield method.invoke(null, input, toolbox);
            }
            case "permutation" -> {
                PermutationToolbox<?> toolbox = new PermutationToolboxImpl<>();
                yield method.invoke(null, input, toolbox);
            }
            case "goldbach" -> {
                // input이 maxN 값
                int maxN = input instanceof Number ? ((Number) input).intValue() : 1_000_000;
                GoldbachToolbox toolbox = new GoldbachToolboxImpl(maxN);
                yield method.invoke(null, input, toolbox);
            }
            default -> method.invoke(null, input);
        };
    }

    private Object prepareInput(String inputType, LevelTestData.TestCase testCase) {
        LevelTestData.MasterData masterData = levelTestData.getMasterData();
        List<Long> inputIds = testCase.getInputIds();

        return switch (inputType.toLowerCase()) {
            case "students" -> filterById(
                    assembler.assembleStudents(masterData.getStudents()),
                    masterData.getStudents(),
                    inputIds
            );
            case "products" -> filterById(
                    assembler.assembleProducts(masterData.getProducts()),
                    masterData.getProducts(),
                    inputIds
            );
            case "characters" -> filterCharactersById(masterData, inputIds);
            case "orders" -> filterById(
                    assembler.assembleOrders(masterData.getOrders()),
                    masterData.getOrders(),
                    inputIds
            );
            case "integerset" -> convertToIntegerSet(testCase.getInputRaw());
            default -> testCase.getInputRaw();
        };
    }

    @SuppressWarnings("unchecked")
    private <T, D> List<T> filterById(List<T> assembled, List<D> dataList, List<Long> inputIds) {
        if (inputIds == null || inputIds.isEmpty()) {
            return assembled;
        }

        // JSON 파싱 시 Integer로 들어올 수 있으므로 Long으로 변환
        Set<Long> idSet = new HashSet<>();
        for (Object id : (List<?>) inputIds) {
            idSet.add(((Number) id).longValue());
        }

        List<T> filtered = new ArrayList<>();

        for (int i = 0; i < dataList.size(); i++) {
            try {
                // D has getId() method via reflection
                Method getIdMethod = dataList.get(i).getClass().getMethod("getId");
                Object idObj = getIdMethod.invoke(dataList.get(i));
                Long id = (idObj instanceof Number) ? ((Number) idObj).longValue() : (Long) idObj;
                if (idSet.contains(id)) {
                    filtered.add(assembled.get(i));
                }
            } catch (Exception e) {
                // fallback: use index-based
                if (idSet.contains((long) (i + 1))) {
                    filtered.add(assembled.get(i));
                }
            }
        }

        return filtered;
    }

    private List<?> filterCharactersById(LevelTestData.MasterData masterData, List<Long> inputIds) {
        var allCharacters = assembler.assembleCharacters(masterData);

        if (inputIds == null || inputIds.isEmpty()) {
            return allCharacters;
        }

        // JSON 파싱 시 Integer로 들어올 수 있으므로 Long으로 변환
        Set<Long> idSet = new HashSet<>();
        for (Object id : (List<?>) inputIds) {
            idSet.add(((Number) id).longValue());
        }

        return allCharacters.stream()
                .filter(c -> idSet.contains(c.getId()))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private Set<Integer> convertToIntegerSet(Object inputRaw) {
        if (inputRaw instanceof List<?> list) {
            Set<Integer> result = new HashSet<>();
            for (Object item : list) {
                if (item instanceof Number) {
                    result.add(((Number) item).intValue());
                }
            }
            return result;
        }
        return new HashSet<>();
    }

    private Method findMethod(Class<?> clazz, String methodName) throws NoSuchMethodException {
        for (Method method : clazz.getMethods()) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        throw new NoSuchMethodException("Method not found: " + methodName);
    }

    @SuppressWarnings("unchecked")
    private Object convertExpected(String outputType, Object expected) {
        if (expected instanceof List<?> list) {
            if (outputType.contains("Set<Set")) {
                // Set<Set<T>> 처리 - List<List<T>> -> Set<Set<T>>
                Set<Set<Object>> result = new HashSet<>();
                for (Object item : list) {
                    if (item instanceof List<?> innerList) {
                        Set<Object> innerSet = new HashSet<>();
                        for (Object elem : innerList) {
                            if (elem instanceof Number) {
                                innerSet.add(((Number) elem).intValue());
                            } else {
                                innerSet.add(elem);
                            }
                        }
                        result.add(innerSet);
                    }
                }
                return result;
            }
            if (outputType.contains("Set")) {
                return new HashSet<>(list);
            }
            return list;
        }
        if (expected instanceof Map<?, ?> map && outputType.contains("Map")) {
            // JSON의 Map 키는 String이므로, 필요시 Integer로 변환
            if (outputType.contains("Map<Integer")) {
                Map<Integer, Object> converted = new HashMap<>();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    Integer key = Integer.parseInt(entry.getKey().toString());
                    converted.put(key, entry.getValue());
                }
                return converted;
            }
            return map;
        }
        return expected;
    }

    private boolean compareResults(Object expected, Object actual) {
        if (expected == null && actual == null) return true;
        if (expected == null || actual == null) return false;

        // Double 비교 (부동소수점 오차 허용)
        if (expected instanceof Number && actual instanceof Number) {
            double expectedVal = ((Number) expected).doubleValue();
            double actualVal = ((Number) actual).doubleValue();
            return Math.abs(expectedVal - actualVal) < EPSILON;
        }

        // Set 비교
        if (expected instanceof Set && actual instanceof Set) {
            return expected.equals(actual);
        }

        // List 비교 (순서 무관하게 비교할지는 문제에 따라 다름)
        if (expected instanceof List && actual instanceof List) {
            return expected.equals(actual);
        }

        // Map 비교 (중첩 Map, 숫자 타입 차이 처리)
        if (expected instanceof Map && actual instanceof Map) {
            return compareMaps((Map<?, ?>) expected, (Map<?, ?>) actual);
        }

        // 일반 비교
        return expected.equals(actual);
    }

    private boolean compareMaps(Map<?, ?> expected, Map<?, ?> actual) {
        if (expected.size() != actual.size()) return false;

        for (Map.Entry<?, ?> entry : expected.entrySet()) {
            Object expectedKey = entry.getKey();
            Object expectedValue = entry.getValue();

            // actual에서 매칭되는 키 찾기 (숫자 타입 차이 허용)
            Object actualValue = null;
            boolean keyFound = false;

            for (Map.Entry<?, ?> actualEntry : actual.entrySet()) {
                if (keysMatch(expectedKey, actualEntry.getKey())) {
                    actualValue = actualEntry.getValue();
                    keyFound = true;
                    break;
                }
            }

            if (!keyFound) return false;
            if (!compareResults(expectedValue, actualValue)) return false;
        }

        return true;
    }

    private boolean keysMatch(Object key1, Object key2) {
        if (key1 == null && key2 == null) return true;
        if (key1 == null || key2 == null) return false;

        // 숫자 키 비교 (Integer, Long 등)
        if (key1 instanceof Number && key2 instanceof Number) {
            return ((Number) key1).longValue() == ((Number) key2).longValue();
        }

        // Boolean 키 비교 (String "true"/"false" vs Boolean)
        if (key1 instanceof String && key2 instanceof Boolean) {
            return Boolean.parseBoolean((String) key1) == (Boolean) key2;
        }
        if (key1 instanceof Boolean && key2 instanceof String) {
            return (Boolean) key1 == Boolean.parseBoolean((String) key2);
        }

        return key1.equals(key2);
    }

    /**
     * 채점 결과
     */
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




