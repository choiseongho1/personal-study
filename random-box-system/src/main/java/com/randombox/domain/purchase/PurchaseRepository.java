package com.randombox.domain.purchase;

import com.randombox.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    List<Purchase> findByUserOrderByPurchaseDateTimeDesc(User user);

    @Query("SELECT p FROM Purchase p WHERE p.user.id = :userId AND p.status = 'COMPLETED' ORDER BY p.purchaseDateTime DESC")
    List<Purchase> findCompletedPurchasesByUserId(@Param("userId") Long userId);

    @Query("SELECT p FROM Purchase p WHERE p.randomBox.id = :randomBoxId AND p.status = 'COMPLETED'")
    List<Purchase> findCompletedPurchasesByRandomBoxId(@Param("randomBoxId") Long randomBoxId);

    @Query("SELECT COUNT(p) FROM Purchase p WHERE p.user.id = :userId AND p.randomBox.id = :randomBoxId AND p.status = 'COMPLETED'")
    long countCompletedPurchasesByUserIdAndRandomBoxId(@Param("userId") Long userId, @Param("randomBoxId") Long randomBoxId);

    @Query("SELECT p FROM Purchase p WHERE p.purchaseDateTime BETWEEN :startDate AND :endDate ORDER BY p.purchaseDateTime DESC")
    List<Purchase> findByPurchaseDateTimeBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}
