package com.randombox.domain.purchase;

import com.randombox.config.TestRedisConfig;
import com.randombox.domain.notification.NotificationService;
import com.randombox.domain.queue.QueueService;
import com.randombox.domain.randombox.RandomBox;
import com.randombox.domain.randombox.RandomBoxInventoryService;
import com.randombox.domain.randombox.RandomBoxItem;
import com.randombox.domain.randombox.RandomBoxRepository;
import com.randombox.domain.randombox.RandomBoxService;
import com.randombox.domain.user.User;
import com.randombox.domain.user.UserRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@Import(TestRedisConfig.class)
class RedisPurchaseServiceTest {

    @Autowired
    private PurchaseService purchaseService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @MockBean
    private PurchaseRepository purchaseRepository;

    @MockBean
    private PurchaseResultRepository purchaseResultRepository;

    @MockBean
    private RandomBoxRepository randomBoxRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private RandomBoxService randomBoxService;

    @MockBean
    private RandomBoxInventoryService randomBoxInventoryService;

    @MockBean
    private QueueService queueService;

    @MockBean
    private NotificationService notificationService;

    private static final Long TEST_USER_ID = 101L;
    private static final Long TEST_RANDOM_BOX_ID = 1L;
    private User testUser;
    private RandomBox testRandomBox;
    private RandomBoxItem testRandomBoxItem;
    private Purchase testPurchase;
    private PurchaseResult testPurchaseResult;

    @BeforeEach
    void setUp() {
        // 테스트 전 Redis 데이터 초기화
        if (redisTemplate != null && redisTemplate.getConnectionFactory() != null) {
            redisTemplate.execute(connection -> {
                connection.flushDb();
                return null;
            }, true);
        }

        // 테스트용 객체 생성
        testUser = User.builder()
                .nickname("테스트 사용자")
                .email("test@example.com")
                .password("password123")
                .build();

        testRandomBox = RandomBox.builder()
                .name("테스트 랜덤박스")
                .description("테스트용 랜덤박스입니다.")
                .price(10000)
                .quantity(100)
                .salesStartTime(LocalDateTime.now().minusDays(1))
                .salesEndTime(LocalDateTime.now().plusDays(7))
                .build();

        testRandomBoxItem = RandomBoxItem.builder()
                .randomBox(testRandomBox)
                .name("테스트 아이템")
                .description("테스트용 아이템입니다.")
                .rarity(RandomBoxItem.Rarity.RARE)
                .build();

        testPurchase = Purchase.builder()
                .user(testUser)
                .randomBox(testRandomBox)
                .quantity(1)
                .totalPrice(10000)
                .build();

        testPurchaseResult = PurchaseResult.builder()
                .purchase(testPurchase)
                .randomBoxItem(testRandomBoxItem)
                .build();

        // Mock 설정
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(randomBoxRepository.findById(TEST_RANDOM_BOX_ID)).thenReturn(Optional.of(testRandomBox));
        when(randomBoxService.drawRandomItem(TEST_RANDOM_BOX_ID)).thenReturn(testRandomBoxItem);
        when(purchaseRepository.save(any(Purchase.class))).thenReturn(testPurchase);
        when(purchaseResultRepository.save(any(PurchaseResult.class))).thenReturn(testPurchaseResult);
        when(randomBoxInventoryService.decreaseInventory(eq(TEST_RANDOM_BOX_ID), any(Integer.class))).thenReturn(true);
        when(randomBoxInventoryService.increaseInventory(eq(TEST_RANDOM_BOX_ID), any(Integer.class))).thenReturn(true);
        when(queueService.getPosition(TEST_RANDOM_BOX_ID, TEST_USER_ID)).thenReturn(0); // 대기열 첫 번째 위치
    }

