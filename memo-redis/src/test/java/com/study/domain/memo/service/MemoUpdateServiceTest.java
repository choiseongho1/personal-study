package com.study.domain.memo.service;

import com.study.domain.memo.domain.entity.Memo;
import com.study.domain.memo.domain.repository.MemoRepository;
import com.study.domain.memo.dto.MemoUpdateDto;
import com.study.domain.memo.exception.MemoException;
import com.study.domain.memoTag.domain.entity.MemoTag;
import com.study.domain.memoTag.domain.repository.MemoTagRepository;
import com.study.domain.tag.domain.entity.Tag;
import com.study.domain.tag.domain.repository.TagRepository;
import com.study.domain.tag.dto.TagSaveDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * 메모 수정 서비스 테스트
 */
@ExtendWith(MockitoExtension.class)
class MemoUpdateServiceTest {

    @InjectMocks
    MemoService memoService;

    @Mock
    MemoRepository memoRepository;

    @Mock
    TagRepository tagRepository;

    @Mock
    MemoTagRepository memoTagRepository;

    @Test
    @DisplayName("메모 제목 수정 테스트")
    void updateMemoTitle() {
        // given
        Long memoId = 1L;
        Memo memo = Memo.builder()
                .id(memoId)
                .title("원래 제목")
                .content("원래 내용")
                .favorite(false)
                .viewCount(0L)
                .build();

        MemoUpdateDto updateDto = MemoUpdateDto.builder()
                .title("수정된 제목")
                .build();

        given(memoRepository.findById(memoId)).willReturn(Optional.of(memo));

        // when
        Long updatedMemoId = memoService.updateMemo(memoId, updateDto);

        // then
        assertEquals(memoId, updatedMemoId);
        assertEquals("수정된 제목", memo.getTitle());
        assertEquals("원래 내용", memo.getContent()); // 내용은 변경되지 않아야 함
        verify(memoTagRepository, never()).deleteByMemoId(anyLong());
    }

    @Test
    @DisplayName("메모 내용 수정 테스트")
    void updateMemoContent() {
        // given
        Long memoId = 1L;
        Memo memo = Memo.builder()
                .id(memoId)
                .title("원래 제목")
                .content("원래 내용")
                .favorite(false)
                .viewCount(0L)
                .build();

        MemoUpdateDto updateDto = MemoUpdateDto.builder()
                .content("수정된 내용")
                .build();

        given(memoRepository.findById(memoId)).willReturn(Optional.of(memo));

        // when
        Long updatedMemoId = memoService.updateMemo(memoId, updateDto);

        // then
        assertEquals(memoId, updatedMemoId);
        assertEquals("원래 제목", memo.getTitle()); // 제목은 변경되지 않아야 함
        assertEquals("수정된 내용", memo.getContent());
        verify(memoTagRepository, never()).deleteByMemoId(anyLong());
    }

    @Test
    @DisplayName("메모 즐겨찾기 상태 변경 테스트")
    void updateMemoFavorite() {
        // given
        Long memoId = 1L;
        Memo memo = Memo.builder()
                .id(memoId)
                .title("제목")
                .content("내용")
                .favorite(false)
                .viewCount(0L)
                .build();

        MemoUpdateDto updateDto = MemoUpdateDto.builder()
                .favorite(true)
                .build();

        given(memoRepository.findById(memoId)).willReturn(Optional.of(memo));

        // when
        Long updatedMemoId = memoService.updateMemo(memoId, updateDto);

        // then
        assertEquals(memoId, updatedMemoId);
        assertEquals(true, memo.getFavorite());
        verify(memoTagRepository, never()).deleteByMemoId(anyLong());
    }

    @Test
    @DisplayName("메모 태그 수정 테스트")
    void updateMemoTags() {
        // given
        Long memoId = 1L;
        Memo memo = Memo.builder()
                .id(memoId)
                .title("제목")
                .content("내용")
                .favorite(false)
                .viewCount(0L)
                .build();

        Tag tag1 = Tag.builder().name("Java").build();
        tag1.setId(1L);

        Tag tag2 = Tag.builder().name("Spring").build();
        tag2.setId(2L);

        List<TagSaveDto> tagSaveDtos = List.of(
            TagSaveDto.builder().name("Java").build(),
            TagSaveDto.builder().name("Spring").build()
        );

        MemoUpdateDto updateDto = MemoUpdateDto.builder()
                .tags(tagSaveDtos)
                .build();

        given(memoRepository.findById(memoId)).willReturn(Optional.of(memo));
        given(tagRepository.save(any(Tag.class)))
            .willReturn(tag1)
            .willReturn(tag2);

        // when
        Long updatedMemoId = memoService.updateMemo(memoId, updateDto);

        // then
        assertEquals(memoId, updatedMemoId);
        verify(memoTagRepository).deleteByMemoId(memoId);
        verify(tagRepository, times(2)).save(any(Tag.class));
        verify(memoTagRepository, times(2)).save(any(MemoTag.class));
    }

    @Test
    @DisplayName("존재하지 않는 메모 수정 시 예외 발생 테스트")
    void updateNonExistingMemo() {
        // given
        Long memoId = 999L;
        MemoUpdateDto updateDto = MemoUpdateDto.builder()
                .title("수정된 제목")
                .build();

        given(memoRepository.findById(memoId)).willReturn(Optional.empty());

        // when & then
        MemoException exception = assertThrows(MemoException.class,
                () -> memoService.updateMemo(memoId, updateDto));

        assertEquals("존재하지 않는 메모입니다. ID: " + memoId, exception.getMessage());
    }
}
