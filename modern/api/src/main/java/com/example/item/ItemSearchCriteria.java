package com.example.item;

/**
 * 변환이 끝난 검색 조건. {@code keyword} · {@code unitCode} · {@code tagName} 은 {@code null} 이면 조건에서 빠진다.
 * {@code level} 이 {@code null} 이면 난이도 미지정({@code level < 5})이다.
 */
public record ItemSearchCriteria(
    String keyword,
    String unitCode,
    Long level,
    String tagName,
    ItemSearchOrder order,
    int limit,
    int offset) {
}
