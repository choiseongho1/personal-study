package com.example.concurrencylab.payment;

import com.example.concurrencylab.order.*;
import com.example.concurrencylab.messaging.*;
import com.example.concurrencylab.work.*;
import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:remaining_flow;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.h2.console.enabled=false", "lab.workers.enabled=false", "lab.messaging=local"
})
class RemainingWorkflowTest {
    @Autowired OrderService orders;
    @Autowired PaymentRequestService requests;
    @Autowired PaymentAttemptTransactions payments;
    @Autowired PaymentAttemptRepository paymentRepository;
    @Autowired RefundRequestRepository refunds;
    @Autowired RefundProcessingService refundService;
    @Autowired OutboxRepository outbox;
    @Autowired InboxRepository inbox;
    @Autowired InboxReceiver receiver;
    @Autowired WorkRepository work;
    @Autowired WorkRunner runner;
    @Autowired WorkTransactions workTransactions;
    @Autowired OrderExpiryWorker expiry;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactionManager;
    @MockitoSpyBean FakePaymentGateway gateway;
    @MockitoSpyBean EventTransport transport;

    @BeforeEach void resetData() {
        jdbc.update("DELETE FROM work_items");
        jdbc.update("DELETE FROM inbox_receipts");
        jdbc.update("DELETE FROM outbox_events");
        jdbc.update("DELETE FROM refund_requests");
        jdbc.update("DELETE FROM payment_attempts");
        jdbc.update("DELETE FROM orders");
        jdbc.update("DELETE FROM inventory");
        jdbc.update("INSERT INTO inventory VALUES (400, 1, 10000)");
    }

    @Test void paymentCallRunsOutsideTransactionAndConfirmsOrder() {
        long id = createOrder();
        doAnswer(call -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return call.callRealMethod();
        }).when(gateway).pay(any());

        requests.requestPayment(id);
        requests.requestPayment(id); // 최종 결과 재요청은 외부 호출을 반복하지 않습니다.

