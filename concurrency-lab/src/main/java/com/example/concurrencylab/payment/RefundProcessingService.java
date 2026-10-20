package com.example.concurrencylab.payment;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@RequiredArgsConstructor
public class RefundProcessingService {
    private final RefundTransactions transactions;
    private final PaymentGateway gateway;

    @Transactional(propagation = Propagation.NEVER)
    public RefundStatus process(Long refundId) {
        var call = transactions.prepare(refundId);
        if (call.status() == RefundStatus.SUCCEEDED || call.status() == RefundStatus.FAILED) return call.status();
        PaymentGateway.RefundResult result;
        try {
            // 타임아웃이나 작업자 종료 뒤에는 동일 환불의 실제 결과부터 조회합니다.
            result = gateway.findRefund(call.idempotencyKey());
            if (result.status() == PaymentGateway.RefundStatus.UNKNOWN
                || result.status() == PaymentGateway.RefundStatus.PENDING) {
                result = gateway.refund(call.providerPaymentId(), call.idempotencyKey());
            }
        } catch (PaymentGatewayTimeoutException exception) {
            transactions.record(refundId, RefundStatus.UNKNOWN);
            return transactions.status(refundId);
        }
        transactions.record(refundId, RefundStatus.valueOf(result.status().name()));
        return transactions.status(refundId);
    }
}
