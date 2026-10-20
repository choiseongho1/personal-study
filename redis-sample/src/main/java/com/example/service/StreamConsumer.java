package com.example.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
@RequiredArgsConstructor
public class StreamConsumer {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String STREAM_KEY = "notice-stream";
    private static final String GROUP_NAME = "notice-group";
    private static final String CONSUMER_NAME = "consumer-1";

    @PostConstruct
    public void initGroup() {
        try {
            redisTemplate.opsForStream().createGroup(STREAM_KEY, GROUP_NAME);
        } catch (Exception e) {
            System.out.println("✅ Consumer Group already exists: " + e.getMessage());
        }
    }

    @Scheduled(fixedDelay = 3000) // 3초마다 polling
    public void poll() {
        List<MapRecord<String, Object, Object>> messages =
                redisTemplate.opsForStream().read(
                        Consumer.from(GROUP_NAME, CONSUMER_NAME),
                        StreamReadOptions.empty().count(10).block(Duration.ofSeconds(2)),
                        StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed())
                );

        for (MapRecord<String, Object, Object> message : messages) {
            System.out.println("📥 수신된 메시지: " + message.getValue());
            redisTemplate.opsForStream().acknowledge(STREAM_KEY, GROUP_NAME, message.getId());
        }
    }
}
