package com.example.concurrencylab.messaging;
public interface EventTransport { void send(PaymentEvent event); }
