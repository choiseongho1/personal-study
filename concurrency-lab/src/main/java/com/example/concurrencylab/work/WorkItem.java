package com.example.concurrencylab.work;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "work_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkItem {
    public enum Kind { OUTBOX, PAYMENT, REFUND }
    public enum Status { READY, RUNNING, DONE, REVIEW }
    @Id @Column(length = 100) private String id;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private Kind kind;
    @Column(name = "reference_id", nullable = false) private Long referenceId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private Status status;
    @Column(nullable = false) private int attempts;
    @Column(name = "next_attempt_at", nullable = false, columnDefinition = "datetime(6)") private Instant nextAttemptAt;
    @Column(name = "lease_until", columnDefinition = "datetime(6)") private Instant leaseUntil;
    @Column(name = "lease_token", length = 36) private String leaseToken;
    @Column(name = "last_error", length = 1000) private String lastError;

    public WorkItem(String id, Kind kind, Long referenceId) {
        this.id = id;
        this.kind = kind;
        this.referenceId = referenceId;
        status = Status.READY;
        nextAttemptAt = Instant.now();
    }

    public boolean claimable(Instant now) {
        return (status == Status.READY && !nextAttemptAt.isAfter(now))
            || (status == Status.RUNNING && leaseUntil != null && !leaseUntil.isAfter(now));
    }
    public void claim(Instant now) {
        status = Status.RUNNING;
        attempts++;
        leaseToken = UUID.randomUUID().toString();
        leaseUntil = now.plusSeconds(30);
    }
    public boolean ownedBy(String token) {
        return status == Status.RUNNING && token != null && token.equals(leaseToken);
    }
    public void complete() { status = Status.DONE; leaseToken = null; leaseUntil = null; }
    public void retry(String error, Instant now) {
        status = attempts >= 10 ? Status.REVIEW : Status.READY;
        lastError = error == null ? null : error.substring(0, Math.min(1000, error.length()));
        nextAttemptAt = now.plusSeconds(Math.min(300, 1L << Math.min(attempts, 8)));
        leaseToken = null;
        leaseUntil = null;
    }
    public void review(String error) {
        retry(error, Instant.now());
        status = Status.REVIEW;
    }
}
