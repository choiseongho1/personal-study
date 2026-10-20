package com.example.listener;

import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.listener.RetryListenerSupport;
import org.springframework.stereotype.Component;

@Component
public class CustomRetryListener extends RetryListenerSupport {

    @Override
    public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
        System.out.println("⚠️ Retry 실패 발생!");
        System.out.println(" - 예외 메시지: " + throwable.getMessage());
        System.out.println(" - 현재 Retry 횟수: " + context.getRetryCount());
    }

    @Override
    public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
        if (context.getRetryCount() > 0) {
            System.out.println("✅ Retry 종료 - 총 시도 횟수: " + context.getRetryCount());
        }
    }
}
