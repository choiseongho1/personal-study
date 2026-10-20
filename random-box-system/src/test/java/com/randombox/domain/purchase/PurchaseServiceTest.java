package com.randombox.domain.purchase;

import com.randombox.domain.coupon.Coupon;
import com.randombox.domain.coupon.UserCoupon;
import com.randombox.domain.coupon.UserCouponRepository;
import com.randombox.domain.randombox.RandomBox;
import com.randombox.domain.randombox.RandomBoxItem;
import com.randombox.domain.randombox.RandomBoxRepository;
import com.randombox.domain.randombox.RandomBoxService;
import com.randombox.domain.randombox.RandomBoxInventoryService;
import com.randombox.domain.queue.QueueService;
import com.randombox.domain.notification.NotificationService;
import com.randombox.domain.user.User;
import com.randombox.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseServiceTest {

    @Mock
    private PurchaseRepository purchaseRepository;

    @Mock
    private PurchaseResultRepository purchaseResultRepository;

    @Mock
    private RandomBoxRepository randomBoxRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RandomBoxService randomBoxService;

    @Mock
    private UserCouponRepository userCouponRepository;

    @Mock
    private RandomBoxInventoryService randomBoxInventoryService;

    @Mock
    private QueueService queueService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private PurchaseService purchaseService;

    private User user;
    private RandomBox randomBox;
    private RandomBoxItem randomBoxItem;
    private Purchase purchase;
    private PurchaseResult purchaseResult;
    private UserCoupon userCoupon;
    private Coupon coupon;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();

        user = User.builder()
                .email("test@example.com")
                .password("password")
                .nickname("tester")
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);

        randomBox = RandomBox.builder()
                .name("테스트 랜덤박스")
                .description("테스트용 랜덤박스입니다.")
                .price(1000)
                .quantity(100)
                .salesStartTime(now.minusDays(1))
                .salesEndTime(now.plusDays(7))
                .build();
        ReflectionTestUtils.setField(randomBox, "id", 1L);

        randomBoxItem = RandomBoxItem.builder()
                .randomBox(randomBox)
                .name("일반 아이템")
                .description("흔한 아이템입니다.")
                .rarity(RandomBoxItem.Rarity.COMMON)
                .probability(new BigDecimal("100.0"))
                .build();
        ReflectionTestUtils.setField(randomBoxItem, "id", 1L);

        purchase = Purchase.builder()
                .user(user)
                .randomBox(randomBox)
                .quantity(1)
                .totalPrice(1000)
                .build();
        ReflectionTestUtils.setField(purchase, "id", 1L);
        ReflectionTestUtils.setField(purchase, "purchaseDateTime", now);
        ReflectionTestUtils.setField(purchase, "status", Purchase.PurchaseStatus.COMPLETED);

        purchaseResult = PurchaseResult.builder()
                .purchase(purchase)
                .randomBoxItem(randomBoxItem)
                .build();
        ReflectionTestUtils.setField(purchaseResult, "id", 1L);

        coupon = Coupon.builder()
                .code("TEST123")
                .name("테스트 쿠폰")
                .discountType(Coupon.DiscountType.PERCENTAGE)
                .discountValue(10)
                .startDate(now.minusDays(1))
                .endDate(now.plusDays(7))
                .build();
        ReflectionTestUtils.setField(coupon, "id", 1L);

        userCoupon = UserCoupon.builder()
                .user(user)
                .coupon(coupon)
                .build();
        ReflectionTestUtils.setField(userCoupon, "id", 1L);
        ReflectionTestUtils.setField(userCoupon, "used", false);
    }

    @Test
    @DisplayName("랜덤박스 구매 성공")
    void purchaseRandomBox_Success() {
        when(randomBoxInventoryService.decreaseInventory(1L, 1)).thenReturn(true);
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(randomBoxRepository.findById(1L)).thenReturn(Optional.of(randomBox));
        when(purchaseRepository.save(any(Purchase.class))).thenReturn(purchase);
        when(randomBoxService.drawRandomItem(1L)).thenReturn(randomBoxItem);
        when(purchaseResultRepository.save(any(PurchaseResult.class))).thenReturn(purchaseResult);

        // when
        Purchase result = purchaseService.purchaseRandomBox(1L, 1L, 1, null);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUser()).isEqualTo(user);
        assertThat(result.getRandomBox()).isEqualTo(randomBox);
        assertThat(result.getQuantity()).isEqualTo(1);
        assertThat(result.getTotalPrice()).isEqualTo(1000);
        assertThat(result.getStatus()).isEqualTo(Purchase.PurchaseStatus.COMPLETED);

        verify(randomBoxRepository, times(1)).findById(1L);
        verify(randomBoxInventoryService).decreaseInventory(1L, 1);
        verify(notificationService).sendPurchaseSuccessNotification(1L, randomBox.getName(), randomBoxItem.getName());
        verify(purchaseRepository, times(1)).save(any(Purchase.class));
        verify(randomBoxService, times(1)).drawRandomItem(1L);
        verify(purchaseResultRepository, times(1)).save(any(PurchaseResult.class));
    }

    @Test
    @DisplayName("랜덤박스 구매 성공 - 쿠폰 적용")
    void purchaseRandomBox_WithCoupon_Success() {
        when(randomBoxInventoryService.decreaseInventory(1L, 1)).thenReturn(true);
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(randomBoxRepository.findById(1L)).thenReturn(Optional.of(randomBox));
        when(userCouponRepository.findById(1L)).thenReturn(Optional.of(userCoupon));
        when(purchaseRepository.save(any(Purchase.class))).thenReturn(purchase);
        when(randomBoxService.drawRandomItem(1L)).thenReturn(randomBoxItem);
        when(purchaseResultRepository.save(any(PurchaseResult.class))).thenReturn(purchaseResult);

        // when
        Purchase result = purchaseService.purchaseRandomBox(1L, 1L, 1, 1L);

        // then
        assertThat(result).isNotNull();
        verify(userCouponRepository, times(1)).findById(1L);
        verify(randomBoxInventoryService).decreaseInventory(1L, 1);
    }

    @Test
    @DisplayName("랜덤박스 구매 실패 - 판매 중이 아님")
    void purchaseRandomBox_Failure_NotOnSale() {
        // given
        RandomBox notOnSaleBox = RandomBox.builder()
                .name("판매종료 랜덤박스")
                .description("판매가 종료된 랜덤박스입니다.")
                .price(1000)
                .quantity(100)
                .salesStartTime(now.minusDays(10))
                .salesEndTime(now.minusDays(5))
                .build();
        ReflectionTestUtils.setField(notOnSaleBox, "id", 2L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(randomBoxRepository.findById(2L)).thenReturn(Optional.of(notOnSaleBox));

        // when & then
        assertThatThrownBy(() -> purchaseService.purchaseRandomBox(1L, 2L, 1, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("현재 판매 중인 랜덤박스가 아닙니다.");
    }

    @Test
    @DisplayName("구매 내역 조회 성공")
    void getUserPurchases_Success() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(purchaseRepository.findByUserOrderByPurchaseDateTimeDesc(user)).thenReturn(Arrays.asList(purchase));

        // when
        List<Purchase> result = purchaseService.getUserPurchases(1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUser()).isEqualTo(user);
        assertThat(result.get(0).getRandomBox()).isEqualTo(randomBox);
    }

    @Test
    @DisplayName("구매 결과 조회 성공")
    void getPurchaseResults_Success() {
        // given
        when(purchaseResultRepository.findByPurchaseId(1L)).thenReturn(Arrays.asList(purchaseResult));

        // when
        List<PurchaseResult> result = purchaseService.getPurchaseResults(1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPurchase()).isEqualTo(purchase);
        assertThat(result.get(0).getRandomBoxItem()).isEqualTo(randomBoxItem);
    }

    @Test
    @DisplayName("구매 취소 성공")
    void cancelPurchase_Success() {
        // given
        when(purchaseRepository.findById(1L)).thenReturn(Optional.of(purchase));

        // when
        Purchase result = purchaseService.cancelPurchase(1L, 1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(Purchase.PurchaseStatus.CANCELLED);
    }

    @Test
    @DisplayName("구매 취소 실패 - 24시간 초과")
    void cancelPurchase_Failure_TimeExceeded() {
        // given
        Purchase oldPurchase = Purchase.builder()
                .user(user)
                .randomBox(randomBox)
                .quantity(1)
                .totalPrice(1000)
                .build();
        ReflectionTestUtils.setField(oldPurchase, "id", 2L);
        ReflectionTestUtils.setField(oldPurchase, "purchaseDateTime", now.minusDays(2));
        ReflectionTestUtils.setField(oldPurchase, "status", Purchase.PurchaseStatus.COMPLETED);

        when(purchaseRepository.findById(2L)).thenReturn(Optional.of(oldPurchase));

        // when & then
        assertThatThrownBy(() -> purchaseService.cancelPurchase(1L, 2L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("구매 후 24시간이 지나 취소할 수 없습니다.");
    }
}
