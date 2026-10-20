package com.randombox.domain.purchase;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.randombox.domain.randombox.RandomBoxItem;

import java.util.List;

public interface PurchaseResultRepository extends JpaRepository<PurchaseResult, Long> {

    List<PurchaseResult> findByPurchaseId(Long purchaseId);

    @Query("SELECT pr FROM PurchaseResult pr WHERE pr.purchase.user.id = :userId")
    List<PurchaseResult> findByUserId(@Param("userId") Long userId);

    @Query("SELECT pr FROM PurchaseResult pr WHERE pr.randomBoxItem.id = :itemId")
    List<PurchaseResult> findByRandomBoxItemId(@Param("itemId") Long itemId);

    @Query("SELECT COUNT(pr) FROM PurchaseResult pr WHERE pr.randomBoxItem.rarity = :rarity")
    long countByRarity(@Param("rarity") RandomBoxItem.Rarity rarity);
}
