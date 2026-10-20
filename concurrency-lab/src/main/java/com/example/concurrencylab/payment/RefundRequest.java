package com.example.concurrencylab.payment;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 외부 환불을 나중에 실행하기 위한 영속 작업 기록입니다. */
@Entity
@Table(name = "refund_requests",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_refund_payment", columnNames = "payment_attempt_id"),
        @UniqueConstraint(name = "uk_refund_key", columnNames = "idempotency_key")
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefundRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_attempt_id", nullable = false)
    private Long paymentAttemptId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RefundStatus status;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, columnDefinition = "datetime(6)")
    private Instant createdAt;

    public RefundRequest(Long paymentAttemptId, Instant createdAt) {
        this.paymentAttemptId = paymentAttemptId;
        this.status = RefundStatus.PENDING;
        this.idempotencyKey = "refund:" + paymentAttemptId;
        this.createdAt = createdAt;
    }

    public void applyResult(RefundStatus result) {
        if (status == RefundStatus.SUCCEEDED || status == RefundStatus.FAILED) return;
        status = java.util.Objects.requireNonNull(result);
    }
}
