package com.example.jpa.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor
public class OrderCustomDto {

    private Long id;
    private LocalDateTime orderDate;
    private Long userId; // 사용자 ID (조립용)

    @QueryProjection
    public OrderCustomDto(Long id, LocalDateTime orderDate, Long userId) {
        this.id = id;
        this.orderDate = orderDate;
        this.userId = userId;
    }
}
