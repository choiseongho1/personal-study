package com.example.concurrencylab.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "lab.messaging", havingValue = "local", matchIfMissing = true)
public class LocalEventTransport implements EventTransport {
    private final InboxReceiver receiver;
    public void send(PaymentEvent event) { receiver.accept(event); }
}
