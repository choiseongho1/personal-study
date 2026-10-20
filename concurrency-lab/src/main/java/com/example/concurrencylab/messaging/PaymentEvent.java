package com.example.concurrencylab.messaging;

import java.util.UUID;

public record PaymentEvent(String eventId, Long paymentAttemptId, Long orderId) {
    public String encode() { return eventId + "|" + paymentAttemptId + "|" + orderId; }
    public static PaymentEvent decode(String value) {
        String[] parts = value.split("\\|", -1);
        if (parts.length != 3) throw new IllegalArgumentException("이벤트 형식 오류");
        UUID.fromString(parts[0]);
        long attempt = Long.parseLong(parts[1]);
        long order = Long.parseLong(parts[2]);
        if (attempt <= 0 || order <= 0) throw new IllegalArgumentException("이벤트 ID 오류");
        return new PaymentEvent(parts[0], attempt, order);
    }
}
