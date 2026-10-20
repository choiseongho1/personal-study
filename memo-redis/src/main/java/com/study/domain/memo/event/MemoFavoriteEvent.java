package com.study.domain.memo.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 메모 즐겨찾기 상태 변경 이벤트
 */
@Getter
public class MemoFavoriteEvent extends ApplicationEvent {
    private final Long memoId;
    private final boolean favorite;

    public MemoFavoriteEvent(Object source, Long memoId, boolean favorite) {
        super(source);
        this.memoId = memoId;
        this.favorite = favorite;
    }
}
