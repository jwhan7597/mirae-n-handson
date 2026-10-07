package com.example.item;

import java.util.List;

/**
 * 문항 검색 응답. {@code count} 는 현재 페이지 행 수가 아니라 조건에 맞는 전체 건수이다.
 * {@code message} 는 전체 건수가 0일 때만 채운다 — 페이지가 범위를 넘어 행만 없는 경우는 {@code null}.
 */
public record ItemSearchResponse(
    int status,
    List<ItemSearchRowResponse> rows,
    long count,
    String message) {

    static final String NO_RESULT_MESSAGE = "검색 결과가 없습니다";

    static ItemSearchResponse of(List<ItemSearchRowResponse> rows, long total) {
        return new ItemSearchResponse(200, rows, total, total == 0 ? NO_RESULT_MESSAGE : null);
    }
}
