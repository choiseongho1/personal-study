package com.randombox.api.v1.queue;

import com.randombox.api.v1.queue.dto.QueueResponse;
import com.randombox.domain.queue.QueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/queue")
public class QueueController {

    private final QueueService queueService;

    /**
     * 사용자를 대기열에 추가
     * @param randomBoxId 랜덤박스 ID
     * @param userId 사용자 ID
     * @return 대기열 정보
     */
    @PostMapping("/{randomBoxId}/users/{userId}")
    public ResponseEntity<QueueResponse> addToQueue(
            @PathVariable Long randomBoxId,
            @PathVariable Long userId) {

        int position = queueService.addToQueue(randomBoxId, userId);
        int estimatedWaitTime = queueService.getEstimatedWaitTime(randomBoxId, userId);
        long waitingCount = queueService.getWaitingCount(randomBoxId);

        QueueResponse response = QueueResponse.of(
                userId,
                randomBoxId,
                position,
                estimatedWaitTime,
                waitingCount
        );

        return ResponseEntity.ok(response);
    }

    /**
     * 대기열에서 사용자 위치 조회
     * @param randomBoxId 랜덤박스 ID
     * @param userId 사용자 ID
     * @return 대기열 정보
     */
    @GetMapping("/{randomBoxId}/users/{userId}")
    public ResponseEntity<QueueResponse> getQueueInfo(
            @PathVariable Long randomBoxId,
            @PathVariable Long userId) {

        int position = queueService.getPosition(randomBoxId, userId);

        if (position == -1) {
            return ResponseEntity.notFound().build();
        }

        int estimatedWaitTime = queueService.getEstimatedWaitTime(randomBoxId, userId);
        long waitingCount = queueService.getWaitingCount(randomBoxId);

        QueueResponse response = QueueResponse.of(
                userId,
                randomBoxId,
                position,
                estimatedWaitTime,
                waitingCount
        );

        return ResponseEntity.ok(response);
    }

    /**
     * 대기열에서 사용자 제거
     * @param randomBoxId 랜덤박스 ID
     * @param userId 사용자 ID
     * @return 성공 여부
     */
    @DeleteMapping("/{randomBoxId}/users/{userId}")
    public ResponseEntity<Void> removeFromQueue(
            @PathVariable Long randomBoxId,
            @PathVariable Long userId) {

        boolean removed = queueService.removeFromQueue(randomBoxId, userId);

        if (removed) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 다음 사용자 가져오기 (관리자용)
     * @param randomBoxId 랜덤박스 ID
     * @return 다음 사용자 ID
     */
    @GetMapping("/{randomBoxId}/next")
    public ResponseEntity<Long> getNextUser(
            @PathVariable Long randomBoxId) {

        Long nextUserId = queueService.getNextUser(randomBoxId);

        if (nextUserId == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(nextUserId);
    }

    /**
     * 대기 인원 수 조회
     * @param randomBoxId 랜덤박스 ID
     * @return 대기 인원 수
     */
    @GetMapping("/{randomBoxId}/count")
    public ResponseEntity<Long> getWaitingCount(
            @PathVariable Long randomBoxId) {

        long waitingCount = queueService.getWaitingCount(randomBoxId);
        return ResponseEntity.ok(waitingCount);
    }
}
