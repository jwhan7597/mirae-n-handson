package com.example.item;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 문항 검색 서비스. 레거시 {@code search.php} 의 입력 변환을 그대로 옮긴다.
 * 잘못된 입력을 거부하지 않는다 — 없는 단원 · 범위 밖 난이도는 0건으로 끝난다.
 */
@Service
@Transactional(readOnly = true)
public class ItemSearchService {

    private static final Logger log = LoggerFactory.getLogger(ItemSearchService.class);

    static final int PAGE_SIZE = 20;
    static final int MAX_PAGE = 999;
    static final int MAX_KEYWORD_LENGTH = 100;
    static final int MAX_TAG_LENGTH = 50;

    /** PHP {@code (int)} 가 읽는 숫자 접두사: 부호, 정수 · 소수, 지수. */
    private static final Pattern PHP_NUMERIC_PREFIX =
        Pattern.compile("^[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:[eE][+-]?[0-9]+)?");

    /** 정규화 도구(JavaScript {@code \s})가 공백으로 보는 문자. */
    private static final Pattern DISPLAY_WHITESPACE = Pattern.compile(
        "[\\t\\n\\u000B\\f\\r \\u00A0\\u1680\\u2000-\\u200A\\u2028\\u2029\\u202F\\u205F\\u3000\\uFEFF]+");

    private final ItemSearchRepository itemSearchRepository;

    public ItemSearchService(ItemSearchRepository itemSearchRepository) {
        this.itemSearchRepository = itemSearchRepository;
    }

    /** 전체 건수와 현재 페이지 행을 따로 읽는다. 페이지를 실제 총 페이지 수로 보정하지 않는다. */
    public ItemSearchResponse search(ItemSearchRequest request) {
        ItemSearchCriteria criteria = toCriteria(request);
        long total = itemSearchRepository.count(criteria);
        List<ItemSearchRowResponse> rows = itemSearchRepository.findPage(criteria).stream()
            .map(ItemSearchService::toRowResponse)
            .toList();
        log.debug("item search: total={}, rows={}, offset={}", total, rows.size(), criteria.offset());
        return ItemSearchResponse.of(rows, total);
    }

    private static ItemSearchCriteria toCriteria(ItemSearchRequest request) {
        String keyword = truncate(phpTrim(orEmpty(request.q())), MAX_KEYWORD_LENGTH);
        String unitCode = phpTrim(orEmpty(request.unit()));
        String level = phpTrim(orEmpty(request.level()));
        String tagName = truncate(phpTrim(orEmpty(request.tag())), MAX_TAG_LENGTH);
        int page = toPage(request.page() == null ? "1" : request.page());
        return new ItemSearchCriteria(
            emptyToNull(keyword),
            emptyToNull(unitCode),
            level.isEmpty() ? null : phpIntValue(level),
            emptyToNull(tagName),
            toOrder(request.sort(), request.dir()),
            PAGE_SIZE,
            (page - 1) * PAGE_SIZE);
    }

    /** id · title · unit 은 {@code desc} 일 때만 내림차순, level · created 는 {@code asc} 일 때만 오름차순이다. */
    private static ItemSearchOrder toOrder(String sortRaw, String dirRaw) {
        String sort = asciiLowerCase(phpTrim(orEmpty(sortRaw)));
        String dir = asciiLowerCase(phpTrim(orEmpty(dirRaw)));
        boolean asc = dir.equals("asc");
        boolean desc = dir.equals("desc");
        return switch (sort) {
            case "id" -> desc ? ItemSearchOrder.ID_DESC : ItemSearchOrder.ID_ASC;
            case "title" -> desc ? ItemSearchOrder.TITLE_DESC : ItemSearchOrder.TITLE_ASC;
            case "unit" -> desc ? ItemSearchOrder.UNIT_DESC : ItemSearchOrder.UNIT_ASC;
            case "level" -> asc ? ItemSearchOrder.LEVEL_ASC : ItemSearchOrder.LEVEL_DESC;
            case "created" -> asc ? ItemSearchOrder.CREATED_ASC : ItemSearchOrder.CREATED_DESC;
            default -> ItemSearchOrder.LEVEL_DESC;
        };
    }

