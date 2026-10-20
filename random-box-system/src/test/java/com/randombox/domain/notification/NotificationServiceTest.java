package com.randombox.domain.notification;

import com.randombox.config.TestRedisConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestRedisConfig.class)
class NotificationServiceTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final Long TEST_USER_ID = 101L;

    @BeforeEach
    void setUp() {
        // 테스트 전 Redis 데이터 초기화
        if (redisTemplate != null && redisTemplate.getConnectionFactory() != null) {
            redisTemplate.execute(connection -> {
                connection.flushDb();
                return null;
            }, true);
        }
    }

    @AfterEach
    void tearDown() {
        // 테스트 후 Redis 데이터 초기화
        if (redisTemplate != null && redisTemplate.getConnectionFactory() != null) {
            redisTemplate.execute(connection -> {
                connection.flushDb();
                return null;
            }, true);
        }
    }

    @Test
    @DisplayName("알림 생성 성공")
    void createNotification_Success() {
        // when
        Notification notification = notificationService.createNotification(
                TEST_USER_ID,
                "테스트 알림",
                "테스트 알림 내용입니다.",
                Notification.NotificationType.SYSTEM
        );

        // then
        assertThat(notification).isNotNull();
        assertThat(notification.getUserId()).isEqualTo(TEST_USER_ID);
        assertThat(notification.getTitle()).isEqualTo("테스트 알림");
        assertThat(notification.getMessage()).isEqualTo("테스트 알림 내용입니다.");
        assertThat(notification.getType()).isEqualTo(Notification.NotificationType.SYSTEM);
        assertThat(notification.isRead()).isFalse();
    }

    @Test
    @DisplayName("사용자 알림 목록 조회 성공")
    void getNotifications_Success() {
        // given
        notificationService.createNotification(
                TEST_USER_ID,
                "첫 번째 알림",
                "첫 번째 알림 내용입니다.",
                Notification.NotificationType.SYSTEM
        );
        notificationService.createNotification(
                TEST_USER_ID,
                "두 번째 알림",
                "두 번째 알림 내용입니다.",
                Notification.NotificationType.PURCHASE_SUCCESS
        );

        // when
        List<Notification> notifications = notificationService.getNotifications(TEST_USER_ID);

        // then
        assertThat(notifications).hasSize(2);
        assertThat(notifications.get(0).getTitle()).isEqualTo("두 번째 알림"); // 최신 알림이 먼저 오도록 정렬됨
        assertThat(notifications.get(1).getTitle()).isEqualTo("첫 번째 알림");
    }

    @Test
    @DisplayName("알림 읽음 처리 성공")
    void markAsRead_Success() {
        // given
        Notification notification = notificationService.createNotification(
                TEST_USER_ID,
                "테스트 알림",
                "테스트 알림 내용입니다.",
                Notification.NotificationType.SYSTEM
        );

        // when
        boolean marked = notificationService.markAsRead(TEST_USER_ID, notification.getId());
        List<Notification> notifications = notificationService.getNotifications(TEST_USER_ID);

        // then
        assertThat(marked).isTrue();
        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).isRead()).isTrue();
    }

    @Test
    @DisplayName("모든 알림 읽음 처리 성공")
    void markAllAsRead_Success() {
        // given
        notificationService.createNotification(
                TEST_USER_ID,
                "첫 번째 알림",
                "첫 번째 알림 내용입니다.",
                Notification.NotificationType.SYSTEM
        );
        notificationService.createNotification(
                TEST_USER_ID,
                "두 번째 알림",
                "두 번째 알림 내용입니다.",
                Notification.NotificationType.PURCHASE_SUCCESS
        );

        // when
        int count = notificationService.markAllAsRead(TEST_USER_ID);
        List<Notification> notifications = notificationService.getNotifications(TEST_USER_ID);

        // then
        assertThat(count).isEqualTo(2);
        assertThat(notifications).hasSize(2);
        assertThat(notifications.get(0).isRead()).isTrue();
        assertThat(notifications.get(1).isRead()).isTrue();
    }

    @Test
    @DisplayName("알림 삭제 성공")
    void deleteNotification_Success() {
        // given
        Notification notification = notificationService.createNotification(
                TEST_USER_ID,
                "테스트 알림",
                "테스트 알림 내용입니다.",
                Notification.NotificationType.SYSTEM
        );

        // when
        boolean deleted = notificationService.deleteNotification(TEST_USER_ID, notification.getId());
        List<Notification> notifications = notificationService.getNotifications(TEST_USER_ID);

        // then
        assertThat(deleted).isTrue();
        assertThat(notifications).isEmpty();
    }

    @Test
    @DisplayName("구매 성공 알림 생성 성공")
    void sendPurchaseSuccessNotification_Success() {
        // when
        Notification notification = notificationService.sendPurchaseSuccessNotification(
                TEST_USER_ID,
                "테스트 랜덤박스",
                "테스트 아이템"
        );

        // then
        assertThat(notification).isNotNull();
        assertThat(notification.getType()).isEqualTo(Notification.NotificationType.PURCHASE_SUCCESS);
        assertThat(notification.getTitle()).isEqualTo("구매 성공");
        assertThat(notification.getMessage()).contains("테스트 랜덤박스");
        assertThat(notification.getMessage()).contains("테스트 아이템");
    }

    @Test
    @DisplayName("대기열 준비 완료 알림 생성 성공")
    void sendQueueReadyNotification_Success() {
        // when
        Notification notification = notificationService.sendQueueReadyNotification(
                TEST_USER_ID,
                "테스트 랜덤박스"
        );

        // then
        assertThat(notification).isNotNull();
        assertThat(notification.getType()).isEqualTo(Notification.NotificationType.QUEUE_READY);
        assertThat(notification.getTitle()).isEqualTo("대기열 준비 완료");
        assertThat(notification.getMessage()).contains("테스트 랜덤박스");
        assertThat(notification.getMessage()).contains("5분 내에 구매를 완료해주세요");
    }

    @Test
    @DisplayName("쿠폰 발급 알림 생성 성공")
    void sendCouponReceivedNotification_Success() {
        // when
        Notification notification = notificationService.sendCouponReceivedNotification(
                TEST_USER_ID,
                "테스트 쿠폰",
                10,
                "%"
        );

        // then
        assertThat(notification).isNotNull();
        assertThat(notification.getType()).isEqualTo(Notification.NotificationType.COUPON_RECEIVED);
        assertThat(notification.getTitle()).isEqualTo("쿠폰 발급");
        assertThat(notification.getMessage()).contains("테스트 쿠폰");
        assertThat(notification.getMessage()).contains("10%");
    }
}
