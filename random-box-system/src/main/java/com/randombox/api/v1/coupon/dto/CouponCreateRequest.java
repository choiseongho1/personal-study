package com.randombox.api.v1.coupon.dto;

import com.randombox.domain.coupon.Coupon;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponCreateRequest {
    private String name;
    private String code;
    private Coupon.DiscountType discountType;
    private Integer discountValue;
    private Integer minPurchase;
    private Integer maxDiscount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
