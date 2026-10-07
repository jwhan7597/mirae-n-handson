package com.example.item;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 레거시 PHP 의 입력 변환과 응답 구성을 검증한다. 저장소는 목. */
@ExtendWith(MockitoExtension.class)
class ItemSearchServiceTest {

    @Mock
    private ItemSearchRepository itemSearchRepository;

    @InjectMocks
    private ItemSearchService itemSearchService;

    @Test
    @DisplayName("파라미터가 없거나 공백뿐이면 조건 없이 난이도 미지정 · 기본 정렬 · 1페이지")
    void missingOrBlankParametersUseDefaults() {
        ItemSearchCriteria expected =
            new ItemSearchCriteria(null, null, null, null, ItemSearchOrder.LEVEL_DESC, 20, 0);

        assertThat(criteriaFor(request(null, null, null, null, null, null, null))).isEqualTo(expected);
        assertThat(criteriaFor(request(" ", "", " ", "", "", "", ""))).isEqualTo(expected);
    }

    @ParameterizedTest(name = "level=[{0}] → {1}")
    @CsvSource({
        "5, 5",
        "' 4 ', 4",
        "05, 5",
        "3abc, 3",
        "3.7, 3",
        "1e0, 1",
        "-1, -1",
        "abc, 0",
    })
    @DisplayName("비어 있지 않은 난이도는 PHP (int) 처럼 숫자 접두사만 읽는다")
    void levelIsConvertedLikePhpIntCast(String level, long expected) {
        assertThat(criteriaFor(request(null, null, level, null, null, null, null)).level()).isEqualTo(expected);
    }

    @ParameterizedTest(name = "page=[{0}] → offset {1}")
    @CsvSource({
        "'2', 20",
        "'0', 0",
        "'', 0",
        "'999', 19960",
        "'1000', 19960",
        "' 2 ', 0",
        "'-1', 0",
        "'2a', 0",
    })
    @DisplayName("페이지는 trim 없이 숫자만 인정하고 1~999 로 맞춘다")
    void pageIsDigitsOnlyAndClamped(String page, int expectedOffset) {
        assertThat(criteriaFor(request(null, null, null, null, null, null, page)).offset()).isEqualTo(expectedOffset);
    }

    @Test
    @DisplayName("페이지 끝의 개행 1개는 허용해 숫자로 읽는다 (PHP $ 동작)")
    void pageAllowsSingleTrailingNewline() {
        assertThat(criteriaFor(request(null, null, null, null, null, null, "2\n")).offset()).isEqualTo(20);
    }

    @Test
    @DisplayName("페이지 끝의 개행 2개는 거부해 1페이지로 본다")
    void pageRejectsTwoTrailingNewlines() {
        assertThat(criteriaFor(request(null, null, null, null, null, null, "2\n\n")).offset()).isZero();
    }

    @Test
    @DisplayName("페이지의 선행 0은 무시하고 숫자로 읽는다")
    void pageIgnoresLeadingZeros() {
        assertThat(criteriaFor(request(null, null, null, null, null, null, "0002")).offset()).isEqualTo(20);
    }

    @Test
    @DisplayName("아주 긴 숫자 페이지는 오버플로 없이 999 로 맞춘다")
    void pageClampsVeryLongNumber() {
        assertThat(criteriaFor(request(null, null, null, null, null, null, "9".repeat(100))).offset())
            .isEqualTo(19960);
    }

    @ParameterizedTest(name = "sort=[{0}] dir=[{1}] → {2}")
    @CsvSource({
        "id, '', ID_ASC",
        "id, desc, ID_DESC",
        "id, sideways, ID_ASC",
        "title, desc, TITLE_DESC",
        "unit, asc, UNIT_ASC",
        "' UNIT ', ' DESC ', UNIT_DESC",
        "level, '', LEVEL_DESC",
        "level, asc, LEVEL_ASC",
        "created, '', CREATED_DESC",
        "created, asc, CREATED_ASC",
        "stem, asc, LEVEL_DESC",
        "'', asc, LEVEL_DESC",
    })
    @DisplayName("정렬 기준마다 기본 방향이 다르고, 모르는 기준은 기본 정렬")
    void sortAndDirectionResolveToAllowedOrder(String sort, String dir, ItemSearchOrder expected) {
        assertThat(criteriaFor(request(null, null, null, null, sort, dir, null)).order()).isEqualTo(expected);
    }

