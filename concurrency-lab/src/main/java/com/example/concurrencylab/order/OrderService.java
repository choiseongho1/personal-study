package com.example.concurrencylab.order;

import com.example.concurrencylab.common.ExerciseNotImplementedException;
import com.example.concurrencylab.inventory.InventoryRepository;
import com.example.concurrencylab.payment.PaymentAttempt;
import com.example.concurrencylab.payment.PaymentAttemptRepository;
import com.example.concurrencylab.payment.PaymentStatus;
import com.example.concurrencylab.payment.RefundRequest;
import com.example.concurrencylab.payment.RefundRequestRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final InventoryRepository inventoryRepository;
    private final OrderRepository orderRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final RefundRequestRepository refundRequestRepository;
    private final com.example.concurrencylab.messaging.OutboxTransactions outbox;
    private final com.example.concurrencylab.work.WorkEnqueuer enqueuer;

    @Transactional
    public Long placeOrder(Long userId, Long eventId) {
        // TODO 1: 재고 예약 및 주문 생성. 트랜잭션 경계도 직접 결정하세요.
        // 1. 재고 1개 예약 — 실패하면 예외 발생
        // 2. 만료 시각 설정
        // 힌트: Instant.now().plus(Duration.ofMinutes(10))
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(10));
        PurchaseOrder purchaseOrder = new PurchaseOrder(userId, eventId, expiresAt);
        // 3. PurchaseOrder 생성 후 Repository에 저장
        PurchaseOrder newPurchaseOrder = orderRepository.save(purchaseOrder);

        int result = inventoryRepository.decreaseIfAvailable(eventId);
        if (result != 1) {
            throw new RuntimeException("재고를 예약할 수 없습니다.");
        }

        var inventory = inventoryRepository.findById(eventId).orElseThrow();
        // 주문·재고 예약·결제 시도를 같은 트랜잭션에 저장합니다.
        PaymentAttempt paymentAttempt = paymentAttemptRepository.save(new PaymentAttempt(
            newPurchaseOrder.getId(), inventory.getUnitPrice(), UUID.randomUUID().toString()
        ));
        // TODO 10: 결제 요청 의도와 주문을 같은 트랜잭션에 저장합니다.
        outbox.create(paymentAttempt.getId(), newPurchaseOrder.getId());
        // 4. 저장된 주문 ID 반환
        return newPurchaseOrder.getId();
