package com.example.item;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * 검색 저장소 슬라이스 — H2(MariaDB 모드)에 실제 SQL 을 날린다.
 * H2 에는 운영 스키마의 뷰가 없으므로 이 테스트 안에서만 같은 정의의 {@code v_item_public} 을 만든다.
 */
@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(ItemSearchRepository.class)
class ItemSearchRepositoryTest {

    private static final String CREATE_VIEW = """
        CREATE OR REPLACE VIEW v_item_public AS
        SELECT
          i.id,
          i.unit_id,
          u.code  AS unit_code,
          u.name  AS unit_name,
          u.grade AS unit_grade,
          i.title,
          i.stem,
          i.level,
          i.created_at,
          i.updated_at,
          (SELECT GROUP_CONCAT(t.name ORDER BY t.id SEPARATOR ',')
             FROM item_tag it
             JOIN tag t ON t.id = it.tag_id
            WHERE it.item_id = i.id) AS tag_names
        FROM item i
        JOIN unit u ON u.id = i.unit_id
        WHERE i.status = 'A'
        """;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ItemSearchRepository itemSearchRepository;

    private Unit fraction;

    @BeforeEach
    void seed() {
        // H2 의 DDL 은 트랜잭션을 커밋하므로 데이터를 넣기 전에 뷰부터 만든다.
        jdbcTemplate.execute(CREATE_VIEW);

        fraction = entityManager.persist(ItemFixtures.unit(1, "M5-1", "분수의 덧셈과 뺄셈", 5));
        Unit decimal = entityManager.persist(ItemFixtures.unit(3, "M5-3", "소수의 곱셈", 5));
        Tag calc = entityManager.persist(ItemFixtures.tag(1, "계산"));
        Tag word = entityManager.persist(ItemFixtures.tag(2, "문장제"));

        entityManager.persist(ItemFixtures.item(null, fraction, "분모가 같은 분수의 덧셈", 2, ItemStatus.ACTIVE, calc));
        // 태그를 번호 역순으로 붙인다 — 응답 순서는 붙인 순서가 아니라 태그 번호 순이어야 한다.
        entityManager.persist(ItemFixtures.item(null, fraction, "분수 문장제", 4, ItemStatus.ACTIVE, word, calc));
        entityManager.persist(ItemFixtures.item(null, fraction, "검수 중 문항", 3, ItemStatus.REVIEWING));
        entityManager.persist(ItemFixtures.item(null, fraction, "삭제된 문항", 4, ItemStatus.DELETED));
        entityManager.persist(ItemFixtures.item(null, fraction, "난이도 5 분수", 5, ItemStatus.ACTIVE, word));
        entityManager.persist(ItemFixtures.item(null, decimal, "소수 곱셈", 1, ItemStatus.ACTIVE));
        entityManager.persist(ItemFixtures.item(null, decimal, "소수 문장제", 4, ItemStatus.ACTIVE, word));
        entityManager.persist(new Item(decimal, "소수 서술형", "분수로 고쳐 설명하시오", 4,
            ItemStatus.ACTIVE, ItemFixtures.SEED_TIME));
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("조건 없음: 공개 문항 중 난이도 5 미만만, level DESC · id ASC")
    void defaultSearchReturnsPublicItemsBelowLevelFive() {
        ItemSearchCriteria criteria = criteria(null, null, null, null, ItemSearchOrder.LEVEL_DESC, 0);

        assertThat(itemSearchRepository.count(criteria)).isEqualTo(5);
        assertThat(titles(criteria))
            .containsExactly("분수 문장제", "소수 문장제", "소수 서술형", "분모가 같은 분수의 덧셈", "소수 곱셈");
    }

    @Test
    @DisplayName("난이도 지정: 5 는 조회되고 삭제 · 검수 중 문항은 빠진다. 음수는 0건")
    void explicitLevelUsesEquality() {
        assertThat(titles(criteria(null, null, 5L, null, ItemSearchOrder.LEVEL_DESC, 0)))
            .containsExactly("난이도 5 분수");
        assertThat(titles(criteria(null, "M5-1", 4L, null, ItemSearchOrder.LEVEL_DESC, 0)))
            .containsExactly("분수 문장제");

        ItemSearchCriteria negative = criteria(null, null, -1L, null, ItemSearchOrder.LEVEL_DESC, 0);
        assertThat(itemSearchRepository.count(negative)).isZero();
        assertThat(itemSearchRepository.findPage(negative)).isEmpty();
    }

    @Test
    @DisplayName("키워드: 제목 또는 지문 포함, % 와 _ 는 와일드카드. 없는 단원은 0건")
    void keywordMatchesTitleOrStem() {
        assertThat(titles(criteria("분수", null, null, null, ItemSearchOrder.LEVEL_DESC, 0)))
            .containsExactly("분수 문장제", "소수 서술형", "분모가 같은 분수의 덧셈");
        assertThat(itemSearchRepository.count(criteria("%_", null, null, null, ItemSearchOrder.LEVEL_DESC, 0)))
            .isEqualTo(5);
        assertThat(itemSearchRepository.count(criteria(null, "Z99-99", null, null, ItemSearchOrder.LEVEL_DESC, 0)))
            .isZero();
    }

    @Test
    @DisplayName("태그: 이름 정확 비교, tag_names 는 태그 번호 순 CSV, 태그 없는 문항은 null")
    void tagFilterIsExactAndTagNamesFollowTagId() {
        List<ItemSearchRow> tagged = itemSearchRepository.findPage(
            criteria(null, null, null, "문장제", ItemSearchOrder.LEVEL_DESC, 0));

        assertThat(tagged).extracting(ItemSearchRow::title).containsExactly("분수 문장제", "소수 문장제");
        assertThat(tagged.get(0).tagNames()).isEqualTo("계산,문장제");
        assertThat(itemSearchRepository.count(criteria(null, null, null, "%", ItemSearchOrder.LEVEL_DESC, 0)))
            .isZero();

        List<ItemSearchRow> untagged = itemSearchRepository.findPage(
            criteria(null, null, 1L, null, ItemSearchOrder.LEVEL_DESC, 0));
        assertThat(untagged).singleElement().extracting(ItemSearchRow::tagNames).isNull();
    }

    @Test
    @DisplayName("단원 정렬: 방향이 바뀌어도 보조 키는 level DESC · id ASC")
    void unitOrderKeepsSecondaryKeys() {
        assertThat(titles(criteria(null, null, null, null, ItemSearchOrder.UNIT_ASC, 0)))
            .containsExactly("분수 문장제", "분모가 같은 분수의 덧셈", "소수 문장제", "소수 서술형", "소수 곱셈");
        assertThat(titles(criteria(null, null, null, null, ItemSearchOrder.UNIT_DESC, 0)))
            .containsExactly("소수 문장제", "소수 서술형", "소수 곱셈", "분수 문장제", "분모가 같은 분수의 덧셈");
    }

    @Test
    @DisplayName("id 정렬은 번호 자체를 뒤집고, level 오름차순의 보조 키는 id ASC")
    void idAndLevelOrders() {
        assertThat(titles(criteria(null, null, null, null, ItemSearchOrder.ID_DESC, 0)))
            .containsExactly("소수 서술형", "소수 문장제", "소수 곱셈", "분수 문장제", "분모가 같은 분수의 덧셈");
        assertThat(titles(criteria(null, null, null, null, ItemSearchOrder.LEVEL_ASC, 0)))
            .containsExactly("소수 곱셈", "분모가 같은 분수의 덧셈", "분수 문장제", "소수 문장제", "소수 서술형");
    }

    @Test
    @DisplayName("전체 건수는 페이지와 무관하다: 마지막 페이지는 남은 행만, 범위 밖은 빈 목록")
    void countIsIndependentOfPage() {
        for (int i = 1; i <= 20; i++) {
            entityManager.persist(ItemFixtures.item(null, fraction, "추가 문항 " + i, 3, ItemStatus.ACTIVE));
        }
        entityManager.flush();

        ItemSearchCriteria secondPage = criteria(null, null, null, null, ItemSearchOrder.LEVEL_DESC, 20);
        ItemSearchCriteria thirdPage = criteria(null, null, null, null, ItemSearchOrder.LEVEL_DESC, 40);

        assertThat(itemSearchRepository.findPage(criteria(null, null, null, null, ItemSearchOrder.LEVEL_DESC, 0)))
            .hasSize(20);
        assertThat(itemSearchRepository.count(secondPage)).isEqualTo(25);
        assertThat(titles(secondPage))
            .containsExactly("추가 문항 18", "추가 문항 19", "추가 문항 20", "분모가 같은 분수의 덧셈", "소수 곱셈");
        assertThat(itemSearchRepository.count(thirdPage)).isEqualTo(25);
        assertThat(itemSearchRepository.findPage(thirdPage)).isEmpty();
    }

    private List<String> titles(ItemSearchCriteria criteria) {
        return itemSearchRepository.findPage(criteria).stream().map(ItemSearchRow::title).toList();
    }

    private static ItemSearchCriteria criteria(
        String keyword, String unitCode, Long level, String tagName, ItemSearchOrder order, int offset) {
        return new ItemSearchCriteria(keyword, unitCode, level, tagName, order, 20, offset);
    }
}
