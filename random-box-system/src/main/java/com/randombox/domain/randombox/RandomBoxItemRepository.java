package com.randombox.domain.randombox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RandomBoxItemRepository extends JpaRepository<RandomBoxItem, Long> {

    List<RandomBoxItem> findByRandomBoxId(Long randomBoxId);

    @Query("SELECT rbi FROM RandomBoxItem rbi WHERE rbi.randomBox.id = :randomBoxId ORDER BY rbi.probability ASC")
    List<RandomBoxItem> findByRandomBoxIdOrderByProbabilityAsc(@Param("randomBoxId") Long randomBoxId);

    List<RandomBoxItem> findByRandomBoxIdAndRarity(Long randomBoxId, RandomBoxItem.Rarity rarity);
}
