package com.randombox.domain.coupon;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountType discountType;

    @Column(nullable = false)
    private Integer discountValue;

    private Integer minPurchase;

    private Integer maxDiscount;

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column(nullable = false)
    private LocalDateTime endDate;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    public Coupon(String code, String name, DiscountType discountType, Integer discountValue,
                 Integer minPurchase, Integer maxDiscount, LocalDateTime startDate, LocalDateTime endDate) {
        this.code = code;
        this.name = name;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.minPurchase = minPurchase;
        this.maxDiscount = maxDiscount;
        this.startDate = startDate;
        this.endDate = endDate;
        this.createdAt = LocalDateTime.now();
    }

    public void update(String name, DiscountType discountType, Integer discountValue,
                      Integer minPurchase, Integer maxDiscount, LocalDateTime startDate, LocalDateTime endDate) {
        this.name = name;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.minPurchase = minPurchase;
        this.maxDiscount = maxDiscount;
        this.startDate = startDate;
        this.endDate = endDate;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(startDate) && now.isBefore(endDate);
    }

    public int calculateDiscount(int originalPrice) {
        if (minPurchase != null && originalPrice < minPurchase) {
            return 0;
        }

        int discount;
        if (discountType == DiscountType.PERCENTAGE) {
            discount = (int) (originalPrice * (discountValue / 100.0));
        } else {
            discount = discountValue;
        }

        if (maxDiscount != null && discount > maxDiscount) {
            discount = maxDiscount;
        }

        return Math.min(discount, originalPrice);
    }

    public enum DiscountType {
        PERCENTAGE, FIXED
    }
}