        assertPayment(id, PaymentStatus.SUCCEEDED, "CONFIRMED");
        verify(gateway, times(1)).pay(any());
        assertThat(stock()).isZero();
    }

    @Test void finalFailureCancelsOrder() {
        long id = createOrder();
        gateway.configureScenario(key(id), FakePaymentGateway.Scenario.FINAL_FAILURE);
        requests.requestPayment(id);
        assertPayment(id, PaymentStatus.FAILED, "CANCELLED");
        assertThat(stock()).isEqualTo(1);
    }

    @Test void timeoutReconcilesByOriginalKey() {
        long id = createOrder();
        gateway.configureScenario(key(id), FakePaymentGateway.Scenario.TIMEOUT_AFTER_APPROVAL);
        requests.requestPayment(id);
        assertPayment(id, PaymentStatus.UNKNOWN, "PENDING");
        assertThat(stock()).isZero();

        requests.requestPayment(id); // UNKNOWN은 pay가 아니라 find로 연결됩니다.

        assertPayment(id, PaymentStatus.SUCCEEDED, "CONFIRMED");
        verify(gateway, times(1)).pay(any());
    }

    @Test void lateApprovalIsRefundedOutsideTransactionAndPreservesApprovalHistory() {
        long id = createOrder();
        gateway.configureScenario(key(id), FakePaymentGateway.Scenario.TIMEOUT_AFTER_APPROVAL);
        requests.requestPayment(id);
        pastDeadline(id);
        expiry.runOnce();
        requests.reconcilePayment(id);

        var refund = refunds.findByPaymentAttemptId(id).orElseThrow();
        doAnswer(call -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return call.callRealMethod();
        }).when(gateway).refund(anyString(), anyString());

        runner.run("refund:" + refund.getId());

        assertThat(refunds.findById(refund.getId()).orElseThrow().getStatus()).isEqualTo(RefundStatus.SUCCEEDED);
        assertPayment(id, PaymentStatus.SUCCEEDED, "EXPIRED");
        assertThat(stock()).isEqualTo(1);
        assertThat(work.findById("refund:" + refund.getId()).orElseThrow().getStatus()).isEqualTo(WorkItem.Status.DONE);
    }

    @Test void refundTimeoutIsRecoveredAfterLeaseAndRetryWithoutDuplicateRefund() {
        long id = lateRefund();
        var refund = refunds.findByPaymentAttemptId(id).orElseThrow();
        String workId = "refund:" + refund.getId();
        gateway.configureRefundScenario(refund.getIdempotencyKey(), FakePaymentGateway.Scenario.TIMEOUT_AFTER_APPROVAL);

        runner.run(workId);
        assertThat(refunds.findById(refund.getId()).orElseThrow().getStatus()).isEqualTo(RefundStatus.UNKNOWN);
        makeDue(workId);
        runner.run(workId);

        assertThat(refunds.findById(refund.getId()).orElseThrow().getStatus()).isEqualTo(RefundStatus.SUCCEEDED);
        verify(gateway, times(1)).refund(anyString(), eq(refund.getIdempotencyKey()));
    }

    @Test void finalRefundFailureRemainsVisibleForReview() {
        long id = lateRefund();
        var refund = refunds.findByPaymentAttemptId(id).orElseThrow();
        gateway.configureRefundScenario(refund.getIdempotencyKey(), FakePaymentGateway.Scenario.FINAL_FAILURE);
        runner.run("refund:" + refund.getId());
        assertThat(refunds.findById(refund.getId()).orElseThrow().getStatus()).isEqualTo(RefundStatus.FAILED);
        assertThat(work.findById("refund:" + refund.getId()).orElseThrow().getStatus()).isEqualTo(WorkItem.Status.REVIEW);
    }

    @Test void timeoutCannotOverwriteFinalState() {
        long id = createOrder();
        requests.requestPayment(id);
        payments.markUnknown(id);
        assertPayment(id, PaymentStatus.SUCCEEDED, "CONFIRMED");
    }

    @Test void expiredOrderIsQueriedButNeverNewlyCharged() {
        long id = createOrder();
        pastDeadline(id);
        expiry.runOnce();
        requests.requestPayment(id);
        verify(gateway, never()).pay(any());
        assertPayment(id, PaymentStatus.UNKNOWN, "EXPIRED");
        assertThat(stock()).isEqualTo(1);
        assertThat(refunds.count()).isZero();
    }

    @Test void expiryWorkerIsIdempotent() {
        long id = createOrder();
        pastDeadline(id);
        assertThat(expiry.runOnce()).isEqualTo(1);
        assertThat(expiry.runOnce()).isZero();
        assertThat(stock()).isEqualTo(1);
    }

    @Test void localOutboxCreatesDurableJobBeforeExecutingPayment() {
        long id = createOrder();
        var event = outbox.findAll().getFirst();
        runner.run("outbox:" + event.getEventId());
        assertThat(inbox.count()).isEqualTo(1);
        assertThat(work.existsById("payment:" + id)).isTrue();
        assertPayment(id, PaymentStatus.PENDING, "PENDING");

        runner.run("payment:" + id);

        assertPayment(id, PaymentStatus.SUCCEEDED, "CONFIRMED");
    }

    @Test void publishSuccessBeforeCompletionCrashCanRedeliverSafely() {
        long id = createOrder();
        var event = outbox.findAll().getFirst();
        String workId = "outbox:" + event.getEventId();
        doAnswer(call -> {
            call.callRealMethod(); // 수신 DB 저장까지 성공했지만 확인 응답이 유실된 상황
            throw new IllegalStateException("전송 확인 유실");
        }).when(transport).send(any());

        runner.run(workId);
        assertThat(inbox.count()).isEqualTo(1);
        assertThat(outbox.findById(event.getId()).orElseThrow().getPublishedAt()).isNull();
        doCallRealMethod().when(transport).send(any());
        makeDue(workId);
        runner.run(workId);

        assertThat(inbox.count()).isEqualTo(1);
        assertThat(work.existsById("payment:" + id)).isTrue();
        assertThat(outbox.findById(event.getId()).orElseThrow().getPublishedAt()).isNotNull();
    }

    @Test void differentEventsForSamePaymentStillCreateOneJob() {
        long id = createOrder();
        long orderId = paymentRepository.findById(id).orElseThrow().getOrderId();
        receiver.accept(new PaymentEvent(UUID.randomUUID().toString(), id, orderId));
        receiver.accept(new PaymentEvent(UUID.randomUUID().toString(), id, orderId));
        assertThat(inbox.count()).isEqualTo(2);
        assertThat(work.findAll().stream().filter(w -> w.getKind() == WorkItem.Kind.PAYMENT).count()).isEqualTo(1);
    }

    @Test void staleWorkerCannotCompleteReclaimedLease() {
        createOrder();
        String id = work.findAll().getFirst().getId();
        var oldClaim = workTransactions.claim(id).orElseThrow();
        assertThat(workTransactions.claim(id)).isEmpty();
        jdbc.update("UPDATE work_items SET lease_until = ? WHERE id = ?", utcPast(), id);
        var newClaim = workTransactions.claim(id).orElseThrow();
        assertThat(workTransactions.complete(oldClaim)).isFalse();
        assertThat(workTransactions.complete(newClaim)).isTrue();
    }

    @Test void exhaustedRetriesPreserveWorkForReview() {
        createOrder();
        String id = work.findAll().getFirst().getId();
        jdbc.update("UPDATE work_items SET attempts = 9 WHERE id = ?", id);
        doThrow(new IllegalStateException("broker offline")).when(transport).send(any());
        runner.run(id);
        var item = work.findById(id).orElseThrow();
        assertThat(item.getStatus()).isEqualTo(WorkItem.Status.REVIEW);
        assertThat(item.getLastError()).contains("broker offline");
    }

    @Test void failedOrderRollsBackOutboxAndPaymentAttempt() {
        jdbc.update("UPDATE inventory SET available = 0");
        assertThatThrownBy(() -> orders.placeOrder(1L, 400L)).isInstanceOf(RuntimeException.class);
        assertThat(paymentRepository.count()).isZero();
        assertThat(outbox.count()).isZero();
        assertThat(work.count()).isZero();
    }

    @Test void outerTransactionCannotWrapGatewayOrchestration() {
        long id = createOrder();
        var template = new TransactionTemplate(transactionManager);
        assertThatThrownBy(() -> template.executeWithoutResult(status -> requests.requestPayment(id)))
            .isInstanceOf(org.springframework.transaction.IllegalTransactionStateException.class);
        verify(gateway, never()).pay(any());
    }

    private long lateRefund() {
        long id = createOrder();
        gateway.configureScenario(key(id), FakePaymentGateway.Scenario.TIMEOUT_AFTER_APPROVAL);
        requests.requestPayment(id);
        pastDeadline(id);
        requests.reconcilePayment(id);
        return id;
    }
    private long createOrder() {
        long orderId = orders.placeOrder(1L, 400L);
        return paymentRepository.findByOrderId(orderId).orElseThrow().getId();
    }
    private String key(long id) { return paymentRepository.findById(id).orElseThrow().getIdempotencyKey(); }
    private void assertPayment(long id, PaymentStatus status, String orderStatus) {
        var payment = paymentRepository.findById(id).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(status);
        assertThat(jdbc.queryForObject("SELECT status FROM orders WHERE id = ?", String.class, payment.getOrderId())).isEqualTo(orderStatus);
    }
    private int stock() { return jdbc.queryForObject("SELECT available FROM inventory WHERE event_id=400", Integer.class); }
    private LocalDateTime utcPast() { return LocalDateTime.ofInstant(Instant.now().minusSeconds(60), ZoneOffset.UTC); }
    private void pastDeadline(long id) {
        jdbc.update("UPDATE orders SET expires_at = ? WHERE id = ?", utcPast(), paymentRepository.findById(id).orElseThrow().getOrderId());
    }
    private void makeDue(String id) { jdbc.update("UPDATE work_items SET next_attempt_at = ? WHERE id = ?", utcPast(), id); }
}
