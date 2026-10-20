package com.randombox.api.v1.randombox.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RandomBoxCreateRequest {
    private String name;
    private String description;
    private Integer price;
    private Integer quantity;
    private LocalDateTime salesStartTime;
    private LocalDateTime salesEndTime;
}
