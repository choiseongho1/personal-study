package com.randombox.domain.randombox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RandomBoxInventoryService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedissonClient redissonClient;
    private final RandomBoxRepository randomBoxRepository;

    private static final String INVENTORY_KEY_PREFIX = "randombox:inventory:";
    private static final String LOCK_KEY_PREFIX = "randombox:lock:";
    private static final int LOCK_WAIT_TIME = 5; // 초
    private static final int LOCK_LEASE_TIME = 3; // 초

    /**
     * 랜덤박스 재고 초기화
     * @param randomBoxId 랜덤박스 ID
     */
    public void initializeInventory(Long randomBoxId) {
        RandomBox randomBox = randomBoxRepository.findById(randomBoxId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 랜덤박스입니다."));

        String key = INVENTORY_KEY_PREFIX + randomBoxId;
        redisTemplate.opsForValue().set(key, randomBox.getQuantity());

        log.info("랜덤박스 {} 재고가 {}개로 초기화되었습니다.", randomBoxId, randomBox.getQuantity());
    }

    /**
     * 랜덤박스 재고 조회
     * @param randomBoxId 랜덤박스 ID
     * @return 재고 수량
     */
    public int getInventory(Long randomBoxId) {
        String key = INVENTORY_KEY_PREFIX + randomBoxId;
        Object value = redisTemplate.opsForValue().get(key);

        if (value == null) {
            initializeInventory(randomBoxId);
            value = redisTemplate.opsForValue().get(key);
        }

        return value instanceof Integer ? (Integer) value : 0;
    }

    /**
     * 랜덤박스 재고 감소
     * @param randomBoxId 랜덤박스 ID
     * @param quantity 감소시킬 수량
     * @return 성공 여부
     */
    public boolean decreaseInventory(Long randomBoxId, int quantity) {
        String lockKey = LOCK_KEY_PREFIX + randomBoxId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 분산 락 획득 시도
            boolean locked = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);

            if (!locked) {
                log.warn("랜덤박스 {} 재고 감소를 위한 락 획득에 실패했습니다.", randomBoxId);
                return false;
            }

            // 재고 확인
            int currentInventory = getInventory(randomBoxId);

            if (currentInventory < quantity) {
                log.warn("랜덤박스 {} 재고가 부족합니다. 현재 재고: {}, 요청 수량: {}", randomBoxId, currentInventory, quantity);
                return false;
            }

            // 재고 감소
            String key = INVENTORY_KEY_PREFIX + randomBoxId;
            redisTemplate.opsForValue().set(key, currentInventory - quantity);

            // DB 재고 업데이트 (비동기로 처리할 수도 있음)
            RandomBox randomBox = randomBoxRepository.findById(randomBoxId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 랜덤박스입니다."));
            randomBox.updateQuantity(randomBox.getQuantity() - quantity);
            randomBoxRepository.save(randomBox);

            log.info("랜덤박스 {} 재고가 {}개 감소했습니다. 남은 재고: {}", randomBoxId, quantity, currentInventory - quantity);

            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("랜덤박스 {} 재고 감소 중 인터럽트가 발생했습니다.", randomBoxId, e);
            return false;
        } finally {
            // 락 해제
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 랜덤박스 재고 증가
     * @param randomBoxId 랜덤박스 ID
     * @param quantity 증가시킬 수량
     * @return 성공 여부
     */
    public boolean increaseInventory(Long randomBoxId, int quantity) {
        String lockKey = LOCK_KEY_PREFIX + randomBoxId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 분산 락 획득 시도
            boolean locked = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);

            if (!locked) {
                log.warn("랜덤박스 {} 재고 증가를 위한 락 획득에 실패했습니다.", randomBoxId);
                return false;
            }

            // 재고 증가
            int currentInventory = getInventory(randomBoxId);
            String key = INVENTORY_KEY_PREFIX + randomBoxId;
            redisTemplate.opsForValue().set(key, currentInventory + quantity);

            // DB 재고 업데이트 (비동기로 처리할 수도 있음)
            RandomBox randomBox = randomBoxRepository.findById(randomBoxId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 랜덤박스입니다."));
            randomBox.updateQuantity(randomBox.getQuantity() + quantity);
            randomBoxRepository.save(randomBox);

            log.info("랜덤박스 {} 재고가 {}개 증가했습니다. 현재 재고: {}", randomBoxId, quantity, currentInventory + quantity);

            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("랜덤박스 {} 재고 증가 중 인터럽트가 발생했습니다.", randomBoxId, e);
            return false;
        } finally {
            // 락 해제
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 랜덤박스 재고 동기화
     * @param randomBoxId 랜덤박스 ID
     */
    public void synchronizeInventory(Long randomBoxId) {
        String lockKey = LOCK_KEY_PREFIX + randomBoxId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 분산 락 획득 시도
            boolean locked = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);

            if (!locked) {
                log.warn("랜덤박스 {} 재고 동기화를 위한 락 획득에 실패했습니다.", randomBoxId);
                return;
            }

            // DB에서 최신 재고 조회
            RandomBox randomBox = randomBoxRepository.findById(randomBoxId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 랜덤박스입니다."));

            // Redis 재고 업데이트
            String key = INVENTORY_KEY_PREFIX + randomBoxId;
            redisTemplate.opsForValue().set(key, randomBox.getQuantity());

            log.info("랜덤박스 {} 재고가 DB와 동기화되었습니다. 현재 재고: {}", randomBoxId, randomBox.getQuantity());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("랜덤박스 {} 재고 동기화 중 인터럽트가 발생했습니다.", randomBoxId, e);
        } finally {
            // 락 해제
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
