package com.study.domain.tag.dto;

import com.querydsl.core.annotations.QueryProjection;
import com.study.domain.tag.domain.entity.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
public class TagDetailDto {

    private Long id;
    private String name;

    @QueryProjection
    public TagDetailDto(Long id, String name){
        this.id = id;
        this.name = name;
    }

}
