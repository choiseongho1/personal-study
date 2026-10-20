# 랜덤박스·구매·쿠폰 도메인 실습

티켓과 랜덤박스 구매 시나리오에 기간·수량·할인·확률 검증을 적용한 프로젝트입니다. Redis를 사용하는 재고·대기열·알림 실습도 포함합니다.

## 읽는 순서

1. [RandomBoxService](src/main/java/com/randombox/domain/randombox/RandomBoxService.java): 판매 기간, 아이템 확률과 누적 확률 추첨.
2. [도메인 테스트](src/test/java/com/randombox/domain): 구매와 쿠폰의 성공·실패 조건, 재고 및 대기열 테스트.
3. [RandomBoxServiceTest](src/test/java/com/randombox/domain/randombox/RandomBoxServiceTest.java): 외부 인프라 없이 Mockito로 도메인 규칙 확인.

## 실행

JDK 17을 사용합니다. 기본 DB는 H2 메모리 DB이며 Redis 기능에는 전용 로컬 Redis가 필요합니다.

```powershell
.\gradlew.bat test --tests 'com.randombox.domain.randombox.RandomBoxServiceTest' --tests 'com.randombox.domain.coupon.CouponServiceTest' --tests 'com.randombox.domain.purchase.PurchaseServiceTest'
.\gradlew.bat bootRun
```

Redis 통합 테스트는 embedded Redis를 6379 포트로 시작하고 데이터를 초기화합니다. 이미 사용 중인 Redis와 함께 실행하지 않습니다.

현재 추첨은 확률 합계가 100보다 작으면 남은 확률을 마지막 아이템에 할당하는 동작이 됩니다. 확률 합계를 정확히 100으로 제한할지, 미당첨 상태를 둘지는 추가로 정의할 도메인 규칙입니다.