    @Test
    @DisplayName("대기열 위치 확인 성공")
    void checkQueuePosition_Success() {
        // given
        when(queueService.getPosition(TEST_RANDOM_BOX_ID, TEST_USER_ID)).thenReturn(-1); // 대기열에 없음
        when(queueService.addToQueue(TEST_RANDOM_BOX_ID, TEST_USER_ID)).thenReturn(0); // 대기열에 추가 후 첫 번째 위치

        // when
        int position = purchaseService.checkQueuePosition(TEST_USER_ID, TEST_RANDOM_BOX_ID);

        // then
        assertThat(position).isEqualTo(0);
        verify(queueService, times(1)).getPosition(TEST_RANDOM_BOX_ID, TEST_USER_ID);
        verify(queueService, times(1)).addToQueue(TEST_RANDOM_BOX_ID, TEST_USER_ID);
    }

    @Test
    @DisplayName("구매 준비 성공")
    void prepareForPurchase_Success() {
        // given
        when(queueService.getNextUser(TEST_RANDOM_BOX_ID)).thenReturn(TEST_USER_ID);

        // when
        Long userId = purchaseService.prepareForPurchase(TEST_RANDOM_BOX_ID);

        // then
        assertThat(userId).isEqualTo(TEST_USER_ID);
        verify(queueService, times(1)).getNextUser(TEST_RANDOM_BOX_ID);
        verify(notificationService, times(1)).sendQueueReadyNotification(TEST_USER_ID, testRandomBox.getName());
    }

    @Test
    @DisplayName("랜덤박스 구매 성공")
    void purchaseRandomBox_Success() {
        // when
        Purchase purchase = purchaseService.purchaseRandomBox(TEST_USER_ID, TEST_RANDOM_BOX_ID, 1, null);

        // then
        assertThat(purchase).isNotNull();
        assertThat(purchase.getUser()).isEqualTo(testUser);
        assertThat(purchase.getRandomBox()).isEqualTo(testRandomBox);
        assertThat(purchase.getQuantity()).isEqualTo(1);
        assertThat(purchase.getTotalPrice()).isEqualTo(10000);

        verify(randomBoxInventoryService, times(1)).decreaseInventory(TEST_RANDOM_BOX_ID, 1);
        verify(randomBoxService, times(1)).drawRandomItem(TEST_RANDOM_BOX_ID);
        verify(purchaseResultRepository, times(1)).save(any(PurchaseResult.class));
        verify(notificationService, times(1)).sendPurchaseSuccessNotification(
                eq(TEST_USER_ID),
                eq(testRandomBox.getName()),
                eq(testRandomBoxItem.getName())
        );
    }

    @Test
    @DisplayName("랜덤박스 구매 실패 - 재고 부족")
    void purchaseRandomBox_Failure_InsufficientInventory() {
        // given
        when(randomBoxInventoryService.decreaseInventory(TEST_RANDOM_BOX_ID, 1)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> purchaseService.purchaseRandomBox(TEST_USER_ID, TEST_RANDOM_BOX_ID, 1, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("재고가 부족하거나 재고 감소에 실패했습니다.");

        verify(randomBoxInventoryService, times(1)).decreaseInventory(TEST_RANDOM_BOX_ID, 1);
        verify(randomBoxService, times(0)).drawRandomItem(TEST_RANDOM_BOX_ID);
        verify(purchaseResultRepository, times(0)).save(any(PurchaseResult.class));
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
    @DisplayName("구매 취소 성공")
    void cancelPurchase_Success() {
        // given
        when(purchaseRepository.findById(anyLong())).thenReturn(Optional.of(testPurchase));

        // when
        Purchase canceledPurchase = purchaseService.cancelPurchase(TEST_USER_ID, 1L);

        // then
        assertThat(canceledPurchase).isNotNull();
        assertThat(canceledPurchase.getStatus()).isEqualTo(Purchase.PurchaseStatus.CANCELLED);

        verify(randomBoxInventoryService, times(1)).increaseInventory(eq(testRandomBox.getId()), eq(testPurchase.getQuantity()));
    }
}
