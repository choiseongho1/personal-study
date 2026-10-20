# Spring Batch 처리 흐름 실습

Job과 Step의 구성부터 실패·분기·재시작까지 여러 상황을 독립된 설정 클래스로 실습합니다.

| 학습 주제 | 설정 클래스 |
| --- | --- |
| Tasklet과 Chunk | `LogTaskletJobConfig`, `BatchConfig` |
| CSV 읽기·DB 쓰기 | `FlatFileUserJobConfig`, `UserNameUpperJobConfig` |
| 재시도와 건너뛰기 | `RetrySkipUserJobConfig`, `RetrySkipUserJobWithListenerConfig` |
| 조건 분기 | `ConditionalStepJobConfig`, `JobExecutionDeciderJobConfig` |
| 실행 컨텍스트와 리스너 | `StepExecutionSharingJobConfig`, `StepExecutionListenerJobConfig` |
| 재시작과 병렬 Step | `StepAllowRestartJobConfig`, `ParallelStepJobConfig` |

## 실행

JDK 17과 H2 메모리 DB를 사용합니다. 여러 Job의 자동 실행을 끈 뒤 콘솔에서 실행할 Job을 고릅니다.

```powershell
.\gradlew.bat bootJar
java -jar build/libs/batch-0.0.1-SNAPSHOT.jar --spring.batch.job.enabled=false
```

출력된 목록에서 `simpleJob` 등 실행할 Bean 이름을 입력합니다. 설정 코드는 [config](src/main/java/com/example/config)에 있습니다.

의도적으로 실패를 발생시키는 Job이 있습니다. 기존 자동 테스트는 컨텍스트 로딩만 확인하므로 각 Job의 상태·재시도 횟수·Skip 결과는 실행 로그와 메타 테이블에서 별도로 관찰합니다.
