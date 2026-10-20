package com.example.concurrencylab.payment;

import com.example.concurrencylab.common.ExerciseNotImplementedException;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * 실제 네트워크 호출 대신 사용하는 가짜 결제사입니다.
 * 결제 결과는 DB가 아닌 메모리에 보관하며 앱을 재시작하면 사라집니다.
 */
@Component
public class FakePaymentGateway implements PaymentGateway {

    public enum Scenario {
        SUCCESS,
        FINAL_FAILURE,
        TIMEOUT_AFTER_APPROVAL
    }

    // 결제사 측 거래 저장소를 흉내 냅니다. 키는 결제 멱등성 키입니다.
    private final Map<String, PaymentRequest> requests = new ConcurrentHashMap<>();
    private final Map<String, PaymentResult> payments = new ConcurrentHashMap<>();
    private final Map<String, Scenario> scenarios = new ConcurrentHashMap<>();

    // 전역 모드 대신 결제 키별로 시나리오를 설정합니다.
    // 테스트에서 PaymentAttempt의 idempotencyKey를 읽어 설정할 수 있습니다.
    public void configureScenario(String idempotencyKey, Scenario scenario) {
        scenarios.put(Objects.requireNonNull(idempotencyKey), Objects.requireNonNull(scenario));
    }

    @Override
    public synchronized PaymentResult pay(PaymentRequest request) {
        // TODO 5-1: 같은 키로 이미 처리한 요청이면 기존 결과를 반환하세요.
        // 힌트: 같은 키인데 orderId 또는 amount가 다르면 오류로 처리합니다.
        // 힌트: Map이 thread-safe여도 조회 → 신규 생성 조합은 원자적이지 않습니다.
        //       키별 원자 연산이나 잠금으로 중복 승인 생성을 막아야 합니다.

        Objects.requireNonNull(request, "결제 요청이 필요합니다.");
        Objects.requireNonNull(request.orderId(), "주문 ID가 필요합니다.");
        Objects.requireNonNull(request.amount(), "결제 금액이 필요합니다.");
        String idempotencyKey = request.idempotencyKey();
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("결제 멱등성 키가 필요합니다.");
        }

        // record의 접근자는 getXxx()가 아니라 xxx()입니다.
        // 요청의 주문 ID·금액은 PaymentResult가 아닌 requests에 저장해 비교합니다.
        PaymentRequest originalRequest = requests.get(idempotencyKey);
        if (originalRequest != null
            && (!originalRequest.orderId().equals(request.orderId())
                || originalRequest.amount().compareTo(request.amount()) != 0)) {
            throw new IllegalArgumentException("같은 멱등성 키에 다른 주문 또는 금액을 사용할 수 없습니다.");
        }
        // compareTo를 사용해 10000.0과 10000.00을 같은 금액으로 취급합니다.
        PaymentResult existingResult = payments.get(idempotencyKey);
        if (existingResult != null) {
            persistLedger();
            return existingResult;
        }

        // 이 키가 처음 사용된 요청 내용을 보관합니다.
        // TODO 5-2에서 만든 결과도 같은 키로 payments에 저장해야 합니다.
        requests.putIfAbsent(idempotencyKey, request);



        // TODO 5-2: scenarios에서 해당 키의 시나리오를 읽으세요. 기본값은 SUCCESS입니다.
        // SUCCESS: 고유 거래 ID를 만들어 SUCCEEDED를 기록하고 반환합니다.
        // FINAL_FAILURE: FAILED를 기록하고 반환합니다.
        // TIMEOUT_AFTER_APPROVAL: SUCCEEDED를 먼저 기록한 후 PaymentGatewayTimeoutException 발생.
        // 중요: 타임아웃을 재현해도 가짜 결제사에는 승인 결과가 남아 있어야 합니다.
        Scenario scenario = scenarios.getOrDefault(idempotencyKey, Scenario.SUCCESS);
        PaymentResult result = switch (scenario) {
            case FINAL_FAILURE -> new PaymentResult(PaymentStatus.FAILED, null);
            case SUCCESS, TIMEOUT_AFTER_APPROVAL -> new PaymentResult(
                PaymentStatus.SUCCEEDED, "fake-payment-" + UUID.randomUUID()
            );
        };
        payments.put(idempotencyKey, result);
        persistLedger();

