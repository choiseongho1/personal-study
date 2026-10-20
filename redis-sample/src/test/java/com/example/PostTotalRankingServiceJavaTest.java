package com.example;

import com.example.service.PostLikeRankingService;
import com.example.service.PostRankingService;
import com.example.service.PostTotalRankingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class PostTotalRankingServiceJavaTest {

    @Autowired private PostRankingService postRankingService;
    @Autowired private PostLikeRankingService postLikeRankingService;
    @Autowired private PostTotalRankingService postTotalRankingService;

    @BeforeEach
    void setup() {
        postRankingService.reset();
        postLikeRankingService.reset();
        postTotalRankingService.reset();

        // 조회수
        postRankingService.increaseViewCount(1L); // 1
        postRankingService.increaseViewCount(2L); // 2
        postRankingService.increaseViewCount(3L); // 3

        // 좋아요
        postLikeRankingService.increaseLike(1L); // 3
        postLikeRankingService.increaseLike(1L);
        postLikeRankingService.increaseLike(1L);

        postLikeRankingService.increaseLike(2L); // 2
        postLikeRankingService.increaseLike(2L);

        postLikeRankingService.increaseLike(3L); // 1
    }

    @Test
    void 자바기반_종합랭킹_정렬_테스트() {
        // 조회수 * 1.0, 좋아요 * 2.0
        postTotalRankingService.calculateTotalRankingByJava(1.0, 2.0);

        List<String> top = postTotalRankingService.getTopRankedPosts(3);

        System.out.println("🧮 Java 계산 기반 종합 TOP 3: " + top);

        /**
         * 가중치 적용 결과:
         * 1번: 1 + 3*2 = 7
         * 2번: 2 + 2*2 = 6
         * 3번: 3 + 1*2 = 5
         */
        assertThat(top).containsExactly("1", "2", "3");
    }
}
