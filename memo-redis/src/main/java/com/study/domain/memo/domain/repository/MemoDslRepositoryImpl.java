package com.study.domain.memo.domain.repository;

import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.study.domain.memo.dto.*;
import com.study.domain.tag.dto.QTagDetailDto;
import com.study.domain.tag.dto.TagDetailDto;
import com.study.global.util.QueryUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.study.domain.memo.domain.entity.QMemo.memo;
import static com.study.domain.memoTag.domain.entity.QMemoTag.memoTag;
import static com.study.domain.tag.domain.entity.QTag.tag;

/**
 * MemoDslRepository 구현체
 */
@Repository
@RequiredArgsConstructor
public class MemoDslRepositoryImpl implements MemoDslRepository {

    private final JPAQueryFactory queryFactory;


    // ------------------------------------------------------------------------------------------------------------------------------------------//

    @Override
    public Page<MemoListDto> findMemoListWithPaging(MemoSearchDto memoSearchDto, Pageable pageable) {
        // 전체 카운트 조회
        final Long totalCount = findMemoListCount(memoSearchDto).fetchOne();

        // 데이터 조회
        final List<MemoListDto> content = findMemoList(memoSearchDto)
                .limit(pageable.getPageSize())
                .offset(pageable.getOffset())
                .fetch();

        return new PageImpl<>(content, pageable, (totalCount==null)? 0 : totalCount);

    }

    // findMemoPaging 전체 카운트 조회
    private JPAQuery<Long> findMemoListCount(MemoSearchDto memoSearchDto){
        return queryFactory
                .select(memo.count())
                .from(memo)
                .where(memoListSearchCondition(memoSearchDto));
    }

    // findMemoPaging 목록 조회
    private JPAQuery<MemoListDto> findMemoList(MemoSearchDto memoSearchDto){
        return queryFactory
                .select(new QMemoListDto(
                        memo.id,
                        memo.title,
                        memo.content,
                        memo.favorite,
                        memo.viewCount,
                        memo.createdAt
                ))
                .from(memo)
                .where(memoListSearchCondition(memoSearchDto));
    }

    // findMemoPaging 조회 조건
    private Predicate[] memoListSearchCondition(MemoSearchDto memoSearchDto){
        List<Predicate> predicates = new ArrayList<>();

        if ("title".equals(memoSearchDto.getSearchType())) {
            predicates.add(QueryUtils.contains(memoSearchDto.getKeyword(), memo.title));
        }

        if ("content".equals(memoSearchDto.getSearchType())) {
            predicates.add(QueryUtils.eq(memoSearchDto.getKeyword(), memo.content));
        }

        if ("both".equals(memoSearchDto.getSearchType())) {
            predicates.add(
                QueryUtils.eq(memoSearchDto.getKeyword(), memo.title)
                          .or(QueryUtils.eq(memoSearchDto.getKeyword(), memo.content))
            );
        }

        return predicates.toArray(new Predicate[0]);
    }

    // ------------------------------------------------------------------------------------------------------------------------------------------//

    @Override
    public Optional<MemoDetailDto> findMemoDetail(Long id){
        // 메모 기본 정보 조회
        MemoDetailDto memoDetailDto = queryFactory
                .select(new QMemoDetailDto(
                        memo.id,
                        memo.title,
                        memo.content,
                        memo.favorite,
                        memo.viewCount,
                        memo.createdAt
                ))
                .from(memo)
                .where(memo.id.eq(id))
                .fetchOne();

        // 메모가 존재하지 않으면 빈 Optional 반환
        if (memoDetailDto == null) {
            return Optional.empty();
        }

        // 메모와 연관된 태그 목록 조회

        List<TagDetailDto> tagList = queryFactory
                .select(new QTagDetailDto(
                        tag.id,
                        tag.name
                ))
                .from(memoTag)
                .join(tag).on(memoTag.tagId.eq(tag.id))
                .where(memoTag.memoId.eq(id))
                .fetch();

        // 태그 목록 설정
        memoDetailDto.setTags(tagList);

        return Optional.of(memoDetailDto);
    }


}
