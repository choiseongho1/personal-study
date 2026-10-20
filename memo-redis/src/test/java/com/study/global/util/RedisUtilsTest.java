package com.study.global.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Redis 유틸리티 테스트
 * 참고: 이 테스트를 실행하려면 Redis 서버가 실행 중이어야 합니다.
 */
@SpringBootTest
class RedisUtilsTest {

    @Autowired
    private RedisUtils redisUtils;

    @Test
    @DisplayName("Redis 키-값 저장 및 조회 테스트")
    void setAndGetTest() {
        // given
        String key = "test:key";
        String value = "test-value";

        // when
        redisUtils.set(key, value);
        Object result = redisUtils.get(key);

        // then
        assertEquals(value, result);

        // cleanup
        redisUtils.delete(key);
    }

    @Test
    @DisplayName("Redis 만료 시간 설정 테스트")
    void expireTest() throws InterruptedException {
        // given
        String key = "test:expire";
        String value = "expire-value";

        // when
        redisUtils.set(key, value, 1, TimeUnit.SECONDS);

        // then
        assertTrue(redisUtils.hasKey(key));

        // 1.5초 대기
        Thread.sleep(1500);

        // 만료 확인
        assertFalse(redisUtils.hasKey(key));
    }

    @Test
    @DisplayName("Redis 증분 테스트")
    void incrementTest() {
        // given
        String key = "test:counter";

        // when
        Long value1 = redisUtils.increment(key);
        Long value2 = redisUtils.increment(key);
        Long value3 = redisUtils.increment(key, 3);

        // then
        assertEquals(1L, value1);
        assertEquals(2L, value2);
        assertEquals(5L, value3);

        // cleanup
        redisUtils.delete(key);
    }

    @Test
    @DisplayName("Redis Sorted Set 테스트")
    void zSetTest() {
        // given
        String key = "test:zset";

        // when
        redisUtils.zAdd(key, "item1", 1.0);
        redisUtils.zAdd(key, "item2", 2.0);
        redisUtils.zAdd(key, "item3", 3.0);

        // 점수 증가
        Double newScore = redisUtils.zIncrement(key, "item1", 2.5);

        // 조회
        Set<Object> top2 = redisUtils.zReverseRange(key, 0, 1);
        Set<ZSetOperations.TypedTuple<Object>> top2WithScores = redisUtils.zReverseRangeWithScores(key, 0, 1);

        // then
        assertEquals(3.5, newScore);
        assertEquals(2, top2.size());
        assertTrue(top2.contains("item1"));
        assertTrue(top2.contains("item3"));

        assertEquals(2, top2WithScores.size());

        // cleanup
        redisUtils.delete(key);
    }
}
