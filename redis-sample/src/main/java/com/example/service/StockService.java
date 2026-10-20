package com.example.service;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class StockService {

    private final RedissonClient redissonClient;
    private final String stockKey = "product:stock";

    public void decreaseStock(String productId) {

        RLock lock = redissonClient.getLock("lock:product:" + productId);

        try {
            boolean isLocked = lock.tryLock(5, 2, TimeUnit.SECONDS); // 5초 대기, 2초 유지
            if (!isLocked) {
                throw new RuntimeException("락 획득 실패");
            }

            int stock = Integer.parseInt(redisTemplate.opsForValue().get(stockKey));
            if (stock <= 0) {
                throw new RuntimeException("재고 없음");
            }

            redisTemplate.opsForValue().set(stockKey, String.valueOf(stock - 1));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    public void decreaseStockDoNotLock(String productId) {
        String key = "product:stock";
        String value = redisTemplate.opsForValue().get(key);
        int stock = Integer.parseInt(value);

        System.out.println("📥 호출됨 - 현재 재고: " + stock);
        // 🔥 인위적으로 딜레이 추가 → Race Condition 유도
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (stock <= 0) {
            throw new RuntimeException("재고 없음");
        }

        int newStock = stock - 1;
        System.out.println("✅ 감소 시도 - 새로운 재고: " + newStock);
        redisTemplate.opsForValue().set(key, String.valueOf(newStock));

    }

    public Long decreaseStockWithLua(String productId) {
        String stockKey = "product:stock";

        String script = """
            if tonumber(redis.call('GET', KEYS[1])) > 0 then
                return redis.call('DECR', KEYS[1])
            else
                return -1
            end
        """;

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(script);
        redisScript.setResultType(Long.class);

        return redisTemplate.execute(redisScript, List.of(stockKey));
    }



    public boolean decreaseStockWithWatch(String productId) {
        String key = "product:stock";

        for (int i = 0; i < 3; i++) { // 최대 3회 재시도
            List<Object> result = redisTemplate.execute(new SessionCallback<>() {
                @Override
                public List<Object> execute(RedisOperations operations) {
                    operations.watch(key);

                    String value = (String) operations.opsForValue().get(key);
                    int stock = Integer.parseInt(value);

                    if (stock <= 0) {
                        operations.unwatch();
                        return null;
                    }

                    operations.multi();
                    operations.opsForValue().set(key, String.valueOf(stock - 1));
                    return operations.exec();
                }
            });

            if (result != null) {
                System.out.println("✅ 감소 성공");
                return true;
            }

            System.out.println("🔁 충돌 발생, 재시도 시도: " + (i + 1));
        }

        return false;
    }





    @Autowired
    private RedisTemplate<String, String> redisTemplate;
}
