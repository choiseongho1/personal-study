package com.example.jpa.repository;

import com.example.jpa.dto.PostSearchDto;
import com.example.jpa.dto.PostWithCommentsDto;
import com.example.jpa.dto.UserCustomDto;
import com.example.jpa.dto.UserSearchDto;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostRepositoryCustom {
    Page<PostSearchDto> searchPosts(String author, String category, LocalDate from, LocalDate to, Pageable pageable);

    Page<PostWithCommentsDto> findAllPostsWithComments(
        String author,
        String category,
        LocalDate from,
        LocalDate to,
        Pageable pageable
    );

}
