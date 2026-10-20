package com.example.concurrencylab.payment;

/** 통신 타임아웃을 표현합니다. 결제의 최종 실패를 뜻하지 않습니다. */
public class PaymentGatewayTimeoutException extends RuntimeException {
    public PaymentGatewayTimeoutException(String message) {
        super(message);
    }
}
