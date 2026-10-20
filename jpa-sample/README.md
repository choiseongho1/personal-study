# JPA 동작 원리 실습

엔티티 매핑을 바꾸고 SQL이 실행되는 시점을 관찰하는 프로젝트입니다. H2를 사용해 외부 DB 없이 실습합니다.

| 주제 | 읽을 테스트 |
| --- | --- |
| 변경 감지와 flush | `MemberPersistenceTest` |
| 영속성 컨텍스트 상태 변화 | `CustomerPersistenceTest` |
| 연관관계 접근과 지연 로딩 | `ProjectFetchTypeTest` |
| Embeddable·값 타입·컬렉션 | `EmployeeEmbeddableTest`, `EmployeeValueTypeTest` |
| 상속 매핑 | `InheritanceTest`, `MappedSuperclassTest` |
| 생성·수정 시각 | `AuditingTest` |

## 실행

JDK 17을 준비합니다.

```powershell
.\gradlew.bat test --tests '*MemberPersistenceTest' --info
.\gradlew.bat test
```

[테스트 코드](src/test/java/com/example/jpa)에는 SQL 로그 관찰 위주의 실습이 포함되어 있습니다. 테스트의 실행 성공만으로 조회 횟수나 매핑의 적절함까지 검증되지는 않습니다. QueryDSL 검색 구현은 [repository](src/main/java/com/example/jpa/repository)에 있습니다.
