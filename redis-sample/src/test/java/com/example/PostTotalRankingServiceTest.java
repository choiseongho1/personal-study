package com.example;

import com.example.service.PostLikeRankingService;
import com.example.service.PostRankingService;
import com.example.service.PostTotalRankingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class PostTotalRankingServiceTest {

    @Autowired private PostRankingService postRankingService;
    @Autowired private PostLikeRankingService postLikeRankingService;
    @Autowired private PostTotalRankingService postTotalRankingService;
    @Autowired private StringRedisTemplate redisTemplate;

    @BeforeEach
    void 초기화() {
        // 조회수
        postRankingService.increaseViewCount(1L); // 1
        postRankingService.increaseViewCount(2L); // 2
        postRankingService.increaseViewCount(3L); // 3

        // 좋아요
        postLikeRankingService.increaseLike(1L); // 1
        postLikeRankingService.increaseLike(2L); // 2
        postLikeRankingService.increaseLike(2L);
        postLikeRankingService.increaseLike(3L); // 3
        postLikeRankingService.increaseLike(3L);
        postLikeRankingService.increaseLike(3L);

        redisTemplate.delete("post:total-ranking");
    }

//    @Test
//    void 종합_랭킹_가중치_기반_정렬() {
//        // when
//        postTotalRankingService.calculateTotalRanking();
//
//        List<String> top = postTotalRankingService.getTopRankedPosts(3);
//
//        // then
//        System.out.println("🏆 종합 인기 랭킹 TOP 3: " + top);
//
//        assertThat(top).containsExactly("3", "2", "1");
//    }
}
