package com.example.concurrencylab.payment;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** 가짜 결제 요청 및 재조회를 실행하는 로컬 학습용 API입니다. */
@RestController
@RequestMapping("/api/practice/payments")
@RequiredArgsConstructor
public class PaymentRequestController {
    private final PaymentRequestService paymentRequestService;

    @PostMapping("/{attemptId}/request")
    public ResponseEntity<Void> request(@PathVariable @Positive Long attemptId) {
        paymentRequestService.requestPayment(attemptId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{attemptId}/reconcile")
    public ResponseEntity<Void> reconcile(@PathVariable @Positive Long attemptId) {
        paymentRequestService.reconcilePayment(attemptId);
        return ResponseEntity.noContent().build();
    }
}
