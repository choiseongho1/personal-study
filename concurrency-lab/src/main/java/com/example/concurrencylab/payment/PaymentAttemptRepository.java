package com.example.concurrencylab.payment;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, Long> {
    // TODO: 결제 시도 조회와 상태 변경 메서드를 직접 추가하세요.

    @Query("select p.orderId from PaymentAttempt p where p.id = :attemptId")
    Optional<Long> findOrderId(@Param("attemptId") Long attemptId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PaymentAttempt p where p.id = :attemptId")
    Optional<PaymentAttempt> findByIdForUpdate(@Param("attemptId") Long attemptId);

    Optional<PaymentAttempt> findByOrderId(Long orderId);
}
