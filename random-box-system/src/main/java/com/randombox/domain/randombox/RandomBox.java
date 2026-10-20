package com.randombox.domain.randombox;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "random_boxes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RandomBox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private Integer price;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private LocalDateTime salesStartTime;

    @Column(nullable = false)
    private LocalDateTime salesEndTime;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    public RandomBox(String name, String description, Integer price, Integer quantity,
                    LocalDateTime salesStartTime, LocalDateTime salesEndTime) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.quantity = quantity;
        this.salesStartTime = salesStartTime;
        this.salesEndTime = salesEndTime;
        this.createdAt = LocalDateTime.now();
    }

    public void update(String name, String description, Integer price, Integer quantity,
                      LocalDateTime salesStartTime, LocalDateTime salesEndTime) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.quantity = quantity;
        this.salesStartTime = salesStartTime;
        this.salesEndTime = salesEndTime;
        this.updatedAt = LocalDateTime.now();
    }

    public void decreaseQuantity(int amount) {
        if (this.quantity < amount) {
            throw new IllegalStateException("재고가 부족합니다.");
        }
        this.quantity -= amount;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isOnSale() {
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(salesStartTime) && now.isBefore(salesEndTime) && quantity > 0;
    }

    public void updateQuantity(int newQuantity) {
        this.quantity = newQuantity;
        this.updatedAt = LocalDateTime.now();
    }
}
