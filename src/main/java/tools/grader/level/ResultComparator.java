package tools.grader.level;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 기대값과 실제값을 비교하는 클래스
 * - 타입 변환 (JSON -> Java 컬렉션)
 * - 부동소수점 오차 허용 비교
 * - 중첩 컬렉션/맵 비교
 */
public class ResultComparator {
    private static final double EPSILON = 1e-6;

    public Object convertExpected(String outputType, Object expected) {
        if (expected instanceof List<?> list) {
            return convertListExpected(outputType, list);
        }
        if (expected instanceof Map<?, ?> map && outputType.contains("Map")) {
            return convertMapExpected(outputType, map);
        }
        return expected;
    }

    private Object convertListExpected(String outputType, List<?> list) {
        if (outputType.contains("Set<Set")) {
            return convertToNestedSet(list);
        }
        if (outputType.contains("Set")) {
            return new HashSet<>(list);
        }
        return list;
    }

    private Set<Set<Object>> convertToNestedSet(List<?> list) {
        Set<Set<Object>> result = new HashSet<>();
        for (Object item : list) {
            if (item instanceof List<?> innerList) {
                result.add(convertToInnerSet(innerList));
            }
        }
        return result;
    }

    private Set<Object> convertToInnerSet(List<?> innerList) {
        Set<Object> innerSet = new HashSet<>();
        for (Object elem : innerList) {
            innerSet.add(elem instanceof Number ? ((Number) elem).intValue() : elem);
        }
        return innerSet;
    }

    private Object convertMapExpected(String outputType, Map<?, ?> map) {
        if (!outputType.contains("Map<Integer")) {
            return map;
        }
        Map<Integer, Object> converted = new HashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Integer key = Integer.parseInt(entry.getKey().toString());
            converted.put(key, entry.getValue());
        }
        return converted;
    }

    public boolean compareResults(Object expected, Object actual) {
        if (expected == null && actual == null) return true;
        if (expected == null || actual == null) return false;

        if (expected instanceof Number && actual instanceof Number) {
            return compareNumbers((Number) expected, (Number) actual);
        }
        if (expected instanceof Map && actual instanceof Map) {
            return compareMaps((Map<?, ?>) expected, (Map<?, ?>) actual);
        }
        return expected.equals(actual);
    }

    private boolean compareNumbers(Number expected, Number actual) {
        double expectedVal = expected.doubleValue();
        double actualVal = actual.doubleValue();
        return Math.abs(expectedVal - actualVal) < EPSILON;
    }

    private boolean compareMaps(Map<?, ?> expected, Map<?, ?> actual) {
        if (expected.size() != actual.size()) {
            return false;
        }
        for (Map.Entry<?, ?> entry : expected.entrySet()) {
            if (!compareMapEntry(entry, actual)) {
                return false;
            }
        }
        return true;
    }

    private boolean compareMapEntry(Map.Entry<?, ?> expectedEntry, Map<?, ?> actual) {
        Object actualValue = findMatchingValue(expectedEntry.getKey(), actual);
        if (actualValue == null) {
            return false;
        }
        return compareResults(expectedEntry.getValue(), actualValue);
    }

    private Object findMatchingValue(Object expectedKey, Map<?, ?> actual) {
        for (Map.Entry<?, ?> actualEntry : actual.entrySet()) {
            if (keysMatch(expectedKey, actualEntry.getKey())) {
                return actualEntry.getValue();
            }
        }
        return null;
    }

    private boolean keysMatch(Object key1, Object key2) {
        if (key1 == null && key2 == null) return true;
        if (key1 == null || key2 == null) return false;

        if (key1 instanceof Number && key2 instanceof Number) {
            return ((Number) key1).longValue() == ((Number) key2).longValue();
        }
        if (key1 instanceof String && key2 instanceof Boolean) {
            return Boolean.parseBoolean((String) key1) == (Boolean) key2;
        }
        if (key1 instanceof Boolean && key2 instanceof String) {
            return (Boolean) key1 == Boolean.parseBoolean((String) key2);
        }
        return key1.equals(key2);
    }
}
