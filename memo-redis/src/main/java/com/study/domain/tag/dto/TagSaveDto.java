package com.study.domain.tag.dto;

import com.study.domain.tag.domain.entity.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TagSaveDto {

    private String name;

    public Tag toEntity(){
        return Tag.builder()
                .name(name)
                .build();
    }

}
