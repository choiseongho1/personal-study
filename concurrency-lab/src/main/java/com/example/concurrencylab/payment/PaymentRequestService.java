package com.example.concurrencylab.payment;

import com.example.concurrencylab.common.ExerciseNotImplementedException;
import com.example.concurrencylab.order.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 외부 호출의 순서만 조율합니다. DB 작업은 별도 Bean에 위임합니다.
 * NEVER는 트랜잭션이 열린 상태에서 잘못 호출되면 즉시 오류로 알립니다.
 */
@Service
@RequiredArgsConstructor
public class PaymentRequestService {
    private final PaymentGateway paymentGateway;
    private final PaymentAttemptTransactions paymentTransactions;
    private final OrderService orderService;

    @Transactional(propagation = Propagation.NEVER)
    public void requestPayment(Long paymentAttemptId) {
        // TODO 6-3: paymentTransactions.prepareRequest()로 DTO를 받고 조회 트랜잭션 종료.
        // TODO 6-4: paymentGateway.pay()를 DB 트랜잭션 밖에서 호출하세요.
        // TODO 6-5: SUCCEEDED → orderService.paymentSucceeded()
        //           FAILED → orderService.paymentFailed()
        //           PENDING/UNKNOWN → paymentTransactions.markUnknown()
        // 중요: orderService의 메서드는 각자 별도 DB 트랜잭션으로 실행됩니다.

        // TODO 7-4: PaymentGatewayTimeoutException은 markUnknown()으로 연결하세요.
        // 힌트: catch 범위는 외부 pay() 호출에 한정합니다.
        //       결과 반영 중 DB 오류까지 결제 타임아웃으로 취급하지 마세요.
        // 다른 RuntimeException을 일괄적으로 paymentFailed()에 연결하지 마세요.
        var preparation = paymentTransactions.prepareRequest(paymentAttemptId);
        if (preparation.action() == PaymentAttemptTransactions.Action.SKIP) return;
        if (preparation.action() == PaymentAttemptTransactions.Action.QUERY) {
            reconcilePayment(paymentAttemptId);
            return;
        }
        PaymentGateway.PaymentResult result;
        try {
            result = paymentGateway.pay(preparation.request());
        } catch (PaymentGatewayTimeoutException exception) {
            paymentTransactions.markUnknown(paymentAttemptId);
            return;
        }
        applyResult(paymentAttemptId, result);
    }

    @Transactional(propagation = Propagation.NEVER)
    public void reconcilePayment(Long paymentAttemptId) {
        // TODO 7-5: findIdempotencyKey()로 원래 키 조회 → paymentGateway.find() 호출.
        // 실제 성공/최종 실패가 확인되면 기존 OrderService로 결과를 반영하세요.
        // UNKNOWN/PENDING이면 결과 확인이 더 필요합니다. 재고를 임의 복구하지 않습니다.
        // 만료 뒤 성공이면 기존 paymentSucceeded()가 환불 대기 기록을 만듭니다.
        String key = paymentTransactions.findIdempotencyKey(paymentAttemptId);
        PaymentGateway.PaymentResult result;
        try {
            result = paymentGateway.find(key);
        } catch (PaymentGatewayTimeoutException exception) {
            paymentTransactions.markUnknown(paymentAttemptId);
            return;
        }
        applyResult(paymentAttemptId, result);
    }

    private void applyResult(Long attemptId, PaymentGateway.PaymentResult result) {
        switch (result.status()) {
            case SUCCEEDED -> orderService.paymentSucceeded(attemptId, result.providerPaymentId());
            case FAILED -> orderService.paymentFailed(attemptId);
            case PENDING, UNKNOWN -> paymentTransactions.markUnknown(attemptId);
        }
    }
}
