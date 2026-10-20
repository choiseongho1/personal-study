package com.study.domain.memo.dto;

import com.study.domain.tag.dto.TagSaveDto;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Getter
@AllArgsConstructor @NoArgsConstructor
public class MemoUpdateDto {

    private String title;
    private String content;
    private Boolean favorite;
    private List<TagSaveDto> tags;

    public boolean existTags() {
        return tags != null && !tags.isEmpty();
    }
}
