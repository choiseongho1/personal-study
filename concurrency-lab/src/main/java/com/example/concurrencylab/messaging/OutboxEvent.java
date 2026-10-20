package com.example.concurrencylab.messaging;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "outbox_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "event_id", nullable = false, unique = true, length = 36) private String eventId;
    @Column(name = "payment_attempt_id", nullable = false) private Long paymentAttemptId;
    @Column(name = "order_id", nullable = false) private Long orderId;
    @Column(name = "created_at", nullable = false, columnDefinition = "datetime(6)") private Instant createdAt;
    @Column(name = "published_at", columnDefinition = "datetime(6)") private Instant publishedAt;
    public OutboxEvent(Long paymentAttemptId, Long orderId) {
        eventId = UUID.randomUUID().toString();
        this.paymentAttemptId = paymentAttemptId;
        this.orderId = orderId;
        createdAt = Instant.now();
    }
    public void markPublished() { publishedAt = Instant.now(); }
    public PaymentEvent message() { return new PaymentEvent(eventId, paymentAttemptId, orderId); }
}
