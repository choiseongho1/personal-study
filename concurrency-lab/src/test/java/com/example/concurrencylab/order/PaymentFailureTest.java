package com.example.concurrencylab.order;

import com.example.concurrencylab.inventory.InventoryRepository;
import com.example.concurrencylab.payment.PaymentAttemptRepository;
import com.example.concurrencylab.payment.PaymentStatus;
import com.example.concurrencylab.payment.RefundRequestRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:payment_failure_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.h2.console.enabled=false"
})
class PaymentFailureTest {
    private static final Long EVENT_ID = 300L;

    @Autowired OrderService orderService;
    @Autowired OrderRepository orderRepository;
    @Autowired PaymentAttemptRepository paymentRepository;
    @Autowired RefundRequestRepository refundRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    // 평소에는 실제 Repository를 실행하고 복구 실패 테스트에서만 결과를 바꿉니다.
    @MockitoSpyBean InventoryRepository inventoryRepository;

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
    @DisplayName("기한 전 최종 실패도 주문을 취소하고 재고를 반환한다")
    void cancelsBeforeDeadline() {
        var fixture = createOrder();

        orderService.paymentFailed(fixture.attemptId());

        assertFailure(fixture, OrderStatus.CANCELLED);
        assertThat(refundRepository.count()).isZero();
    }

