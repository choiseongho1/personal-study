package com.randombox.api.v1.randombox.dto;

import com.randombox.domain.randombox.RandomBox;
import com.randombox.domain.randombox.RandomBoxItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RandomBoxResponse {
    // setter 추가
    public void setItems(List<RandomBoxItemResponse> items) {
        this.items = items;
    }
    private Long id;
    private String name;
    private String description;
    private Integer price;
    private Integer quantity;
    private Integer remainingQuantity;
    private LocalDateTime salesStartTime;
    private LocalDateTime salesEndTime;
    private List<RandomBoxItemResponse> items;

    public static RandomBoxResponse from(RandomBox randomBox) {
        return RandomBoxResponse.builder()
                .id(randomBox.getId())
                .name(randomBox.getName())
                .description(randomBox.getDescription())
                .price(randomBox.getPrice())
                .quantity(randomBox.getQuantity())
                .remainingQuantity(randomBox.getQuantity()) // 남은 수량은 그냥 quantity로 설정
                .salesStartTime(randomBox.getSalesStartTime())
                .salesEndTime(randomBox.getSalesEndTime())
                .items(Collections.emptyList()) // 컨트롤러에서 설정해야 함
                .build();
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RandomBoxItemResponse {
        private Long id;
        private String name;
        private String rarity;
        private Double probability;

        public static RandomBoxItemResponse from(RandomBoxItem item) {
            return RandomBoxItemResponse.builder()
                    .id(item.getId())
                    .name(item.getName())
                    .rarity(item.getRarity().name()) // Rarity 열거형을 문자열로 변환
                    .probability(item.getProbability().doubleValue()) // BigDecimal을 Double로 변환
                    .build();
        }
    }
}