//        throw new ExerciseNotImplementedException("주문 생성");
    }

    @Transactional
    public void expireOrder(Long orderId) {
        // TODO 2: 만료 판정 및 재고 반환.

        // 1. 주문 조회 — 재고를 반환할 eventId 확인
        PurchaseOrder order = orderRepository.findById(orderId).orElseThrow();
        // 2. PENDING이고 기한이 지난 경우에만 EXPIRED로 변경
        Instant now = Instant.now();
        if (order.getStatus() == OrderStatus.PENDING && !order.getExpiresAt().isAfter(now)) {
            int result = orderRepository.updateStatus(orderId, OrderStatus.EXPIRED, now);
            // 3. 변경된 행이 1개일 때만 재고 1개 반환
            if (result == 1) {
                restoreStock(order.getEventId());
            }
        }
    }

    /**
     * 로컬 실습용 승인 결과 반영. 실제 결제사를 호출하지 않습니다.
     * 실제 웹훅에서는 서명·금액·통화·거래 식별자 검증 후 호출해야 합니다.
     */
    @Transactional
    public void paymentSucceeded(Long paymentAttemptId, String providerPaymentId) {
        // TODO 3: 승인 반영. 만료된 주문에서의 후속 처리도 설계하세요.

        // 1. 결제 시도 조회 → 연결된 주문 확인
        // 먼저 주문 ID만 조회합니다. 잠금 대기 전에 결제 엔티티를 캐시하지 않습니다.
        Long orderId = paymentAttemptRepository.findOrderId(paymentAttemptId)
            .orElseThrow(() -> new EntityNotFoundException("결제 시도가 없습니다."));

        // 동일 주문의 승인 요청들을 직렬화합니다.
        // 다른 결제 결과 처리도 주문 → 결제 시도 순서로 잠가야 합니다.
        PurchaseOrder order = orderRepository.findByIdForUpdate(orderId)
            .orElseThrow(() -> new EntityNotFoundException("주문이 없습니다."));
        PaymentAttempt payment = paymentAttemptRepository.findByIdForUpdate(paymentAttemptId)
            .orElseThrow(() -> new EntityNotFoundException("결제 시도가 없습니다."));

        // 2. 결제 승인 사실 기록
        boolean alreadySucceeded = payment.getStatus() == PaymentStatus.SUCCEEDED;
        payment.recordSuccess(providerPaymentId);
        if (alreadySucceeded) {
            // 이전 성공 트랜잭션에서 확정 또는 환불 요청 저장까지 완료했습니다.
            return;
        }

        // 3. 주문 상태와 기한에 따라 위 표의 처리 수행
        Instant now = Instant.now();
        if (order.getStatus() == OrderStatus.PENDING) {
            int confirmed = orderRepository.confirmIfPendingAndNotExpired(orderId, now);
            if (confirmed == 1) {
                return;
            }
            // 기한이 지났지만 만료 작업이 아직 실행되지 않은 경우 직접 만료합니다.
            int expired = orderRepository.updateStatus(orderId, OrderStatus.EXPIRED, now);
            if (expired != 1) {
                throw new IllegalStateException("주문 상태를 반영할 수 없습니다.");
            }
            restoreStock(order.getEventId());
        } else if (order.getStatus() == OrderStatus.CONFIRMED) {
            // 단일 결제 시도 모델에서 승인 기록 없는 확정 주문은 정상 상태가 아닙니다.
            throw new IllegalStateException("확정 주문의 결제 기록을 확인해야 합니다.");
        }
        // EXPIRED / CANCELLED는 주문을 유지하며 재고를 다시 반환하지 않습니다.
        // 승인 사실과 환불 요청을 함께 커밋합니다. 외부 환불 호출은 이후 작업입니다.
        RefundRequest refund = refundRequestRepository.save(new RefundRequest(paymentAttemptId, now));
        enqueuer.enqueue("refund:" + refund.getId(), com.example.concurrencylab.work.WorkItem.Kind.REFUND, refund.getId());
    }

    @Transactional
    public void paymentFailed(Long paymentAttemptId) {
        // TODO 4: 최종 실패 반영 및 필요한 재고 반환.

        // 1. 주문 ID 조회
        Long orderId = paymentAttemptRepository.findOrderId(paymentAttemptId)
            .orElseThrow(() -> new EntityNotFoundException("결제 시도가 없습니다."));

        // 2. 주문 → 결제 시도 순서로 잠금
        PurchaseOrder order = orderRepository.findByIdForUpdate(orderId)
            .orElseThrow(() -> new EntityNotFoundException("주문이 없습니다."));
        PaymentAttempt payment = paymentAttemptRepository.findByIdForUpdate(paymentAttemptId)
            .orElseThrow(() -> new EntityNotFoundException("결제 시도가 없습니다."));

        // 3. 결제 상태 확인 후 FAILED 기록
        // 이미 성공한 결제를 실패로 덮어쓰지 않습니다. 중복 실패는 종료합니다.
        if (!payment.recordFailure()) {
            return;
        }
        if (order.getStatus() == OrderStatus.CONFIRMED) {
            throw new IllegalStateException("확정 주문의 결제 기록을 확인해야 합니다.");
        }

        // 4. PENDING 주문만 CANCELLED로 변경
        // 최종 실패는 기한 전에도 발생하므로 만료 쿼리와 분리합니다.
        int cancelled = orderRepository.cancelIfPending(orderId);

        // 5. 주문 변경 성공 시 재고 반환
        // EXPIRED / CANCELLED 주문은 이미 재고를 반환했으므로 다시 반환하지 않습니다.
        if (cancelled == 1) {
            restoreStock(order.getEventId());
        }
    }


    private void restoreStock(Long eventId) {
        if (inventoryRepository.increase(eventId) != 1) {
            throw new IllegalStateException("재고를 복구할 수 없습니다.");
        }
    }
}
