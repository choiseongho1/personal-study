package com.example.jpa.repository;

import static com.querydsl.core.group.GroupBy.groupBy;

import com.example.jpa.dto.CommentDto;
import com.example.jpa.dto.OrderCustomDto;
import com.example.jpa.dto.PostSearchDto;
import com.example.jpa.dto.PostWithCommentsDto;
import com.example.jpa.dto.QCommentDto;
import com.example.jpa.dto.QOrderCustomDto;
import com.example.jpa.dto.QPostSearchDto;
import com.example.jpa.dto.QPostWithCommentsDto;
import com.example.jpa.dto.QUserCustomDto;
import com.example.jpa.dto.UserCustomDto;
import com.example.jpa.dto.UserSearchDto;
import com.example.jpa.entity.QComment;
import com.example.jpa.entity.QOrder;
import com.example.jpa.entity.QPost;
import com.example.jpa.entity.QUser;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.group.GroupBy;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@RequiredArgsConstructor
public class PostRepositoryImpl implements PostRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<PostSearchDto> searchPosts(String author, String category, LocalDate from, LocalDate to, Pageable pageable) {
        QPost post = QPost.post;

        BooleanBuilder builder = new BooleanBuilder();

        if (author != null) {
            builder.and(post.author.eq(author));
        }
        if (category != null) {
            builder.and(post.category.eq(category));
        }
        if (from != null) {
            builder.and(post.createdAt.goe(from.atStartOfDay()));
        }
        if (to != null) {
            builder.and(post.createdAt.loe(to.atTime(23, 59, 59)));
        }

        List<PostSearchDto> content = queryFactory
            .select(new QPostSearchDto(
                post.id, post.title, post.author, post.category, post.createdAt))
            .from(post)
            .where(builder)
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        long total = queryFactory
            .select(post.count())
            .from(post)
            .where(builder)
            .fetchOne();

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Page<PostWithCommentsDto> findAllPostsWithComments(
        String author,
        String category,
        LocalDate from,
        LocalDate to,
        Pageable pageable) {

        QPost post = QPost.post;
        QComment comment = QComment.comment;

        BooleanBuilder builder = new BooleanBuilder();

        if (author != null && !author.isBlank()) {
            builder.and(post.author.eq(author));
        }
        if (category != null && !category.isBlank()) {
            builder.and(post.category.eq(category));
        }
        if (from != null) {
            builder.and(post.createdAt.goe(from.atStartOfDay()));
        }
        if (to != null) {
            builder.and(post.createdAt.loe(to.atTime(23, 59, 59)));
        }

        // 1. 게시글 페이징 조회
        List<PostWithCommentsDto> posts = queryFactory
            .select(new QPostWithCommentsDto(
                post.id,
                post.title,
                post.author,
                post.category,
                post.createdAt
            ))
            .from(post)
            .where(builder)
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .orderBy(post.createdAt.desc()) // 예시 정렬
            .fetch();

        long total = queryFactory
            .select(post.count())
            .from(post)
            .where(builder)
            .fetchOne();

        if (posts.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, total);
        }

        // 2. 댓글 조회 + postId 기준 그룹핑
        List<Long> postIds = posts.stream().map(PostWithCommentsDto::getId).toList();

        Map<Long, List<CommentDto>> commentMap = queryFactory
            .select(new QCommentDto(
                comment.id,
                comment.writer,
                comment.content,
                comment.post.id
            ))
            .from(comment)
            .where(comment.post.id.in(postIds))
            .fetch()
            .stream()
            .collect(Collectors.groupingBy(CommentDto::getPostId));

        // 3. 댓글 조립
        posts.forEach(newPost -> {
            newPost.setComments(commentMap.getOrDefault(newPost.getId(), List.of()));
        });

        return new PageImpl<>(posts, pageable, total);
    }

}
