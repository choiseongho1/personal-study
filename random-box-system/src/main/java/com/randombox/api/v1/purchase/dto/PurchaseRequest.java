package com.randombox.api.v1.purchase.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseRequest {
    private Long userId;
    private Long randomBoxId;
    private Integer quantity;
    private Long userCouponId;
}
