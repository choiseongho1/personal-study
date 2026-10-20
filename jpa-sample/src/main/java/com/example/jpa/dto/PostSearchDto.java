package com.example.jpa.dto;

import com.querydsl.core.annotations.QueryProjection;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@NoArgsConstructor
public class PostSearchDto {
    private Long id;
    private String title;
    private String author;
    private String category;
    private LocalDateTime createdAt;

    @QueryProjection
    public PostSearchDto(Long id, String title, String author, String category, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.category = category;
        this.createdAt = createdAt;
    }

}
