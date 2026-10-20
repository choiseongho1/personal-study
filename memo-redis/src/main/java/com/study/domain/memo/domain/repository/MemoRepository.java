package com.study.domain.memo.domain.repository;

import com.study.domain.memo.domain.entity.Memo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemoRepository extends JpaRepository<Memo, Long>, MemoDslRepository {
}
