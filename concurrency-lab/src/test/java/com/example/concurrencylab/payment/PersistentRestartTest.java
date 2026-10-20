package com.example.concurrencylab.payment;

import com.example.concurrencylab.ConcurrencyLabApplication;
import com.example.concurrencylab.order.OrderService;
import com.example.concurrencylab.work.*;
import java.nio.file.Path;
import java.time.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.*;

class PersistentRestartTest {
    @TempDir Path directory;

    private ConfigurableApplicationContext start() {
        String db = directory.resolve("orders").toString().replace('\\', '/');
        return new SpringApplicationBuilder(ConcurrencyLabApplication.class).web(WebApplicationType.NONE).run(
            "--spring.main.web-application-type=none",
            "--spring.datasource.url=jdbc:h2:file:" + db + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_ON_EXIT=FALSE",
            "--spring.datasource.username=sa", "--spring.datasource.password=",
            "--lab.fake-ledger-file=" + directory.resolve("provider.properties"),
            "--lab.workers.enabled=false", "--lab.messaging=local", "--spring.h2.console.enabled=false"
        );
    }

    @Test
    @Timeout(90)
    void restartsDatabaseAndProviderThenRecoversUnfinishedWork() {
        long orderId;
        long attemptId;
        String taskId;
        String providerId;
        try (var first = start()) {
            orderId = first.getBean(OrderService.class).placeOrder(1L, 1L);
            var repository = first.getBean(PaymentAttemptRepository.class);
            var attempt = repository.findByOrderId(orderId).orElseThrow();
            attemptId = attempt.getId();
            var gateway = first.getBean(FakePaymentGateway.class);
            gateway.configureScenario(attempt.getIdempotencyKey(), FakePaymentGateway.Scenario.TIMEOUT_AFTER_APPROVAL);
            first.getBean(PaymentRequestService.class).requestPayment(attemptId);
            providerId = gateway.find(attempt.getIdempotencyKey()).providerPaymentId();
            taskId = first.getBean(WorkRepository.class).findAll().getFirst().getId();
            first.getBean(WorkTransactions.class).claim(taskId).orElseThrow();
            // 프로세스 종료 후 시간이 지나 선점이 만료된 상태를 저장합니다.
            first.getBean(JdbcTemplate.class).update("UPDATE work_items SET lease_until = ? WHERE id = ?",
                LocalDateTime.ofInstant(Instant.now().minusSeconds(60), ZoneOffset.UTC), taskId);
        }

        try (var restarted = start()) {
            var runner = restarted.getBean(WorkRunner.class);
            runner.run(taskId);
            runner.run("payment:" + attemptId);
            var attempt = restarted.getBean(PaymentAttemptRepository.class).findById(attemptId).orElseThrow();
            assertThat(attempt.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
            assertThat(attempt.getProviderPaymentId()).isEqualTo(providerId);
            var jdbc = restarted.getBean(JdbcTemplate.class);
            assertThat(jdbc.queryForObject("SELECT status FROM orders WHERE id = ?", String.class, orderId))
                .isEqualTo("CONFIRMED");
            assertThat(jdbc.queryForObject("SELECT available FROM inventory WHERE event_id=1", Integer.class))
                .isEqualTo(99);
        }
    }
}
