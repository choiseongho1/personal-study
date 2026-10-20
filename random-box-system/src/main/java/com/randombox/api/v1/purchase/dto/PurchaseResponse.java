package com.randombox.api.v1.purchase.dto;

import com.randombox.domain.purchase.Purchase;
import com.randombox.domain.purchase.PurchaseResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseResponse {
    // setter 추가
    public void setResults(List<PurchaseResultResponse> results) {
        this.results = results;
    }
    private Long id;
    private Long userId;
    private Long randomBoxId;
    private String randomBoxName;
    private Integer quantity;
    private Integer totalPrice;
    private Integer discountAmount;
    private Integer finalPrice;
    private LocalDateTime purchaseDate;
    private List<PurchaseResultResponse> results;

    public static PurchaseResponse from(Purchase purchase) {
        // 할인 금액과 최종 가격 계산
        int originalPrice = purchase.getRandomBox().getPrice() * purchase.getQuantity();
        int discountAmount = originalPrice - purchase.getTotalPrice();

        return PurchaseResponse.builder()
                .id(purchase.getId())
                .userId(purchase.getUser().getId())
                .randomBoxId(purchase.getRandomBox().getId())
                .randomBoxName(purchase.getRandomBox().getName())
                .quantity(purchase.getQuantity())
                .totalPrice(originalPrice)
                .discountAmount(discountAmount)
                .finalPrice(purchase.getTotalPrice())
                .purchaseDate(purchase.getPurchaseDateTime())
                .results(Collections.emptyList()) // 결과는 컨트롤러에서 설정해야 함
                .build();
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PurchaseResultResponse {
        private Long id;
        private String itemName;
        private String rarity;

        public static PurchaseResultResponse from(PurchaseResult result) {
            return PurchaseResultResponse.builder()
                    .id(result.getId())
                    .itemName(result.getRandomBoxItem().getName())
                    .rarity(result.getRandomBoxItem().getRarity().name()) // Rarity 열거형을 문자열로 변환
                    .build();
        }
    }
}
