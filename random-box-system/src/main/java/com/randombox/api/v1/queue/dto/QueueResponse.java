package com.randombox.api.v1.queue.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueResponse {
    private Long userId;
    private Long randomBoxId;
    private int position;
    private int estimatedWaitTimeSeconds;
    private long totalWaitingCount;

    public static QueueResponse of(Long userId, Long randomBoxId, int position, int estimatedWaitTimeSeconds, long totalWaitingCount) {
        return QueueResponse.builder()
                .userId(userId)
                .randomBoxId(randomBoxId)
                .position(position)
                .estimatedWaitTimeSeconds(estimatedWaitTimeSeconds)
                .totalWaitingCount(totalWaitingCount)
                .build();
    }
}
