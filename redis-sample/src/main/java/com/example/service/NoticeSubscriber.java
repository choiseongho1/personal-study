package com.example.service;

import com.example.dto.NoticeMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NoticeSubscriber implements MessageListener {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(message.getBody());
            NoticeMessage notice = objectMapper.readValue(json, NoticeMessage.class);
            log.info("📨 알림 수신됨: {} - {}", notice.getTitle(), notice.getContent());
        } catch (Exception e) {
            log.error("메시지 처리 실패", e);
        }
    }
}
