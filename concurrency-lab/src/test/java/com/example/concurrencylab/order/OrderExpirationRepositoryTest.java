package com.example.concurrencylab.order;

import com.example.concurrencylab.inventory.InventoryRepository;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:expiration_repository_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.h2.console.enabled=false"
})
@Transactional
class OrderExpirationRepositoryTest {
    private static final Instant NOW = Instant.parse("2026-10-19T00:00:00Z");

    @Autowired OrderRepository orderRepository;
    @Autowired InventoryRepository inventoryRepository;
    @Autowired OrderService orderService;
    @Autowired JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("INSERT INTO inventory(event_id, available, unit_price) VALUES (100, 0, 10000)");
    }

    @Test
    void expiresAtBoundaryOnlyOnce() {
        Long id = saveOrder(NOW);

        assertThat(orderRepository.updateStatus(id, OrderStatus.EXPIRED, NOW)).isEqualTo(1);
        assertThat(orderRepository.updateStatus(id, OrderStatus.EXPIRED, NOW)).isZero();
        assertThat(statusOf(id)).isEqualTo("EXPIRED");
    }

    @Test
    void doesNotExpireBeforeDeadline() {
        Long id = saveOrder(NOW.plusSeconds(60));

        assertThat(orderRepository.updateStatus(id, OrderStatus.EXPIRED, NOW)).isZero();
        assertThat(statusOf(id)).isEqualTo("PENDING");
    }

    @Test
    void doesNotOverwriteConfirmedOrder() {
        Long id = saveOrder(NOW.minusSeconds(60));
        jdbcTemplate.update("UPDATE orders SET status = 'CONFIRMED' WHERE id = ?", id);

        assertThat(orderRepository.updateStatus(id, OrderStatus.EXPIRED, NOW)).isZero();
        assertThat(statusOf(id)).isEqualTo("CONFIRMED");
    }

    @Test
    void stockRestoreReportsAffectedRows() {
        assertThat(inventoryRepository.increase(100L)).isEqualTo(1);
        assertThat(inventoryRepository.increase(999L)).isZero();
        assertThat(jdbcTemplate.queryForObject(
            "SELECT available FROM inventory WHERE event_id = 100", Integer.class
        )).isEqualTo(1);
    }

    @Test
    void serviceConnectsExpirationAndStockRestore() {
        Long id = saveOrder(Instant.now().minusSeconds(60));

        orderService.expireOrder(id);

        assertThat(statusOf(id)).isEqualTo("EXPIRED");
        assertThat(jdbcTemplate.queryForObject(
            "SELECT available FROM inventory WHERE event_id = 100", Integer.class
        )).isEqualTo(1);
    }

    private Long saveOrder(Instant expiresAt) {
        return orderRepository.saveAndFlush(new PurchaseOrder(1L, 100L, expiresAt)).getId();
    }

    private String statusOf(Long id) {
        // 벌크 UPDATE는 영속성 컨텍스트를 갱신하지 않으므로 DB 값을 직접 확인합니다.
        return jdbcTemplate.queryForObject("SELECT status FROM orders WHERE id = ?", String.class, id);
    }
}
