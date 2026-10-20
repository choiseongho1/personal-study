package com.example.service;

import com.example.dto.NoticeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StreamPublisher {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String STREAM_KEY = "notice-stream";

    public void publish(NoticeMessage notice) {
        Map<String, String> message = new HashMap<>();
        message.put("title", notice.getTitle());
        message.put("content", notice.getContent());

        redisTemplate.opsForStream()
                .add(StreamRecords.mapBacked(message).withStreamKey(STREAM_KEY));
    }
}
