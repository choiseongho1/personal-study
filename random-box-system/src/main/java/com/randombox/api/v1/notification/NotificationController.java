package com.randombox.api.v1.notification;

import com.randombox.api.v1.notification.dto.NotificationResponse;
import com.randombox.domain.notification.Notification;
import com.randombox.domain.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 사용자의 모든 알림 조회
     * @param userId 사용자 ID
     * @return 알림 목록
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            @PathVariable Long userId) {

        List<Notification> notifications = notificationService.getNotifications(userId);
        List<NotificationResponse> responses = notifications.stream()
                .map(NotificationResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * 알림 읽음 처리
     * @param userId 사용자 ID
     * @param notificationId 알림 ID
     * @return 성공 여부
     */
    @PutMapping("/users/{userId}/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long userId,
            @PathVariable Long notificationId) {

        boolean marked = notificationService.markAsRead(userId, notificationId);

        if (marked) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 모든 알림 읽음 처리
     * @param userId 사용자 ID
     * @return 읽음 처리된 알림 수
     */
    @PutMapping("/users/{userId}/read-all")
    public ResponseEntity<Integer> markAllAsRead(
            @PathVariable Long userId) {

        int count = notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(count);
    }

    /**
     * 알림 삭제
     * @param userId 사용자 ID
     * @param notificationId 알림 ID
     * @return 성공 여부
     */
    @DeleteMapping("/users/{userId}/{notificationId}")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable Long userId,
            @PathVariable Long notificationId) {

        boolean deleted = notificationService.deleteNotification(userId, notificationId);

        if (deleted) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
