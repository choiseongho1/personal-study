package com.study.domain.memo.dto;

import com.querydsl.core.annotations.QueryProjection;
import com.study.domain.tag.dto.TagDetailDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor @AllArgsConstructor
public class MemoDetailDto {
    private Long id;
    private String title;
    private String content;
    private Boolean favorite;

    @Setter
    private Long viewCount;

    private LocalDateTime createdAt;

    @Setter
    private List<TagDetailDto> tags;


    @QueryProjection
    public MemoDetailDto(Long id, String title, String content, Boolean favorite, Long viewCount, LocalDateTime createdAt){
        this.id = id;
        this.title = title;
        this.content = content;
        this.favorite = favorite;
        this.viewCount = viewCount;
        this.createdAt = createdAt;

    }


}
