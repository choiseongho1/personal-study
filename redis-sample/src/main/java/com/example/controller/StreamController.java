package com.example.controller;

import com.example.dto.NoticeMessage;
import com.example.service.StreamPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stream")
public class StreamController {

    private final StreamPublisher streamPublisher;

    @PostMapping("/publish")
    public ResponseEntity<String> publish(@RequestBody NoticeMessage notice) {
        streamPublisher.publish(notice);
        return ResponseEntity.ok("✅ Stream 메시지 발행 완료");
    }
}
