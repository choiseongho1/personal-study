package com.example.service;

import com.example.dto.NoticeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NoticePublisher {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String CHANNEL = "notice-channel";

    public void publish(NoticeMessage message) {
        redisTemplate.convertAndSend(CHANNEL, message);
    }
}
