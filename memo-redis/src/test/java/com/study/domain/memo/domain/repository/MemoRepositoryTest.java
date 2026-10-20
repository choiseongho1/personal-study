package com.study.domain.memo.domain.repository;

import com.study.domain.memo.domain.entity.Memo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * explain the Class File
 *
 * @author : choiseongho
 * @fileName : MemoRepositoryTest.java
 * @since : 2025-09-17
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MemoRepositoryTest {

    @Autowired
    MemoRepository memoRepository;

    @Test
    @DisplayName("메모 저장 (성공)")
    void create_new_memo_test(){
        Memo memo = Memo.builder()
                .title("제목입니다.")
                .content("내용입니다.")
                .favorite(false)
                .build();


        Memo saveMemo = memoRepository.save(memo);

        assertNotNull(saveMemo);
    }


    @Test
    @DisplayName("메모 favorite 없이 저장")
    void create_new_memo_without_favorite(){
        Memo memo = Memo.builder()
                .title("제목입니다.")
                .content("내용입니다.")
                .build();


        Memo saveMemo = memoRepository.save(memo);

        assertEquals(false, saveMemo.getFavorite());
    }

    @Test
    @DisplayName("ID로 메모 정보 조회")
    void search_memo_by_id_test(){
        Memo memo = Memo.builder()
                .title("제목입니다.")
                .content("내용입니다.")
                .build();


        Memo saveMemo = memoRepository.save(memo);

        assertEquals(false, saveMemo.getFavorite());
    }
}
