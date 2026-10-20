package com.example.concurrencylab.payment;

import java.math.BigDecimal;

/**
 * 외부 결제사의 경계만 정의합니다. 가짜 구현체는 결제 실습 단계에서 작성하세요.
 * Spring Bean으로 등록된 구현체는 아직 없습니다.
 * 학습 진행: 위 문구는 초기 틀의 설명입니다. TODO 5에서 FakePaymentGateway Bean을 추가했습니다.
 */
public interface PaymentGateway {
    PaymentResult pay(PaymentRequest request);
    PaymentResult find(String idempotencyKey);
    RefundResult refund(String providerPaymentId, String refundIdempotencyKey);
    RefundResult findRefund(String refundIdempotencyKey);

    record PaymentRequest(Long orderId, BigDecimal amount, String idempotencyKey) {}
    record PaymentResult(PaymentStatus status, String providerPaymentId) {}
    record RefundResult(RefundStatus status) {}

    enum RefundStatus {
        PENDING, UNKNOWN, SUCCEEDED, FAILED
    }
}
