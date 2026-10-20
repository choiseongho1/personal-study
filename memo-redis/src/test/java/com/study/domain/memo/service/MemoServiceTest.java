package com.study.domain.memo.service;

import com.study.domain.memo.domain.entity.Memo;
import com.study.domain.memo.domain.repository.MemoRepository;
import com.study.domain.memo.dto.MemoListDto;
import com.study.domain.memo.dto.MemoSearchDto;
import com.study.domain.memo.dto.MemoSaveDto;
import com.study.domain.memoTag.domain.entity.MemoTag;
import com.study.domain.memoTag.domain.repository.MemoTagRepository;
import com.study.domain.tag.domain.entity.Tag;
import com.study.domain.tag.domain.repository.TagRepository;
import com.study.domain.tag.dto.TagSaveDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * explain the Class File
 *
 * @author : choiseongho
 * @fileName : MemoServiceTest.java
 * @since : 2025-09-17
 */

@ExtendWith(MockitoExtension.class)
class MemoServiceTest {

    @InjectMocks
    MemoService memoService;

    @Mock
    MemoRepository memoRepository;

    @Mock
    TagRepository tagRepository;

    @Mock
    MemoTagRepository memoTagRepository;

    @Test
    @DisplayName("태그 없는 메모 저장 서비스 테스트")
    void create_memo_without_tags_test(){
        // given
        Memo memo = new Memo("title", "content", false, 0L);
        memo.setId(1L); // ID 설정

        MemoSaveDto saveMemoDto = MemoSaveDto.builder()
                .title("title")
                .content("content")
                .build();

        given(memoRepository.save(any(Memo.class))).willReturn(memo);

        // when
        Long savedMemoId = memoService.saveMemo(saveMemoDto);

        // then
        assertEquals(1L, savedMemoId);
        verify(memoRepository).save(any(Memo.class));
        verify(tagRepository, org.mockito.Mockito.never()).save(any(Tag.class));
        verify(memoTagRepository, org.mockito.Mockito.never()).save(any(MemoTag.class));
    }

    @Test
    @DisplayName("태그가 있는 메모 저장 서비스 테스트")
    void create_memo_with_tags_test(){
        // given
        Memo memo = new Memo("title", "content", false, 0L);
        memo.setId(1L); // ID 설정

        Tag tag1 = Tag.builder().name("Java").build();
        tag1.setId(1L);

        Tag tag2 = Tag.builder().name("Spring").build();
        tag2.setId(2L);

        List<TagSaveDto> tagSaveDtos = List.of(
            TagSaveDto.builder().name("Java").build(),
            TagSaveDto.builder().name("Spring").build()
        );

        MemoSaveDto saveMemoDto = MemoSaveDto.builder()
                .title("title")
                .content("content")
                .tags(tagSaveDtos)
                .build();

        given(memoRepository.save(any(Memo.class))).willReturn(memo);
        given(tagRepository.save(any(Tag.class)))
            .willReturn(tag1)
            .willReturn(tag2);

        // when
        Long savedMemoId = memoService.saveMemo(saveMemoDto);

        // then
        assertEquals(1L, savedMemoId);
        verify(memoRepository).save(any(Memo.class));
        verify(tagRepository, org.mockito.Mockito.times(2)).save(any(Tag.class));
        verify(memoTagRepository, org.mockito.Mockito.times(2)).save(any(MemoTag.class));
    }

    @Test
    @DisplayName("메모 목록 조회 서비스 테스트")
    void search_memo_list_with_paging(){
        // given
        MemoSearchDto searchDto = MemoSearchDto.builder()
                .searchType("title")
                .keyword("테스트")
                .build();

        Pageable pageable = PageRequest.of(0, 10);

        // 테스트용 메모 목록 데이터 생성
        LocalDateTime now = LocalDateTime.now();
        MemoListDto memo1 = MemoListDto.builder()
                .id(1L)
                .title("테스트 제목1")
                .content("테스트 내용1")
                .favorite(true)
                .viewCount(10L)
                .createdAt(now)
                .build();

        MemoListDto memo2 = MemoListDto.builder()
                .id(2L)
                .title("테스트 제목2")
                .content("다른 내용2")
                .favorite(false)
                .viewCount(5L)
                .createdAt(now)
                .build();

        List<MemoListDto> memoList = List.of(memo1, memo2);
        Page<MemoListDto> expectedPage = new PageImpl<>(memoList, pageable, 2);

        // when
        given(memoRepository.findMemoListWithPaging(any(MemoSearchDto.class), any(Pageable.class)))
                .willReturn(expectedPage);

        // then
        Page<MemoListDto> result = memoService.findMemoPaging(searchDto, pageable);

        // 결과 검증
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        assertEquals("테스트 제목1", result.getContent().get(0).getTitle());
        assertEquals("테스트 제목2", result.getContent().get(1).getTitle());

        // 메소드 호출 확인
        verify(memoRepository).findMemoListWithPaging(any(MemoSearchDto.class), any(Pageable.class));
    }

}
