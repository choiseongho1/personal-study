package com.example.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostLikeRankingService {

    private final StringRedisTemplate redisTemplate;
    private static final String LIKE_RANKING_KEY = "post:like-ranking";

    public void increaseLike(Long postId) {
        redisTemplate.opsForZSet().incrementScore(LIKE_RANKING_KEY, postId.toString(), 1);
    }

    public List<String> getTopLikedPosts(int limit) {
        return redisTemplate.opsForZSet()
                .reverseRange(LIKE_RANKING_KEY, 0, limit - 1)
                .stream()
                .collect(Collectors.toList());
    }

    public Double getLikeScore(Long postId) {
        return redisTemplate.opsForZSet().score(LIKE_RANKING_KEY, postId.toString());
    }

    public void reset() {
        redisTemplate.delete("post:like-ranking");
    }
}
