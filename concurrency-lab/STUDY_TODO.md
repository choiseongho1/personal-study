# 결제 요청과 장애 복구 실습 TODO

TODO 1부터 11까지 구현했습니다. 순서대로 코드를 읽고 실패 시나리오를 재현하며 복습합니다. 기존 학습용 주석과 TODO는 완료 후에도 남깁니다.

## 완료한 단계

- [x] TODO 1 주문 생성과 조건부 재고 차감
- [x] TODO 2 예약 만료와 재고 반환
- [x] TODO 3 결제 성공 반영과 늦은 승인에 대한 환불 대기 기록
- [x] TODO 4 최종 실패 반영과 주문 취소

## TODO 5 가짜 결제사

작성 파일: src/main/java/com/example/concurrencylab/payment/FakePaymentGateway.java

- [x] 먼저 SUCCESS 하나만 구현해 키별로 승인 결과를 저장합니다.
- [x] 같은 멱등성 키의 재요청은 같은 거래 ID와 결과를 반환합니다.
- [x] 같은 키에 다른 주문 또는 금액이 들어오면 오류로 처리합니다.
- [x] 동시 재요청에서도 신규 승인이 중복 생성되지 않게 합니다.
- [x] FINAL_FAILURE 시나리오를 추가합니다.
- [x] TIMEOUT_AFTER_APPROVAL은 승인 결과 저장 후 타임아웃 예외를 발생시킵니다.

requests, payments, scenarios 맵과 키별 시나리오 설정 메서드 및 pay를 구현했습니다. TIMEOUT_AFTER_APPROVAL의 최초 호출은 승인 저장 후 타임아웃이 나며, 같은 키의 재요청은 저장된 승인 결과를 반환합니다. find도 TODO 7에서 구현했습니다. configureScenario에 저장된 결제 키와 Scenario를 전달해 테스트별 상황을 선택할 수 있습니다. 맵은 앱 재시작 시 초기화됩니다.

완료 조건: 정상 성공, 최종 실패, 동일 키 재요청, 동시 재요청, 다른 요청 내용 거부를 테스트합니다.

## TODO 6 결제 요청 흐름 연결

작성 파일: PaymentAttemptTransactions.java, PaymentRequestService.java

- [x] prepareRequest에서 결제 요청 가능 여부를 확인하고 불변 PaymentRequest를 반환합니다.
- [x] 가짜 결제 API 호출 전에 조회 트랜잭션이 끝나도록 합니다.
- [x] SUCCEEDED와 FAILED를 기존 OrderService의 결과 반영 메서드에 연결합니다.
- [x] 이미 종료된 주문이나 결제에 불필요한 새 결제를 요청하지 않습니다.

구조:

1. PaymentAttemptTransactions.prepareRequest에서 짧은 DB 조회
2. 트랜잭션 종료 후 PaymentGateway.pay 호출
3. OrderService에서 새로운 DB 트랜잭션으로 결과 반영

같은 클래스에서 this를 통해 @Transactional 메서드를 호출하는 방식은 사용하지 않습니다. 별도 Bean을 통해 호출하도록 구현했습니다. PaymentRequestService에는 NEVER를 설정해 외부 호출이 트랜잭션 안에서 실행되는 실수를 드러냅니다.

완료 조건: 성공 요청은 주문 확정, 최종 실패는 주문 취소와 재고 복구로 이어집니다. 가짜 결제사의 pay 실행 시 트랜잭션이 없는지도 테스트합니다.

## TODO 7 타임아웃과 결과 재조회

작성 파일: FakePaymentGateway.find, PaymentAttemptTransactions, PaymentRequestService.reconcilePayment

- [x] 타임아웃 발생 시 결제만 UNKNOWN으로 기록하고 주문과 재고는 유지합니다.
- [x] 웹훅과 경합해도 SUCCEEDED 또는 FAILED를 UNKNOWN으로 되돌리지 않습니다.
- [x] 저장된 멱등성 키로 가짜 결제사 결과를 조회합니다.
- [x] 주문이 만료돼도 원래 결제 결과 조회를 허용합니다.
- [x] 결과가 불명확하면 재결제를 만들지 않습니다.
- [x] 늦은 성공 확인 시 기존 paymentSucceeded로 환불 대기를 기록합니다.

완료 조건: 승인 후 타임아웃 → 내부 UNKNOWN → 조회로 승인 확인 → 주문 확정 또는 환불 대기로 수렴하는 흐름을 테스트합니다.

## TODO 8 환불 작업자

- [x] refund_requests의 PENDING 작업을 읽어 선점합니다.
- [x] 기존 환불 멱등성 키로 트랜잭션 밖에서 환불 API를 호출합니다.
- [x] 환불 결과는 별도 트랜잭션으로 기록합니다.
- [x] 결과 불명, 재시도, 중복 작업과 작업자 종료를 다룹니다.
- [x] 승인 이력과 환불 이력을 분리합니다.

완료 조건: 만료 후 승인된 결제가 중복 환불 없이 환불 완료 상태로 이어집니다. auto 또는 kafka 프로필에서 선점·재조회·재시도 작업자가 자동 환불을 처리합니다.

## TODO 9 예약 만료 자동화

- [x] 만료된 PENDING 주문을 적절한 크기로 조회합니다.
- [x] 기존 expireOrder를 별도 Bean을 통해 호출합니다.
- [x] 배치 전체에 긴 트랜잭션을 유지하지 않습니다.
- [x] 여러 실행 인스턴스에서도 재고를 중복 반환하지 않는지 확인합니다.

## TODO 10 Outbox

- [x] 업무 데이터와 이벤트 발행 의도를 같은 트랜잭션에 저장합니다.
- [x] 재시작 후 미발행 작업을 재처리합니다.
- [x] 전송 성공 직후 완료 표시 전 종료되는 경우를 재현합니다.

## TODO 11 Kafka

- [x] Outbox 이벤트를 Kafka로 발행하고 결제 작업을 소비합니다.
- [x] 중복 전달, 오프셋 커밋 실패, 작업자 종료를 재현합니다.
- [x] 이벤트 ID 중복 방지와 외부 결제 멱등성을 별도로 유지합니다.

## 실행 안내

프로젝트 폴더에서 다음을 실행합니다.

```powershell
.\gradlew.bat bootRun
```

기존 주문 생성 후 H2 콘솔에서 SELECT id, order_id, idempotency_key FROM payment_attempts; 로 결제 시도 ID를 확인합니다.

- POST /api/practice/payments/{attemptId}/request 는 TODO 6의 요청 진입점입니다.
- POST /api/practice/payments/{attemptId}/reconcile 은 TODO 7의 조회 진입점입니다.
- 두 API는 구현됐으며 결제 요청과 결과 재조회를 실행합니다.
- 기존 수동 succeeded, failed API는 계속 사용할 수 있습니다.

기본 모드는 수동 실행이며 auto 프로필에서 자동 작업자를 켭니다. persistent 프로필로 DB와 가짜 결제사 장부를 유지하고 재시작을 실습할 수 있습니다. 실행 방법은 README를 참고하세요.
