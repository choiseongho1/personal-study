package com.study.domain.memo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MemoSearchDto {
    private String keyword;           // 검색 키워드
    private String searchType;        // 검색 타입 (title, content, both)
}
