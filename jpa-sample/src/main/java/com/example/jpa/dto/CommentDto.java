package com.example.jpa.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@NoArgsConstructor
public class CommentDto {
    private Long id;
    private String writer;
    private String content;
    private Long postId;

    @QueryProjection
    public CommentDto(Long id, String writer, String content, Long postId) {
        this.id = id;
        this.writer = writer;
        this.content = content;
        this.postId = postId;
    }

}
