package com.randombox.domain.purchase;

import com.randombox.domain.randombox.RandomBoxItem;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "purchase_results")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PurchaseResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_id", nullable = false)
    private Purchase purchase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "random_box_item_id", nullable = false)
    private RandomBoxItem randomBoxItem;

    private LocalDateTime createdAt;

    @Builder
    public PurchaseResult(Purchase purchase, RandomBoxItem randomBoxItem) {
        this.purchase = purchase;
        this.randomBoxItem = randomBoxItem;
        this.createdAt = LocalDateTime.now();
    }
}
