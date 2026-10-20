package com.example.concurrencylab.messaging;

import com.example.concurrencylab.order.OrderService;
import com.example.concurrencylab.payment.*;
import com.example.concurrencylab.work.*;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:kafka_flow;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "lab.workers.enabled=false", "spring.h2.console.enabled=false"
})
@ActiveProfiles("kafka")
@EmbeddedKafka(partitions = 3, topics = KafkaMessaging.TOPIC,
    bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@DirtiesContext
class KafkaWorkflowTest {
    @Autowired OrderService orders;
    @Autowired OutboxRepository outbox;
    @Autowired InboxRepository inbox;
    @Autowired WorkRepository work;
    @Autowired WorkRunner runner;
    @Autowired EventTransport transport;
    @Autowired PaymentAttemptRepository payments;
    @Autowired JdbcTemplate jdbc;
    @MockitoSpyBean InboxReceiver receiver;
    @MockitoSpyBean KafkaMessaging.PaymentListener listener;

    @BeforeEach void resetData() {
        jdbc.update("DELETE FROM work_items");
        jdbc.update("DELETE FROM inbox_receipts");
        jdbc.update("DELETE FROM outbox_events");
        jdbc.update("DELETE FROM refund_requests");
        jdbc.update("DELETE FROM payment_attempts");
        jdbc.update("DELETE FROM orders");
        jdbc.update("DELETE FROM inventory");
        jdbc.update("INSERT INTO inventory VALUES (500, 1, 10000)");
    }

    @Test
    @Timeout(60)
    void kafkaRoundTripAndDuplicateDelivery() throws Exception {
        long orderId = orders.placeOrder(1L, 500L);
        var event = outbox.findAll().getFirst();
        runner.run("outbox:" + event.getEventId());
        await(() -> inbox.existsById(event.getEventId()));
        assertThat(work.existsById("payment:" + event.getPaymentAttemptId())).isTrue();

        transport.send(event.message());
        verify(receiver, timeout(15000).atLeast(2)).accept(event.message());
        assertThat(inbox.count()).isEqualTo(1);
        assertThat(work.findAll().stream().filter(w -> w.getKind() == WorkItem.Kind.PAYMENT).count()).isEqualTo(1);

        runner.run("payment:" + event.getPaymentAttemptId());
        assertThat(payments.findByOrderId(orderId).orElseThrow().getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(jdbc.queryForObject("SELECT status FROM orders WHERE id = ?", String.class, orderId)).isEqualTo("CONFIRMED");
    }

    @Test
    @Timeout(60)
    void listenerDatabaseFailureRedeliversBeforeOffsetAdvances() throws Exception {
        orders.placeOrder(1L, 500L);
        var event = outbox.findAll().getFirst();
        var attempts = new AtomicInteger();
        doAnswer(call -> {
            if (attempts.getAndIncrement() == 0) {
                throw new org.springframework.dao.DataAccessResourceFailureException("일시적 DB 오류");
            }
            return call.callRealMethod();
        }).when(receiver).accept(event.message());

        runner.run("outbox:" + event.getEventId());

        await(() -> inbox.existsById(event.getEventId()));
        assertThat(attempts.get()).isGreaterThanOrEqualTo(2);
        assertThat(work.existsById("payment:" + event.getPaymentAttemptId())).isTrue();
        assertThat(inbox.count()).isEqualTo(1);
    }

    @Test
    @Timeout(60)
    void failureAfterDatabaseCommitReplaysWithoutCreatingAnotherJob() throws Exception {
        orders.placeOrder(1L, 500L);
        var event = outbox.findAll().getFirst();
        var attempts = new AtomicInteger();
        doAnswer(call -> {
            call.callRealMethod(); // receiver의 DB 트랜잭션은 이미 커밋됐습니다.
            assertThat(inbox.existsById(event.getEventId())).isTrue();
            if (attempts.incrementAndGet() == 1) {
                // DB 커밋 후 리스너 종료/오프셋 커밋 전에 실패한 상황을 재현합니다.
                throw new IllegalStateException("DB 커밋 후 처리 중단");
            }
            return null;
        }).when(listener).receive(event.message().encode());

        runner.run("outbox:" + event.getEventId());

        await(() -> attempts.get() >= 2);
        assertThat(inbox.count()).isEqualTo(1);
        assertThat(work.findAll().stream().filter(w -> w.getKind() == WorkItem.Kind.PAYMENT).count()).isEqualTo(1);
    }



    private void await(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(25).toNanos();
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) Thread.sleep(50);
        assertThat(condition.getAsBoolean()).isTrue();
    }
}
