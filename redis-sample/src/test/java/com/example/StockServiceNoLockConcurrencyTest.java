package com.example;

import com.example.service.StockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class StockServiceNoLockConcurrencyTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private StockService stockService;


    private final String stockKey = "product:stock";

    @BeforeEach
    void setUp() {
        redisTemplate.opsForValue().set(stockKey, "10");
    }



    @Test
    void 동시에_10번_재고감소_요청_락_없을때() throws InterruptedException {
        // 1. 초기 재고 10으로 세팅
        redisTemplate.opsForValue().set("product:stock", "10");

        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    stockService.decreaseStockDoNotLock("1");
                } catch (Exception e) {
                    // 예외 발생 무시 (재고 없음 등)
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        // 2. 최종 재고 값 확인 (Race Condition 때문에 0 이상일 수 있음)
        String finalStock = redisTemplate.opsForValue().get("product:stock");
        System.out.println("최종 재고: " + finalStock);
    }

}
