package com.study.domain.memo.service;

import com.study.domain.memo.domain.entity.Memo;
import com.study.domain.memo.domain.repository.MemoRepository;
import com.study.domain.memo.exception.MemoException;
import com.study.domain.memoTag.domain.repository.MemoTagRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * 메모 삭제 서비스 테스트
 */
@ExtendWith(MockitoExtension.class)
class MemoDeleteServiceTest {

    @InjectMocks
    MemoService memoService;

    @Mock
    MemoRepository memoRepository;

    @Mock
    MemoTagRepository memoTagRepository;

    @Test
    @DisplayName("메모 삭제 성공 테스트")
    void deleteMemoSuccess() {
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
        memoService.deleteMemo(memoId);

        // then
        verify(memoTagRepository).deleteByMemoId(memoId);
        verify(memoRepository).delete(memo);
    }

    @Test
    @DisplayName("존재하지 않는 메모 삭제 시 예외 발생 테스트")
    void deleteNonExistingMemo() {
        // given
        Long memoId = 999L;
        given(memoRepository.findById(memoId)).willReturn(Optional.empty());

        // when & then
        MemoException exception = assertThrows(MemoException.class,
                () -> memoService.deleteMemo(memoId));

        verify(memoTagRepository, never()).deleteByMemoId(anyLong());
        verify(memoRepository, never()).delete(any(Memo.class));
    }
}