        // 승인 결과를 먼저 저장한 뒤 응답만 유실된 상황을 재현합니다.
        // 같은 키로 재요청하면 TODO 5-1에서 이 결과를 반환하므로 중복 승인되지 않습니다.
        if (scenario == Scenario.TIMEOUT_AFTER_APPROVAL) {
            throw new PaymentGatewayTimeoutException("결제는 승인됐지만 응답을 받지 못했습니다.");
        }
        return result;
    }

    @Override
    public PaymentResult find(String idempotencyKey) {
        // TODO 7-1: payments에 저장한 실제 결과를 반환하세요.
        // 이 가짜 구현에서 기록이 없으면 UNKNOWN을 반환하고 재결제하지 않습니다.
        Objects.requireNonNull(idempotencyKey, "결제 조회 키가 필요합니다.");
        return payments.getOrDefault(idempotencyKey, new PaymentResult(PaymentStatus.UNKNOWN, null));
    }

    @Override
    public synchronized RefundResult refund(String providerPaymentId, String refundIdempotencyKey) {
        // TODO 8-1: 환불 키별 결과 저장소를 추가하고 동일 환불의 중복 실행을 막으세요.
        // 결제 승인 사실은 유지하고 환불 이력은 별도로 기록합니다.
        Objects.requireNonNull(providerPaymentId);
        Objects.requireNonNull(refundIdempotencyKey);
        String original = refundPayments.get(refundIdempotencyKey);
        if (original != null && !original.equals(providerPaymentId)) {
            throw new IllegalArgumentException("같은 환불 키에 다른 거래를 사용할 수 없습니다.");
        }
        RefundResult existing = refunds.get(refundIdempotencyKey);
        if (existing != null) { persistLedger(); return existing; }
        boolean approved = payments.values().stream().anyMatch(p ->
            p.status() == PaymentStatus.SUCCEEDED && providerPaymentId.equals(p.providerPaymentId()));
        if (!approved) throw new IllegalArgumentException("승인 거래가 없습니다.");
        refundPayments.put(refundIdempotencyKey, providerPaymentId);
        Scenario scenario = refundScenarios.getOrDefault(refundIdempotencyKey, Scenario.SUCCESS);
        var result = new RefundResult(scenario == Scenario.FINAL_FAILURE ? RefundStatus.FAILED : RefundStatus.SUCCEEDED);
        refunds.put(refundIdempotencyKey, result);
        persistLedger();
        if (scenario == Scenario.TIMEOUT_AFTER_APPROVAL) {
            throw new PaymentGatewayTimeoutException("환불 완료 후 응답이 유실됐습니다.");
        }
        return result;
    }

    private final Map<String, String> refundPayments = new ConcurrentHashMap<>();
    private final Map<String, RefundResult> refunds = new ConcurrentHashMap<>();
    private final Map<String, Scenario> refundScenarios = new ConcurrentHashMap<>();
    public void configureRefundScenario(String key, Scenario scenario) {
        refundScenarios.put(Objects.requireNonNull(key), Objects.requireNonNull(scenario));
    }
    @Override
    public RefundResult findRefund(String key) {
        return refunds.getOrDefault(Objects.requireNonNull(key), new RefundResult(RefundStatus.UNKNOWN));
    }
    @org.springframework.beans.factory.annotation.Value("${lab.fake-ledger-file:}")
    private String ledgerFile = "";
    public FakePaymentGateway() {}
    public FakePaymentGateway(java.nio.file.Path path) {
        ledgerFile = path.toString();
        restoreLedger();
    }
    @jakarta.annotation.PostConstruct
    public synchronized void restoreLedger() {
        if (!ledgerFile.isBlank()) {
            FakeGatewayLedger.read(java.nio.file.Path.of(ledgerFile), requests, payments, refundPayments, refunds);
        }
    }
    private void persistLedger() {
        if (!ledgerFile.isBlank()) {
            FakeGatewayLedger.write(java.nio.file.Path.of(ledgerFile), requests, payments, refundPayments, refunds);
        }
    }
}
