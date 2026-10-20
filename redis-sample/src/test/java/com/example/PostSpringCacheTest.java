package com.example;

import com.example.dto.PostDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = RedisApplication.class)
@AutoConfigureMockMvc
public class PostSpringCacheTest {

    @Autowired
    private MockMvc mockMvc;

    private final String cacheEvictUrl = "/post/1/cache";
    private final String postUrl = "/post/1";

    @BeforeEach
    void clearManually() throws Exception {
        // 테스트 전에 캐시 직접 삭제
        mockMvc.perform(delete(cacheEvictUrl)).andExpect(status().isOk());
    }

    @Test
    void 캐시_미스_then_DB조회_then_캐시저장() throws Exception {
        // 1. 첫 요청 → DB 조회
        MvcResult result1 = mockMvc.perform(get(postUrl))
                .andExpect(status().isOk())
                .andReturn();
        String json1 = result1.getResponse().getContentAsString();

        // 2. 두 번째 요청 → 캐시에서 가져옴
        MvcResult result2 = mockMvc.perform(get(postUrl))
                .andExpect(status().isOk())
                .andReturn();
        String json2 = result2.getResponse().getContentAsString();

        assertThat(json1).isEqualTo(json2); // 결과는 같아야 함
    }

    @Test
    void 캐시_삭제_then_DB_재조회() throws Exception {
        // 1. 캐시 저장
        mockMvc.perform(get(postUrl)).andExpect(status().isOk());

        // 2. 캐시 삭제
        mockMvc.perform(delete(cacheEvictUrl)).andExpect(status().isOk());

        // 3. 삭제 후 다시 요청 → DB 조회
        MvcResult result = mockMvc.perform(get(postUrl))
                .andExpect(status().isOk())
                .andReturn();

        PostDto response = new ObjectMapper().readValue(result.getResponse().getContentAsString(), PostDto.class);
        assertThat(response.getTitle()).isEqualTo("제목입니다");
    }

    @Test
    void TTL_만료_후_재조회() throws Exception {
        // 1. 캐시 저장
        mockMvc.perform(get(postUrl)).andExpect(status().isOk());

        // 2. TTL 대기 (CacheConfig에서 10초로 설정됨)
        Thread.sleep(11_000);

        // 3. TTL 만료 후 다시 요청 → DB 재조회
        MvcResult result = mockMvc.perform(get(postUrl))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        assertThat(json).contains("제목입니다");
    }
}
