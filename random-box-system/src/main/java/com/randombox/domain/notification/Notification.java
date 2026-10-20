package com.randombox.domain.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    private Long id;
    private Long userId;
    private String title;
    private String message;
    private NotificationType type;
    private boolean read;
    private LocalDateTime createdAt;

    public enum NotificationType {
        PURCHASE_SUCCESS,
        PURCHASE_FAILED,
        QUEUE_POSITION_CHANGED,
        QUEUE_READY,
        COUPON_RECEIVED,
        COUPON_EXPIRED,
        SYSTEM
    }
}
