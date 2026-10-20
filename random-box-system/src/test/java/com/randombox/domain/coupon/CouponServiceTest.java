package com.randombox.domain.coupon;

import com.randombox.domain.coupon.Coupon.DiscountType;
import com.randombox.domain.user.User;
import com.randombox.domain.user.UserRepository;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private UserCouponRepository userCouponRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CouponService couponService;

    private User user;
    private Coupon coupon;
    private UserCoupon userCoupon;
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
    @DisplayName("쿠폰 생성 성공")
    void createCoupon_Success() {
        // given
        when(couponRepository.save(any(Coupon.class))).thenReturn(coupon);

        // when
        Coupon result = couponService.createCoupon(
                "테스트 쿠폰",
                Coupon.DiscountType.PERCENTAGE,
                10,
                null,
                null,
                now.minusDays(1),
                now.plusDays(7)
        );

        // then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("테스트 쿠폰");
        assertThat(result.getDiscountType()).isEqualTo(Coupon.DiscountType.PERCENTAGE);
        assertThat(result.getDiscountValue()).isEqualTo(10);
        verify(couponRepository, times(1)).save(any(Coupon.class));
    }

    @Test
    @DisplayName("쿠폰 생성 실패 - 잘못된 할인 값")
    void createCoupon_Failure_InvalidDiscountValue() {
        // when & then
        assertThatThrownBy(() -> couponService.createCoupon(
                "테스트 쿠폰",
                Coupon.DiscountType.PERCENTAGE,
                -10,
                null,
                null,
                now.minusDays(1),
                now.plusDays(7)
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("할인 값은 0보다 커야 합니다.");

        assertThatThrownBy(() -> couponService.createCoupon(
                "테스트 쿠폰",
                Coupon.DiscountType.PERCENTAGE,
                110,
                null,
                null,
                now.minusDays(1),
                now.plusDays(7)
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("퍼센트 할인은 100%를 초과할 수 없습니다.");
    }

    @Test
    @DisplayName("쿠폰 생성 실패 - 잘못된 기간")
    void createCoupon_Failure_InvalidPeriod() {
        // when & then
        assertThatThrownBy(() -> couponService.createCoupon(
                "테스트 쿠폰",
                DiscountType.PERCENTAGE,
                10,
                null,
                null,
                now.plusDays(7),
                now.minusDays(1)
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("쿠폰 종료 시간은 시작 시간 이후여야 합니다.");
    }

    @Test
    @DisplayName("쿠폰 조회 성공")
    void getCoupon_Success() {
        // given
        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));

        // when
        Coupon result = couponService.getCoupon(1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("테스트 쿠폰");
    }

    @Test
    @DisplayName("코드로 쿠폰 조회 성공")
    void getCouponByCode_Success() {
        // given
        when(couponRepository.findByCode("TEST123")).thenReturn(Optional.of(coupon));

        // when
        Coupon result = couponService.getCouponByCode("TEST123");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo("TEST123");
        assertThat(result.getName()).isEqualTo("테스트 쿠폰");
    }

    @Test
    @DisplayName("유효한 쿠폰 목록 조회")
    void getValidCoupons_Success() {
        // given
        when(couponRepository.findAllValid(any(LocalDateTime.class))).thenReturn(Arrays.asList(coupon));

        // when
        List<Coupon> result = couponService.getValidCoupons();

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("테스트 쿠폰");
    }

    @Test
    @DisplayName("쿠폰 발급 성공")
    void issueCouponToUser_Success() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));
        when(userCouponRepository.findByUserIdAndCouponId(1L, 1L)).thenReturn(Optional.empty());
        when(userCouponRepository.save(any(UserCoupon.class))).thenReturn(userCoupon);

        // when
        UserCoupon result = couponService.issueCouponToUser(1L, 1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUser()).isEqualTo(user);
        assertThat(result.getCoupon()).isEqualTo(coupon);
        assertThat(result.isUsed()).isFalse();
        verify(userCouponRepository, times(1)).save(any(UserCoupon.class));
    }

    @Test
    @DisplayName("쿠폰 발급 실패 - 이미 발급됨")
    void issueCouponToUser_Failure_AlreadyIssued() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));
        when(userCouponRepository.findByUserIdAndCouponId(1L, 1L)).thenReturn(Optional.of(userCoupon));

        // when & then
        assertThatThrownBy(() -> couponService.issueCouponToUser(1L, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 발급된 쿠폰입니다.");
    }

    @Test
    @DisplayName("코드로 쿠폰 발급 성공")
    void issueCouponByCode_Success() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(couponRepository.findByCode("TEST123")).thenReturn(Optional.of(coupon));
        when(userCouponRepository.findByUserIdAndCouponId(1L, 1L)).thenReturn(Optional.empty());
        when(userCouponRepository.save(any(UserCoupon.class))).thenReturn(userCoupon);

        // when
        UserCoupon result = couponService.issueCouponByCode(1L, "TEST123");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUser()).isEqualTo(user);
        assertThat(result.getCoupon()).isEqualTo(coupon);
        assertThat(result.isUsed()).isFalse();
        verify(userCouponRepository, times(1)).save(any(UserCoupon.class));
    }

    @Test
    @DisplayName("사용자 쿠폰 목록 조회")
    void getUserCoupons_Success() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userCouponRepository.findByUserAndUsedFalse(user)).thenReturn(Arrays.asList(userCoupon));

        // when
        List<UserCoupon> result = couponService.getUserCoupons(1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUser()).isEqualTo(user);
        assertThat(result.get(0).getCoupon()).isEqualTo(coupon);
    }

    @Test
    @DisplayName("유효한 사용자 쿠폰 목록 조회")
    void getValidUserCoupons_Success() {
        // given
        when(userCouponRepository.findValidCouponsByUserId(1L)).thenReturn(Arrays.asList(userCoupon));

        // when
        List<UserCoupon> result = couponService.getValidUserCoupons(1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUser()).isEqualTo(user);
        assertThat(result.get(0).getCoupon()).isEqualTo(coupon);
    }
}
