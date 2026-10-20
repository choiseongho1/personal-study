package com.study.global.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 작업을 위한 유틸리티 클래스
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisUtils {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 키-값 저장
     *
     * @param key 키
     * @param value 값
     */
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
        log.debug("Redis SET: key={}", key);
    }

    /**
     * 키-값 저장 (만료 시간 설정)
     *
     * @param key 키
     * @param value 값
     * @param timeout 만료 시간
     * @param unit 시간 단위
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
        log.debug("Redis SET with expiration: key={}, timeout={}, unit={}", key, timeout, unit);
    }

    /**
     * 키-값 저장 (만료 시간 설정 - Duration)
     *
     * @param key 키
     * @param value 값
     * @param duration 만료 시간
     */
    public void set(String key, Object value, Duration duration) {
        redisTemplate.opsForValue().set(key, value, duration);
        log.debug("Redis SET with duration: key={}, duration={}", key, duration);
    }

    /**
     * 값 조회
     *
     * @param key 키
     * @return 값
     */
    public Object get(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        log.debug("Redis GET: key={}", key);
        return value;
    }

    /**
     * 키 삭제
     *
     * @param key 키
     * @return 삭제 여부
     */
    public boolean delete(String key) {
        Boolean result = redisTemplate.delete(key);
        log.debug("Redis DELETE: key={}, result={}", key, result);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 키 존재 여부 확인
     *
     * @param key 키
     * @return 존재 여부
     */
    public boolean hasKey(String key) {
        Boolean result = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 값 증가
     *
     * @param key 키
     * @return 증가된 값
     */
    public Long increment(String key) {
        Long value = redisTemplate.opsForValue().increment(key);
        log.debug("Redis INCR: key={}, value={}", key, value);
        return value;
    }

    /**
     * 값 증가 (지정된 값만큼)
     *
     * @param key 키
     * @param delta 증가량
     * @return 증가된 값
     */
    public Long increment(String key, long delta) {
        Long value = redisTemplate.opsForValue().increment(key, delta);
        log.debug("Redis INCR by delta: key={}, delta={}, value={}", key, delta, value);
        return value;
    }

    /**
     * Sorted Set에 값 추가
     *
     * @param key 키
     * @param value 값
     * @param score 점수
     * @return 추가 여부
     */
    public Boolean zAdd(String key, Object value, double score) {
        Boolean result = redisTemplate.opsForZSet().add(key, value, score);
        log.debug("Redis ZADD: key={}, value={}, score={}", key, value, score);
        return result;
    }

    /**
     * Sorted Set 점수 증가
     *
     * @param key 키
     * @param value 값
     * @param delta 증가량
     * @return 새로운 점수
     */
    public Double zIncrement(String key, Object value, double delta) {
        Double score = redisTemplate.opsForZSet().incrementScore(key, value, delta);
        log.debug("Redis ZINCRBY: key={}, value={}, delta={}, score={}", key, value, delta, score);
        return score;
    }

    /**
     * Sorted Set 범위 조회 (높은 점수 순)
     *
     * @param key 키
     * @param start 시작 인덱스
     * @param end 종료 인덱스
     * @return 값 목록
     */
    public Set<Object> zReverseRange(String key, long start, long end) {
        Set<Object> result = redisTemplate.opsForZSet().reverseRange(key, start, end);
        log.debug("Redis ZREVRANGE: key={}, start={}, end={}", key, start, end);
        return result;
    }

    /**
     * Sorted Set 범위 조회 (점수 포함, 높은 점수 순)
     *
     * @param key 키
     * @param start 시작 인덱스
     * @param end 종료 인덱스
     * @return 값과 점수 목록
     */
    public Set<ZSetOperations.TypedTuple<Object>> zReverseRangeWithScores(String key, long start, long end) {
        Set<ZSetOperations.TypedTuple<Object>> result = redisTemplate.opsForZSet().reverseRangeWithScores(key, start, end);
        log.debug("Redis ZREVRANGE with scores: key={}, start={}, end={}", key, start, end);
        return result;
    }

    /**
     * 키의 만료 시간 설정
     *
     * @param key 키
     * @param timeout 만료 시간
     * @param unit 시간 단위
     * @return 설정 성공 여부
     */
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        Boolean result = redisTemplate.expire(key, timeout, unit);
        log.debug("Redis EXPIRE: key={}, timeout={}, unit={}", key, timeout, unit);
        return result;
    }

    /**
     * 키의 만료 시간 설정 (Duration)
     *
     * @param key 키
     * @param duration 만료 시간
     * @return 설정 성공 여부
     */
    public Boolean expire(String key, Duration duration) {
        Boolean result = redisTemplate.expire(key, duration);
        log.debug("Redis EXPIRE: key={}, duration={}", key, duration);
        return result;
    }

    /**
     * 키의 만료 시간 조회
     *
     * @param key 키
     * @return 만료 시간 (초)
     */
    public Long getExpire(String key) {
        return redisTemplate.getExpire(key);
    }
}
