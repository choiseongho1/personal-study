package com.randombox.domain.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String NOTIFICATION_KEY_PREFIX = "randombox:notification:";
    private static final String NOTIFICATION_COUNT_KEY_PREFIX = "randombox:notification-count:";
    private static final int NOTIFICATION_EXPIRY_DAYS = 30;

    /**
     * 알림 생성 및 저장
     * @param userId 사용자 ID
     * @param title 알림 제목
     * @param message 알림 내용
     * @param type 알림 유형
     * @return 생성된 알림
     */
    public Notification createNotification(Long userId, String title, String message, Notification.NotificationType type) {
        Notification notification = Notification.builder()
                .id(generateNotificationId(userId))
                .userId(userId)
                .title(title)
                .message(message)
                .type(type)
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();

        saveNotification(notification);

        log.info("사용자 {}에게 알림이 생성되었습니다. 제목: {}", userId, title);

        return notification;
    }

    /**
     * 알림 ID 생성
     * @param userId 사용자 ID
     * @return 알림 ID
     */
    private Long generateNotificationId(Long userId) {
        String countKey = NOTIFICATION_COUNT_KEY_PREFIX + userId;
        Long id = redisTemplate.opsForValue().increment(countKey);

        if (id == 1) {
            redisTemplate.expire(countKey, NOTIFICATION_EXPIRY_DAYS, TimeUnit.DAYS);
        }

        return id;
    }

    /**
     * 알림 저장
     * @param notification 알림
     */
    private void saveNotification(Notification notification) {
        String key = NOTIFICATION_KEY_PREFIX + notification.getUserId() + ":" + notification.getId();
        redisTemplate.opsForValue().set(key, notification);
        redisTemplate.expire(key, NOTIFICATION_EXPIRY_DAYS, TimeUnit.DAYS);

        // 알림 목록에 추가
        String listKey = NOTIFICATION_KEY_PREFIX + notification.getUserId();
        redisTemplate.opsForList().leftPush(listKey, notification.getId());
        redisTemplate.expire(listKey, NOTIFICATION_EXPIRY_DAYS, TimeUnit.DAYS);
    }

    /**
     * 사용자의 모든 알림 조회
     * @param userId 사용자 ID
     * @return 알림 목록
     */
    public List<Notification> getNotifications(Long userId) {
        String listKey = NOTIFICATION_KEY_PREFIX + userId;
        List<Object> notificationIds = redisTemplate.opsForList().range(listKey, 0, -1);

        if (notificationIds == null || notificationIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<Notification> notifications = new ArrayList<>();

        for (Object id : notificationIds) {
            String key = NOTIFICATION_KEY_PREFIX + userId + ":" + id;
            Object obj = redisTemplate.opsForValue().get(key);

            if (obj instanceof Notification) {
                notifications.add((Notification) obj);
            }
        }

        return notifications;
    }

    /**
     * 알림 읽음 처리
     * @param userId 사용자 ID
     * @param notificationId 알림 ID
     * @return 성공 여부
     */
    public boolean markAsRead(Long userId, Long notificationId) {
        String key = NOTIFICATION_KEY_PREFIX + userId + ":" + notificationId;
        Object obj = redisTemplate.opsForValue().get(key);

        if (obj instanceof Notification) {
            Notification notification = (Notification) obj;
            notification = Notification.builder()
                    .id(notification.getId())
                    .userId(notification.getUserId())
                    .title(notification.getTitle())
                    .message(notification.getMessage())
                    .type(notification.getType())
                    .read(true)
                    .createdAt(notification.getCreatedAt())
                    .build();

            redisTemplate.opsForValue().set(key, notification);
            redisTemplate.expire(key, NOTIFICATION_EXPIRY_DAYS, TimeUnit.DAYS);

            log.info("사용자 {}의 알림 {}이 읽음 처리되었습니다.", userId, notificationId);

            return true;
        }

        return false;
    }

    /**
     * 모든 알림 읽음 처리
     * @param userId 사용자 ID
     * @return 읽음 처리된 알림 수
     */
    public int markAllAsRead(Long userId) {
        List<Notification> notifications = getNotifications(userId);
        int count = 0;

        for (Notification notification : notifications) {
            if (!notification.isRead()) {
                boolean marked = markAsRead(userId, notification.getId());

                if (marked) {
                    count++;
                }
            }
        }

        log.info("사용자 {}의 모든 알림({})이 읽음 처리되었습니다.", userId, count);

        return count;
    }

    /**
     * 알림 삭제
     * @param userId 사용자 ID
     * @param notificationId 알림 ID
     * @return 성공 여부
     */
    public boolean deleteNotification(Long userId, Long notificationId) {
        String key = NOTIFICATION_KEY_PREFIX + userId + ":" + notificationId;
        Boolean deleted = redisTemplate.delete(key);

        if (Boolean.TRUE.equals(deleted)) {
            String listKey = NOTIFICATION_KEY_PREFIX + userId;
            redisTemplate.opsForList().remove(listKey, 0, notificationId);

            log.info("사용자 {}의 알림 {}이 삭제되었습니다.", userId, notificationId);

            return true;
        }

        return false;
    }

    /**
     * 구매 성공 알림 생성
     * @param userId 사용자 ID
     * @param randomBoxName 랜덤박스 이름
     * @param itemName 아이템 이름
     * @return 생성된 알림
     */
    public Notification sendPurchaseSuccessNotification(Long userId, String randomBoxName, String itemName) {
        String title = "구매 성공";
        String message = String.format("'%s' 랜덤박스에서 '%s' 아이템을 획득했습니다!", randomBoxName, itemName);

        return createNotification(userId, title, message, Notification.NotificationType.PURCHASE_SUCCESS);
    }

    /**
     * 대기열 준비 완료 알림 생성
     * @param userId 사용자 ID
     * @param randomBoxName 랜덤박스 이름
     * @return 생성된 알림
     */
    public Notification sendQueueReadyNotification(Long userId, String randomBoxName) {
        String title = "대기열 준비 완료";
        String message = String.format("'%s' 랜덤박스 구매를 위한 대기열에서 당신의 차례가 되었습니다. 5분 내에 구매를 완료해주세요.", randomBoxName);

        return createNotification(userId, title, message, Notification.NotificationType.QUEUE_READY);
    }

    /**
     * 쿠폰 발급 알림 생성
     * @param userId 사용자 ID
     * @param couponName 쿠폰 이름
     * @param discountValue 할인 값
     * @param discountType 할인 유형
     * @return 생성된 알림
     */
    public Notification sendCouponReceivedNotification(Long userId, String couponName, int discountValue, String discountType) {
        String title = "쿠폰 발급";
        String message = String.format("'%s' 쿠폰이 발급되었습니다. %d%s 할인 혜택을 받으세요!", couponName, discountValue, discountType);

        return createNotification(userId, title, message, Notification.NotificationType.COUPON_RECEIVED);
    }
}
