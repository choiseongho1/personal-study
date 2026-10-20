package com.example.concurrencylab.work;

import com.example.concurrencylab.messaging.*;
import com.example.concurrencylab.payment.*;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@RequiredArgsConstructor
public class WorkRunner {
    private final WorkRepository repository;
    private final WorkTransactions transactions;
    private final OutboxTransactions outbox;
    private final EventTransport transport;
    private final PaymentRequestService payments;
    private final PaymentAttemptTransactions paymentTransactions;
    private final RefundProcessingService refunds;

    @Transactional(propagation = Propagation.NEVER)
    public int runOnce() {
        var ids = repository.findDue(Instant.now(), PageRequest.of(0, 50));
        for (String id : ids) run(id);
        return ids.size();
    }

    @Transactional(propagation = Propagation.NEVER)
    public void run(String id) {
        var optional = transactions.claim(id);
        if (optional.isEmpty()) return;
        var claim = optional.get();
        try {
            switch (claim.kind()) {
                case OUTBOX -> {
                    transport.send(outbox.read(claim.referenceId()));
                    // 브로커 확인 후 종료되면 이 코드가 실행되지 않아 동일 이벤트가 재발행됩니다.
                    outbox.complete(claim);
                    return;
                }
                case PAYMENT -> {
                    payments.requestPayment(claim.referenceId());
                    if (!paymentTransactions.isFinal(claim.referenceId())) {
                        transactions.retry(claim, "결제 결과 확인 대기");
                        return;
                    }
                }
                case REFUND -> {
                    RefundStatus status = refunds.process(claim.referenceId());
                    if (status == RefundStatus.FAILED) {
                        transactions.review(claim, "환불 최종 실패: 운영 확인 필요");
                        return;
                    }
                    if (status != RefundStatus.SUCCEEDED) {
                        transactions.retry(claim, "환불 결과 확인 대기");
                        return;
                    }
                }
            }
            transactions.complete(claim);
        } catch (Exception exception) {
            // 재시도 소진 후에도 REVIEW에 원인과 작업을 남깁니다. 조용히 삭제하지 않습니다.
            transactions.retry(claim, exception.toString());
        }
    }
}
