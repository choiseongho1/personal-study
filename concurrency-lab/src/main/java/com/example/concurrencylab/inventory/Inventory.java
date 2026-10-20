package com.example.concurrencylab.inventory;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "inventory")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inventory {
    @Id
    @Column(name = "event_id")
    private Long eventId;

    @Column(nullable = false)
    private int available;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    public Inventory(Long eventId, int available, BigDecimal unitPrice) {
        this.eventId = eventId;
        this.available = available;
        this.unitPrice = unitPrice;
    }

    // TODO: 실습에서 선택한 재고 변경 방식에 필요한 메서드를 작성하세요.
}
