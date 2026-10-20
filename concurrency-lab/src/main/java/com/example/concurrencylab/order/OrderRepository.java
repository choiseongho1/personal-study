package com.example.concurrencylab.order;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<PurchaseOrder, Long> {
    // TODO: 중복 조회, 만료 대상 조회, 조건부 상태 변경 등을 단계별로 추가하세요.

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from PurchaseOrder o where o.id = :orderId")
    Optional<PurchaseOrder> findByIdForUpdate(@Param("orderId") Long orderId);

    @Modifying(flushAutomatically = true)
    @Query("""
        update PurchaseOrder o
        set o.status = com.example.concurrencylab.order.OrderStatus.CONFIRMED
        where o.id = :orderId
          and o.status = com.example.concurrencylab.order.OrderStatus.PENDING
          and o.expiresAt > :now
        """)
    int confirmIfPendingAndNotExpired(@Param("orderId") Long orderId, @Param("now") Instant now);

    @Modifying(flushAutomatically = true)
    @Query("""
        update PurchaseOrder o
        set o.status = :status
        where o.id = :orderId
          and o.status = com.example.concurrencylab.order.OrderStatus.PENDING
          and o.expiresAt <= :now
        """)
    int updateStatus(
        @Param("orderId") Long orderId,
        @Param("status") OrderStatus status,
        @Param("now") Instant now
    );

    @Modifying(flushAutomatically = true)
    @Query("""
        update PurchaseOrder o
        set o.status = com.example.concurrencylab.order.OrderStatus.CANCELLED
        where o.id = :orderId
          and o.status = com.example.concurrencylab.order.OrderStatus.PENDING
        """)
    int cancelIfPending(@Param("orderId") Long orderId);


    @Query("select o.id from PurchaseOrder o where o.status = com.example.concurrencylab.order.OrderStatus.PENDING and o.expiresAt <= :now order by o.expiresAt, o.id")
    java.util.List<Long> findExpiredPending(@Param("now") Instant now, org.springframework.data.domain.Pageable pageable);
}
