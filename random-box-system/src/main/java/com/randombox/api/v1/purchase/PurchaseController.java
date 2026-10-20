package com.randombox.api.v1.purchase;

import com.randombox.api.v1.purchase.dto.PurchaseQueueResponse;
import com.randombox.api.v1.purchase.dto.PurchaseRequest;
import com.randombox.api.v1.purchase.dto.PurchaseResponse;
import com.randombox.domain.purchase.Purchase;
import com.randombox.domain.purchase.PurchaseResult;
import com.randombox.domain.purchase.PurchaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/purchases")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService purchaseService;

    /**
     * 랜덤박스 구매 전 대기열 확인
     * @param userId 사용자 ID
     * @param randomBoxId 랜덤박스 ID
     * @return 대기열 정보
     */
    @GetMapping("/queue/check/{userId}/{randomBoxId}")
    public ResponseEntity<PurchaseQueueResponse> checkQueuePosition(
            @PathVariable Long userId,
            @PathVariable Long randomBoxId) {

        int position = purchaseService.checkQueuePosition(userId, randomBoxId);
        int estimatedWaitTime = position * 30; // 한 사용자당 처리 시간을 30초로 가정

        PurchaseQueueResponse response = PurchaseQueueResponse.builder()
                .userId(userId)
                .randomBoxId(randomBoxId)
                .position(position)
                .estimatedWaitTimeSeconds(estimatedWaitTime)
                .canPurchase(position == 0)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * 랜덤박스 구매
     * @param request 구매 요청 정보
     * @return 구매 결과
     */
    @PostMapping
    public ResponseEntity<?> purchaseRandomBox(@RequestBody PurchaseRequest request) {
        // 대기열 확인
        int position = purchaseService.checkQueuePosition(request.getUserId(), request.getRandomBoxId());

        // 대기열의 첫 번째가 아니면 대기해야 함
        if (position > 0) {
            int estimatedWaitTime = position * 30; // 한 사용자당 처리 시간을 30초로 가정

            PurchaseQueueResponse response = PurchaseQueueResponse.builder()
                    .userId(request.getUserId())
                    .randomBoxId(request.getRandomBoxId())
                    .position(position)
                    .estimatedWaitTimeSeconds(estimatedWaitTime)
                    .canPurchase(false)
                    .message("대기열에서 기다려야 합니다. 현재 순서: " + position)
                    .build();

            return ResponseEntity.accepted().body(response);
        }

        // 구매 진행
        Purchase purchase = purchaseService.purchaseRandomBox(
                request.getUserId(),
                request.getRandomBoxId(),
                request.getQuantity(),
                request.getUserCouponId()
        );

        // 다음 사용자 구매 준비
        purchaseService.prepareForPurchase(request.getRandomBoxId());

        // PurchaseResponse 생성
        PurchaseResponse response = PurchaseResponse.from(purchase);

        // 구매 결과 목록 설정
        List<PurchaseResult> results = purchaseService.getPurchaseResults(purchase.getId());
        List<PurchaseResponse.PurchaseResultResponse> resultResponses = results.stream()
                .map(PurchaseResponse.PurchaseResultResponse::from)
                .collect(Collectors.toList());
        response.setResults(resultResponses);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseResponse> getPurchase(@PathVariable Long id) {
        Purchase purchase = purchaseService.getPurchase(id);

        // PurchaseResponse 생성
        PurchaseResponse response = PurchaseResponse.from(purchase);

        // 구매 결과 목록 설정
        List<PurchaseResult> results = purchaseService.getPurchaseResults(purchase.getId());
        List<PurchaseResponse.PurchaseResultResponse> resultResponses = results.stream()
                .map(PurchaseResponse.PurchaseResultResponse::from)
                .collect(Collectors.toList());
        response.setResults(resultResponses);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PurchaseResponse>> getUserPurchases(@PathVariable Long userId) {
        List<Purchase> purchases = purchaseService.getUserPurchases(userId);
        List<PurchaseResponse> responses = purchases.stream()
                .map(purchase -> {
                    // PurchaseResponse 생성
                    PurchaseResponse response = PurchaseResponse.from(purchase);

                    // 구매 결과 목록 설정
                    List<PurchaseResult> results = purchaseService.getPurchaseResults(purchase.getId());
                    List<PurchaseResponse.PurchaseResultResponse> resultResponses = results.stream()
                            .map(PurchaseResponse.PurchaseResultResponse::from)
                            .collect(Collectors.toList());
                    response.setResults(resultResponses);

                    return response;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/{userId}/{id}")
    public ResponseEntity<PurchaseResponse> cancelPurchase(
            @PathVariable Long userId,
            @PathVariable Long id) {
        Purchase canceledPurchase = purchaseService.cancelPurchase(userId, id);

        // PurchaseResponse 생성
        PurchaseResponse response = PurchaseResponse.from(canceledPurchase);

        // 구매 결과 목록 설정
        List<PurchaseResult> results = purchaseService.getPurchaseResults(canceledPurchase.getId());
        List<PurchaseResponse.PurchaseResultResponse> resultResponses = results.stream()
                .map(PurchaseResponse.PurchaseResultResponse::from)
                .collect(Collectors.toList());
        response.setResults(resultResponses);

        return ResponseEntity.ok(response);
    }
}
