package com.study.domain.memo.service;

import com.study.domain.memo.domain.entity.Memo;
import com.study.domain.memo.domain.repository.MemoDslRepository;
import com.study.domain.memo.domain.repository.MemoRepository;
import com.study.domain.memo.dto.MemoDetailDto;
import com.study.domain.memo.dto.MemoListDto;
import com.study.domain.memo.dto.MemoSearchDto;
import com.study.domain.memo.dto.MemoSaveDto;
import com.study.domain.memo.dto.MemoUpdateDto;
import com.study.domain.memo.event.MemoFavoriteEvent;
import com.study.domain.memo.exception.MemoException;
import com.study.domain.memoTag.domain.entity.MemoTag;
import com.study.domain.memoTag.domain.repository.MemoTagRepository;
import com.study.domain.tag.domain.entity.Tag;
import com.study.domain.tag.domain.repository.TagRepository;
import com.study.domain.tag.dto.TagSaveDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemoService {

    private final MemoRepository memoRepository;

    private final TagRepository tagRepository;

    private final MemoTagRepository memoTagRepository;

    private final MemoViewCountService memoViewCountService;

    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Long saveMemo(MemoSaveDto saveMemoDto) {
        Memo memo = saveMemoDto.toEntity();
        Memo saveMemo = memoRepository.save(memo);


        if(saveMemoDto.existTags()){

            for(TagSaveDto tagSaveDto : saveMemoDto.getTags()){
                Tag tag = tagSaveDto.toEntity();
                Tag saveTag = tagRepository.save(tag);


                MemoTag memoTag = MemoTag.builder().memoId(saveMemo.getId()).tagId(saveTag.getId()).build();
                memoTagRepository.save(memoTag);

            }
        }

        log.info("메모 저장 완료: ID={}", saveMemo.getId());
        return saveMemo.getId();
    }

    @Transactional(readOnly = true)
    public Page<MemoListDto> findMemoPaging(MemoSearchDto memoSearchDto, Pageable pageable){
        return memoRepository.findMemoListWithPaging(memoSearchDto, pageable);
    }

    @Transactional
    public Long updateMemo(Long id, MemoUpdateDto memoUpdateDto) {
        // 메모 조회
        Memo memo = memoRepository.findById(id)
                .orElseThrow(() -> MemoException.memoNotFound(id));

        // 메모 정보 업데이트
        if (memoUpdateDto.getTitle() != null) {
            memo.setTitle(memoUpdateDto.getTitle());
        }

        if (memoUpdateDto.getContent() != null) {
            memo.setContent(memoUpdateDto.getContent());
        }

        if (memoUpdateDto.getFavorite() != null) {
            memo.setFavorite(memoUpdateDto.getFavorite());
        }

        // 태그 업데이트
        if (memoUpdateDto.existTags()) {
            // 기존 태그 연결 삭제
            memoTagRepository.deleteByMemoId(id);

            // 새로운 태그 추가
            for (TagSaveDto tagSaveDto : memoUpdateDto.getTags()) {
                Tag tag = tagSaveDto.toEntity();
                Tag savedTag = tagRepository.save(tag);

                MemoTag memoTag = MemoTag.builder()
                        .memoId(memo.getId())
                        .tagId(savedTag.getId())
                        .build();

                memoTagRepository.save(memoTag);
            }
        }

        log.info("메모 수정 완료: ID={}", memo.getId());
        return memo.getId();
    }

    @Transactional
    public void deleteMemo(Long id) {
        // 메모 조회
        Memo memo = memoRepository.findById(id)
                .orElseThrow(() -> MemoException.memoNotFound(id));

        // 메모와 관련된 태그 연결 삭제
        memoTagRepository.deleteByMemoId(id);

        // 메모 삭제
        memoRepository.delete(memo);

        log.info("메모 삭제 완료: ID={}", id);
    }

    @Transactional(readOnly = true)
    public MemoDetailDto findMemoDetail(Long id) {
        // 메모 상세 정보 조회
        MemoDetailDto memoDetailDto = memoRepository.findMemoDetail(id)
                .orElseThrow(() -> MemoException.memoNotFound(id));

        // Redis에 조회수 증가
        Long viewCount = memoViewCountService.incrementViewCount(id);

        // 현재 조회수 설정 (Redis + DB)
        memoDetailDto.setViewCount(memoViewCountService.getCurrentViewCount(id));

        log.debug("메모 상세 조회: ID={}, 조회수={}", id, viewCount);
        return memoDetailDto;
    }

    @Transactional
    public boolean toggleFavorite(Long id) {
        // 메모 조회
        Memo memo = memoRepository.findById(id)
                .orElseThrow(() -> MemoException.memoNotFound(id));

        // 즐겨찾기 상태 토글
        boolean favoriteStatus = memo.toggleFavorite();

        // 즐겨찾기 상태 변경 이벤트 발행
        eventPublisher.publishEvent(new MemoFavoriteEvent(this, id, favoriteStatus));

        log.info("메모 즐겨찾기 상태 변경: ID={}, 상태={}", id, favoriteStatus);
        return favoriteStatus;
    }


}
