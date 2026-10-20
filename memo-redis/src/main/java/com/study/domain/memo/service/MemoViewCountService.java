package com.study.domain.memo.service;

import com.study.domain.memo.domain.entity.Memo;
import com.study.domain.memo.domain.repository.MemoRepository;
import com.study.domain.memo.exception.MemoException;
import com.study.global.util.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 메모 조회수 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemoViewCountService {

    private final MemoRepository memoRepository;
    private final RedisUtils redisUtils;

    // 키 접두사
    private static final String VIEW_COUNT_KEY_PREFIX = "memo:views:";
    private static final String HOT_MEMO_KEY = "memo:hot";

    /**
     * 메모 조회수 증가
     *
     * @param memoId 메모 ID
     * @return 증가된 조회수
     */
    public Long incrementViewCount(Long memoId) {
        // 메모 존재 여부 확인
        if (!memoRepository.existsById(memoId)) {
            throw MemoException.memoNotFound(memoId);
        }

        String key = VIEW_COUNT_KEY_PREFIX + memoId;
        Long viewCount = redisUtils.increment(key);

        // 인기 메모 목록에 점수 추가 (조회수 1당 1점)
        redisUtils.zIncrement(HOT_MEMO_KEY, memoId.toString(), 1.0);

        log.debug("메모 조회수 증가: ID={}, 조회수={}", memoId, viewCount);
        return viewCount;
    }

    /**
     * 메모의 현재 조회수 조회 (Redis + DB)
     *
     * @param memoId 메모 ID
     * @return 현재 조회수
     */
    public Long getCurrentViewCount(Long memoId) {
        // 메모 조회
        Memo memo = memoRepository.findById(memoId)
                .orElseThrow(() -> MemoException.memoNotFound(memoId));

        // DB에 저장된 조회수
        Long dbViewCount = memo.getViewCount();

        // Redis에 저장된 증분 조회수
        String key = VIEW_COUNT_KEY_PREFIX + memoId;
        Object redisViewCount = redisUtils.get(key);

        // Redis에 증분 조회수가 있으면 더하고, 없으면 DB 조회수만 반환
        if (redisViewCount != null) {
            if (redisViewCount instanceof Integer) {
                return dbViewCount + ((Integer) redisViewCount).longValue();
            } else if (redisViewCount instanceof Long) {
                return dbViewCount + (Long) redisViewCount;
            }
        }

        return dbViewCount;
    }

    /**
     * Redis의 조회수를 DB에 동기화 (5분마다 실행)
     */
    @Scheduled(fixedRate = 300000) // 5분(300,000ms)마다 실행
    @Transactional
    public void syncViewCountsToDB() {
        log.info("메모 조회수 동기화 배치 작업 시작");

        try {
            // Redis에서 모든 조회수 키 패턴으로 조회
            Set<Object> keys = redisUtils.zReverseRange(HOT_MEMO_KEY, 0, -1);

            if (keys == null || keys.isEmpty()) {
                log.info("동기화할 조회수 데이터가 없습니다.");
                return;
            }

            // 각 메모 ID별 조회수 증분 수집
            Map<Long, Long> viewCountsMap = keys.stream()
                    .map(Object::toString)
                    .map(Long::parseLong)
                    .collect(Collectors.toMap(
                            memoId -> memoId,
                            memoId -> {
                                String key = VIEW_COUNT_KEY_PREFIX + memoId;
                                Object value = redisUtils.get(key);
                                if (value instanceof Integer) {
                                    return ((Integer) value).longValue();
                                } else if (value instanceof Long) {
                                    return (Long) value;
                                }
                                return 0L;
                            },
                            (v1, v2) -> v1 + v2
                    ));

            // 메모 ID 목록으로 메모 조회
            List<Memo> memos = memoRepository.findAllById(viewCountsMap.keySet());

            // 각 메모의 조회수 업데이트
            for (Memo memo : memos) {
                Long increment = viewCountsMap.getOrDefault(memo.getId(), 0L);
                if (increment > 0) {
                    memo.setViewCount(memo.getViewCount() + increment);

                    // Redis에서 해당 키 삭제 (또는 0으로 리셋)
                    String key = VIEW_COUNT_KEY_PREFIX + memo.getId();
                    redisUtils.set(key, 0L);

                    log.debug("메모 조회수 동기화: ID={}, 증분={}, 총 조회수={}",
                            memo.getId(), increment, memo.getViewCount());
                }
            }

            // DB에 저장
            memoRepository.saveAll(memos);

            log.info("메모 조회수 동기화 완료: {}개 메모 업데이트", memos.size());
        } catch (Exception e) {
            log.error("메모 조회수 동기화 중 오류 발생: {}", e.getMessage(), e);
        }
    }
}
