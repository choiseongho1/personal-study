package com.example.concurrencylab.payment;

import com.example.concurrencylab.common.ExerciseNotImplementedException;
import com.example.concurrencylab.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 짧은 DB 작업을 담당하는 별도 Bean입니다.
 * PaymentRequestService와 분리해 Spring 프록시를 통한 트랜잭션 경계를 만듭니다.
 */
@Service
@RequiredArgsConstructor
public class PaymentAttemptTransactions {
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public Preparation prepareRequest(Long paymentAttemptId) {
        // TODO 6-1: 결제 시도와 주문을 조회하고 요청 가능한 상태인지 확인하세요.
        // 힌트: 확정/만료/취소 주문에 새 결제를 요청하지 않습니다.
        // 힌트: UNKNOWN이면 새 결제 요청보다 TODO 7의 결과 조회로 연결합니다.
        // 힌트: 조회 직후 주문이 만료될 수 있으므로 이 검사만으로 정합성이 보장되지는 않습니다.

        // TODO 6-2: 저장된 orderId, amount, idempotencyKey로 불변 DTO를 만들어 반환하세요.
        // 엔티티를 반환하지 않고 기존 PaymentRequest record를 사용합니다.
        var payment = paymentAttemptRepository.findById(paymentAttemptId).orElseThrow();
        var order = orderRepository.findById(payment.getOrderId()).orElseThrow();
        var request = new PaymentGateway.PaymentRequest(payment.getOrderId(), payment.getAmount(), payment.getIdempotencyKey());
        Action action = Action.PAY;
        if (payment.getStatus() == PaymentStatus.SUCCEEDED || payment.getStatus() == PaymentStatus.FAILED) {
            action = Action.SKIP;
        } else if (payment.getStatus() == PaymentStatus.UNKNOWN
            || order.getStatus() != com.example.concurrencylab.order.OrderStatus.PENDING
            || !order.getExpiresAt().isAfter(java.time.Instant.now())) {
            action = Action.QUERY;
        }
        return new Preparation(request, action);
    }

    @Transactional(readOnly = true)
    public String findIdempotencyKey(Long paymentAttemptId) {
        // TODO 7-2: 주문 기한이나 상태에 관계없이 원래 결제의 조회 키를 반환하세요.
        // 만료된 주문도 늦은 승인 여부를 확인해야 하므로 prepareRequest와 분리합니다.
        return paymentAttemptRepository.findById(paymentAttemptId).orElseThrow().getIdempotencyKey();
    }

    @Transactional
    public void markUnknown(Long paymentAttemptId) {
        // TODO 7-3: 주문 → 결제 시도 순서로 잠그고 PENDING만 UNKNOWN으로 변경하세요.
        // 타임아웃 직후 웹훅으로 SUCCEEDED/FAILED가 반영됐을 수 있습니다.
        // 최종 상태는 UNKNOWN으로 되돌리지 않으며 주문·재고는 변경하지 않습니다.
        Long orderId = paymentAttemptRepository.findOrderId(paymentAttemptId).orElseThrow();
        orderRepository.findByIdForUpdate(orderId).orElseThrow();
        paymentAttemptRepository.findByIdForUpdate(paymentAttemptId).orElseThrow().recordUnknown();
    }

    public enum Action { PAY, QUERY, SKIP }
    public record Preparation(PaymentGateway.PaymentRequest request, Action action) {}
    @Transactional(readOnly = true)
    public boolean isFinal(Long id) {
        var status = paymentAttemptRepository.findById(id).orElseThrow().getStatus();
        return status == PaymentStatus.SUCCEEDED || status == PaymentStatus.FAILED;
    }
}
