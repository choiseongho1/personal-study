package com.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SpringBootTest
@AutoConfigureMockMvc
public class RedisStreamMultiConsumerTest {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private final String STREAM_KEY = "notice-stream";
    private final String GROUP = "notice-group";

    @BeforeEach
    void setup() {
        try {
            redisTemplate.opsForStream().createGroup(STREAM_KEY, ReadOffset.latest(), GROUP);
        } catch (Exception ignored) {
        }
    }

    @Test
    void 여러_컨슈머가_분산_처리한다() throws Exception {
        // given: 10개의 메시지를 미리 발행
        for (int i = 1; i <= 10; i++) {
            Map<String, String> map = new HashMap<>();
            map.put("title", "제목 " + i);
            map.put("content", "내용 " + i);

            redisTemplate.opsForStream()
                    .add(StreamRecords.mapBacked(map).withStreamKey(STREAM_KEY));
        }

        // when: 3명의 Consumer가 동시에 메시지 처리
        ExecutorService executor = Executors.newFixedThreadPool(3);

        List<String> consumerIds = List.of("consumer-1", "consumer-2", "consumer-3");

        for (String consumerId : consumerIds) {
            executor.submit(() -> {
                List<MapRecord<String, Object, Object>> messages =
                        redisTemplate.opsForStream().read(
                                Consumer.from(GROUP, consumerId),
                                StreamReadOptions.empty().block(Duration.ofSeconds(2)).count(5),
                                StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed())
                        );

                for (MapRecord<String, Object, Object> msg : messages) {
                    System.out.println("✅ " + consumerId + " 수신: " + msg.getValue());
                    redisTemplate.opsForStream().acknowledge(STREAM_KEY, GROUP, msg.getId());
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }
}
