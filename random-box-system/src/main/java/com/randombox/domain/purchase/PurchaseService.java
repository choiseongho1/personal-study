package com.randombox.domain.purchase;

import com.randombox.domain.coupon.Coupon;
import com.randombox.domain.coupon.UserCoupon;
import com.randombox.domain.coupon.UserCouponRepository;
import com.randombox.domain.notification.Notification;
import com.randombox.domain.notification.NotificationService;
import com.randombox.domain.queue.QueueService;
import com.randombox.domain.randombox.RandomBox;
import com.randombox.domain.randombox.RandomBoxInventoryService;
import com.randombox.domain.randombox.RandomBoxItem;
import com.randombox.domain.randombox.RandomBoxRepository;
import com.randombox.domain.randombox.RandomBoxService;
import com.randombox.domain.user.User;
import com.randombox.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final PurchaseResultRepository purchaseResultRepository;
    private final RandomBoxRepository randomBoxRepository;
    private final UserRepository userRepository;
    private final RandomBoxService randomBoxService;
    private final UserCouponRepository userCouponRepository;
    private final RandomBoxInventoryService randomBoxInventoryService;
    private final QueueService queueService;
    private final NotificationService notificationService;

    /**
     * 랜덤박스 구매 전 대기열 확인
     * @param userId 사용자 ID
     * @param randomBoxId 랜덤박스 ID
     * @return 대기열 위치 (0이면 바로 구매 가능)
     */
    public int checkQueuePosition(Long userId, Long randomBoxId) {
        // 대기열에서 사용자 위치 확인
        int position = queueService.getPosition(randomBoxId, userId);

        // 대기열에 없으면 추가
        if (position == -1) {
            position = queueService.addToQueue(randomBoxId, userId);
        }

        // 첫 번째 위치면 바로 구매 가능
        if (position == 0) {
            return 0;
        }

        // 대기열 위치 반환
        return position;
    }

    /**
     * 랜덤박스 구매 준비 (대기열 첫 번째 사용자에게 알림)
     * @param randomBoxId 랜덤박스 ID
     * @return 구매 가능한 사용자 ID
     */
    public Long prepareForPurchase(Long randomBoxId) {
        // 대기열에서 다음 사용자 가져오기
        Long userId = queueService.getNextUser(randomBoxId);

        if (userId != null) {
            // 랜덤박스 정보 조회
            RandomBox randomBox = randomBoxRepository.findById(randomBoxId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 랜덤박스입니다."));

            // 구매 준비 알림 전송
            notificationService.sendQueueReadyNotification(userId, randomBox.getName());

            log.info("사용자 {}에게 랜덤박스 {} 구매 준비 알림을 전송했습니다.", userId, randomBoxId);
        }

        return userId;
    }

    @Transactional
    public Purchase purchaseRandomBox(Long userId, Long randomBoxId, int quantity, Long userCouponId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        RandomBox randomBox = randomBoxRepository.findById(randomBoxId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 랜덤박스입니다."));

        if (!randomBox.isOnSale()) {
            throw new IllegalStateException("현재 판매 중인 랜덤박스가 아닙니다.");
        }

        // Redis에서 재고 확인 및 감소
        boolean inventoryDecreased = randomBoxInventoryService.decreaseInventory(randomBoxId, quantity);
        if (!inventoryDecreased) {
            throw new IllegalStateException("재고가 부족하거나 재고 감소에 실패했습니다.");
        }

        int totalPrice = randomBox.getPrice() * quantity;

        // 쿠폰 적용
        if (userCouponId != null) {
            UserCoupon userCoupon = userCouponRepository.findById(userCouponId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다."));

            if (userCoupon.isUsed()) {
                // 재고 복구
                randomBoxInventoryService.increaseInventory(randomBoxId, quantity);
                throw new IllegalStateException("이미 사용된 쿠폰입니다.");
            }

            if (!userCoupon.getUser().getId().equals(userId)) {
                // 재고 복구
                randomBoxInventoryService.increaseInventory(randomBoxId, quantity);
                throw new IllegalArgumentException("해당 쿠폰을 사용할 권한이 없습니다.");
            }

            Coupon coupon = userCoupon.getCoupon();
            if (!coupon.isValid()) {
                // 재고 복구
                randomBoxInventoryService.increaseInventory(randomBoxId, quantity);
                throw new IllegalStateException("유효하지 않은 쿠폰입니다.");
            }

            if (coupon.getMinPurchase() != null && totalPrice < coupon.getMinPurchase()) {
                // 재고 복구
                randomBoxInventoryService.increaseInventory(randomBoxId, quantity);
                throw new IllegalStateException("최소 구매 금액을 만족하지 않습니다.");
            }

            int discountAmount = coupon.calculateDiscount(totalPrice);
            totalPrice -= discountAmount;

            userCoupon.use();
        }

        // 데이터베이스 재고 감소 (Redis와 동기화 용도)
        randomBox.decreaseQuantity(quantity);

        Purchase purchase = Purchase.builder()
                .user(user)
                .randomBox(randomBox)
                .quantity(quantity)
                .totalPrice(totalPrice)
                .build();

        Purchase savedPurchase = purchaseRepository.save(purchase);

        // 랜덤 아이템 추첨 및 결과 저장
        List<PurchaseResult> results = new ArrayList<>();
        for (int i = 0; i < quantity; i++) {
            RandomBoxItem randomItem = randomBoxService.drawRandomItem(randomBoxId);
            PurchaseResult result = PurchaseResult.builder()
                    .purchase(savedPurchase)
                    .randomBoxItem(randomItem)
                    .build();
            results.add(purchaseResultRepository.save(result));

            // 구매 성공 알림 전송
            notificationService.sendPurchaseSuccessNotification(userId, randomBox.getName(), randomItem.getName());
        }

        log.info("사용자 {}가 랜덤박스 {}를 {}개 구매했습니다. 총 가격: {}", userId, randomBoxId, quantity, totalPrice);

        return savedPurchase;
    }

    public Purchase getPurchase(Long purchaseId) {
        return purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 구매 내역입니다."));
    }

    public List<Purchase> getUserPurchases(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        return purchaseRepository.findByUserOrderByPurchaseDateTimeDesc(user);
    }

    public List<PurchaseResult> getPurchaseResults(Long purchaseId) {
        return purchaseResultRepository.findByPurchaseId(purchaseId);
    }

    @Transactional
    public Purchase cancelPurchase(Long userId, Long purchaseId) {
        Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 구매 내역입니다."));

        if (!purchase.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("해당 구매를 취소할 권한이 없습니다.");
        }

        if (purchase.getStatus() == Purchase.PurchaseStatus.CANCELLED) {
            throw new IllegalStateException("이미 취소된 구매입니다.");
        }

        // 구매 시간으로부터 24시간이 지났는지 확인
        if (purchase.getPurchaseDateTime().plusHours(24).isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("구매 후 24시간이 지나 취소할 수 없습니다.");
        }

        purchase.cancel();

        // 랜덤박스 수량 복구 (Redis와 DB 모두 업데이트)
        RandomBox randomBox = purchase.getRandomBox();
        randomBoxInventoryService.increaseInventory(randomBox.getId(), purchase.getQuantity());

        // DB 재고 복구 (Redis와 동기화 용도)
        randomBox.update(
                randomBox.getName(),
                randomBox.getDescription(),
                randomBox.getPrice(),
                randomBox.getQuantity() + purchase.getQuantity(),
                randomBox.getSalesStartTime(),
                randomBox.getSalesEndTime()
        );

        log.info("사용자 {}의 구매 {}가 취소되었습니다. 랜덤박스 {}의 재고가 {}개 복구되었습니다.",
                userId, purchaseId, randomBox.getId(), purchase.getQuantity());

        return purchase;
    }
}
