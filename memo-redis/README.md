# 메모 서비스에 Redis 적용하기

메모·태그 검색과 즐겨찾기 도메인에 조회수 집계 및 인기 순위를 연결한 학습 프로젝트입니다.

- [MemoViewCountService](src/main/java/com/study/domain/memo/service/MemoViewCountService.java): Redis 증분 조회수를 DB 값과 합산하고 5분 주기로 동기화합니다.
- [MemoHotListService](src/main/java/com/study/domain/memo/service/MemoHotListService.java): Sorted Set 점수로 인기 메모를 조회하고 즐겨찾기에 가중치를 줍니다.
- [서비스 테스트](src/test/java/com/study/domain/memo/service): 생성·수정·삭제·즐겨찾기와 예외를 확인합니다.
- [저장소 테스트](src/test/java/com/study/domain/memo/domain/repository): 메모 및 검색 조회를 확인합니다.

## 실행

JDK 17, H2 메모리 DB, `localhost:6379`의 전용 Redis를 사용합니다.

```powershell
.\gradlew.bat test --tests '*MemoServiceTest' --tests '*MemoFavoriteServiceTest' --tests '*MemoDeleteServiceTest' --tests '*MemoUpdateServiceTest'
.\gradlew.bat bootRun
```

전체 테스트에는 Redis 연결이 필요합니다. 현재 조회수 동기화는 Redis 읽기와 초기화, DB 커밋을 하나의 원자적 작업으로 묶지 않습니다. 동기화 도중 들어온 조회수와 DB 실패 시 복구가 다음 실험 주제입니다.
