package com.example.concurrencylab.payment;

import java.math.BigDecimal;
import java.nio.file.Path;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.*;

class FakeGatewayRestartTest {
    @TempDir Path directory;

    @Test void recoversApprovalAndRefundAcrossGatewayInstances() {
        Path file = directory.resolve("provider.properties");
        var request = new PaymentGateway.PaymentRequest(1L, new BigDecimal("10000"), "restart-key");
        var first = new FakePaymentGateway(file);
        first.configureScenario("restart-key", FakePaymentGateway.Scenario.TIMEOUT_AFTER_APPROVAL);
        assertThatThrownBy(() -> first.pay(request)).isInstanceOf(PaymentGatewayTimeoutException.class);

        var restarted = new FakePaymentGateway(file);
        var approved = restarted.find("restart-key");
        assertThat(approved.status()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(restarted.pay(request)).isEqualTo(approved);
        restarted.configureRefundScenario("refund-key", FakePaymentGateway.Scenario.TIMEOUT_AFTER_APPROVAL);
        assertThatThrownBy(() -> restarted.refund(approved.providerPaymentId(), "refund-key"))
            .isInstanceOf(PaymentGatewayTimeoutException.class);

        var again = new FakePaymentGateway(file);
        assertThat(again.findRefund("refund-key").status()).isEqualTo(PaymentGateway.RefundStatus.SUCCEEDED);
        assertThat(again.refund(approved.providerPaymentId(), "refund-key")).isEqualTo(again.findRefund("refund-key"));
        assertThat(again.find("restart-key")).isEqualTo(approved);
    }

    @Test void retainedRequestRejectsChangedAmountAfterRestart() {
        Path file = directory.resolve("provider.properties");
        new FakePaymentGateway(file).pay(new PaymentGateway.PaymentRequest(1L, BigDecimal.TEN, "key"));
        var restarted = new FakePaymentGateway(file);
        assertThatThrownBy(() -> restarted.pay(new PaymentGateway.PaymentRequest(1L, BigDecimal.ONE, "key")))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
