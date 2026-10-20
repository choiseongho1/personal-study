package com.example.concurrencylab.messaging;

import com.example.concurrencylab.order.OrderRepository;
import com.example.concurrencylab.payment.PaymentAttemptRepository;
import com.example.concurrencylab.work.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InboxReceiver {
    private final InboxRepository inbox;
    private final PaymentAttemptRepository payments;
    private final OrderRepository orders;
    private final WorkEnqueuer enqueuer;

    @Transactional
    public void accept(PaymentEvent event) {
        Long orderId = payments.findOrderId(event.paymentAttemptId()).orElseThrow();
        if (!orderId.equals(event.orderId())) throw new IllegalArgumentException("주문과 결제 시도가 다릅니다.");
        // 같은 결제의 서로 다른 이벤트 ID도 주문 잠금으로 직렬화합니다.
        orders.findByIdForUpdate(orderId).orElseThrow();
        if (inbox.existsById(event.eventId())) return;
        inbox.save(new InboxReceipt(event));
        enqueuer.enqueue("payment:" + event.paymentAttemptId(), WorkItem.Kind.PAYMENT, event.paymentAttemptId());
        // DB 커밋 후 리스너가 반환돼야 Kafka 오프셋이 커밋됩니다.
    }
}
