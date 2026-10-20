package com.study.domain.memo.domain.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.study.domain.memo.domain.entity.Memo;
import com.study.domain.memo.dto.MemoListDto;
import com.study.domain.memo.dto.MemoSearchDto;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.study.global.config.QuerydslConfig;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * MemoDslRepositoryImpl 테스트 클래스
 *
 * @author : choiseongho
 * @fileName : MemoDslRepositoryImplTest.java
 * @since : 2025-09-17
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(QuerydslConfig.class)
class MemoDslRepositoryImplTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private MemoRepository memoRepository;

    private MemoDslRepositoryImpl memoDslRepositoryImpl;

    @BeforeEach
    void setUp() {
        // JPAQueryFactory 및 MemoDslRepositoryImpl 초기화
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        memoDslRepositoryImpl = new MemoDslRepositoryImpl(queryFactory);

        // 테스트 데이터 생성
        createTestData();
    }

    private void createTestData() {
        // 테스트 데이터 초기화
        memoRepository.deleteAll();

        // 테스트용 메모 데이터 생성
        Memo memo1 = Memo.builder()
                .title("테스트 제목1")
                .content("테스트 내용1")
                .favorite(true)
                .viewCount(10L)
                .build();

        Memo memo2 = Memo.builder()
                .title("테스트 제목2")
                .content("다른 내용2")
                .favorite(false)
                .viewCount(5L)
                .build();

        Memo memo3 = Memo.builder()
                .title("다른 제목3")
                .content("테스트 내용3")
                .favorite(true)
                .viewCount(15L)
                .build();

        // 데이터 저장
        memoRepository.saveAll(List.of(memo1, memo2, memo3));
    }

    @Test
    @DisplayName("제목으로 메모 검색 테스트")
    void findMemoPagingByTitle() {
        // given
        MemoSearchDto searchDto = MemoSearchDto.builder()
                .searchType("title")
                .keyword("테스트")
                .build();

        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<MemoListDto> result = memoDslRepositoryImpl.findMemoListWithPaging(searchDto, pageable);

        // then
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());

        // 검색된 메모의 제목에 "테스트"가 포함되어 있는지 확인
        result.getContent().forEach(memo ->
            assertThat(memo.getTitle()).contains("테스트")
        );
    }

    @Test
    @DisplayName("내용으로 메모 검색 테스트")
    void findMemoPagingByContent() {
        // given
        MemoSearchDto searchDto = MemoSearchDto.builder()
                .searchType("content")
                .keyword("테스트 내용1")
                .build();

        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<MemoListDto> result = memoDslRepositoryImpl.findMemoListWithPaging(searchDto, pageable);

        // then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals("테스트 제목1", result.getContent().get(0).getTitle());
    }

    @Test
    @DisplayName("제목 또는 내용으로 메모 검색 테스트")
    void findMemoPagingByTitleOrContent() {
        // given
        MemoSearchDto searchDto = MemoSearchDto.builder()
                .searchType("both")
                .keyword("테스트 내용1")
                .build();

        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<MemoListDto> result = memoDslRepositoryImpl.findMemoListWithPaging(searchDto, pageable);

        // then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals("테스트 제목1", result.getContent().get(0).getTitle());
    }

    @Test
    @DisplayName("검색 조건 없이 전체 메모 조회 테스트")
    void findAllMemoPaging() {
        // given
        MemoSearchDto searchDto = new MemoSearchDto();
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<MemoListDto> result = memoDslRepositoryImpl.findMemoListWithPaging(searchDto, pageable);

        // then
        assertNotNull(result);
        assertEquals(3, result.getTotalElements());
        assertEquals(3, result.getContent().size());
    }

    @Test
    @DisplayName("페이징 처리 테스트")
    void testPagination() {
        // given
        MemoSearchDto searchDto = new MemoSearchDto();
        Pageable pageable = PageRequest.of(0, 2); // 페이지 크기를 2로 설정

        // when
        Page<MemoListDto> result = memoDslRepositoryImpl.findMemoListWithPaging(searchDto, pageable);

        // then
        assertNotNull(result);
        assertEquals(3, result.getTotalElements()); // 전체 요소 수는 3개
        assertEquals(2, result.getContent().size()); // 현재 페이지에는 2개만 표시
        assertEquals(2, result.getPageable().getPageSize());
        assertEquals(0, result.getPageable().getPageNumber());
        assertEquals(2, result.getTotalPages()); // 총 2페이지 (3개 항목을 2개씩 나누면)
    }

    @Test
    @DisplayName("두 번째 페이지 조회 테스트")
    void testSecondPage() {
        // given
        MemoSearchDto searchDto = new MemoSearchDto();
        Pageable pageable = PageRequest.of(1, 2); // 두 번째 페이지, 페이지 크기 2

        // when
        Page<MemoListDto> result = memoDslRepositoryImpl.findMemoListWithPaging(searchDto, pageable);

        // then
        assertNotNull(result);
        assertEquals(3, result.getTotalElements());
        assertEquals(1, result.getContent().size()); // 두 번째 페이지에는 1개 항목만 있어야 함
        assertEquals(1, result.getPageable().getPageNumber());
    }
}
