package com.randombox.domain.coupon;

import com.randombox.domain.user.User;
import com.randombox.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final UserRepository userRepository;

    @Transactional
    public Coupon createCoupon(String name, Coupon.DiscountType discountType, Integer discountValue,
                              Integer minPurchase, Integer maxDiscount, LocalDateTime startDate, LocalDateTime endDate) {
        validateCouponTimes(startDate, endDate);
        validateDiscountValue(discountType, discountValue, maxDiscount);

        String code = generateUniqueCode();

        Coupon coupon = Coupon.builder()
                .code(code)
                .name(name)
                .discountType(discountType)
                .discountValue(discountValue)
                .minPurchase(minPurchase)
                .maxDiscount(maxDiscount)
                .startDate(startDate)
                .endDate(endDate)
                .build();

        return couponRepository.save(coupon);
    }

    private void validateCouponTimes(LocalDateTime startDate, LocalDateTime endDate) {
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("쿠폰 종료 시간은 시작 시간 이후여야 합니다.");
        }
    }

    private void validateDiscountValue(Coupon.DiscountType discountType, Integer discountValue, Integer maxDiscount) {
        if (discountValue <= 0) {
            throw new IllegalArgumentException("할인 값은 0보다 커야 합니다.");
        }

        if (discountType == Coupon.DiscountType.PERCENTAGE && discountValue > 100) {
            throw new IllegalArgumentException("퍼센트 할인은 100%를 초과할 수 없습니다.");
        }

        if (maxDiscount != null && maxDiscount <= 0) {
            throw new IllegalArgumentException("최대 할인 금액은 0보다 커야 합니다.");
        }
    }

    private String generateUniqueCode() {
        String code;
        do {
            code = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (couponRepository.findByCode(code).isPresent());

        return code;
    }

    public Coupon getCoupon(Long couponId) {
        return couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다."));
    }

    public Coupon getCouponByCode(String code) {
        return couponRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰 코드입니다."));
    }

    public List<Coupon> getValidCoupons() {
        return couponRepository.findAllValid(LocalDateTime.now());
    }

    @Transactional
    public Coupon updateCoupon(Long couponId, String name, Coupon.DiscountType discountType, Integer discountValue,
                              Integer minPurchase, Integer maxDiscount, LocalDateTime startDate, LocalDateTime endDate) {
        validateCouponTimes(startDate, endDate);
        validateDiscountValue(discountType, discountValue, maxDiscount);

        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다."));

        coupon.update(name, discountType, discountValue, minPurchase, maxDiscount, startDate, endDate);
        return coupon;
    }

    @Transactional
    public UserCoupon issueCouponToUser(Long userId, Long couponId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다."));

        if (userCouponRepository.findByUserIdAndCouponId(userId, couponId).isPresent()) {
            throw new IllegalStateException("이미 발급된 쿠폰입니다.");
        }

        UserCoupon userCoupon = UserCoupon.builder()
                .user(user)
                .coupon(coupon)
                .build();

        return userCouponRepository.save(userCoupon);
    }

    @Transactional
    public UserCoupon issueCouponByCode(Long userId, String couponCode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        Coupon coupon = couponRepository.findByCode(couponCode)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰 코드입니다."));

        if (!coupon.isValid()) {
            throw new IllegalStateException("유효하지 않은 쿠폰입니다.");
        }

        if (userCouponRepository.findByUserIdAndCouponId(userId, coupon.getId()).isPresent()) {
            throw new IllegalStateException("이미 발급된 쿠폰입니다.");
        }

        UserCoupon userCoupon = UserCoupon.builder()
                .user(user)
                .coupon(coupon)
                .build();

        return userCouponRepository.save(userCoupon);
    }

    public List<UserCoupon> getUserCoupons(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        return userCouponRepository.findByUserAndUsedFalse(user);
    }

    public List<UserCoupon> getValidUserCoupons(Long userId) {
        return userCouponRepository.findValidCouponsByUserId(userId);
    }


}
