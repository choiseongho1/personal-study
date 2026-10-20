package com.example.concurrencylab.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    // TODO: 조건부 차감/복구 쿼리 또는 선택한 동시성 제어 방식을 직접 구현하세요.

    @Modifying
    @Query("""
        update Inventory i
        set i.available = i.available - 1
        where i.eventId = :eventId
          and i.available > 0
        """)
    int decreaseIfAvailable(@Param("eventId") Long eventId);

    @Modifying
    @Query("""
        update Inventory i
        set i.available = i.available + 1
        where i.eventId = :eventId
        """)
    int increase(@Param("eventId") Long eventId);
}
