package com.example.concurrencylab.payment;

import com.example.concurrencylab.order.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** 실제 결제사 웹훅이 아닌, 로컬 실습용 수동 결과 입력입니다. */
@RestController
@RequestMapping("/api/practice/payments")
@RequiredArgsConstructor
public class PaymentSimulationController {
    private final OrderService orderService;

    @PostMapping("/{attemptId}/succeeded")
    public ResponseEntity<Void> succeeded(
        @PathVariable @Positive Long attemptId,
        @Valid @RequestBody SuccessRequest request
    ) {
        orderService.paymentSucceeded(attemptId, request.providerPaymentId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{attemptId}/failed")
    public ResponseEntity<Void> failed(@PathVariable @Positive Long attemptId) {
        orderService.paymentFailed(attemptId);
        return ResponseEntity.noContent().build();
    }

    public record SuccessRequest(@NotBlank String providerPaymentId) {}
}
