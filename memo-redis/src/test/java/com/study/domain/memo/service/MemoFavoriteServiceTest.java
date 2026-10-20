package com.study.domain.memo.service;

import com.study.domain.memo.domain.entity.Memo;
import com.study.domain.memo.domain.repository.MemoRepository;
import com.study.domain.memo.exception.MemoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import com.study.domain.memo.event.MemoFavoriteEvent;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.argThat;

/**
 * 메모 즐겨찾기 서비스 테스트
 */
@ExtendWith(MockitoExtension.class)
class MemoFavoriteServiceTest {

    @InjectMocks
    MemoService memoService;

    @Mock
    MemoRepository memoRepository;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @Test
    @DisplayName("메모 즐겨찾기 상태 토글 테스트 - false에서 true로 변경")
    void toggleFavoriteFromFalseToTrue() {
        // given
        Long memoId = 1L;
        Memo memo = Memo.builder()
                .id(memoId)
                .title("제목")
                .content("내용")
                .favorite(false)
                .viewCount(0L)
                .build();

        given(memoRepository.findById(memoId)).willReturn(Optional.of(memo));

        // when
        boolean result = memoService.toggleFavorite(memoId);

        // then
        assertTrue(result);
        assertTrue(memo.getFavorite());
        verify(eventPublisher).publishEvent(argThat((MemoFavoriteEvent event) ->
                event.getMemoId().equals(memoId) && event.isFavorite()));
        verify(memoRepository).findById(memoId);
    }

    @Test
    @DisplayName("메모 즐겨찾기 상태 토글 테스트 - true에서 false로 변경")
    void toggleFavoriteFromTrueToFalse() {
        // given
        Long memoId = 1L;
        Memo memo = Memo.builder()
                .id(memoId)
                .title("제목")
                .content("내용")
                .favorite(true)
                .viewCount(0L)
                .build();

        given(memoRepository.findById(memoId)).willReturn(Optional.of(memo));

        // when
        boolean result = memoService.toggleFavorite(memoId);

        // then
        assertFalse(result);
        assertFalse(memo.getFavorite());
        verify(eventPublisher).publishEvent(argThat((MemoFavoriteEvent event) ->
                event.getMemoId().equals(memoId) && !event.isFavorite()));
        verify(memoRepository).findById(memoId);
    }

    @Test
    @DisplayName("존재하지 않는 메모 즐겨찾기 토글 시 예외 발생 테스트")
    void toggleFavoriteNonExistingMemo() {
        // given
        Long memoId = 999L;
        given(memoRepository.findById(memoId)).willReturn(Optional.empty());

        // when & then
        MemoException exception = assertThrows(MemoException.class,
                () -> memoService.toggleFavorite(memoId));

        assertEquals("존재하지 않는 메모입니다. ID: " + memoId, exception.getMessage());
    }
}
