package com.randombox.domain.randombox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RandomBoxRepository extends JpaRepository<RandomBox, Long> {

    @Query("SELECT rb FROM RandomBox rb WHERE rb.salesStartTime <= :now AND rb.salesEndTime >= :now AND rb.quantity > 0")
    List<RandomBox> findAllOnSale(@Param("now") LocalDateTime now);

    List<RandomBox> findByNameContaining(String keyword);
}
