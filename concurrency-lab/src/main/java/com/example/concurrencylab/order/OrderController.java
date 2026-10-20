package com.example.concurrencylab.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<CreateOrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        Long id = orderService.placeOrder(request.userId(), request.eventId());
        return ResponseEntity.created(URI.create("/api/orders/" + id))
            .body(new CreateOrderResponse(id));
    }

    // 배치 구현 전 만료 처리를 수동 호출하기 위한 학습용 진입점입니다.
    @PostMapping("/{orderId}/expire")
    public ResponseEntity<Void> expire(@PathVariable @Positive Long orderId) {
        orderService.expireOrder(orderId);
        return ResponseEntity.noContent().build();
    }

    public record CreateOrderRequest(
        @NotNull @Positive Long userId,
        @NotNull @Positive Long eventId
    ) {}
    public record CreateOrderResponse(Long orderId) {}
}
