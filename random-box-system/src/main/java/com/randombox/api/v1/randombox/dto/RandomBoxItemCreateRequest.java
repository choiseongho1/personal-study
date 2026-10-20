package com.randombox.api.v1.randombox.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RandomBoxItemCreateRequest {
    private String name;
    private String rarity;
    private Double probability;
}