    @Test
    @DisplayName("키워드는 100자, 태그는 50자를 넘을 때만 자르고 단원은 입력 그대로 넘긴다")
    void keywordAndTagAreTruncatedByCharacterCount() {
        ItemSearchCriteria atLimit = criteriaFor(
            request("가".repeat(100), " m5-1 ", null, "나".repeat(50), null, null, null));
        ItemSearchCriteria overLimit = criteriaFor(
            request("가".repeat(101), null, null, "나".repeat(51), null, null, null));

        assertThat(atLimit.keyword()).isEqualTo("가".repeat(100));
        assertThat(atLimit.tagName()).isEqualTo("나".repeat(50));
        assertThat(atLimit.unitCode()).isEqualTo("m5-1");
        assertThat(overLimit.keyword()).isEqualTo("가".repeat(100));
        assertThat(overLimit.tagName()).isEqualTo("나".repeat(50));
    }

    @Test
    @DisplayName("전체 0건이면 결과 없음 문구, 행은 빈 목록")
    void zeroTotalReturnsNoResultMessage() {
        ItemSearchResponse response = itemSearchService.search(request(null, "Z99-99", null, null, null, null, null));

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.rows()).isEmpty();
        assertThat(response.count()).isZero();
        assertThat(response.message()).isEqualTo("검색 결과가 없습니다");
    }

    @Test
    @DisplayName("전체 건수가 있는데 현재 페이지 행만 없으면 count 는 전체 건수, message 는 null")
    void pageBeyondTotalKeepsCountWithoutMessage() {
        when(itemSearchRepository.count(any())).thenReturn(20L);
        when(itemSearchRepository.findPage(any())).thenReturn(List.of());

        ItemSearchResponse response = itemSearchService.search(request(null, null, null, null, null, null, "2"));

        assertThat(response.rows()).isEmpty();
        assertThat(response.count()).isEqualTo(20);
        assertThat(response.message()).isNull();
    }

    @Test
    @DisplayName("행은 조회 순서 그대로, 표시 문자열은 공백을 정리하고 태그 CSV 는 순서대로 나눈다")
    void rowsAreMappedInOrderWithNormalizedText() {
        when(itemSearchRepository.count(any())).thenReturn(2L);
        when(itemSearchRepository.findPage(any())).thenReturn(List.of(
            new ItemSearchRow(7, " 분수의  덧셈\n", "M5-1", 4, "계산, 문장제,,오답률높음"),
            new ItemSearchRow(3, "소수 곱셈", "M5-3", 1, null)));

        ItemSearchResponse response = itemSearchService.search(request(null, null, null, null, null, null, null));

        assertThat(response.rows()).containsExactly(
            new ItemSearchRowResponse(7, "분수의 덧셈", "M5-1", 4, List.of("계산", "문장제", "오답률높음")),
            new ItemSearchRowResponse(3, "소수 곱셈", "M5-3", 1, List.of()));
        assertThat(response.count()).isEqualTo(2);
        assertThat(response.message()).isNull();
    }

    private ItemSearchCriteria criteriaFor(ItemSearchRequest request) {
        itemSearchService.search(request);
        ArgumentCaptor<ItemSearchCriteria> captor = ArgumentCaptor.forClass(ItemSearchCriteria.class);
        verify(itemSearchRepository).findPage(captor.capture());
        // 전체 건수와 현재 페이지는 같은 조건으로 읽는다.
        verify(itemSearchRepository).count(captor.getValue());
        clearInvocations(itemSearchRepository);
        return captor.getValue();
    }

    private static ItemSearchRequest request(
        String q, String unit, String level, String tag, String sort, String dir, String page) {
        return new ItemSearchRequest(q, unit, level, tag, sort, dir, page);
    }
}
