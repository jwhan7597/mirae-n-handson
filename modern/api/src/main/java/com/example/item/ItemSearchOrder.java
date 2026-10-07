package com.example.item;

/**
 * 문항 검색에서 허용하는 정렬. 이 목록 밖의 정렬은 없다 — SQL 은 {@code ItemSearchRepository} 가 상수별로 정한다.
 * 정렬 기준이 없거나 모르는 값이면 {@link #LEVEL_DESC} 를 쓴다.
 */
public enum ItemSearchOrder {
    ID_ASC,
    ID_DESC,
    TITLE_ASC,
    TITLE_DESC,
    UNIT_ASC,
    UNIT_DESC,
    LEVEL_DESC,
    LEVEL_ASC,
    CREATED_DESC,
    CREATED_ASC
}
