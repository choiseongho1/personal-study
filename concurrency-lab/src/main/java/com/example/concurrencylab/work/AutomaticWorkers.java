package com.example.concurrencylab.work;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.*;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
@ConditionalOnProperty(name = "lab.workers.enabled", havingValue = "true")
public class AutomaticWorkers {
    private final WorkRunner work;
    private final OrderExpiryWorker expiry;

    @Scheduled(fixedDelayString = "${lab.workers.delay-ms:1000}")
    public void tick() {
        expiry.runOnce();
        work.runOnce();
    }
}
