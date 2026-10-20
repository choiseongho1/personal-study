package com.example.concurrencylab.order;

import com.example.concurrencylab.inventory.InventoryRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
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

/**
 * placeOrder의 DB 반영 결과를 확인하는 통합 테스트.
 *
 * @author : choiseongho
 * @fileName : OrderServiceTest.java
 * @since : 2026-10-19
 */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:order_service_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.h2.console.enabled=false"
})
class OrderServiceTest {

    private static final Long EVENT_ID = 100L;

    @Autowired
    OrderService orderService;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    InventoryRepository inventoryRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // 이 클래스 전용 H2 DB이므로 앱과 다른 테스트 클래스에 영향을 주지 않습니다.
        // 외래 키의 자식 테이블부터 정리합니다.
        jdbcTemplate.update("DELETE FROM work_items");
        jdbcTemplate.update("DELETE FROM inbox_receipts");
        jdbcTemplate.update("DELETE FROM outbox_events");
        jdbcTemplate.update("DELETE FROM payment_attempts");
        jdbcTemplate.update("DELETE FROM orders");
        jdbcTemplate.update("DELETE FROM inventory");
        jdbcTemplate.update(
            "INSERT INTO inventory(event_id, available, unit_price) VALUES (?, ?, ?)",
            EVENT_ID, 1, 10000
        );
    }

    @Test
    @DisplayName("재고가 있으면 PENDING 주문을 만들고 재고를 1개 차감한다")
    void placesOrderAndDecreasesStock() {
        Instant before = Instant.now();

        Long orderId = orderService.placeOrder(1L, EVENT_ID);

        Instant after = Instant.now();
        PurchaseOrder order = orderRepository.findById(orderId).orElseThrow();
        assertThat(order.getUserId()).isEqualTo(1L);
        assertThat(order.getEventId()).isEqualTo(EVENT_ID);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        // DB의 마이크로초 정밀도 변환을 고려해 1초 여유를 둡니다.
        assertThat(order.getExpiresAt()).isBetween(
            before.plus(Duration.ofMinutes(10)).minusSeconds(1),
            after.plus(Duration.ofMinutes(10)).plusSeconds(1)
        );
        assertThat(orderRepository.count()).isEqualTo(1);
        assertThat(availableStock()).isZero();
    }

    @Test
    @DisplayName("재고가 없으면 예외가 발생하고 먼저 저장한 주문도 롤백된다")
    void rollsBackOrderWhenStockIsEmpty() {
        setStock(0);

        assertThatThrownBy(() -> orderService.placeOrder(1L, EVENT_ID))
            .isExactlyInstanceOf(RuntimeException.class)
            .hasMessage("재고를 예약할 수 없습니다.");

        // 테스트에 @Transactional이 없으므로 서비스 트랜잭션 종료 후 결과를 읽습니다.
        assertThat(orderRepository.count()).isZero();
        assertThat(availableStock()).isZero();
    }

    @Test
    @DisplayName("같은 사용자의 중복 주문은 실패하고 추가 재고 차감이 없다")
    void rejectsDuplicatePurchase() {
        setStock(2);
        Long firstOrderId = orderService.placeOrder(1L, EVENT_ID);

        assertThatThrownBy(() -> orderService.placeOrder(1L, EVENT_ID))
            .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(orderRepository.count()).isEqualTo(1);
        assertThat(orderRepository.existsById(firstOrderId)).isTrue();
        assertThat(availableStock()).isEqualTo(1);
    }

    @Test
    @Timeout(40)
    @DisplayName("8명이 마지막 재고 1개를 동시에 요청하면 1명만 성공한다")
    void onlyOneBuyerGetsLastStock() throws Exception {
        int buyerCount = 8;
        var executor = Executors.newFixedThreadPool(buyerCount);
        var ready = new CountDownLatch(buyerCount);
        var start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();

        try {
            for (long userId = 1; userId <= buyerCount; userId++) {
                long buyerId = userId;
                results.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("동시 시작 신호 대기 시간 초과");
                    }
                    try {
                        // Spring Bean을 호출하므로 각 스레드에 서비스 트랜잭션이 생깁니다.
                        orderService.placeOrder(buyerId, EVENT_ID);
                        return true;
                    } catch (RuntimeException exception) {
                        // 재고 부족만 예상 실패로 계산합니다.
                        // SQL 오류, 잠금 시간 초과 등은 테스트를 실패시킵니다.
                        if (exception.getClass() == RuntimeException.class
                            && "재고를 예약할 수 없습니다.".equals(exception.getMessage())) {
                            return false;
                        }
                        throw exception;
                    }
                }));
            }

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            int successCount = 0;
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(25);
            for (Future<Boolean> result : results) {
                long remaining = deadline - System.nanoTime();
                assertThat(remaining).isPositive();
                if (result.get(remaining, TimeUnit.NANOSECONDS)) {
                    successCount++;
                }
            }

            assertThat(successCount).isEqualTo(1);
            assertThat(orderRepository.count()).isEqualTo(1);
            assertThat(availableStock()).isZero();
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    private void setStock(int stock) {
        jdbcTemplate.update(
            "UPDATE inventory SET available = ? WHERE event_id = ?", stock, EVENT_ID
        );
    }

    private int availableStock() {
        return inventoryRepository.findById(EVENT_ID).orElseThrow().getAvailable();
    }
}
