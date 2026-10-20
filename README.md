# Personal Study

Java와 Spring으로 백엔드의 동작 원리를 직접 확인한 학습 모음입니다.
동시성 제어, 캐시, 영속성 컨텍스트, 배치 처리와 대량 삽입을 코드와 실험으로 살펴봅니다.

## 학습 지도

| 주제 | 폴더 | 확인할 내용 |
| --- | --- | --- |
| Redis 동시성·캐시 | [redis-sample](redis-sample) | 락 없음 / Redisson / WATCH / Lua 비교, TTL, 랭킹, Pub/Sub, Streams |
| Redis 도메인 적용 | [memo-redis](memo-redis) | 조회수 누적, 인기 메모 점수, 즐겨찾기 이벤트, QueryDSL 검색 |
| 도메인 모델링 | [random-box-system](random-box-system) | 구매·쿠폰 검증, 확률 기반 추첨, 재고·대기열·알림 |
| JPA 동작 원리 | [jpa-sample](jpa-sample) | 변경 감지, 영속 상태, 지연 로딩, 값 타입, 상속, Auditing |
| Spring Batch | [batch-sample](batch-sample) | Tasklet / Chunk, 조건 분기, Retry / Skip, 재시작, 실행 컨텍스트 |
| 대량 삽입 비교 | [bulk-insert-lab](bulk-insert-lab) | JPA saveAll과 JDBC executeBatch의 처리 방식 비교 |

## 먼저 읽을 코드

1. [StockService](redis-sample/src/main/java/com/example/service/StockService.java)와 [Lua 동시성 테스트](redis-sample/src/test/java/com/example/StockLuaConcurrencyTest.java): 같은 재고 문제를 여러 방식으로 풀어보기.
2. [MemoViewCountService](memo-redis/src/main/java/com/study/domain/memo/service/MemoViewCountService.java): Redis에 증분을 모아 DB로 반영하는 과정과 원자성의 경계 살펴보기.
3. [RandomBoxServiceTest](random-box-system/src/test/java/com/randombox/domain/randombox/RandomBoxServiceTest.java): 도메인 규칙과 예외 시나리오 읽기.
4. [RetrySkipUserJobConfig](batch-sample/src/main/java/com/example/config/RetrySkipUserJobConfig.java): 실패가 발생했을 때 배치가 진행하는 방식 확인하기.
5. [BulkInsertRepository](bulk-insert-lab/src/main/java/com/example/hightraffic/repository/BulkInsertRepository.java): PreparedStatement 배치가 실제로 실행되는 지점 따라가기.

## 실행

각 폴더는 독립된 Gradle 프로젝트입니다. JDK 17을 사용하며 Gradle Wrapper를 포함합니다.

```powershell
cd redis-sample
.\gradlew.bat test
.\gradlew.bat bootRun
```

macOS / Linux에서는 `./gradlew`를 사용합니다. Redis가 필요한 프로젝트는 각 README의 준비 사항을 확인하세요. 데이터 초기화 테스트는 전용 로컬 Redis에서 실행합니다.

JPA와 Batch에는 SQL·실행 로그를 관찰하는 실습이 포함됩니다. 테스트 개수는 자동 검증의 충실도나 운영 환경의 안정성을 뜻하지 않습니다. 실행 확인 범위는 [VALIDATION.md](VALIDATION.md)에 기록합니다.

## 원본 기록

기존 저장소의 선별된 코드를 일반 폴더로 통합했습니다. 원본 저장소·커밋·부분 이관 경로는 [SOURCES.json](SOURCES.json)에 남겼습니다. 개별 Git 이력은 이 저장소의 커밋 이력에 합치지 않았습니다.
