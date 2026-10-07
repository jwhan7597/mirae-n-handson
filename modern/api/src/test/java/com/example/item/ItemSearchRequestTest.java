package com.example.item;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** PHP parse_str와 search.php의 reset/(string) 실행 결과를 기준으로 한 회귀 테스트. */
class ItemSearchRequestTest {

    @ParameterizedTest(name = "{0} → q={1}")
    @CsvSource(delimiter = '|', textBlock = """
        q[]=분수&q[]=소수 | 분수
        q[a]=분수&q[a]=소수 | 소수
        q[a]=분수&q[b]=소수 | 분수
        q[7]=분수&q[2]=소수 | 분수
        q=분수&q[]=소수&q=분수 | 분수
        q=분수&q[]=소수 | 소수
        q[a][b]=분수 | Array
        q[0]=분수&q[]=소수&q[0]=계산 | 계산
        q[]=분수&q[0]=소수 | 소수
        """)
    @DisplayName("배열 색인·반복·스칼라 혼용은 PHP의 출현 순서와 첫 값 규칙을 보존한다")
    void parsesQueryLikePhp(String query, String expected) {
        assertThat(ItemSearchRequest.fromQueryString(query).q()).isEqualTo(expected);
    }

    @Test
    @DisplayName("UTF-8 percent/plus 디코딩, 원문 공백, 파라미터 누락을 보존한다")
    void decodesWithoutTrimmingOrInventingMissingValues() {
        ItemSearchRequest request = ItemSearchRequest.fromQueryString(
            "q%5Ba%5D=%EB%B6%84%EC%88%98&unit=M5-1&level=05&page=+2+");
        assertThat(request).isEqualTo(new ItemSearchRequest("분수", "M5-1", "05", null, null, null, " 2 "));
        assertThat(ItemSearchRequest.fromQueryString(null))
            .isEqualTo(new ItemSearchRequest(null, null, null, null, null, null, null));
    }
}
