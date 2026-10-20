package com.randombox.domain.queue;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RQueue;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueueService {

    private final RedissonClient redissonClient;
    private static final String QUEUE_KEY_PREFIX = "randombox:queue:";
    private static final String WAITING_COUNT_KEY_PREFIX = "randombox:waiting-count:";
    private static final int QUEUE_TIMEOUT_SECONDS = 300; // 5분

    /**
     * 사용자를 대기열에 추가
     * @param randomBoxId 랜덤박스 ID
     * @param userId 사용자 ID
     * @return 대기 번호
     */
    public int addToQueue(Long randomBoxId, Long userId) {
        String queueKey = QUEUE_KEY_PREFIX + randomBoxId;
        RQueue<Long> queue = redissonClient.getQueue(queueKey);

        // 이미 대기열에 있는지 확인
        if (queue.contains(userId)) {
            return getPosition(randomBoxId, userId);
        }

        // 대기열에 추가
        queue.add(userId);

        // 대기 인원 수 증가
        String waitingCountKey = WAITING_COUNT_KEY_PREFIX + randomBoxId;
        redissonClient.getAtomicLong(waitingCountKey).incrementAndGet();

        log.info("사용자 {}가 랜덤박스 {} 대기열에 추가되었습니다. 대기 번호: {}", userId, randomBoxId, queue.size());

        return queue.size();
    }

    /**
     * 대기열에서 사용자 위치 조회
     * @param randomBoxId 랜덤박스 ID
     * @param userId 사용자 ID
     * @return 대기 번호 (0부터 시작, -1은 대기열에 없음)
     */
    public int getPosition(Long randomBoxId, Long userId) {
        String queueKey = QUEUE_KEY_PREFIX + randomBoxId;
        RQueue<Long> queue = redissonClient.getQueue(queueKey);

        int position = -1;
        int index = 0;

        for (Long id : queue) {
            if (id.equals(userId)) {
                position = index;
                break;
            }
            index++;
        }

        return position;
    }

    /**
     * 대기열에서 다음 사용자 가져오기
     * @param randomBoxId 랜덤박스 ID
     * @return 다음 사용자 ID, 없으면 null
     */
    public Long getNextUser(Long randomBoxId) {
        String queueKey = QUEUE_KEY_PREFIX + randomBoxId;
        RQueue<Long> queue = redissonClient.getQueue(queueKey);

        if (queue.isEmpty()) {
            return null;
        }

        Long userId = queue.poll();

        // 대기 인원 수 감소
        String waitingCountKey = WAITING_COUNT_KEY_PREFIX + randomBoxId;
        redissonClient.getAtomicLong(waitingCountKey).decrementAndGet();

        log.info("사용자 {}가 랜덤박스 {} 대기열에서 제거되었습니다.", userId, randomBoxId);

        return userId;
    }

    /**
     * 대기열에서 사용자 제거
     * @param randomBoxId 랜덤박스 ID
     * @param userId 사용자 ID
     * @return 제거 성공 여부
     */
    public boolean removeFromQueue(Long randomBoxId, Long userId) {
        String queueKey = QUEUE_KEY_PREFIX + randomBoxId;
        RQueue<Long> queue = redissonClient.getQueue(queueKey);

        boolean removed = queue.remove(userId);

        if (removed) {
            // 대기 인원 수 감소
            String waitingCountKey = WAITING_COUNT_KEY_PREFIX + randomBoxId;
            redissonClient.getAtomicLong(waitingCountKey).decrementAndGet();

            log.info("사용자 {}가 랜덤박스 {} 대기열에서 제거되었습니다.", userId, randomBoxId);
        }

        return removed;
    }

    /**
     * 대기 인원 수 조회
     * @param randomBoxId 랜덤박스 ID
     * @return 대기 인원 수
     */
    public long getWaitingCount(Long randomBoxId) {
        String waitingCountKey = WAITING_COUNT_KEY_PREFIX + randomBoxId;
        return redissonClient.getAtomicLong(waitingCountKey).get();
    }

    /**
     * 예상 대기 시간 계산 (초 단위)
     * @param randomBoxId 랜덤박스 ID
     * @param userId 사용자 ID
     * @return 예상 대기 시간 (초)
     */
    public int getEstimatedWaitTime(Long randomBoxId, Long userId) {
        int position = getPosition(randomBoxId, userId);

        if (position == -1) {
            return -1;
        }

        // 한 사용자당 처리 시간을 30초로 가정
        int processingTimePerUser = 30;
        return position * processingTimePerUser;
    }
}
