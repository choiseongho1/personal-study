package com.example.concurrencylab.work;

import com.example.concurrencylab.payment.*;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/practice")
@RequiredArgsConstructor
public class WorkPracticeController {
    private final WorkRunner runner;
    private final OrderExpiryWorker expiry;
    private final WorkRepository work;
    private final PaymentAttemptRepository payments;
    private final FakePaymentGateway gateway;

    @PostMapping("/work/run")
    public int runWork() { return runner.runOnce(); }
    @PostMapping("/expiry/run")
    public int runExpiry() { return expiry.runOnce(); }
    @GetMapping("/work")
    public List<WorkItem> list() { return work.findAll(); }
    @PostMapping("/payments/{attemptId}/scenario")
    public void scenario(@PathVariable Long attemptId, @RequestParam FakePaymentGateway.Scenario scenario) {
        var payment = payments.findById(attemptId).orElseThrow();
        gateway.configureScenario(payment.getIdempotencyKey(), scenario);
    }
}
