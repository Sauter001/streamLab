package tools.grader.level;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 테스트 입력 데이터를 준비하는 클래스
 * - 마스터 데이터에서 ID로 필터링
 * - 원시 입력을 적절한 타입으로 변환
 * - 도메인 객체 조립
 */
public class InputPreparer {
    private final LevelTestData levelTestData;
    private final TestDataAssembler assembler;

    public InputPreparer(LevelTestData levelTestData) {
        this.levelTestData = levelTestData;
        this.assembler = new TestDataAssembler();
    }

    public Object prepareInput(String inputType, LevelTestData.TestCase testCase) {
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

    private <T, D> List<T> filterById(List<T> assembled, List<D> dataList, List<Long> inputIds) {
        if (inputIds == null || inputIds.isEmpty()) {
            return assembled;
        }

        Set<Long> idSet = toLongSet(inputIds);
        List<T> filtered = new ArrayList<>();

        for (int i = 0; i < dataList.size(); i++) {
            Long id = extractId(dataList.get(i), i);
            if (idSet.contains(id)) {
                filtered.add(assembled.get(i));
            }
        }
        return filtered;
    }

    private Long extractId(Object data, int index) {
        try {
            Method getIdMethod = data.getClass().getMethod("getId");
            Object idObj = getIdMethod.invoke(data);
            return (idObj instanceof Number) ? ((Number) idObj).longValue() : (Long) idObj;
        } catch (Exception e) {
            return (long) (index + 1);
        }
    }

    private List<?> filterCharactersById(LevelTestData.MasterData masterData, List<Long> inputIds) {
        var allCharacters = assembler.assembleCharacters(masterData);

        if (inputIds == null || inputIds.isEmpty()) {
            return allCharacters;
        }

        Set<Long> idSet = toLongSet(inputIds);
        return allCharacters.stream()
                .filter(c -> idSet.contains(c.getId()))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private Set<Long> toLongSet(List<Long> inputIds) {
        Set<Long> idSet = new HashSet<>();
        for (Object id : (List<?>) inputIds) {
            idSet.add(((Number) id).longValue());
        }
        return idSet;
    }

    private Set<Integer> convertToIntegerSet(Object inputRaw) {
        if (!(inputRaw instanceof List<?> list)) {
            return new HashSet<>();
        }

        Set<Integer> result = new HashSet<>();
        for (Object item : list) {
            addIfNumber(result, item);
        }
        return result;
    }

    private void addIfNumber(Set<Integer> set, Object item) {
        if (item instanceof Number number) {
            set.add(number.intValue());
        }
    }
}
