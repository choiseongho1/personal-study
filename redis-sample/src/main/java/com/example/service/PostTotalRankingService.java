package com.example.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.RedisZSetCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostTotalRankingService {

    private final StringRedisTemplate redisTemplate;

    private static final String VIEW_KEY = "post:ranking";
    private static final String LIKE_KEY = "post:like-ranking";
    private static final String TOTAL_KEY = "post:total-ranking";

    public void calculateTotalRankingByJava(double viewWeight, double likeWeight) {
        // 1. 조회수 ZSET 모든 항목 조회
        Set<ZSetOperations.TypedTuple<String>> viewSet =
                redisTemplate.opsForZSet().rangeWithScores(VIEW_KEY, 0, -1);

        // 2. 좋아요 ZSET 모든 항목 조회
        Set<ZSetOperations.TypedTuple<String>> likeSet =
                redisTemplate.opsForZSet().rangeWithScores(LIKE_KEY, 0, -1);

        // 3. Map으로 변환
        Map<String, Double> viewMap = new HashMap<>();
        Map<String, Double> likeMap = new HashMap<>();

        if (viewSet != null) {
            for (ZSetOperations.TypedTuple<String> tuple : viewSet) {
                viewMap.put(tuple.getValue(), tuple.getScore());
            }
        }

        if (likeSet != null) {
            for (ZSetOperations.TypedTuple<String> tuple : likeSet) {
                likeMap.put(tuple.getValue(), tuple.getScore());
            }
        }

        // 4. 전체 키 집합
        Set<String> allPostIds = new HashSet<>();
        allPostIds.addAll(viewMap.keySet());
        allPostIds.addAll(likeMap.keySet());

        // 5. 점수 계산 및 ZADD
        redisTemplate.delete(TOTAL_KEY);

        for (String postId : allPostIds) {
            double viewScore = viewMap.getOrDefault(postId, 0.0);
            double likeScore = likeMap.getOrDefault(postId, 0.0);
            double total = viewScore * viewWeight + likeScore * likeWeight;

            redisTemplate.opsForZSet().add(TOTAL_KEY, postId, total);
        }
    }

    public List<String> getTopRankedPosts(int limit) {
        return redisTemplate.opsForZSet()
                .reverseRange(TOTAL_KEY, 0, limit - 1)
                .stream()
                .collect(Collectors.toList());
    }

    public void reset() {
        redisTemplate.delete(TOTAL_KEY);
    }
}
