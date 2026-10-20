package com.example;

import com.example.dto.NoticeMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = RedisApplication.class)
@AutoConfigureMockMvc
public class NoticePubSubTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    private static final String CHANNEL = "notice-channel";

    private final CountDownLatch latch = new CountDownLatch(1);

    @BeforeEach
    void setUp() {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);

        container.addMessageListener((Message message, byte[] pattern) -> {
            try {
                String json = new String(message.getBody());
                ObjectMapper mapper = new ObjectMapper();
                NoticeMessage notice = mapper.readValue(json, NoticeMessage.class);
                System.out.println("📨 수신된 알림: " + notice.getTitle() + " - " + notice.getContent());
                latch.countDown();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, new ChannelTopic(CHANNEL));

        container.afterPropertiesSet();
        container.start();
    }

    @Test
    void 공지_메시지_발행_then_수신_확인() throws Exception {
        NoticeMessage notice = new NoticeMessage("테스트 제목", "테스트 내용");
        ObjectMapper objectMapper = new ObjectMapper();

        mockMvc.perform(post("/api/notice/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(notice)))
                .andExpect(status().isOk());

        boolean received = latch.await(3, TimeUnit.SECONDS);
        assertThat(received).isTrue(); // 메시지 수신 여부 검증
    }
}
