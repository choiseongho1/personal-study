package com.example.concurrencylab.work;

import com.example.concurrencylab.order.*;
import com.example.concurrencylab.payment.*;
import java.time.*;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:auto_worker_flow;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "lab.messaging=local", "lab.workers.enabled=true", "lab.workers.delay-ms=50", "spring.h2.console.enabled=false"
})
@DirtiesContext
class AutomaticWorkersTest {
    @Autowired OrderService orders;
    @Autowired OrderRepository orderRepository;
    @Autowired PaymentAttemptRepository payments;
    @Autowired WorkRepository work;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactionManager;

    @Test
    @Timeout(45)
    void scheduledWorkersAutomaticallyPayAndExpire() throws Exception {
        long firstOrder = orders.placeOrder(1L, 1L);
        await(() -> orderRepository.findById(firstOrder).orElseThrow().getStatus() == OrderStatus.CONFIRMED);
        assertThat(payments.findByOrderId(firstOrder).orElseThrow().getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(stock()).isEqualTo(99);

        // 만료 시각까지 같은 트랜잭션에 준비해 작업자가 중간 상태를 보지 않게 합니다.
        long expiredOrder = new TransactionTemplate(transactionManager).execute(status -> {
            long id = orders.placeOrder(2L, 1L);
            jdbc.update("UPDATE orders SET expires_at = ? WHERE id = ?",
                LocalDateTime.ofInstant(Instant.now().minusSeconds(60), ZoneOffset.UTC), id);
            return id;
        });
        await(() -> orderRepository.findById(expiredOrder).orElseThrow().getStatus() == OrderStatus.EXPIRED);
        assertThat(stock()).isEqualTo(99);
        assertThat(payments.findByOrderId(expiredOrder).orElseThrow().getProviderPaymentId()).isNull();
    }

    private int stock() { return jdbc.queryForObject("SELECT available FROM inventory WHERE event_id=1", Integer.class); }
    private void await(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos();
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) Thread.sleep(50);
        assertThat(condition.getAsBoolean()).isTrue();
    }
}
