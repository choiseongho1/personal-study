package com.example.concurrencylab.payment;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "payment_attempts",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_payment_key", columnNames = "idempotency_key"),
        @UniqueConstraint(name = "uk_payment_order", columnNames = "order_id"),
        @UniqueConstraint(name = "uk_provider_payment", columnNames = "provider_payment_id")
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PaymentStatus status;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "provider_payment_id", length = 100)
    private String providerPaymentId;

    public PaymentAttempt(Long orderId, BigDecimal amount, String idempotencyKey) {
        this.orderId = orderId;
        this.amount = amount;
        this.idempotencyKey = idempotencyKey;
        this.status = PaymentStatus.PENDING;
    }

    // TODO: 승인/실패/결과 불명 반영 메서드를 직접 작성하세요.
    public void recordSuccess(String providerPaymentId) {
        if (providerPaymentId == null || providerPaymentId.isBlank() || providerPaymentId.length() > 100) {
            throw new IllegalArgumentException("결제사 거래 ID가 올바르지 않습니다.");
        }
        if (this.providerPaymentId != null && !this.providerPaymentId.equals(providerPaymentId)) {
            throw new IllegalStateException("기존 승인과 다른 결제사 거래 ID입니다.");
        }
        if (status == PaymentStatus.FAILED) {
            throw new IllegalStateException("최종 실패와 승인 결과가 충돌합니다. 결제사 조회가 필요합니다.");
        }
        this.status = PaymentStatus.SUCCEEDED;
        this.providerPaymentId = providerPaymentId;
    }

    /**
     * @return 처음 실패를 기록하면 true, 이미 FAILED이면 false
     */
    public boolean recordFailure() {
        if (status == PaymentStatus.SUCCEEDED) {
            throw new IllegalStateException("승인과 최종 실패 결과가 충돌합니다. 결제사 조회가 필요합니다.");
        }
        if (status == PaymentStatus.FAILED) {
            return false;
        }
        this.status = PaymentStatus.FAILED;
        return true;
    }


    public void recordUnknown() {
        if (status == PaymentStatus.PENDING) status = PaymentStatus.UNKNOWN;
    }
}
