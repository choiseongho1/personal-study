package com.example.jpa.controller;

import com.example.jpa.dto.PostSearchDto;
import com.example.jpa.dto.PostWithCommentsDto;
import com.example.jpa.repository.PostRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/post")
public class PostController {

    private final PostRepository postRepository;

    @GetMapping("/search")
    public Page<PostSearchDto> search(
        @RequestParam(required = false) String author,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        Pageable pageable
    ) {
        return postRepository.searchPosts(author, category, from, to, pageable);
    }

    @GetMapping("/with-comments")
    public Page<PostWithCommentsDto> getPostsWithComments(
        @RequestParam(required = false) String author,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        Pageable pageable
    ) {
        return postRepository.findAllPostsWithComments(author, category, from, to, pageable);
    }

}
