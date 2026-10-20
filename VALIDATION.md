# 검증 기록

검증일: 2026-10-20. Windows, Amazon Corretto JDK 17.0.13, Gradle 8.14.3에서 실행했습니다.

| 프로젝트 | 소스·테스트 코드 컴파일 | 실행한 기존 테스트 |
| --- | --- | --- |
| concurrency-lab | 통과 (JDK 21, Gradle 9.8.1) | 전체 64개 통과, 실패·오류·건너뜀 0개 |
| redis-sample | 통과 | Redis 서버 필요, 실행하지 않음 |
| memo-redis | 통과 | MemoServiceTest / MemoFavoriteServiceTest / MemoDeleteServiceTest / MemoUpdateServiceTest, 13개 통과 |
| random-box-system | 통과 | RandomBoxServiceTest / CouponServiceTest / PurchaseServiceTest, 27개 통과 |
| jpa-sample | 통과 | 전체 9개 통과 |
| batch-sample | 통과 | contextLoads 1개 통과 |
| bulk-insert-lab | 통과 | contextLoads 1개 통과 |

## 통합 시 복구한 부분

- 누락된 Gradle Wrapper JAR를 복구하고 8.14.3으로 맞췄습니다. 배포 ZIP의 공식 SHA-256을 확인하고 Wrapper 설정에도 고정했습니다.
- 메모 즐겨찾기 테스트에 이벤트 발행자 Mock을 추가하고 메모 ID·즐겨찾기 상태가 이벤트에 담기는지 검증했습니다.
- 구매 테스트에 추가된 재고·대기열·알림 의존성의 Mock과 재고 감소 성공 응답을 준비했습니다. 구매 시 재고 감소와 성공 알림 호출을 확인했습니다.

## concurrency-lab 추가 검증

2026-10-20, Windows와 Amazon Corretto JDK 21.0.8, 프로젝트의 Gradle 9.8.1 Wrapper로 `build`를 실행했습니다. 소스·테스트 컴파일과 JAR 패키징, 기존 전체 테스트가 통과했습니다. 내장 KRaft 브로커를 사용하는 Kafka 발행·소비·중복·재전달 테스트 및 파일 DB·가짜 결제사 장부의 재시작 복구 테스트를 포함합니다.

Docker Compose로 구동한 외부 MySQL·Kafka 및 실제 결제사 연동은 이번 검증 범위에 포함하지 않습니다. 기존 6개 프로젝트에 대한 아래 한계와 별도로 해석합니다.

## 확인 범위와 남은 과제

- Redis 통합 테스트는 검증 완료로 표시하지 않습니다. 랜덤박스의 RedisPurchaseServiceTest를 시도했으나 Redis 연결을 포함한 애플리케이션 컨텍스트가 시작되지 않았습니다. 최종 단위 테스트는 완전한 클래스 이름으로 별도 지정했습니다.
- Redis 동시성, 조회수 동기화 중 장애, 추첨 분포, 개별 Batch Job의 완료·실패·재시작 결과와 대량 삽입 성능은 별도 실험이 필요합니다.
- JPA 테스트 일부는 로그 관찰 방식입니다. Batch와 대량 삽입의 contextLoads 통과는 기능 전체나 성능 검증을 뜻하지 않습니다.
- Batch 테스트 종료 시 JpaPagingItemReader의 close 경고가 출력됩니다. Reader 종료 생명주기는 추가 점검 대상입니다.
- 기존 코드의 deprecated API, unchecked 연산, Lombok 초기값 관련 컴파일 경고가 남아 있습니다.
- 원본의 과거 성능 수치는 이번에 재측정하지 않았습니다.
