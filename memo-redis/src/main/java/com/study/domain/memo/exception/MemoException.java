package com.study.domain.memo.exception;

/**
 * 메모 관련 예외를 처리하기 위한 클래스
 */
public class MemoException extends RuntimeException {

    /**
     * 기본 생성자
     */
    public MemoException() {
        super();
    }

    /**
     * 예외 메시지를 가진 생성자
     *
     * @param message 예외 메시지
     */
    public MemoException(String message) {
        super(message);
    }

    /**
     * 예외 메시지와 원인 예외를 가진 생성자
     *
     * @param message 예외 메시지
     * @param cause 원인 예외
     */
    public MemoException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 메모를 찾을 수 없을 때 발생하는 예외
     *
     * @param id 메모 ID
     * @return MemoException 인스턴스
     */
    public static MemoException memoNotFound(Long id) {
        return new MemoException("존재하지 않는 메모입니다. ID: " + id);
    }
}
