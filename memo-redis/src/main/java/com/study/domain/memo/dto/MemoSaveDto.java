package com.study.domain.memo.dto;

import com.study.domain.memo.domain.entity.Memo;
import com.study.domain.tag.domain.entity.Tag;
import com.study.domain.tag.dto.TagSaveDto;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Getter
@AllArgsConstructor @NoArgsConstructor
public class MemoSaveDto {

    @NotBlank(message = "제목은 필수 입력 사항입니다.")
    private String title;

    @NotBlank(message = "내용은 필수 입력 사항입니다.")
    private String content;

    private List<TagSaveDto> tags;

    public Memo toEntity(){
        return Memo.builder()
                .title(title)
                .content(content)
                .build();
    }

    public boolean existTags(){
        return tags != null && !tags.isEmpty();
    }
}
