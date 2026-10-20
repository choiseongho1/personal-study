# 랜덤박스 시스템 ERD

## 👤 USER (users)

| 컬럼명      | 타입      | 길이 | 제약조건      | 설명         |
|-------------|-----------|------|---------------|--------------|
| ID          | BIGINT    | -    | PK            | 사용자 ID    |
| EMAIL       | VARCHAR   | 50   | NOT NULL, UNIQUE | 이메일      |
| PASSWORD    | VARCHAR   | 255  | NOT NULL      | 비밀번호 (암호화됨) |
| NICKNAME    | VARCHAR   | 50   | NOT NULL      | 닉네임      |
| CREATED_AT  | DATETIME  | -    | NOT NULL      | 가입일       |
| UPDATED_AT  | DATETIME  | -    | -             | 정보 수정일  |

## 📦 RANDOM_BOX (random_boxes)

| 컬럼명           | 타입      | 길이 | 제약조건      | 설명           |
|------------------|-----------|------|---------------|----------------|
| ID               | BIGINT    | -    | PK            | 랜덤박스 ID     |
| NAME             | VARCHAR   | 255  | NOT NULL      | 랜덤박스 이름   |
| DESCRIPTION      | TEXT      | -    | NOT NULL      | 랜덤박스 설명   |
| PRICE            | INT       | -    | NOT NULL      | 가격           |
| QUANTITY         | INT       | -    | NOT NULL      | 수량           |
| SALES_START_TIME | DATETIME  | -    | NOT NULL      | 판매 시작 시간 |
| SALES_END_TIME   | DATETIME  | -    | NOT NULL      | 판매 종료 시간 |
| CREATED_AT       | DATETIME  | -    | NOT NULL      | 생성일         |
| UPDATED_AT       | DATETIME  | -    | -             | 수정일         |

## 🎁 RANDOM_BOX_ITEM (random_box_items)

| 컬럼명           | 타입      | 길이 | 제약조건      | 설명                 |
|------------------|-----------|------|---------------|----------------------|
| ID               | BIGINT    | -    | PK            | 아이템 ID            |
| RANDOM_BOX_ID    | BIGINT    | -    | FK            | 랜덤박스 ID          |
| NAME             | VARCHAR   | 255  | NOT NULL      | 아이템 이름          |
| DESCRIPTION      | TEXT      | -    | NOT NULL      | 아이템 설명          |
| RARITY           | VARCHAR   | 20   | NOT NULL      | 희귀도(COMMON, RARE, EPIC, LEGENDARY) |
| PROBABILITY      | DECIMAL   | -    | NOT NULL      | 확률(0.0 ~ 100.0)    |
| CREATED_AT       | DATETIME  | -    | NOT NULL      | 생성일               |
| UPDATED_AT       | DATETIME  | -    | -             | 수정일               |

## 🛒 PURCHASE (purchases)

| 컬럼명               | 타입      | 길이 | 제약조건      | 설명                        |
|----------------------|-----------|------|---------------|-----------------------------||
| ID                   | BIGINT    | -    | PK            | 구매 ID                     |
| USER_ID              | BIGINT    | -    | FK            | 사용자 ID                   |
| RANDOM_BOX_ID        | BIGINT    | -    | FK            | 랜덤박스 ID                 |
| PURCHASE_DATE_TIME   | DATETIME  | -    | NOT NULL      | 구매 일시                   |
| QUANTITY             | INT       | -    | NOT NULL      | 구매 수량                   |
| TOTAL_PRICE          | INT       | -    | NOT NULL      | 총 가격                     |
| STATUS               | VARCHAR   | 20   | NOT NULL      | 상태(COMPLETED, CANCELLED)  |
| CREATED_AT           | DATETIME  | -    | NOT NULL      | 생성일                      |
| UPDATED_AT           | DATETIME  | -    | -             | 수정일                      |

## 🎯 PURCHASE_RESULT (purchase_results)

| 컬럼명               | 타입      | 길이 | 제약조건      | 설명                        |
|----------------------|-----------|------|---------------|-----------------------------||
| ID                   | BIGINT    | -    | PK            | 결과 ID                     |
| PURCHASE_ID          | BIGINT    | -    | FK            | 구매 ID                     |
| RANDOM_BOX_ITEM_ID   | BIGINT    | -    | FK            | 랜덤박스 아이템 ID           |
| CREATED_AT           | DATETIME  | -    | NOT NULL      | 생성일                      |

## 🎫 COUPON (coupons)

| 컬럼명           | 타입      | 길이 | 제약조건      | 설명                 |
|------------------|-----------|------|---------------|----------------------|
| ID               | BIGINT    | -    | PK            | 쿠폰 ID              |
| CODE             | VARCHAR   | 50   | NOT NULL, UNIQUE | 쿠폰 코드           |
| NAME             | VARCHAR   | 255  | NOT NULL      | 쿠폰 이름            |
| DISCOUNT_TYPE    | VARCHAR   | 20   | NOT NULL      | 할인 유형(PERCENTAGE, FIXED) |
| DISCOUNT_VALUE   | INT       | -    | NOT NULL      | 할인 값              |
| MIN_PURCHASE     | INT       | -    | -             | 최소 구매 금액        |
| MAX_DISCOUNT     | INT       | -    | -             | 최대 할인 금액        |
| START_DATE       | DATETIME  | -    | NOT NULL      | 시작일               |
| END_DATE         | DATETIME  | -    | NOT NULL      | 종료일               |
| CREATED_AT       | DATETIME  | -    | NOT NULL      | 생성일               |
| UPDATED_AT       | DATETIME  | -    | -             | 수정일               |

## 💳 USER_COUPON (user_coupons)

| 컬럼명           | 타입      | 길이 | 제약조건      | 설명                 |
|------------------|-----------|------|---------------|----------------------|
| ID               | BIGINT    | -    | PK            | 사용자 쿠폰 ID        |
| USER_ID          | BIGINT    | -    | FK            | 사용자 ID            |
| COUPON_ID        | BIGINT    | -    | FK            | 쿠폰 ID              |
| USED             | BOOLEAN   | -    | NOT NULL      | 사용 여부            |
| USED_DATE        | DATETIME  | -    | -             | 사용일               |
| CREATED_AT       | DATETIME  | -    | NOT NULL      | 생성일               |
| UPDATED_AT       | DATETIME  | -    | -             | 수정일               |

## 테이블 간 관계

- **USER - PURCHASE**: 1:N 관계 (한 사용자는 여러 구매 정보를 가질 수 있음)
- **RANDOM_BOX - PURCHASE**: 1:N 관계 (한 랜덤박스는 여러 구매 정보를 가질 수 있음)
- **RANDOM_BOX - RANDOM_BOX_ITEM**: 1:N 관계 (한 랜덤박스는 여러 아이템을 가질 수 있음)
- **PURCHASE - PURCHASE_RESULT**: 1:N 관계 (한 구매는 여러 결과를 가질 수 있음)
- **RANDOM_BOX_ITEM - PURCHASE_RESULT**: 1:N 관계 (한 아이템은 여러 결과에 포함될 수 있음)
- **USER - USER_COUPON**: 1:N 관계 (한 사용자는 여러 쿠폰을 가질 수 있음)
- **COUPON - USER_COUPON**: 1:N 관계 (한 쿠폰은 여러 사용자에게 지급될 수 있음)