    /**
     * 원문을 trim 하지 않는다. 숫자만으로 된 값이 아니면 1페이지, 그 밖에는 1~999 로 맞춘다.
     * PHP 정규식의 {@code $} 는 끝의 개행 하나를 허용하므로 그것도 따른다.
     */
    private static int toPage(String pageRaw) {
        String digits = pageRaw.endsWith("\n") ? pageRaw.substring(0, pageRaw.length() - 1) : pageRaw;
        if (digits.isEmpty() || !digits.chars().allMatch(c -> c >= '0' && c <= '9')) {
            return 1;
        }
        String significant = digits.replaceFirst("^0+", "");
        if (significant.isEmpty()) {
            return 1;
        }
        if (significant.length() > 3) {
            return MAX_PAGE;
        }
        return Integer.parseInt(significant);
    }

    /**
     * PHP {@code (int)$string} 과 같은 변환: 앞의 숫자 부분만 읽고 소수는 버린다.
     * 예: {@code 05} → 5, {@code 3abc} → 3, {@code 3.7} → 3, {@code 1e0} → 1, {@code abc} → 0.
     */
    private static long phpIntValue(String value) {
        int start = 0;
        while (start < value.length() && isPhpNumericWhitespace(value.charAt(start))) {
            start++;
        }
        Matcher matcher = PHP_NUMERIC_PREFIX.matcher(value.substring(start));
        if (!matcher.find()) {
            return 0;
        }
        // double → long 변환은 0 쪽으로 버리고 범위를 넘으면 최댓값 · 최솟값에 머문다.
        return (long) Double.parseDouble(matcher.group());
    }

    private static boolean isPhpNumericWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\u000B' || c == '\f';
    }

    /** PHP {@code trim()} 기본 문자만 제거한다: 공백, 탭, 개행, CR, NUL, 수직 탭. */
    private static String phpTrim(String value) {
        int start = 0;
        int end = value.length();
        while (start < end && isPhpTrimCharacter(value.charAt(start))) {
            start++;
        }
        while (end > start && isPhpTrimCharacter(value.charAt(end - 1))) {
            end--;
        }
        return value.substring(start, end);
    }

    private static boolean isPhpTrimCharacter(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\0' || c == '\u000B';
    }

    /** 글자(코드 포인트) 수 기준으로 자른다. 자른 뒤 다시 trim 하지 않는다. */
    private static String truncate(String value, int maxLength) {
        if (value.codePointCount(0, value.length()) <= maxLength) {
            return value;
        }
        return value.substring(0, value.offsetByCodePoints(0, maxLength));
    }

    private static String asciiLowerCase(String value) {
        StringBuilder lowered = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            lowered.append(c >= 'A' && c <= 'Z' ? (char) (c + ('a' - 'A')) : c);
        }
        return lowered.toString();
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String emptyToNull(String value) {
        return value.isEmpty() ? null : value;
    }

    private static ItemSearchRowResponse toRowResponse(ItemSearchRow row) {
        return new ItemSearchRowResponse(
            row.id(),
            displayText(row.title()),
            displayText(row.unitCode()),
            row.level(),
            splitTags(row.tagNames()));
    }

    /** 연속한 공백을 한 칸으로 줄이고 앞뒤 공백을 뺀다. */
    private static String displayText(String value) {
        return stripSpaces(DISPLAY_WHITESPACE.matcher(orEmpty(value)).replaceAll(" "));
    }

    /** 태그 번호 순 CSV 를 순서대로 나눈다. 빈 조각은 버린다. */
    private static List<String> splitTags(String tagNames) {
        return Arrays.stream(displayText(tagNames).split(",", -1))
            .map(ItemSearchService::stripSpaces)
            .filter(tag -> !tag.isEmpty())
            .toList();
    }

    private static String stripSpaces(String value) {
        int start = 0;
        int end = value.length();
        while (start < end && value.charAt(start) == ' ') {
            start++;
        }
        while (end > start && value.charAt(end - 1) == ' ') {
            end--;
        }
        return value.substring(start, end);
    }
}
