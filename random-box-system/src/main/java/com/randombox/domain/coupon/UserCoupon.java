package com.randombox.domain.coupon;

import com.randombox.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_coupons")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCoupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @Column(nullable = false)
    private boolean used;

    private LocalDateTime usedDate;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    public UserCoupon(User user, Coupon coupon) {
        this.user = user;
        this.coupon = coupon;
        this.used = false;
        this.createdAt = LocalDateTime.now();
    }

    public void use() {
        if (this.used) {
            throw new IllegalStateException("이미 사용된 쿠폰입니다.");
        }
        if (!this.coupon.isValid()) {
            throw new IllegalStateException("유효하지 않은 쿠폰입니다.");
        }
        this.used = true;
        this.usedDate = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void cancel() {
        if (!this.used) {
            throw new IllegalStateException("사용되지 않은 쿠폰입니다.");
        }
        this.used = false;
        this.usedDate = null;
        this.updatedAt = LocalDateTime.now();
    }
}
