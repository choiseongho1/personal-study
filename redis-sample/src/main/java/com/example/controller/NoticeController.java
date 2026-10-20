package com.example.controller;

import com.example.dto.NoticeMessage;
import com.example.service.NoticePublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notice")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticePublisher noticePublisher;

    @PostMapping("/publish")
    public ResponseEntity<String> publish(@RequestBody NoticeMessage message) {
        noticePublisher.publish(message);
        return ResponseEntity.ok("✅ 알림 발행 완료");
    }
}
