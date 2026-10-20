package com.example;

import com.example.service.PostLikeRankingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
public class PostLikeRankingServiceTest {

    @Autowired
    private PostLikeRankingService postLikeRankingService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String LIKE_RANKING_KEY = "post:like-ranking";

    @BeforeEach
    void clearRankingData() {
        redisTemplate.delete(LIKE_RANKING_KEY);
    }

    @Test
    void 좋아요_점수_증가_및_랭킹_조회() {
        // given
        postLikeRankingService.increaseLike(101L); // score = 1
        postLikeRankingService.increaseLike(102L); // score = 1
        postLikeRankingService.increaseLike(102L); // score = 2
        postLikeRankingService.increaseLike(103L); // score = 3
        postLikeRankingService.increaseLike(103L); // score = 4
        postLikeRankingService.increaseLike(103L); // score = 5

        // when
        List<String> topPosts = postLikeRankingService.getTopLikedPosts(3);

        // then
        System.out.println("❤️ 좋아요 순 인기글 TOP 3: " + topPosts);
        assertThat(topPosts).containsExactly("103", "102", "101");

        assertThat(postLikeRankingService.getLikeScore(103L)).isEqualTo(3.0);
        assertThat(postLikeRankingService.getLikeScore(102L)).isEqualTo(2.0);
        assertThat(postLikeRankingService.getLikeScore(101L)).isEqualTo(1.0);
    }
}
