package com.example.concurrencylab.messaging;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "inbox_receipts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InboxReceipt {
    @Id @Column(name = "event_id", length = 36) private String eventId;
    @Column(name = "payment_attempt_id", nullable = false) private Long paymentAttemptId;
    @Column(name = "received_at", nullable = false, columnDefinition = "datetime(6)") private Instant receivedAt;
    public InboxReceipt(PaymentEvent event) {
        eventId = event.eventId();
        paymentAttemptId = event.paymentAttemptId();
        receivedAt = Instant.now();
    }
}
