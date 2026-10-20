package com.study.domain.memo.controller;

import com.study.domain.memo.dto.MemoDetailDto;
import com.study.domain.memo.dto.MemoListDto;
import com.study.domain.memo.dto.MemoSearchDto;
import com.study.domain.memo.dto.MemoSaveDto;
import com.study.domain.memo.dto.MemoUpdateDto;
import com.study.domain.memo.service.MemoHotListService;
import com.study.domain.memo.service.MemoService;
import com.study.domain.memo.service.MemoViewCountService;
import com.study.global.common.response.ResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/memo")
@RequiredArgsConstructor
public class MemoController {

    /**
     * 메모 관련 응답 코드
     */
    private static final String MEMO_CREATED = "MEMO_CREATED";
    private static final String MEMO_CREATION_FAILED = "MEMO_CREATION_FAILED";
    private static final String MEMO_SEARCH_SUCCESS = "MEMO_SEARCH_SUCCESS";
    private static final String MEMO_SEARCH_FAILED = "MEMO_SEARCH_FAILED";
    private static final String MEMO_DETAIL_SUCCESS = "MEMO_DETAIL_SUCCESS";
    private static final String MEMO_DETAIL_FAILED = "MEMO_DETAIL_FAILED";
    private static final String MEMO_FAVORITE_TOGGLE_SUCCESS = "MEMO_FAVORITE_TOGGLE_SUCCESS";
    private static final String MEMO_FAVORITE_TOGGLE_FAILED = "MEMO_FAVORITE_TOGGLE_FAILED";
    private static final String MEMO_UPDATE_SUCCESS = "MEMO_UPDATE_SUCCESS";
    private static final String MEMO_UPDATE_FAILED = "MEMO_UPDATE_FAILED";
    private static final String MEMO_DELETE_SUCCESS = "MEMO_DELETE_SUCCESS";
    private static final String MEMO_DELETE_FAILED = "MEMO_DELETE_FAILED";
    private static final String MEMO_HOT_LIST_SUCCESS = "MEMO_HOT_LIST_SUCCESS";
    private static final String MEMO_HOT_LIST_FAILED = "MEMO_HOT_LIST_FAILED";

    private final MemoService memoService;
    private final MemoHotListService memoHotListService;

