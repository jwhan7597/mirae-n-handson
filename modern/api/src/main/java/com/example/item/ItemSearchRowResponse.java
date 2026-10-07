package com.example.item;

import java.util.List;

/** 문항 검색 결과의 한 행. {@code unit} 은 단원 코드, {@code tags} 는 태그 이름 목록(태그 번호 순). */
public record ItemSearchRowResponse(
    Integer id,
    String title,
    String unit,
    Integer level,
    List<String> tags) {
}
