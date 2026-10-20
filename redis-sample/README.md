# Redis: 동시성, 캐시와 자료구조

재고 감소라는 같은 문제를 여러 방식으로 구현하고, Redis 자료구조를 캐시·랭킹·메시징에 적용한 실습입니다.

| 질문 | 구현 / 실습 |
| --- | --- |
| 동시에 읽고 쓰면 재고가 왜 어긋날까? | `StockServiceNoLockConcurrencyTest` |
| 분산 락과 낙관적 트랜잭션은 어떻게 다를까? | `StockServiceConcurrencyTest`, `StockServiceOptimisticConcurrencyTest` |
| Lua로 읽기와 감소를 묶으면 어떤 결과가 나올까? | `StockLuaConcurrencyTest` |
| 캐시 미스와 TTL 만료 후에는 무엇이 실행될까? | `PostCacheTest`, `PostDtoCacheTest`, `PostSpringCacheTest` |
| 점수와 기간별 랭킹을 어떻게 표현할까? | `PostLikeRankingService`, `WeeklyPostRankingService` |
| Pub/Sub과 Streams의 소비 방식은 어떻게 다를까? | `NoticePubSubTest`, `RedisStreamMultiConsumerTest` |

## 실행

JDK 17과 `localhost:6379`의 전용 Redis가 필요합니다. 테스트에서 공유 키를 초기화하므로 다른 프로젝트의 Redis와 분리합니다.

```powershell
.\gradlew.bat test
.\gradlew.bat test --tests '*StockLuaConcurrencyTest'
```

구현은 [StockService.java](src/main/java/com/example/service/StockService.java), 테스트는 [src/test](src/test/java/com/example)에서 읽을 수 있습니다.

현재 재고 키는 상품별로 분리되지 않은 `product:stock`입니다. 락 만료 시간과 키 설계, Streams의 실패 메시지 회수 등은 추가 검증할 주제입니다. 락 없는 테스트는 경쟁 상태를 관찰하는 용도로 결과가 매번 같지 않을 수 있습니다.
