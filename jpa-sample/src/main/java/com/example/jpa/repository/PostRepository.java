package com.example.jpa.repository;

import com.example.jpa.entity.Post;
import com.example.jpa.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long>, PostRepositoryCustom {
}
