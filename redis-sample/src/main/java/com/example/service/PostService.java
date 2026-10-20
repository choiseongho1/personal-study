package com.example.service;

import com.example.dto.PostDto;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class PostService {

    private final RedisTemplate<String, PostDto> postRedisTemplate;

    @Cacheable(value = "post", key = "#id")
    public PostDto getPost(Long id) {
        System.out.println("💾 DB에서 조회됨");
        return new PostDto(id, "제목입니다", "캐시 테스트용 본문입니다");
    }

    @CacheEvict(value = "post", key = "#id")
    public void clearPostCache(Long id) {
        System.out.println("🗑 캐시 삭제됨");
    }
}
