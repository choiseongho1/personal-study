package com.randombox.domain.randombox;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "random_box_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RandomBoxItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "random_box_id", nullable = false)
    private RandomBox randomBox;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rarity rarity;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal probability;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    public RandomBoxItem(RandomBox randomBox, String name, String description, Rarity rarity, BigDecimal probability) {
        this.randomBox = randomBox;
        this.name = name;
        this.description = description;
        this.rarity = rarity;
        this.probability = probability;
        this.createdAt = LocalDateTime.now();
    }

    public void update(String name, String description, Rarity rarity, BigDecimal probability) {
        this.name = name;
        this.description = description;
        this.rarity = rarity;
        this.probability = probability;
        this.updatedAt = LocalDateTime.now();
    }

    public enum Rarity {
        COMMON, RARE, EPIC, LEGENDARY
    }
}
