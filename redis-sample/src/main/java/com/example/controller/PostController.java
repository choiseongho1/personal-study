package com.example.controller;

import com.example.dto.PostDto;
import com.example.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/post")
public class PostController {

    private final PostService postService;

    @GetMapping("/{id}")
    public ResponseEntity<PostDto> getPost(@PathVariable Long id) {
        return ResponseEntity.ok(postService.getPost(id));
    }

    @DeleteMapping("/{id}/cache")
    public ResponseEntity<Void> evict(@PathVariable Long id) {
        postService.clearPostCache(id);
        return ResponseEntity.ok().build();
    }
}
