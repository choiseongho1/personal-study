package com.randombox.api.v1.coupon.dto;

import com.randombox.domain.coupon.UserCoupon;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCouponResponse {
    private Long id;
    private Long userId;
    private CouponResponse coupon;
    private boolean used;
    private LocalDateTime createdAt;

    public static UserCouponResponse from(UserCoupon userCoupon) {
        return UserCouponResponse.builder()
                .id(userCoupon.getId())
                .userId(userCoupon.getUser().getId())
                .coupon(CouponResponse.from(userCoupon.getCoupon()))
                .used(userCoupon.isUsed())
                .createdAt(userCoupon.getCreatedAt())
                .build();
    }
}
