package com.example;


import com.example.dto.NoticeMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = RedisApplication.class)
@AutoConfigureMockMvc
public class RedisStreamTest {

    private static final String STREAM_KEY = "notice-stream";
    private static final String GROUP_NAME = "notice-group";
    private static final String CONSUMER_NAME = "consumer-test";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @BeforeEach
    void setUp() {
        try {
            redisTemplate.opsForStream().createGroup(STREAM_KEY, ReadOffset.latest(), GROUP_NAME);
        } catch (Exception e) {
            // 이미 그룹이 있다면 무시
        }
    }

    @Test
    void 스트림_메시지_발행_then_수신확인() throws Exception {
        // given
        NoticeMessage message = new NoticeMessage("Stream 테스트", "Redis Stream 동작 확인");
        ObjectMapper mapper = new ObjectMapper();

        // when - 메시지 발행 API 호출
        mockMvc.perform(post("/api/stream/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(message)))
                .andExpect(status().isOk());

        // then - Stream에서 직접 메시지 읽기 (소비자 역할)
        List<MapRecord<String, Object, Object>> messages =
                redisTemplate.opsForStream().read(
                        Consumer.from(GROUP_NAME, CONSUMER_NAME),
                        StreamReadOptions.empty().block(Duration.ofSeconds(2)),
                        StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed())
                );

        assertThat(messages).isNotEmpty();
        Map<Object, Object> value = messages.get(0).getValue();

        System.out.println("✅ 수신 메시지: " + value);

        assertThat(value.get("title")).isEqualTo("Stream 테스트");
        assertThat(value.get("content")).isEqualTo("Redis Stream 동작 확인");

        // ACK 처리
        redisTemplate.opsForStream().acknowledge(STREAM_KEY, GROUP_NAME, messages.get(0).getId());
    }
}
