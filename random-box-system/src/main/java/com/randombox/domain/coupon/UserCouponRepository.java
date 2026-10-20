package com.randombox.domain.coupon;

import com.randombox.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {

    List<UserCoupon> findByUserAndUsedFalse(User user);

    @Query("SELECT uc FROM UserCoupon uc WHERE uc.user.id = :userId AND uc.used = false AND uc.coupon.startDate <= CURRENT_TIMESTAMP AND uc.coupon.endDate >= CURRENT_TIMESTAMP")
    List<UserCoupon> findValidCouponsByUserId(@Param("userId") Long userId);

    Optional<UserCoupon> findByUserIdAndCouponId(Long userId, Long couponId);

    @Query("SELECT COUNT(uc) FROM UserCoupon uc WHERE uc.coupon.id = :couponId AND uc.used = true")
    long countUsedCouponsByCouponId(@Param("couponId") Long couponId);
}
