# JPA와 JDBC 대량 삽입 비교

같은 사용자 데이터를 JPA `saveAll()`과 JDBC `PreparedStatement.executeBatch()`로 저장하는 실험입니다.

- [BulkInsertService](src/main/java/com/example/hightraffic/service/BulkInsertService.java): JPA는 10,000건, JDBC는 100,000건 단위로 데이터를 생성합니다.
- [BulkInsertRepository](src/main/java/com/example/hightraffic/repository/BulkInsertRepository.java): JDBC에서는 10,000건마다 배치를 실행합니다.
- [BulkInsertController](src/main/java/com/example/hightraffic/controller/BulkInsertController.java): 요청 시작부터 저장 완료까지의 시간을 응답합니다.

## 실행

JDK 17, H2 메모리 DB를 사용합니다.

```powershell
.\gradlew.bat bootRun
```

애플리케이션 시작 후 아래 API를 각각 호출합니다. 먼저 적은 건수로 실행하고, 동일한 초기 상태에서 비교하려면 애플리케이션을 재시작합니다.

```http
POST http://localhost:8080/api/bulk/insert/jpa?total=10000
POST http://localhost:8080/api/bulk/insert/jdbc?total=10000
```

## 결과 해석

원본 README에는 H2 기준 300만 건에서 JPA 약 170초, JDBC 약 9.9초가 기록되어 있었습니다. 해당 값은 과거 기록이며 이번 통합에서 재측정하지 않았습니다. JVM 워밍업·장비·트랜잭션·청크 크기가 통제된 벤치마크는 아니므로 일반적인 성능 배수로 해석하지 않습니다.

비교를 확장할 때는 두 방식의 청크 크기와 커밋 조건을 맞추고, 여러 번 반복하여 중앙값과 메모리 사용량을 기록합니다.
