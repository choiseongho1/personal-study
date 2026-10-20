package com.study.domain.memo.dto;

import com.querydsl.core.annotations.QueryProjection;
import com.study.domain.memo.domain.entity.Memo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
public class MemoListDto {
    private Long id;
    private String title;
    private String content;
    private Boolean favorite;
    private Long viewCount;
    private LocalDateTime createdAt;

    @QueryProjection
    public MemoListDto(Long id, String title, String content, Boolean favorite, Long viewCount, LocalDateTime createdAt){
        this.id = id;
        this.title = title;
        this.content = content;
        this.favorite = favorite;
        this.viewCount = viewCount;
        this.createdAt = createdAt;

    }


}
