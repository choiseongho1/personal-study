package com.example.concurrencylab.payment;

import com.example.concurrencylab.payment.PaymentGateway.*;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** persistent 프로필에서 외부 결제사의 별도 영속 장부를 흉내 냅니다. 단일 JVM 실습용입니다. */
final class FakeGatewayLedger {
    private FakeGatewayLedger() {}
    static String encode(String key) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(key.getBytes(StandardCharsets.UTF_8));
    }
    static String decode(String key) {
        return new String(Base64.getUrlDecoder().decode(key), StandardCharsets.UTF_8);
    }
    static void read(Path path, Map<String, PaymentRequest> requests, Map<String, PaymentResult> payments,
                     Map<String, String> refundPayments, Map<String, RefundResult> refunds) {
        if (!Files.exists(path)) return;
        var properties = new Properties();
        try (var input = Files.newInputStream(path)) {
            properties.load(input);
        } catch (IOException exception) { throw new UncheckedIOException(exception); }
        // 손상된 파일은 시작 오류로 드러냅니다. 빈 장부로 덮어쓰지 않습니다.
        for (String property : properties.stringPropertyNames()) {
            if (property.startsWith("p.") && property.endsWith(".status")) {
                String prefix = property.substring(0, property.length() - ".status".length());
                String key = decode(prefix.substring(2));
                requests.put(key, new PaymentRequest(Long.valueOf(properties.getProperty(prefix + ".order")),
                    new BigDecimal(properties.getProperty(prefix + ".amount")), key));
                payments.put(key, new PaymentResult(PaymentStatus.valueOf(properties.getProperty(property)),
                    properties.getProperty(prefix + ".provider")));
            } else if (property.startsWith("r.") && property.endsWith(".status")) {
                String prefix = property.substring(0, property.length() - ".status".length());
                String key = decode(prefix.substring(2));
                refundPayments.put(key, properties.getProperty(prefix + ".provider"));
                refunds.put(key, new RefundResult(PaymentGateway.RefundStatus.valueOf(properties.getProperty(property))));
            }
        }
    }
    static void write(Path path, Map<String, PaymentRequest> requests, Map<String, PaymentResult> payments,
                      Map<String, String> refundPayments, Map<String, RefundResult> refunds) {
        var properties = new Properties();
        payments.forEach((key, result) -> {
            String prefix = "p." + encode(key);
            var request = requests.get(key);
            properties.setProperty(prefix + ".order", request.orderId().toString());
            properties.setProperty(prefix + ".amount", request.amount().toPlainString());
            properties.setProperty(prefix + ".status", result.status().name());
            if (result.providerPaymentId() != null) properties.setProperty(prefix + ".provider", result.providerPaymentId());
        });
        refunds.forEach((key, result) -> {
            String prefix = "r." + encode(key);
            properties.setProperty(prefix + ".provider", refundPayments.get(key));
            properties.setProperty(prefix + ".status", result.status().name());
        });
        Path absolute = path.toAbsolutePath();
        Path temporary = null;
        try {
            Files.createDirectories(absolute.getParent());
            temporary = Files.createTempFile(absolute.getParent(), "fake-ledger-", ".tmp");
            try (var output = Files.newOutputStream(temporary)) { properties.store(output, "Fake provider ledger"); }
            try {
                Files.move(temporary, absolute, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, absolute, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) { throw new UncheckedIOException(exception); }
        finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (IOException ignored) { /* 다음 기동에 장부를 덮어쓰지 않습니다. */ }
            }
        }
    }
}
