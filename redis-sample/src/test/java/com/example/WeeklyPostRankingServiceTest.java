package com.example;

import com.example.service.WeeklyPostRankingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class WeeklyPostRankingServiceTest {

    @Autowired
    private WeeklyPostRankingService weeklyRankingService;

    @Test
    void 주간_조회수_랭킹_증가_및_조회() {
        weeklyRankingService.increaseWeeklyView(1L);
        weeklyRankingService.increaseWeeklyView(2L);
        weeklyRankingService.increaseWeeklyView(2L);
        weeklyRankingService.increaseWeeklyView(3L);
        weeklyRankingService.increaseWeeklyView(3L);
        weeklyRankingService.increaseWeeklyView(3L);

        List<String> top = weeklyRankingService.getWeeklyTopPosts(3);

        System.out.println("📅 이번 주 랭킹 TOP 3: " + top);
        assertThat(top).containsExactly("3", "2", "1");
    }
}
