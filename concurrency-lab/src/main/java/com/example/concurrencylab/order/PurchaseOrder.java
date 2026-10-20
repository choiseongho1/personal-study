package com.example.concurrencylab.order;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "orders",
    uniqueConstraints = @UniqueConstraint(name = "uk_orders_user_event",
        columnNames = {"user_id", "event_id"}),
    indexes = @Index(name = "idx_orders_expiration", columnList = "status,expires_at,id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PurchaseOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OrderStatus status;

    @Column(name = "expires_at", nullable = false, columnDefinition = "datetime(6)")
    private Instant expiresAt;

    public PurchaseOrder(Long userId, Long eventId, Instant expiresAt) {
        this.userId = userId;
        this.eventId = eventId;
        this.expiresAt = expiresAt;
        this.status = OrderStatus.PENDING;
    }

    // TODO: 상태 전이와 전이 조건은 직접 설계하세요.
}
