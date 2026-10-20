package com.example.concurrencylab.work;

import com.example.concurrencylab.order.*;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderExpiryWorker {
    private final OrderRepository orders;
    private final OrderService service;

    @Transactional(propagation = Propagation.NEVER)
    public int runOnce() {
        var ids = orders.findExpiredPending(Instant.now(), PageRequest.of(0, 50));
        for (Long id : ids) {
            try { service.expireOrder(id); }
            catch (RuntimeException exception) { log.error("주문 만료 실패 orderId={}", id, exception); }
        }
        return ids.size();
    }
}
