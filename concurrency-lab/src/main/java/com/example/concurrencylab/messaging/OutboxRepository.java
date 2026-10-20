package com.example.concurrencylab.messaging;
import org.springframework.data.jpa.repository.JpaRepository;
public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {}
