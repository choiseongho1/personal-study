package com.example.concurrencylab.order;

import com.example.concurrencylab.payment.PaymentAttemptRepository;
import com.example.concurrencylab.payment.PaymentStatus;
import com.example.concurrencylab.payment.RefundRequestRepository;
import com.example.concurrencylab.payment.RefundStatus;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:payment_success_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.h2.console.enabled=false"
})
class PaymentSuccessTest {
    private static final Long EVENT_ID = 200L;

    @Autowired OrderService orderService;
    @Autowired OrderRepository orderRepository;
    @Autowired PaymentAttemptRepository paymentRepository;
    @Autowired RefundRequestRepository refundRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM refund_requests");
        jdbcTemplate.update("DELETE FROM work_items");
        jdbcTemplate.update("DELETE FROM inbox_receipts");
        jdbcTemplate.update("DELETE FROM outbox_events");
        jdbcTemplate.update("DELETE FROM payment_attempts");
        jdbcTemplate.update("DELETE FROM orders");
        jdbcTemplate.update("DELETE FROM inventory");
        jdbcTemplate.update(
            "INSERT INTO inventory(event_id, available, unit_price) VALUES (?, 1, 10000)", EVENT_ID
        );
    }

    @Test
    @DisplayName("주문 생성 시 금액과 멱등성 키를 가진 결제 시도가 저장된다")
    void createsPaymentAttemptWithOrder() {
        Long orderId = orderService.placeOrder(1L, EVENT_ID);
        var payment = paymentRepository.findByOrderId(orderId).orElseThrow();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getAmount()).isEqualByComparingTo("10000.00");
        assertThat(payment.getIdempotencyKey()).isNotBlank();
        assertThat(stock()).isZero();
    }

    @Test
    @DisplayName("기한 전 승인은 주문을 확정하고 재고를 그대로 유지한다")
    void confirmsValidReservation() {
        var fixture = createOrder();

        orderService.paymentSucceeded(fixture.attemptId(), "provider-1");
        orderService.expireOrder(fixture.orderId());

        assertOrder(fixture, OrderStatus.CONFIRMED);
        assertPaymentSucceeded(fixture, "provider-1");
        assertThat(stock()).isZero();
        assertThat(refundRepository.count()).isZero();
    }

    @Test
    @DisplayName("동일 승인 재전송은 주문과 재고에 추가 영향을 주지 않는다")
    void ignoresDuplicateSuccess() {
        var fixture = createOrder();

        orderService.paymentSucceeded(fixture.attemptId(), "provider-1");
        orderService.paymentSucceeded(fixture.attemptId(), "provider-1");

        assertOrder(fixture, OrderStatus.CONFIRMED);
        assertThat(stock()).isZero();
        assertThat(refundRepository.count()).isZero();
    }

    @Test
    @DisplayName("이미 만료된 주문의 승인은 재고 추가 반환 없이 환불 요청을 남긴다")
    void queuesRefundForExpiredOrder() {
        var fixture = createOrder();
        makePastDeadline(fixture);
        orderService.expireOrder(fixture.orderId());
        assertThat(stock()).isEqualTo(1);

        orderService.paymentSucceeded(fixture.attemptId(), "provider-late");
        orderService.paymentSucceeded(fixture.attemptId(), "provider-late");

        assertLateSuccess(fixture, "provider-late");
    }

    @Test
    @DisplayName("만료 배치 전이라도 기한이 지난 승인은 직접 만료하고 환불을 준비한다")
    void expiresLatePendingOrder() {
        var fixture = createOrder();
        makePastDeadline(fixture);

        orderService.paymentSucceeded(fixture.attemptId(), "provider-late");

        assertLateSuccess(fixture, "provider-late");
    }

    @Test
    @DisplayName("취소 주문의 승인은 주문을 유지하고 환불 요청만 남긴다")
    void queuesRefundForCancelledOrder() {
        var fixture = createOrder();
        // TODO 4가 아직 없으므로 취소와 재고 반환이 끝난 상태를 준비합니다.
        jdbcTemplate.update("UPDATE orders SET status = 'CANCELLED' WHERE id = ?", fixture.orderId());
        jdbcTemplate.update("UPDATE inventory SET available = 1 WHERE event_id = ?", EVENT_ID);

        orderService.paymentSucceeded(fixture.attemptId(), "provider-cancelled");

        assertOrder(fixture, OrderStatus.CANCELLED);
        assertPaymentSucceeded(fixture, "provider-cancelled");
        assertThat(stock()).isEqualTo(1);
        assertThat(refundRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("결과 불명 상태도 승인 확인 후 성공으로 반영한다")
    void resolvesUnknownPayment() {
        var fixture = createOrder();
        jdbcTemplate.update(
            "UPDATE payment_attempts SET status = 'UNKNOWN' WHERE id = ?", fixture.attemptId()
        );

        orderService.paymentSucceeded(fixture.attemptId(), "provider-unknown");

        assertOrder(fixture, OrderStatus.CONFIRMED);
        assertPaymentSucceeded(fixture, "provider-unknown");
    }

    @Test
    @DisplayName("중복 성공에 다른 거래 ID가 오면 기존 기록을 변경하지 않는다")
    void rejectsConflictingProviderId() {
        var fixture = createOrder();
        orderService.paymentSucceeded(fixture.attemptId(), "provider-1");

        assertThatThrownBy(() -> orderService.paymentSucceeded(fixture.attemptId(), "provider-other"))
            .isInstanceOf(IllegalStateException.class);

        assertOrder(fixture, OrderStatus.CONFIRMED);
        assertPaymentSucceeded(fixture, "provider-1");
        assertThat(refundRepository.count()).isZero();
    }

    @Test
    @DisplayName("다른 결제에 사용된 거래 ID면 승인과 주문 확정이 함께 롤백된다")
    void rollsBackOnDuplicateProviderTransaction() {
        jdbcTemplate.update("UPDATE inventory SET available = 2 WHERE event_id = ?", EVENT_ID);
        var first = createOrder();
        Long secondOrderId = orderService.placeOrder(2L, EVENT_ID);
        var second = new Fixture(secondOrderId,
            paymentRepository.findByOrderId(secondOrderId).orElseThrow().getId());
        orderService.paymentSucceeded(first.attemptId(), "shared-provider-id");

        assertThatThrownBy(() -> orderService.paymentSucceeded(second.attemptId(), "shared-provider-id"))
            .isInstanceOf(DataIntegrityViolationException.class);

        assertOrder(second, OrderStatus.PENDING);
        var payment = paymentRepository.findById(second.attemptId()).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getProviderPaymentId()).isNull();
        assertThat(refundRepository.count()).isZero();
    }

    @Test
    @DisplayName("최종 실패와 승인 결과가 충돌하면 조회가 필요하므로 자동 반영하지 않는다")
    void rejectsSuccessConflictingWithFinalFailure() {
        var fixture = createOrder();
        jdbcTemplate.update(
            "UPDATE payment_attempts SET status = 'FAILED' WHERE id = ?", fixture.attemptId()
        );

        assertThatThrownBy(() -> orderService.paymentSucceeded(fixture.attemptId(), "provider-conflict"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("결제사 조회");

        assertOrder(fixture, OrderStatus.PENDING);
        assertThat(paymentRepository.findById(fixture.attemptId()).orElseThrow().getStatus())
            .isEqualTo(PaymentStatus.FAILED);
        assertThat(refundRepository.count()).isZero();
    }

    @Test
    @Timeout(40)
    @DisplayName("같은 승인을 동시에 처리해도 주문은 한 번만 확정된다")
    void handlesConcurrentDuplicateSuccess() throws Exception {
        var fixture = createOrder();

        runTogether(
            () -> orderService.paymentSucceeded(fixture.attemptId(), "provider-1"),
            () -> orderService.paymentSucceeded(fixture.attemptId(), "provider-1")
        );

        assertOrder(fixture, OrderStatus.CONFIRMED);
        assertPaymentSucceeded(fixture, "provider-1");
        assertThat(stock()).isZero();
        assertThat(refundRepository.count()).isZero();
    }

    @Test
    @Timeout(40)
    @DisplayName("만료와 늦은 승인이 경합해도 재고는 한 번 반환하고 환불 요청은 하나 남긴다")
    void handlesExpirationRacingWithLateSuccess() throws Exception {
        var fixture = createOrder();
        makePastDeadline(fixture);

        runTogether(
            () -> orderService.expireOrder(fixture.orderId()),
            () -> orderService.paymentSucceeded(fixture.attemptId(), "provider-race")
        );

        assertLateSuccess(fixture, "provider-race");
    }

    private Fixture createOrder() {
        Long orderId = orderService.placeOrder(1L, EVENT_ID);
        return new Fixture(orderId, paymentRepository.findByOrderId(orderId).orElseThrow().getId());
    }

    private void makePastDeadline(Fixture fixture) {
        // Hibernate는 DATETIME에 UTC를 저장하므로 JDBC 준비 데이터도 UTC로 맞춥니다.
        jdbcTemplate.update("UPDATE orders SET expires_at = ? WHERE id = ?",
            LocalDateTime.ofInstant(Instant.now().minusSeconds(60), ZoneOffset.UTC), fixture.orderId());
    }

    private void assertLateSuccess(Fixture fixture, String providerId) {
        assertOrder(fixture, OrderStatus.EXPIRED);
        assertPaymentSucceeded(fixture, providerId);
        assertThat(stock()).isEqualTo(1);
        assertThat(refundRepository.count()).isEqualTo(1);
        var refund = refundRepository.findByPaymentAttemptId(fixture.attemptId()).orElseThrow();
        assertThat(refund.getStatus()).isEqualTo(RefundStatus.PENDING);
        assertThat(refund.getIdempotencyKey()).isEqualTo("refund:" + fixture.attemptId());
    }

    private void assertOrder(Fixture fixture, OrderStatus status) {
        assertThat(orderRepository.findById(fixture.orderId()).orElseThrow().getStatus()).isEqualTo(status);
    }

    private void assertPaymentSucceeded(Fixture fixture, String providerId) {
        var payment = paymentRepository.findById(fixture.attemptId()).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(payment.getProviderPaymentId()).isEqualTo(providerId);
    }

    private int stock() {
        return jdbcTemplate.queryForObject(
            "SELECT available FROM inventory WHERE event_id = ?", Integer.class, EVENT_ID
        );
    }

    private void runTogether(Runnable first, Runnable second) throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try {
            var results = List.of(first, second).stream().map(action ->
                executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("동시 시작 대기 시간 초과");
                    }
                    action.run();
                    return null;
                })
            ).toList();
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(25);
            for (Future<?> result : results) {
                long remaining = deadline - System.nanoTime();
                assertThat(remaining).isPositive();
                result.get(remaining, TimeUnit.NANOSECONDS);
            }
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    private record Fixture(Long orderId, Long attemptId) {}
}
