package com.example.item;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** 컨트롤러 슬라이스 — 서비스는 목. 파라미터 수집 규칙과 응답 JSON 모양을 본다. */
@WebMvcTest(ItemSearchController.class)
@ActiveProfiles("test")
class ItemSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ItemSearchService itemSearchService;

    @Test
    @DisplayName("GET /api/items/search → 200, {status, rows, count, message} 만 내보낸다")
    void searchReturnsExactJsonShape() throws Exception {
        when(itemSearchService.search(any())).thenReturn(ItemSearchResponse.of(
            List.of(new ItemSearchRowResponse(3, "분모가 다른 분수의 덧셈", "M5-1", 4, List.of("계산", "오답률높음"))),
            21));

        mockMvc.perform(get("/api/items/search").queryParam("unit", "M5-1"))
            .andExpect(status().isOk())
            .andExpect(content().json("""
                {
                  "status": 200,
                  "rows": [
                    {"id": 3, "title": "분모가 다른 분수의 덧셈", "unit": "M5-1", "level": 4, "tags": ["계산", "오답률높음"]}
                  ],
                  "count": 21,
                  "message": null
                }
                """, true));
    }

    @Test
    @DisplayName("전체 0건 → 200, 빈 rows 와 결과 없음 문구")
    void searchWithoutResultsReturnsMessage() throws Exception {
        when(itemSearchService.search(any())).thenReturn(ItemSearchResponse.of(List.of(), 0));

        mockMvc.perform(get("/api/items/search").queryParam("unit", "Z99-99").queryParam("level", "-1"))
            .andExpect(status().isOk())
            .andExpect(content().json("""
                {"status": 200, "rows": [], "count": 0, "message": "검색 결과가 없습니다"}
                """, true));
    }

    @Test
    @DisplayName("없는 파라미터는 null, 값은 변환 없이 문자열 그대로 넘긴다")
    void parametersArePassedAsRawStrings() throws Exception {
        mockMvc.perform(get("/api/items/search").queryParam("level", "3abc").queryParam("page", " 2 ").queryParam("sort", " UNIT "))
            .andExpect(status().isOk());

        verify(itemSearchService).search(new ItemSearchRequest(null, null, "3abc", null, " UNIT ", null, " 2 "));
    }

    @Test
    @DisplayName("q[] 배열 키는 첫 값을 쓴다")
    void arrayKeyUsesFirstValue() throws Exception {
        mockMvc.perform(get("/api/items/search").queryParam("q[]", "분수", "소수"))
            .andExpect(status().isOk());

        verify(itemSearchService).search(new ItemSearchRequest("분수", null, null, null, null, null, null));
    }

    @Test
    @DisplayName("같은 단일 키가 반복되면 마지막 값을 쓴다")
    void repeatedSingleKeyUsesLastValue() throws Exception {
        mockMvc.perform(get("/api/items/search").queryParam("q", "분수", "소수"))
            .andExpect(status().isOk());

        verify(itemSearchService).search(new ItemSearchRequest("소수", null, null, null, null, null, null));
    }

    @Test
    @DisplayName("스칼라와 배열을 섞은 실제 쿼리 문자열의 마지막 출현을 보존한다")
    void interleavedQueryKeepsWireOrder() throws Exception {
        mockMvc.perform(get(URI.create("/api/items/search?q=first&q%5B%5D=second&q=last")))
            .andExpect(status().isOk());

        verify(itemSearchService).search(new ItemSearchRequest("last", null, null, null, null, null, null));
    }
}
