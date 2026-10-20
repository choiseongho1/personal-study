package com.example;

import com.example.service.PostRankingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class PostRankingServiceTest {

    @Autowired
    private PostRankingService postRankingService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final String RANKING_KEY = "post:ranking";

    @BeforeEach
    void clearZSet() {
        redisTemplate.delete(RANKING_KEY);
    }

    @Test
    void ZSET_조회수_증가_및_랭킹조회() {
        // when
        postRankingService.increaseViewCount(1L); // score = 1
        postRankingService.increaseViewCount(2L); // score = 1
        postRankingService.increaseViewCount(2L); // score = 2
        postRankingService.increaseViewCount(3L); // score = 1
        postRankingService.increaseViewCount(3L); // score = 2
        postRankingService.increaseViewCount(3L); // score = 3

        // then
        List<String> topPosts = postRankingService.getTopPosts(3);
        System.out.println("🔥 실시간 인기 순위: " + topPosts);

        assertThat(topPosts).containsExactly("3", "2", "1");

        Double score1 = postRankingService.getScore(1L);
        Double score2 = postRankingService.getScore(2L);
        Double score3 = postRankingService.getScore(3L);

        assertThat(score1).isEqualTo(1.0);
        assertThat(score2).isEqualTo(2.0);
        assertThat(score3).isEqualTo(3.0);
    }
}
