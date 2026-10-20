package com.example.service;

import com.example.util.RankingKeyUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WeeklyPostRankingService {

    private final StringRedisTemplate redisTemplate;
    private static final long TTL_SECONDS = Duration.ofDays(8).getSeconds();

    // ✅ 주간 조회수 1 증가
    public void increaseWeeklyView(Long postId) {
        String weeklyKey = RankingKeyUtil.getWeeklyRankingKey();
        redisTemplate.opsForZSet().incrementScore(weeklyKey, postId.toString(), 1);

        // TTL 설정 (처음 접근 시에만 설정)
        Boolean hasTtl = redisTemplate.getExpire(weeklyKey) > 0;
        if (!Boolean.TRUE.equals(hasTtl)) {
            redisTemplate.expire(weeklyKey, Duration.ofSeconds(TTL_SECONDS));
        }
    }

    // ✅ 주간 TOP N 조회
    public List<String> getWeeklyTopPosts(int limit) {
        String weeklyKey = RankingKeyUtil.getWeeklyRankingKey();
        return redisTemplate.opsForZSet()
                .reverseRange(weeklyKey, 0, limit - 1)
                .stream().toList();
    }

    // ✅ 특정 주차 랭킹 조회
    public List<String> getRankingByKey(String rankingKey, int limit) {
        return redisTemplate.opsForZSet()
                .reverseRange(rankingKey, 0, limit - 1)
                .stream().toList();
    }
}
