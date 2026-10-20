package com.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = RedisApplication.class)
@AutoConfigureMockMvc
public class StockLuaConcurrencyTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private final String stockKey = "product:stock";

    @BeforeEach
    void initStock() {
        redisTemplate.opsForValue().set(stockKey, "5");
    }

    @Test
    void 동시에_10번_Lua_재고감소_요청() throws Exception {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    MvcResult result = mockMvc.perform(post("/stock/decreaseLua"))
                            .andExpect(status().isOk())
                            .andReturn();

                    String response = result.getResponse().getContentAsString();
                    if (response.contains("-1")) {
                        failCount.incrementAndGet();
                    } else {
                        successCount.incrementAndGet();
                    }

                } catch (Exception e) {
                    System.out.println("요청 실패: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        System.out.println("✅ 성공: " + successCount.get());
        System.out.println("❌ 실패: " + failCount.get());

        // 재고는 음수가 될 수 없어야 한다
        int finalStock = Integer.parseInt(redisTemplate.opsForValue().get(stockKey));
        System.out.println("🔥 최종 재고: " + finalStock);
        assertThat(finalStock).isGreaterThanOrEqualTo(0);
        assertThat(successCount.get()).isEqualTo(5);
    }
}
