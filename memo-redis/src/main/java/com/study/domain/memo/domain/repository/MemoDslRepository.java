package com.study.domain.memo.domain.repository;

import com.study.domain.memo.dto.MemoDetailDto;
import com.study.domain.memo.dto.MemoListDto;
import com.study.domain.memo.dto.MemoSearchDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface MemoDslRepository {

    Page<MemoListDto> findMemoListWithPaging(MemoSearchDto memoSearchDto, Pageable pageable);

    Optional<MemoDetailDto> findMemoDetail(Long id);
}
