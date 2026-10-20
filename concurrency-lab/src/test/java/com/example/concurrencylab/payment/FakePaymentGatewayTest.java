package com.example.concurrencylab.payment;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FakePaymentGatewayTest {

    // Spring이나 DB 없이 가짜 결제사 자체의 동작만 확인합니다.
    private final FakePaymentGateway gateway = new FakePaymentGateway();

    @Test
    @DisplayName("시나리오를 지정하지 않으면 승인 결과와 거래 ID를 반환한다")
    void succeedsByDefault() {
        var result = gateway.pay(request(1L, "10000", "key-1"));

        assertThat(result.status()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(result.providerPaymentId()).startsWith("fake-payment-");
    }

    @Test
    @DisplayName("최종 실패도 저장하며 시나리오를 바꿔 재요청해도 기존 실패를 반환한다")
    void cachesFinalFailure() {
        gateway.configureScenario("key-1", FakePaymentGateway.Scenario.FINAL_FAILURE);
        var request = request(1L, "10000", "key-1");

        var first = gateway.pay(request);
        gateway.configureScenario("key-1", FakePaymentGateway.Scenario.SUCCESS);
        var second = gateway.pay(request);

        assertThat(first.status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(first.providerPaymentId()).isNull();
        assertThat(second).isSameAs(first);
    }

    @Test
    @DisplayName("승인 후 타임아웃이 나도 같은 키의 재요청은 저장된 승인을 반환한다")
    void preservesApprovalAfterTimeout() {
        gateway.configureScenario("key-1", FakePaymentGateway.Scenario.TIMEOUT_AFTER_APPROVAL);
        var request = request(1L, "10000", "key-1");

        assertThatThrownBy(() -> gateway.pay(request))
            .isInstanceOf(PaymentGatewayTimeoutException.class);

        // find는 TODO 7에 남겨뒀으므로 동일 키 재전송으로 승인 결과 보존을 확인합니다.
        var second = gateway.pay(request);
        var third = gateway.pay(request);
        assertThat(second.status()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(second.providerPaymentId()).isNotBlank();
        assertThat(third).isSameAs(second);
    }

    @Test
    @DisplayName("동일 요청은 재승인하지 않고 기존 결과를 그대로 반환한다")
    void reusesResultForSameRequest() {
        var request = request(1L, "10000", "key-1");

        var first = gateway.pay(request);
        var second = gateway.pay(request);

        assertThat(second).isSameAs(first);
    }

    @Test
    @DisplayName("소수 자릿수만 다른 금액은 같은 요청으로 처리한다")
    void acceptsEquivalentAmount() {
        var first = gateway.pay(request(1L, "10000.0", "key-1"));

        var second = gateway.pay(request(1L, "10000.00", "key-1"));

        assertThat(second).isSameAs(first);
    }

    @Test
    @DisplayName("같은 키에 다른 주문을 넣으면 기존 승인을 변경하지 않고 거부한다")
    void rejectsDifferentOrder() {
        var original = request(1L, "10000", "key-1");
        var first = gateway.pay(original);

        assertThatThrownBy(() -> gateway.pay(request(2L, "10000", "key-1")))
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(gateway.pay(original)).isSameAs(first);
    }

    @Test
    @DisplayName("같은 키에 다른 금액을 넣으면 기존 승인을 변경하지 않고 거부한다")
    void rejectsDifferentAmount() {
        var original = request(1L, "10000", "key-1");
        var first = gateway.pay(original);

        assertThatThrownBy(() -> gateway.pay(request(1L, "20000", "key-1")))
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(gateway.pay(original)).isSameAs(first);
    }

    @Test
    @DisplayName("서로 다른 키는 별도 거래 ID를 가진다")
    void createsSeparateTransactionsForDifferentKeys() {
        var first = gateway.pay(request(1L, "10000", "key-1"));
        var second = gateway.pay(request(2L, "10000", "key-2"));

        assertThat(second.providerPaymentId()).isNotEqualTo(first.providerPaymentId());
    }

    @Test
    @Timeout(15)
    @DisplayName("같은 키로 동시에 요청해도 하나의 승인 결과를 공유한다")
    void handlesConcurrentRetries() throws Exception {
        int count = 8;
        var executor = Executors.newFixedThreadPool(count);
        var ready = new CountDownLatch(count);
        var start = new CountDownLatch(1);
        List<Future<PaymentGateway.PaymentResult>> results = new ArrayList<>();
        var request = request(1L, "10000", "key-1");

        try {
            for (int i = 0; i < count; i++) {
                results.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("시작 신호 대기 시간 초과");
                    }
                    return gateway.pay(request);
                }));
            }
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            PaymentGateway.PaymentResult first = null;
            for (var future : results) {
                long remaining = deadline - System.nanoTime();
                assertThat(remaining).isPositive();
                var result = future.get(remaining, TimeUnit.NANOSECONDS);
                assertThat(result.status()).isEqualTo(PaymentStatus.SUCCEEDED);
                if (first == null) {
                    first = result;
                } else {
                    assertThat(result).isSameAs(first);
                }
            }
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(2, TimeUnit.SECONDS)).isTrue();
        }
    }

    private PaymentGateway.PaymentRequest request(Long orderId, String amount, String key) {
        return new PaymentGateway.PaymentRequest(orderId, new BigDecimal(amount), key);
    }
}
