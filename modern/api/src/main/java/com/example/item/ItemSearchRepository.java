package com.example.item;

import java.util.List;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 문항 검색 저장소. 공개 문항 뷰 {@code v_item_public} 를 읽어 정렬 · 문자열 비교 · 태그 집계를 DB 에 맡긴다.
 * 값은 모두 바인딩하고, ORDER BY 는 {@link ItemSearchOrder} 상수별 고정 문자열만 쓴다.
 */
@Repository
public class ItemSearchRepository {

    private static final String FROM = " FROM v_item_public";

    private static final RowMapper<ItemSearchRow> ROW_MAPPER = (rs, rowNum) -> new ItemSearchRow(
        rs.getInt("id"),
        rs.getString("title"),
        rs.getString("unit_code"),
        rs.getInt("level"),
        rs.getString("tag_names"));

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ItemSearchRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 조건에 맞는 전체 건수. 페이지와 무관하다. */
    public long count(ItemSearchCriteria criteria) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        String sql = "SELECT COUNT(*)" + FROM + where(criteria, parameters);
        Long total = jdbcTemplate.queryForObject(sql, parameters, Long.class);
        return total == null ? 0 : total;
    }

    /** 현재 페이지의 행. 전체 건수를 넘는 offset 이면 빈 목록이다. */
    public List<ItemSearchRow> findPage(ItemSearchCriteria criteria) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        String sql = "SELECT id, title, unit_code, level, tag_names" + FROM
            + where(criteria, parameters)
            + orderBy(criteria.order())
            + " LIMIT :limit OFFSET :offset";
        parameters.addValue("limit", criteria.limit());
        parameters.addValue("offset", criteria.offset());
        return jdbcTemplate.query(sql, parameters, ROW_MAPPER);
    }

    private static String where(ItemSearchCriteria criteria, MapSqlParameterSource parameters) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        if (criteria.keyword() != null) {
            // % 와 _ 는 이스케이프하지 않는다 — 레거시와 같이 LIKE 와일드카드로 동작한다.
            where.append(" AND (title LIKE :keyword OR stem LIKE :keyword)");
            parameters.addValue("keyword", "%" + criteria.keyword() + "%");
        }
        if (criteria.unitCode() != null) {
            where.append(" AND unit_code = :unitCode");
            parameters.addValue("unitCode", criteria.unitCode());
        }
        if (criteria.level() == null) {
            // 난이도 미지정은 5 미만만 조회한다(난이도 5 제외).
            where.append(" AND level < 5");
        } else {
            where.append(" AND level = :level");
            parameters.addValue("level", criteria.level());
        }
        if (criteria.tagName() != null) {
            where.append(" AND EXISTS (SELECT 1 FROM item_tag it JOIN tag t ON t.id = it.tag_id"
                + " WHERE it.item_id = v_item_public.id AND t.name = :tagName)");
            parameters.addValue("tagName", criteria.tagName());
        }
        return where.toString();
    }

    /** 단원 정렬의 보조 키 {@code level DESC, id ASC} 는 방향과 무관하게 고정이다. */
    private static String orderBy(ItemSearchOrder order) {
        return switch (order) {
            case ID_ASC -> " ORDER BY id ASC";
            case ID_DESC -> " ORDER BY id DESC";
            case TITLE_ASC -> " ORDER BY title ASC, id ASC";
            case TITLE_DESC -> " ORDER BY title DESC, id ASC";
            case UNIT_ASC -> " ORDER BY unit_code ASC, level DESC, id ASC";
            case UNIT_DESC -> " ORDER BY unit_code DESC, level DESC, id ASC";
            case LEVEL_DESC -> " ORDER BY level DESC, id ASC";
            case LEVEL_ASC -> " ORDER BY level ASC, id ASC";
            case CREATED_DESC -> " ORDER BY created_at DESC, id ASC";
            case CREATED_ASC -> " ORDER BY created_at ASC, id ASC";
        };
    }
}
