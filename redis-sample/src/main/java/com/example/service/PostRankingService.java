package com.example.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostRankingService {

    private final StringRedisTemplate redisTemplate;
    private static final String KEY = "post:ranking";

    public void increaseViewCount(Long postId) {
        redisTemplate.opsForZSet().incrementScore(KEY, postId.toString(), 1);
    }

    public List<String> getTopPosts(int limit) {
        return redisTemplate.opsForZSet()
                .reverseRange(KEY, 0, limit - 1)
                .stream()
                .collect(Collectors.toList());
    }

    public Double getScore(Long postId) {
        return redisTemplate.opsForZSet().score(KEY, postId.toString());
    }

    public void reset() {
        redisTemplate.delete("post:ranking");
    }
}
