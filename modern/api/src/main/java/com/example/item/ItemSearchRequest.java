package com.example.item;

import java.math.BigInteger;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** PHP $_GET의 배열 색인·반복·혼용 순서를 보존하는 검색 요청. */
public record ItemSearchRequest(
    String q, String unit, String level, String tag, String sort, String dir, String page) {

    private static final Set<String> NAMES = Set.of("q", "unit", "level", "tag", "sort", "dir", "page");
    private static final Pattern INDEX = Pattern.compile("\\[([^\\]]*)\\]");
    private static final Pattern NUMERIC_INDEX = Pattern.compile("0|[1-9][0-9]*");

    /** MultiValueMap에서 사라지는 키 사이의 출현 순서를 원래 쿼리 문자열에서 읽는다. */
    static ItemSearchRequest fromQueryString(String query) {
        Map<String, ParameterValue> values = new LinkedHashMap<>();
        if (query != null) {
            for (String part : query.split("&", -1)) {
                int equals = part.indexOf('=');
                String key = decode(equals < 0 ? part : part.substring(0, equals));
                String value = decode(equals < 0 ? "" : part.substring(equals + 1));
                int bracket = key.indexOf('[');
                String name = bracket < 0 ? key : key.substring(0, bracket);
                if (!NAMES.contains(name)) {
                    continue;
                }
                if (bracket < 0) {
                    values.put(name, new ParameterValue(value));
                    continue;
                }
                List<String> indexes = indexes(key.substring(bracket));
                if (indexes.isEmpty()) {
                    continue;
                }
                ParameterValue root = values.get(name);
                if (root == null || root.elements == null) {
                    root = new ParameterValue();
                    values.put(name, root);
                }
                assign(root, indexes, value);
            }
        }
        return new ItemSearchRequest(
            phpValue(values.get("q")), phpValue(values.get("unit")), phpValue(values.get("level")),
            phpValue(values.get("tag")), phpValue(values.get("sort")), phpValue(values.get("dir")),
            phpValue(values.get("page")));
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static List<String> indexes(String suffix) {
        List<String> indexes = new ArrayList<>();
        Matcher matcher = INDEX.matcher(suffix);
        int offset = 0;
        while (matcher.find() && matcher.start() == offset) {
            indexes.add(matcher.group(1));
            offset = matcher.end();
        }
        return indexes;
    }

    private static void assign(ParameterValue root, List<String> indexes, String value) {
        ParameterValue current = root;
        for (int i = 0; i < indexes.size(); i++) {
            String index = indexes.get(i);
            String key = index.isEmpty() ? nextIndex(current.elements) : index;
            if (i == indexes.size() - 1) {
                // 같은 색인은 값을 교체하되 배열에서의 첫 등장 위치는 유지한다.
                current.elements.put(key, new ParameterValue(value));
            } else {
                ParameterValue child = current.elements.get(key);
                if (child == null || child.elements == null) {
                    child = new ParameterValue();
                    current.elements.put(key, child);
                }
                current = child;
            }
        }
    }

    private static String nextIndex(Map<String, ParameterValue> elements) {
        BigInteger maximum = BigInteger.valueOf(-1);
        for (String key : elements.keySet()) {
            if (NUMERIC_INDEX.matcher(key).matches()) {
                BigInteger index = new BigInteger(key);
                if (index.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) <= 0) {
                    maximum = maximum.max(index);
                }
            }
        }
        return maximum.add(BigInteger.ONE).toString();
    }

    private static String phpValue(ParameterValue value) {
        if (value == null) {
            return null;
        }
        if (value.elements == null) {
            return value.scalar;
        }
        if (value.elements.isEmpty()) {
            return "";
        }
        ParameterValue first = value.elements.values().iterator().next();
        return first.elements == null ? first.scalar : "Array";
    }

    private static final class ParameterValue {
        private final String scalar;
        private final Map<String, ParameterValue> elements;

        private ParameterValue(String scalar) {
            this.scalar = scalar;
            this.elements = null;
        }

        private ParameterValue() {
            this.scalar = null;
            this.elements = new LinkedHashMap<>();
        }
    }
}
