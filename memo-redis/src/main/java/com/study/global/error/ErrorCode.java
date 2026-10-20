package com.study.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // 400
    UNKNOWN("000_UNKNOWN", "알 수 없는 에러가 발생했습니다."),
    ENCRYPTION_FAILED("001_ENCRYPTION_FAILED", "암호화에 실패하셨습니다."),
    DECRYPTION_FAILED("002_DECRYPTION_FAILED", "복호화에 실패하셨습니다."),
    DUPLICATED_EMAIL("003_DUPLICATED_EMAIL", "이미 등록되어 있는 이메일입니다."),
    REGISTERED_EMAIL_FOR_THE_OTHER("004_REGISTERED_EMAIL_FOR_THE_OTHER", "다른 서비스로 등록되어 있는 이메일입니다."),
    INVALID_PASSWORD("005_INVALID_PASSWORD", "유효하지 않은 비밀번호 입니다."),
    NOT_EXISTED_EMAIL("006_NOT_EXISTED_EMAIL", "존재하지 않는 회원입니다."),
    BLOCKED_EMAIL("007_BLOCKED_EMAIL", "차단된 사용자 입니다."),
    WRONG_PASSWORD("008_WRONG_PASSWORD", "틀린 비밀번호 입니다."),
    NOT_ALLOW_EMAIL("009_NOT_ALLOW_EMAIL", "이메일 사용이 허용이 되지 않은 사용자입니다."),
    VALIDATION_ERROR("010_VALIDATION_ERROR", "입력값 검증에 실패했습니다."),

    // 401
    INVALID_TOKEN("101_INVALID_TOKEN", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN("102_EXPIRED_TOKEN", "만료된 토큰입니다."),

    // 403
    ACESS_DENIED_EMAIL("301_ACESS_DENIED_EMAIL", "접근 권한이 없는 사용자 요청입니다.");

    private final String code;
    private final String msg;
}
