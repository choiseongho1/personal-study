package com.randombox.api.v1.purchase.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseQueueResponse {
    private Long userId;
    private Long randomBoxId;
    private int position;
    private int estimatedWaitTimeSeconds;
    private boolean canPurchase;
    private String message;
}
