package com.example.concurrencylab.payment;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefundTransactions {
    public record Call(Long id, String providerPaymentId, String idempotencyKey, RefundStatus status) {}
    private final RefundRequestRepository refunds;
    private final PaymentAttemptRepository payments;

    @Transactional(readOnly = true)
    public Call prepare(Long id) {
        var refund = refunds.findById(id).orElseThrow();
        var payment = payments.findById(refund.getPaymentAttemptId()).orElseThrow();
        if (payment.getStatus() != PaymentStatus.SUCCEEDED || payment.getProviderPaymentId() == null) {
            throw new IllegalStateException("승인 거래가 확인되지 않습니다.");
        }
        return new Call(id, payment.getProviderPaymentId(), refund.getIdempotencyKey(), refund.getStatus());
    }

    @Transactional
    public void record(Long id, RefundStatus result) {
        refunds.findByIdForUpdate(id).orElseThrow().applyResult(result);
    }

    @Transactional(readOnly = true)
    public RefundStatus status(Long id) { return refunds.findById(id).orElseThrow().getStatus(); }
}
