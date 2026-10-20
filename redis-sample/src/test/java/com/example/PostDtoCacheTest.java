package com.example;

import com.example.dto.PostDto;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = RedisApplication.class)
@AutoConfigureMockMvc
public class PostDtoCacheTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RedisTemplate<String, PostDto> postRedisTemplate;

    private final String key = "post:1";

    @BeforeEach
    void clearCache() {
        postRedisTemplate.delete(key);
    }

    @Test
    void 객체캐시_미스_then_DB조회_then_캐시저장() throws Exception {
        // 1. 캐시 없음 → DB 조회
        MvcResult result = mockMvc.perform(get("/post/1"))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();

        // 2. 캐시에 PostDto 객체 저장됐는지 확인
        PostDto cached = postRedisTemplate.opsForValue().get(key);
        assertThat(cached).isNotNull();
        assertThat(cached.getTitle()).isEqualTo("제목입니다");
        assertThat(cached.getContent()).isEqualTo("게시글 내용입니다");

        // 3. JSON 응답도 올바른지 확인
        ObjectMapper objectMapper = new ObjectMapper();
        PostDto response = objectMapper.readValue(json, PostDto.class);
        assertThat(response.getId()).isEqualTo(1L);
    }

    @Test
    void 객체캐시_TTL_만료_후_재조회() throws Exception {
        postRedisTemplate.opsForValue().set(key,
                new PostDto(1L, "임시 제목", "임시 내용"),
                Duration.ofSeconds(2));

        // 1. 캐시된 객체 확인
        MvcResult first = mockMvc.perform(get("/post/1"))
                .andExpect(status().isOk())
                .andReturn();

        String response1 = first.getResponse().getContentAsString();
        assertThat(response1).contains("임시 제목");

        // 2. TTL 만료 대기
        Thread.sleep(2500);

        // 3. 캐시 만료 후 DB에서 새로 조회된 데이터 확인
        MvcResult second = mockMvc.perform(get("/post/1"))
                .andExpect(status().isOk())
                .andReturn();

        String response2 = second.getResponse().getContentAsString();
        assertThat(response2).contains("제목입니다");
    }
}
