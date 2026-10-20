package com.example.concurrencylab.messaging;

import com.example.concurrencylab.work.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@RequiredArgsConstructor
public class OutboxTransactions {
    private final OutboxRepository repository;
    private final WorkEnqueuer enqueuer;
    private final WorkTransactions workTransactions;

    @Transactional(propagation = Propagation.MANDATORY)
    public void create(Long paymentAttemptId, Long orderId) {
        var event = repository.save(new OutboxEvent(paymentAttemptId, orderId));
        enqueuer.enqueue("outbox:" + event.getEventId(), WorkItem.Kind.OUTBOX, event.getId());
    }
    @Transactional(readOnly = true)
    public PaymentEvent read(Long id) { return repository.findById(id).orElseThrow().message(); }

    @Transactional
    public void complete(WorkTransactions.Claim claim) {
        // 오래된 작업자가 새 작업자의 완료 표시를 덮어쓰지 못합니다.
        if (workTransactions.complete(claim)) {
            repository.findById(claim.referenceId()).orElseThrow().markPublished();
        }
    }
}
