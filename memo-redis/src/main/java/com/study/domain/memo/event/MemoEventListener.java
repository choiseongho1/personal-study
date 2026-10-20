package com.study.domain.memo.event;

import com.study.domain.memo.service.MemoHotListService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 메모 이벤트 리스너
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemoEventListener {

    private final MemoHotListService memoHotListService;

    /**
     * 메모 즐겨찾기 상태 변경 이벤트 처리
     *
     * @param event 메모 즐겨찾기 이벤트
     */
    @Async
    @EventListener
    public void handleMemoFavoriteEvent(MemoFavoriteEvent event) {
        log.debug("메모 즐겨찾기 이벤트 수신: ID={}, 상태={}", event.getMemoId(), event.isFavorite());
        memoHotListService.updateFavoriteScore(event.getMemoId(), event.isFavorite());
    }
}
