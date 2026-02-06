package tools.grader.level;

import domain.tools.GoldbachToolbox;
import domain.tools.GoldbachToolboxImpl;
import domain.tools.PermutationToolbox;
import domain.tools.PermutationToolboxImpl;
import domain.tools.SetToolbox;
import domain.tools.SetToolboxImpl;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * 테스트 케이스 실행 클래스
 * - 리플렉션을 통한 메서드 찾기
 * - Toolbox와 함께 메서드 호출
 */
public class TestExecutor {

    public Method findMethod(Class<?> clazz, String methodName) throws NoSuchMethodException {
        return Arrays.stream(clazz.getMethods())
                .filter(m -> m.getName().equals(methodName))
                .findFirst()
                .orElseThrow(() -> new NoSuchMethodException("Method not found: " + methodName));
    }

    public Object invokeWithToolbox(Method method, Object input, String toolboxType) throws Exception {
        if (toolboxType == null) {
            return method.invoke(null, input);
        }
        return invokeWithSpecificToolbox(method, input, toolboxType);
    }

    private Object invokeWithSpecificToolbox(Method method, Object input, String toolboxType) throws Exception {
        return switch (toolboxType) {
            case "set" -> invokeWithSetToolbox(method, input);
            case "permutation" -> invokeWithPermutationToolbox(method, input);
            case "goldbach" -> invokeWithGoldbachToolbox(method, input);
            default -> method.invoke(null, input);
        };
    }

    private Object invokeWithSetToolbox(Method method, Object input) throws Exception {
        SetToolbox<?> toolbox = new SetToolboxImpl<>();
        return method.invoke(null, input, toolbox);
    }

    private Object invokeWithPermutationToolbox(Method method, Object input) throws Exception {
        PermutationToolbox<?> toolbox = new PermutationToolboxImpl<>();
        return method.invoke(null, input, toolbox);
    }

    private Object invokeWithGoldbachToolbox(Method method, Object input) throws Exception {
        int maxN = input instanceof Number ? ((Number) input).intValue() : 1_000_000;
        GoldbachToolbox toolbox = new GoldbachToolboxImpl(maxN);
        return method.invoke(null, input, toolbox);
    }
}