    @PostMapping
    public ResponseEntity<ResponseDto<?>> saveMemo(@Valid @RequestBody MemoSaveDto saveMemoDto) {
        log.info("메모 저장 요청: {}", saveMemoDto);

        try {
            // 메모 저장 서비스 호출
            Long savedMemoId = memoService.saveMemo(saveMemoDto);

            log.info("메모 저장 성공: ID={}", savedMemoId);

            // 성공 응답 생성
            ResponseDto<?> response = ResponseDto.builder()
                    .responseCode(MEMO_CREATED)
                    .responseMessage("메모가 성공적으로 저장되었습니다.")
                    .data(savedMemoId)
                    .build();

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("메모 저장 실패: {}", e.getMessage(), e);

            // 실패 응답 생성
            ResponseDto<?> errorResponse = ResponseDto.builder()
                    .responseCode(MEMO_CREATION_FAILED)
                    .responseMessage("메모 저장에 실패했습니다: " + e.getMessage())
                    .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping
    public ResponseEntity<ResponseDto<Page<MemoListDto>>> findMemoList(MemoSearchDto memoSearchDto, @PageableDefault Pageable pageable){
        log.info("메모 목록 조회 요청: {}", memoSearchDto);

        try{
            // 메모 목록 조회 서비스 호출
            Page<MemoListDto> memoPaging = memoService.findMemoPaging(memoSearchDto, pageable);
            log.info("메모 목록 조회 성공: 총 {}개 조회", memoPaging.getTotalElements());

            // 성공 응답 생성
            ResponseDto<Page<MemoListDto>> response = ResponseDto.<Page<MemoListDto>>builder()
                    .responseCode(MEMO_SEARCH_SUCCESS)
                    .responseMessage("메모 목록이 성공적으로 조회되었습니다.")
                    .data(memoPaging)
                    .build();

            return ResponseEntity.status(HttpStatus.OK).body(response);
        }catch (Exception e){
            log.error("메모 목록 조회 실패: {}", e.getMessage(), e);

            // 실패 응답 생성
            ResponseDto<Page<MemoListDto>> errorResponse = ResponseDto.<Page<MemoListDto>>builder()
                    .responseCode(MEMO_SEARCH_FAILED)
                    .responseMessage("메모 목록 조회에 실패했습니다: " + e.getMessage())
                    .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PatchMapping("/memos/{id}")
    public ResponseEntity<ResponseDto<?>> updateMemo(@PathVariable Long id, @RequestBody MemoUpdateDto memoUpdateDto) {
        log.info("메모 수정 요청: ID={}, {}", id, memoUpdateDto);

        try {
            // 메모 수정 서비스 호출
            Long updatedMemoId = memoService.updateMemo(id, memoUpdateDto);

            log.info("메모 수정 성공: ID={}", updatedMemoId);

            // 성공 응답 생성
            ResponseDto<?> response = ResponseDto.builder()
                    .responseCode(MEMO_UPDATE_SUCCESS)
                    .responseMessage("메모가 성공적으로 수정되었습니다.")
                    .data(updatedMemoId)
                    .build();

            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (Exception e) {
            log.error("메모 수정 실패: {}", e.getMessage(), e);

            // 실패 응답 생성
            ResponseDto<?> errorResponse = ResponseDto.builder()
                    .responseCode(MEMO_UPDATE_FAILED)
                    .responseMessage("메모 수정에 실패했습니다: " + e.getMessage())
                    .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @DeleteMapping("/memos/{id}")
    public ResponseEntity<ResponseDto<?>> deleteMemo(@PathVariable Long id) {
        log.info("메모 삭제 요청: ID={}", id);

        try {
            // 메모 삭제 서비스 호출
            memoService.deleteMemo(id);

            log.info("메모 삭제 성공: ID={}", id);

            // 성공 응답 생성
            ResponseDto<?> response = ResponseDto.builder()
                    .responseCode(MEMO_DELETE_SUCCESS)
                    .responseMessage("메모가 성공적으로 삭제되었습니다.")
                    .build();

            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (Exception e) {
            log.error("메모 삭제 실패: {}", e.getMessage(), e);

            // 실패 응답 생성
            ResponseDto<?> errorResponse = ResponseDto.builder()
                    .responseCode(MEMO_DELETE_FAILED)
                    .responseMessage("메모 삭제에 실패했습니다: " + e.getMessage())
                    .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 인기 메모 목록 조회 API
     *
     * @param limit 조회할 메모 수
     * @return 인기 메모 목록
     */
    @GetMapping("/memos/hot")
    public ResponseEntity<ResponseDto<List<MemoDetailDto>>> getHotMemos(@RequestParam(defaultValue = "10") int limit) {
        log.info("인기 메모 목록 조회 요청: limit={}", limit);

        try {
            // 인기 메모 목록 조회 서비스 호출
            List<MemoDetailDto> hotMemos = memoHotListService.getHotMemos(limit);

            log.info("인기 메모 목록 조회 성공: {}개 조회", hotMemos.size());

            // 성공 응답 생성
            ResponseDto<List<MemoDetailDto>> response = ResponseDto.<List<MemoDetailDto>>builder()
                    .responseCode(MEMO_HOT_LIST_SUCCESS)
                    .responseMessage("인기 메모 목록이 성공적으로 조회되었습니다.")
                    .data(hotMemos)
                    .build();

            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (Exception e) {
            log.error("인기 메모 목록 조회 실패: {}", e.getMessage(), e);

            // 실패 응답 생성
            ResponseDto<List<MemoDetailDto>> errorResponse = ResponseDto.<List<MemoDetailDto>>builder()
                    .responseCode(MEMO_HOT_LIST_FAILED)
                    .responseMessage("인기 메모 목록 조회에 실패했습니다: " + e.getMessage())
                    .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseDto<MemoDetailDto>> getMemoDetail(@PathVariable Long id) {
        log.info("메모 상세 조회 요청: ID={}", id);

        try {
            // 메모 상세 조회 서비스 호출
            MemoDetailDto memoDetail = memoService.findMemoDetail(id);

            log.info("메모 상세 조회 성공: ID={}", id);

            // 성공 응답 생성
            ResponseDto<MemoDetailDto> response = ResponseDto.<MemoDetailDto>builder()
                    .responseCode(MEMO_DETAIL_SUCCESS)
                    .responseMessage("메모 상세 정보가 성공적으로 조회되었습니다.")
                    .data(memoDetail)
                    .build();

            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (Exception e) {
            log.error("메모 상세 조회 실패: {}", e.getMessage(), e);

            // 실패 응답 생성
            ResponseDto<MemoDetailDto> errorResponse = ResponseDto.<MemoDetailDto>builder()
                    .responseCode(MEMO_DETAIL_FAILED)
                    .responseMessage("메모 상세 조회에 실패했습니다: " + e.getMessage())
                    .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PatchMapping("/{id}/favorite")
    public ResponseEntity<ResponseDto<?>> toggleFavorite(@PathVariable Long id) {
        log.info("메모 즐겨찾기 토글 요청: ID={}", id);

        try {
            // 즐겨찾기 토글 서비스 호출
            boolean favoriteStatus = memoService.toggleFavorite(id);

            log.info("메모 즐겨찾기 토글 성공: ID={}, 상태={}", id, favoriteStatus);

            // 성공 응답 생성
            ResponseDto<?> response = ResponseDto.builder()
                    .responseCode(MEMO_FAVORITE_TOGGLE_SUCCESS)
                    .responseMessage("메모 즐겨찾기 상태가 변경되었습니다.")
                    .data(favoriteStatus)
                    .build();

            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (Exception e) {
            log.error("메모 즐겨찾기 토글 실패: {}", e.getMessage(), e);

            // 실패 응답 생성
            ResponseDto<?> errorResponse = ResponseDto.builder()
                    .responseCode(MEMO_FAVORITE_TOGGLE_FAILED)
                    .responseMessage("메모 즐겨찾기 상태 변경에 실패했습니다: " + e.getMessage())
                    .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
