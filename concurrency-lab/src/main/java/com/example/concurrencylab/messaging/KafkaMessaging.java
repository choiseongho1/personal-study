package com.example.concurrencylab.messaging;

import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@ConditionalOnProperty(name = "lab.messaging", havingValue = "kafka")
public class KafkaMessaging {
    public static final String TOPIC = "lab-payment-requests";

    @Bean NewTopic paymentTopic() {
        return TopicBuilder.name(TOPIC).partitions(3).replicas(1).build();
    }
    @Bean DefaultErrorHandler retryWithoutDiscarding() {
        // DB 저장 실패 시 이벤트를 버리고 오프셋을 전진시키지 않습니다.
        return new DefaultErrorHandler(new FixedBackOff(1000, FixedBackOff.UNLIMITED_ATTEMPTS));
    }
    @Bean EventTransport kafkaTransport(KafkaTemplate<String, String> template) {
        return event -> {
            try {
                template.send(TOPIC, event.orderId().toString(), event.encode()).get(10, TimeUnit.SECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Kafka 전송 중단", exception);
            } catch (Exception exception) {
                throw new IllegalStateException("Kafka 전송 확인 실패", exception);
            }
        };
    }
    @Bean PaymentListener paymentListener(InboxReceiver receiver) { return new PaymentListener(receiver); }

    @RequiredArgsConstructor
    public static class PaymentListener {
        private final InboxReceiver receiver;
        @KafkaListener(topics = TOPIC, groupId = "lab-payment-worker")
        public void receive(String value) { receiver.accept(PaymentEvent.decode(value)); }
    }
}