    @Test
    @DisplayName("UNKNOWN 상태에서 최종 실패를 확인하면 주문을 취소한다")
    void resolvesUnknownPayment() {
        var fixture = createOrder();
        jdbcTemplate.update("UPDATE payment_attempts SET status = 'UNKNOWN' WHERE id = ?",
            fixture.attemptId());

        orderService.paymentFailed(fixture.attemptId());

        assertFailure(fixture, OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("같은 실패를 두 번 받아도 재고를 한 번만 반환한다")
    void ignoresDuplicateFailure() {
        var fixture = createOrder();

        orderService.paymentFailed(fixture.attemptId());
        orderService.paymentFailed(fixture.attemptId());

        assertFailure(fixture, OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("이미 만료된 주문은 결제 실패만 기록하고 재고를 다시 반환하지 않는다")
    void preservesExpiredOrderAndStock() {
        var fixture = createOrder();
        makePastDeadline(fixture);
        orderService.expireOrder(fixture.orderId());

        orderService.paymentFailed(fixture.attemptId());

        assertFailure(fixture, OrderStatus.EXPIRED);
    }

    @Test
    @DisplayName("성공 뒤 실패 통지가 오면 승인과 확정 주문을 유지한다")
    void doesNotOverwriteSucceededPayment() {
        var fixture = createOrder();
        orderService.paymentSucceeded(fixture.attemptId(), "provider-success");

        assertThatThrownBy(() -> orderService.paymentFailed(fixture.attemptId()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("결제사 조회");

        assertThat(orderRepository.findById(fixture.orderId()).orElseThrow().getStatus())
            .isEqualTo(OrderStatus.CONFIRMED);
        assertThat(paymentRepository.findById(fixture.attemptId()).orElseThrow().getStatus())
            .isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(stock()).isZero();
        assertThat(refundRepository.count()).isZero();
    }

    @Test
    @DisplayName("없는 결제 시도는 오류로 처리하고 데이터를 변경하지 않는다")
    void rejectsMissingAttempt() {
        assertThatThrownBy(() -> orderService.paymentFailed(Long.MAX_VALUE))
            .isInstanceOf(EntityNotFoundException.class);

        assertThat(orderRepository.count()).isZero();
        assertThat(stock()).isEqualTo(1);
    }

    @Test
    @DisplayName("재고 복구가 실패하면 결제 실패 기록과 주문 취소도 롤백된다")
    void rollsBackWhenStockRestoreFails() {
        var fixture = createOrder();
        // 이 테스트에서만 재고 복구 실패를 주입합니다. 다음 테스트 전에 자동 초기화됩니다.
        doReturn(0).when(inventoryRepository).increase(EVENT_ID);

        assertThatThrownBy(() -> orderService.paymentFailed(fixture.attemptId()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("재고를 복구할 수 없습니다.");

        assertThat(orderRepository.findById(fixture.orderId()).orElseThrow().getStatus())
            .isEqualTo(OrderStatus.PENDING);
        assertThat(paymentRepository.findById(fixture.attemptId()).orElseThrow().getStatus())
            .isEqualTo(PaymentStatus.PENDING);
        assertThat(stock()).isZero();
    }

    @Test
    @Timeout(40)
    @DisplayName("실패 통지를 동시에 처리해도 재고를 한 번만 반환한다")
    void handlesConcurrentFailures() throws Exception {
        var fixture = createOrder();

        runTogether(
            () -> orderService.paymentFailed(fixture.attemptId()),
            () -> orderService.paymentFailed(fixture.attemptId())
        );

        assertFailure(fixture, OrderStatus.CANCELLED);
    }

    @Test
    @Timeout(40)
    @DisplayName("만료와 실패가 경합해도 재고를 한 번만 반환한다")
    void handlesFailureRacingWithExpiration() throws Exception {
        var fixture = createOrder();
        makePastDeadline(fixture);

        runTogether(
            () -> orderService.expireOrder(fixture.orderId()),
            () -> orderService.paymentFailed(fixture.attemptId())
        );

        assertThat(orderRepository.findById(fixture.orderId()).orElseThrow().getStatus())
            .isIn(OrderStatus.EXPIRED, OrderStatus.CANCELLED);
        assertThat(paymentRepository.findById(fixture.attemptId()).orElseThrow().getStatus())
            .isEqualTo(PaymentStatus.FAILED);
        assertThat(stock()).isEqualTo(1);
    }

    @Test
    @Timeout(40)
    @DisplayName("성공과 실패가 경합하면 한 결과만 반영하고 다른 결과는 충돌로 알린다")
    void handlesFailureRacingWithSuccess() throws Exception {
        var fixture = createOrder();
        var accepted = new AtomicInteger();
        var conflicts = new AtomicInteger();

        runTogether(
            () -> countResult(
                () -> orderService.paymentSucceeded(fixture.attemptId(), "provider-race"), accepted, conflicts
            ),
            () -> countResult(
                () -> orderService.paymentFailed(fixture.attemptId()), accepted, conflicts
            )
        );

        assertThat(accepted.get()).isEqualTo(1);
        assertThat(conflicts.get()).isEqualTo(1);
        var paymentStatus = paymentRepository.findById(fixture.attemptId()).orElseThrow().getStatus();
        var orderStatus = orderRepository.findById(fixture.orderId()).orElseThrow().getStatus();
        if (paymentStatus == PaymentStatus.SUCCEEDED) {
            assertThat(orderStatus).isEqualTo(OrderStatus.CONFIRMED);
            assertThat(stock()).isZero();
        } else {
            assertThat(paymentStatus).isEqualTo(PaymentStatus.FAILED);
            assertThat(orderStatus).isEqualTo(OrderStatus.CANCELLED);
            assertThat(stock()).isEqualTo(1);
        }
        assertThat(refundRepository.count()).isZero();
    }

    private Fixture createOrder() {
        Long orderId = orderService.placeOrder(1L, EVENT_ID);
        return new Fixture(orderId, paymentRepository.findByOrderId(orderId).orElseThrow().getId());
    }

    private void assertFailure(Fixture fixture, OrderStatus expectedOrderStatus) {
        assertThat(orderRepository.findById(fixture.orderId()).orElseThrow().getStatus())
            .isEqualTo(expectedOrderStatus);
        assertThat(paymentRepository.findById(fixture.attemptId()).orElseThrow().getStatus())
            .isEqualTo(PaymentStatus.FAILED);
        assertThat(stock()).isEqualTo(1);
    }

    private int stock() {
        return jdbcTemplate.queryForObject(
            "SELECT available FROM inventory WHERE event_id = ?", Integer.class, EVENT_ID
        );
    }

    private void makePastDeadline(Fixture fixture) {
        jdbcTemplate.update("UPDATE orders SET expires_at = ? WHERE id = ?",
            LocalDateTime.ofInstant(Instant.now().minusSeconds(60), ZoneOffset.UTC), fixture.orderId());
    }

    private void countResult(Runnable action, AtomicInteger accepted, AtomicInteger conflicts) {
        try {
            action.run();
            accepted.incrementAndGet();
        } catch (IllegalStateException exception) {
            if (exception.getMessage() == null || !exception.getMessage().contains("결제사 조회")) {
                throw exception;
            }
            conflicts.incrementAndGet();
        }
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
