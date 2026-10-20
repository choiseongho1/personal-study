package com.randombox.domain.coupon;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCode(String code);

    @Query("SELECT c FROM Coupon c WHERE c.startDate <= :now AND c.endDate >= :now")
    List<Coupon> findAllValid(@Param("now") LocalDateTime now);

    List<Coupon> findByNameContaining(String keyword);
}
