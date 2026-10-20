package com.example.concurrencylab.messaging;
import org.springframework.data.jpa.repository.JpaRepository;
public interface InboxRepository extends JpaRepository<InboxReceipt, String> {}
