package com.randombox.api.v1.coupon;

import com.randombox.api.v1.coupon.dto.CouponCreateRequest;
import com.randombox.api.v1.coupon.dto.CouponResponse;
import com.randombox.api.v1.coupon.dto.UserCouponResponse;
import com.randombox.domain.coupon.Coupon;
import com.randombox.domain.coupon.CouponService;
import com.randombox.domain.coupon.UserCoupon;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @PostMapping
    public ResponseEntity<CouponResponse> createCoupon(@RequestBody CouponCreateRequest request) {
        Coupon coupon = couponService.createCoupon(
                request.getName(),
                request.getDiscountType(),
                request.getDiscountValue(),
                request.getMinPurchase(),
                request.getMaxDiscount(),
                request.getStartDate(),
                request.getEndDate()
        );
        return ResponseEntity.ok(CouponResponse.from(coupon));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CouponResponse> getCoupon(@PathVariable Long id) {
        Coupon coupon = couponService.getCoupon(id);
        return ResponseEntity.ok(CouponResponse.from(coupon));
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<CouponResponse> getCouponByCode(@PathVariable String code) {
        Coupon coupon = couponService.getCouponByCode(code);
        return ResponseEntity.ok(CouponResponse.from(coupon));
    }

    @GetMapping
    public ResponseEntity<List<CouponResponse>> getValidCoupons() {
        List<Coupon> coupons = couponService.getValidCoupons();
        List<CouponResponse> responses = coupons.stream()
                .map(CouponResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/issue/{userId}/{couponId}")
    public ResponseEntity<UserCouponResponse> issueCouponToUser(
            @PathVariable Long userId,
            @PathVariable Long couponId) {
        UserCoupon userCoupon = couponService.issueCouponToUser(userId, couponId);
        return ResponseEntity.ok(UserCouponResponse.from(userCoupon));
    }

    @PostMapping("/issue/code/{userId}/{code}")
    public ResponseEntity<UserCouponResponse> issueCouponByCode(
            @PathVariable Long userId,
            @PathVariable String code) {
        UserCoupon userCoupon = couponService.issueCouponByCode(userId, code);
        return ResponseEntity.ok(UserCouponResponse.from(userCoupon));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<UserCouponResponse>> getUserCoupons(@PathVariable Long userId) {
        List<UserCoupon> userCoupons = couponService.getUserCoupons(userId);
        List<UserCouponResponse> responses = userCoupons.stream()
                .map(UserCouponResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/user/{userId}/valid")
    public ResponseEntity<List<UserCouponResponse>> getValidUserCoupons(@PathVariable Long userId) {
        List<UserCoupon> userCoupons = couponService.getValidUserCoupons(userId);
        List<UserCouponResponse> responses = userCoupons.stream()
                .map(UserCouponResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
}
