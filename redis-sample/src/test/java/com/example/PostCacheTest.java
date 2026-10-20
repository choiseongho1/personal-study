package com.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = RedisApplication.class)
@AutoConfigureMockMvc
public class PostCacheTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private final String key = "post:1";

    @BeforeEach
    void clearCache() {
        redisTemplate.delete(key);
    }

    @Test
    void 캐시_미스_then_DB조회_then_캐시저장() throws Exception {
        // 1. 캐시 없음 → DB 조회 후 캐싱
        MvcResult result1 = mockMvc.perform(get("/post/1"))
                .andExpect(status().isOk())
                .andReturn();

        String first = result1.getResponse().getContentAsString();
        assertThat(first).contains("게시글 내용입니다");

        // 2. 캐시에 저장되었는지 확인
        String cached = redisTemplate.opsForValue().get(key);
        assertThat(cached).isEqualTo(first);
    }

    @Test
    void 캐시_조회_성공() throws Exception {
        redisTemplate.opsForValue().set(key, "🔄 캐시된 게시글", Duration.ofSeconds(10));

        MvcResult result = mockMvc.perform(get("/post/1"))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        assertThat(response).isEqualTo("🔄 캐시된 게시글");
    }

    @Test
    void 캐시_TTL_만료_후_DB_재조회() throws Exception {
        redisTemplate.opsForValue().set(key, "🕐 만료 예정", Duration.ofSeconds(2));

        // 1. 캐시 확인
        mockMvc.perform(get("/post/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("🕐 만료 예정"));

        // 2. TTL 만료 기다리기
        Thread.sleep(2500);

        // 3. 캐시 만료 → 다시 DB에서 조회되었는지 확인
        MvcResult result = mockMvc.perform(get("/post/1"))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        assertThat(response).contains("게시글 내용입니다");
    }
}
