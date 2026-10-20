package com.study.domain.memoTag.domain.repository;

import com.study.domain.memoTag.domain.entity.MemoTag;
import com.study.domain.memoTag.domain.entity.MemoTagId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MemoTagRepository extends JpaRepository<MemoTag, MemoTagId> {

    /**
     * 메모 ID로 메모-태그 연결 삭제
     *
     * @param memoId 메모 ID
     */
    @Modifying
    @Query("DELETE FROM MemoTag mt WHERE mt.memoId = :memoId")
    void deleteByMemoId(@Param("memoId") Long memoId);
}
