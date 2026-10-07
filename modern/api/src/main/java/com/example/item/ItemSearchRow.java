package com.example.item;

/** {@code v_item_public} 의 검색 결과 한 행. {@code tagNames} 는 태그 번호 순 CSV 이고 태그가 없으면 {@code null}. */
public record ItemSearchRow(
    Integer id,
    String title,
    String unitCode,
    Integer level,
    String tagNames) {
}
