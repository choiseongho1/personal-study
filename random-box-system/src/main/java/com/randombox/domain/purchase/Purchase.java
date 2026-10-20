package com.randombox.domain.purchase;

import com.randombox.domain.randombox.RandomBox;
import com.randombox.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "purchases")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Purchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "random_box_id", nullable = false)
    private RandomBox randomBox;

    @Column(nullable = false)
    private LocalDateTime purchaseDateTime;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Integer totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PurchaseStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    public Purchase(User user, RandomBox randomBox, Integer quantity, Integer totalPrice) {
        this.user = user;
        this.randomBox = randomBox;
        this.purchaseDateTime = LocalDateTime.now();
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.status = PurchaseStatus.COMPLETED;
        this.createdAt = LocalDateTime.now();
    }

    public void cancel() {
        if (this.status == PurchaseStatus.CANCELLED) {
            throw new IllegalStateException("이미 취소된 구매입니다.");
        }
        this.status = PurchaseStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    public enum PurchaseStatus {
        COMPLETED, CANCELLED
    }
}
