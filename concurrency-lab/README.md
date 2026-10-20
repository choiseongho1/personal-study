# 주문과 결제 정합성 실습

Java 21, Spring Boot 4.1.1, Spring Data JPA, H2, Flyway, Gradle을 사용하는 개인 학습 프로젝트입니다. TODO 1부터 11까지 구현했으며 기존 TODO와 단계별 주석은 학습 이력으로 남겼습니다.

## 실행

JDK 21을 준비하고 IntelliJ의 Gradle JVM도 21로 설정합니다.

```powershell
# 기본 H2 메모리 DB. 자동 작업자는 꺼져 있어 수동으로 흐름을 따라갈 수 있습니다.
.\gradlew.bat bootRun

# 자동 만료·Outbox 발행·결제·재조회·환불 작업을 실행합니다.
.\gradlew.bat bootRun --args='--spring.profiles.active=auto'

# DB와 가짜 결제사 장부를 파일에 저장해 재시작 복구를 실습합니다.
.\gradlew.bat bootRun --args='--spring.profiles.active=persistent,auto'
```

기본 포트는 8080입니다. 기본 H2 콘솔은 [http://localhost:8080/h2-console](http://localhost:8080/h2-console)입니다.

| 설정 | 기본값 |
| --- | --- |
| JDBC URL | jdbc:h2:mem:concurrency_lab;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE |
| 사용자 | sa |
| 비밀번호 | 빈 값 |
| 초기 이벤트 | ID 1, 재고 100개, 개당 10,000원 |
| 예약 시간 | 10분 |
| 기본 메시지 전달 | 같은 프로세스의 LocalEventTransport |
| 자동 작업 | auto 또는 kafka 프로필에서 실행 |

persistent 프로필의 콘솔 URL은 jdbc:h2:file:./data/concurrency_lab;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_ON_EXIT=FALSE 입니다. data 폴더에는 H2 DB와 fake-provider.properties 장부가 저장됩니다. 메모리 모드에서는 종료 시 상태가 사라집니다.

Flyway V1부터 V4가 테이블과 샘플 데이터를 만들고 Hibernate ddl-auto=validate로 매핑을 확인합니다. 이미 적용한 migration은 수정하지 않고 새 버전을 추가합니다.

## 전체 처리 구조

1. 주문 생성 트랜잭션: 주문·재고 예약·결제 시도·Outbox 이벤트·발행 작업 저장
2. 발행 작업자: 짧은 DB 선점 후 트랜잭션 밖에서 로컬 전달 또는 Kafka 발행
3. 소비자 트랜잭션: inbox 영수증과 payment 작업을 함께 저장
4. 결제 작업자: 원래 멱등성 키로 외부 결제 호출, 별도 트랜잭션으로 결과 반영
5. 타임아웃: UNKNOWN 유지 후 원래 키로 조회. 최종 실패로 추측하지 않음
6. 늦은 승인: 주문 만료·필요한 재고 반환·환불 요청·환불 작업을 한 트랜잭션으로 저장
7. 환불 작업자: 트랜잭션 밖에서 조회·환불하고 별도 트랜잭션으로 결과 반영

외부 호출은 PaymentRequestService와 RefundProcessingService에서 실행하며 Propagation.NEVER로 열린 DB 트랜잭션 안에서 호출되는 것을 방지합니다. 짧은 DB 작업은 별도 Bean에 두어 Spring 프록시를 거칩니다.

## 학습 순서와 코드 위치

설계 선택과 장애 시나리오는 [재고와 결제 정합성 학습 기록](docs/blog/재고와-결제-정합성-학습기록.md)에 정리했습니다.

[STUDY_TODO.md](STUDY_TODO.md)에 단계별 목표와 완료 조건을 정리했습니다.

| 단계 | 주요 코드 |
| --- | --- |
| TODO 1~4 주문과 결과 반영 | order/OrderService.java, OrderRepository.java |
| TODO 5 가짜 결제사 | payment/FakePaymentGateway.java |
| TODO 6~7 요청과 조회 | payment/PaymentRequestService.java, PaymentAttemptTransactions.java |
| TODO 8 환불 | payment/RefundProcessingService.java, RefundTransactions.java |
| TODO 9 자동 만료 | work/OrderExpiryWorker.java, AutomaticWorkers.java |
| TODO 10 Outbox | messaging/OutboxTransactions.java, OutboxEvent.java |
| TODO 11 Kafka | messaging/KafkaMessaging.java, InboxReceiver.java |
| 공통 선점·복구 | work/WorkItem.java, WorkTransactions.java, WorkRunner.java |

## 수동 실습

requests.http에 요청 예제가 있습니다.

1. POST /api/orders 로 userId와 eventId를 전달합니다.
2. H2 콘솔에서 SELECT id, order_id, idempotency_key, status FROM payment_attempts; 로 결제 시도 ID를 확인합니다.
3. POST /api/practice/payments/{attemptId}/request 로 결제를 요청합니다.
4. orders, payment_attempts, inventory를 조회해 결과를 확인합니다.

시나리오 선택:

```text
POST /api/practice/payments/{attemptId}/scenario?scenario=SUCCESS
POST /api/practice/payments/{attemptId}/scenario?scenario=FINAL_FAILURE
POST /api/practice/payments/{attemptId}/scenario?scenario=TIMEOUT_AFTER_APPROVAL
```

자동 작업자가 꺼진 기본 모드에서 시나리오를 먼저 정한 뒤 request를 호출하세요. 승인 후 타임아웃은 내부 UNKNOWN, 가짜 결제사 SUCCEEDED 상태를 만듭니다. reconcile API로 원래 키를 조회하면 결과가 반영됩니다. 가짜 결제사의 find에 거래가 없으면 UNKNOWN을 반환하며 재결제를 만들지 않습니다.

| API | 역할 |
| --- | --- |
| POST /api/practice/payments/{attemptId}/request | 외부 요청 또는 UNKNOWN 결과 조회 |
| POST /api/practice/payments/{attemptId}/reconcile | 예약 만료 여부와 관계없이 원래 결제 조회 |
| POST /api/orders/{orderId}/expire | 단일 주문 만료 |
| POST /api/practice/expiry/run | 기한 지난 주문을 최대 50건 처리 |
| POST /api/practice/work/run | 실행 가능한 영속 작업 최대 50건 처리 |
| GET /api/practice/work | 선점·재시도·REVIEW 상태 확인 |
| POST /api/practice/payments/{attemptId}/succeeded | 기존 수동 승인 통지 |
| POST /api/practice/payments/{attemptId}/failed | 기존 수동 최종 실패 통지 |

work/run은 한 번에 모든 단계를 재귀적으로 처리하지 않습니다. 첫 호출이 Outbox를 전달하면 다음 호출에서 payment 작업이 실행될 수 있습니다. auto 프로필은 다음 주기에 이어 처리합니다. HTTP 204는 API 처리 종료를 의미하며 최종 결제 상태는 DB에서 확인합니다.

늦은 승인은 TIMEOUT_AFTER_APPROVAL → request → expires_at을 과거 UTC 시각으로 변경 → expiry/run → reconcile → work/run 순으로 재현할 수 있습니다. 실제 가짜 결제 장부에 승인이 있어야 환불할 수 있으므로, 수동 succeeded만 호출해 만든 거래는 가짜 결제사 환불 대상이 아닙니다.

## 상태와 중복 처리

- 조건부 재고 UPDATE와 UNIQUE(user_id, event_id)로 초과 판매·중복 주문을 방지합니다.
- 이 유니크 제약은 실패·만료 후 재주문도 제한하는 학습 정책입니다.
- 성공 처리, 실패 처리, UNKNOWN 기록은 주문 → 결제 시도 순서로 잠급니다.
- 주문 만료와 최종 실패는 PENDING 전이에 성공한 경우에만 재고를 반환합니다.
- EXPIRED 주문을 늦은 승인으로 CONFIRMED로 되돌리지 않습니다.
- 성공·최종 실패 결과가 충돌하면 조용히 덮어쓰지 않고 조회가 필요한 오류로 드러냅니다.
- 환불돼도 payment_attempts의 승인 이력을 유지합니다.
- 이벤트 ID 중복은 inbox에서, 결제 작업 중복은 payment:{attemptId} 키로, 외부 승인 중복은 결제 멱등성 키로 각각 제어합니다.
- Kafka 전송 확인 후 완료 표시 전에 종료되면 같은 이벤트 ID를 재발행합니다.
- inbox 저장과 결제 작업 저장 후 리스너가 반환돼야 오프셋이 커밋됩니다.
- 리스너 저장 오류는 오프셋을 전진시키지 않고 재전달합니다.

## 영속 작업과 재시작

work_items는 OUTBOX, PAYMENT, REFUND 작업을 READY → RUNNING → DONE으로 관리합니다. 작업 선점은 30초이며 선점 토큰이 일치하는 작업자만 완료·재시도 상태를 기록할 수 있습니다. 종료된 작업자의 선점이 만료되면 다른 작업자가 이어받습니다.

실패는 backoff 후 재시도하며 10회 소진 시 REVIEW에 오류와 기록을 남깁니다. 최종 환불 실패도 REVIEW로 남습니다. 실패 작업을 성공으로 표시하거나 삭제하지 않습니다. 원인을 확인한 뒤 로컬 실습에서는 해당 작업의 status를 READY, next_attempt_at을 과거 UTC로 바꿔 재처리할 수 있습니다.

가짜 결제사는 기본적으로 메모리 기반입니다. persistent 프로필에서는 승인·최종 실패·환불을 별도 파일에 원자적으로 저장하고 다음 기동에 복원합니다. 이 파일 장부는 단일 JVM 실습용입니다. 여러 애플리케이션 인스턴스가 같은 외부 결제사를 공유하는 배포에서는 독립된 결제사 서비스가 필요합니다.

H2의 MySQL 호환 모드는 MySQL의 잠금·격리 수준·성능을 재현하지 않습니다. MySQL 비교 실습은 mysql 프로필을 사용합니다.

## Kafka와 MySQL 선택 실행

```powershell
# Docker Desktop의 Linux 엔진이 실행 중이어야 합니다.
docker compose --profile kafka up -d --wait kafka
.\gradlew.bat bootRun --args='--spring.profiles.active=persistent,kafka'

# MySQL까지 함께 사용하는 경우
docker compose --profile kafka up -d --wait mysql kafka
.\gradlew.bat bootRun --args='--spring.profiles.active=mysql,kafka'
```

Kafka는 localhost:9092, MySQL은 localhost:3307 / concurrency_lab / lab / lab입니다. Kafka 프로필은 자동 작업자를 켭니다. 직접 시나리오를 정하며 진행하려면 --lab.workers.enabled=false를 추가하세요.

Kafka 토픽은 lab-payment-requests, 파티션 키는 주문 ID입니다. 문자열 메시지는 eventId|paymentAttemptId|orderId 형식입니다. producer는 acks=all과 멱등 전송을 사용하고 consumer는 자동 커밋을 끕니다. 이것만으로 외부 결제의 정확히 한 번 실행이 보장되지는 않으므로 DB 중복 제어와 결제사 멱등성 키도 함께 유지합니다.

읽을 수 없는 이벤트는 무한 재시도로 해당 파티션을 막을 수 있습니다. 이 실습은 이벤트를 조용히 버리지 않는 정책이며, 운영 확장 시 별도 격리·알림·재처리 정책을 추가할 수 있습니다.

## 검증

```powershell
.\gradlew.bat build
.\gradlew.bat test --tests "*RemainingWorkflowTest"
.\gradlew.bat test --tests "*KafkaWorkflowTest"
.\gradlew.bat test --tests "*PersistentRestartTest"
```

KafkaWorkflowTest는 Docker 없이 실제 내장 KRaft 브로커로 발행·소비·중복 전달·실패 후 재전달을 검증합니다. PersistentRestartTest는 파일 DB와 결제사 장부를 두 애플리케이션 컨텍스트 사이에서 재개해 미완료 작업을 복구합니다. 기본 테스트는 자동 작업자를 꺼서 상태를 명시적으로 검증합니다.

원래 학습용 TODO 주석은 완료 후에도 남아 있습니다. 실행 경로에는 미구현 예외가 남아 있지 않습니다.

## 공식 참고 문서

- [Spring Kafka 테스트](https://docs.spring.io/spring-kafka/reference/testing.html)
- [Kafka 공식 Docker 실행](https://kafka.apache.org/41/getting-started/docker/)
- [H2 기능과 호환 모드](https://h2database.com/html/features.html)
