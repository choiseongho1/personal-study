package com.example.jpa.dto;

import com.querydsl.core.annotations.QueryProjection;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@NoArgsConstructor @AllArgsConstructor
public class PostWithCommentsDto {

    private Long id;
    private String title;
    private String author;
    private String category;
    private LocalDateTime createdAt;
    private List<CommentDto> comments;

    @QueryProjection
    public PostWithCommentsDto(Long id, String title, String author, String category, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.category = category;
        this.createdAt = createdAt;
    }
}
