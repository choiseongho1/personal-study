package com.study.domain.memo.service;

import com.study.domain.memo.domain.repository.MemoRepository;
import com.study.domain.memo.dto.MemoDetailDto;
import com.study.domain.memo.exception.MemoException;
import com.study.global.util.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 인기 메모 목록 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemoHotListService {

    private final RedisUtils redisUtils;
    private final MemoRepository memoRepository;

    // 인기 메모 키
    private static final String HOT_MEMO_KEY = "memo:hot";

    @Transactional(readOnly = true)
    public List<MemoDetailDto> getHotMemos(int limit) {
        // Redis에서 인기 메모 ID 목록 조회 (점수 포함)
        Set<ZSetOperations.TypedTuple<Object>> hotMemoSet =
                redisUtils.zReverseRangeWithScores(HOT_MEMO_KEY, 0, limit - 1);

        if (hotMemoSet == null || hotMemoSet.isEmpty()) {
            log.debug("인기 메모가 없습니다.");
            return new ArrayList<>();
        }

        // 인기 메모 ID 목록 추출
        List<Long> memoIds = hotMemoSet.stream()
                .map(tuple -> tuple.getValue().toString())
                .map(Long::parseLong)
                .collect(Collectors.toList());

        log.debug("인기 메모 ID 목록: {}", memoIds);

        // 각 메모 상세 정보 조회
        List<MemoDetailDto> hotMemos = new ArrayList<>();
        for (Long memoId : memoIds) {
            try {
                // 메모 상세 정보 조회
                MemoDetailDto memoDetail = findMemoDetailById(memoId);
                hotMemos.add(memoDetail);
            } catch (Exception e) {
                log.warn("인기 메모 조회 실패: ID={}, 오류={}", memoId, e.getMessage());
            }
        }

        return hotMemos;
    }

    @Transactional(readOnly = true)
    public MemoDetailDto findMemoDetailById(Long id) {
        // 메모 상세 정보 조회
        return memoRepository.findMemoDetail(id)
                .orElseThrow(() -> MemoException.memoNotFound(id));
    }

    public void updateFavoriteScore(Long memoId, boolean favorite) {
        // 즐겨찾기 추가 시 10점, 제거 시 -10점
        double scoreChange = favorite ? 10.0 : -10.0;

        Double newScore = redisUtils.zIncrement(HOT_MEMO_KEY, memoId.toString(), scoreChange);
        log.debug("메모 즐겨찾기 점수 업데이트: ID={}, 변화량={}, 새 점수={}", memoId, scoreChange, newScore);
    }
}
