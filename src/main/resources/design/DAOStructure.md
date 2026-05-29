# 구조 개선 설계서 — 트랜잭션 & FK 제약

---

## 1. 트랜잭션 지원 (DBA.java)

### 현황과 문제

```
현재 흐름:
Runner → DAO.save() → DBA.executeUpdate()
                         └─ ds.getConnection() → SQL 실행 → Connection.close()
```

`DAO.save()` 안에서 두 테이블에 INSERT해도 각각 별도 커넥션이라 원자성이 없다.

**대표 사례:**

| DAO | 관련 테이블 |
|---|---|
| `EducationExecutionDAO.save()` | `education_executions` + `education_attendances` |
| `PaymentDAO.save()` | `payments` + `payment_items` |
| `InsuranceCancellationRunner` | `cancellations` + `contracts` 상태 업데이트 |

첫 번째 INSERT 성공 후 두 번째가 실패하면 데이터가 반만 들어간 상태로 남는다.

---

### 설계 — ThreadLocal 기반 트랜잭션

**핵심 필드:**
```java
private static final ThreadLocal<Connection> TX_CONN = new ThreadLocal<>();
```

**트랜잭션 흐름:**
```
Runner.run()
  ├─ DBA.beginTransaction()          // 커넥션 1개 꺼내서 autoCommit=false, TX_CONN에 보관
  ├─ DAO.save(A)
  │    └─ DBA.executeUpdate()        // TX_CONN 있으면 재사용 (새 커넥션 X)
  ├─ DAO.save(B)
  │    └─ DBA.executeUpdate()        // 동일 커넥션 재사용
  ├─ DBA.commit()                    // commit → close → TX_CONN 제거
  └─ (예외 발생 시) DBA.rollback()   // rollback → close → TX_CONN 제거
```

**추가할 public 메서드 3개:**

```java
public static void beginTransaction()  // autoCommit=false 커넥션 획득 후 TX_CONN에 보관
public static void commit()            // 커밋 + 커넥션 반납
public static void rollback()          // 롤백 + 커넥션 반납
```

**executeUpdate / executeQuery 내부 수정 원칙:**
- TX_CONN이 있으면 → 그 커넥션 재사용, 직접 닫지 않음
- TX_CONN이 없으면 → 기존처럼 풀에서 꺼내 try-with-resources로 자동 반납

```java
// 내부 헬퍼 — 커넥션 획득
private static Connection acquireConnection() throws SQLException {
    Connection tx = TX_CONN.get();
    return (tx != null) ? tx : DATA_SOURCE.getConnection();
}

// 내부 헬퍼 — 커넥션 반납 (TX 중이면 닫지 않음)
private static void releaseConnection(Connection con) {
    if (TX_CONN.get() == null) {
        try { con.close(); } catch (SQLException ignored) {}
    }
}
```

**구현 스케치:**

```java
public static void beginTransaction() {
    try {
        Connection conn = DATA_SOURCE.getConnection();
        conn.setAutoCommit(false);
        TX_CONN.set(conn);
    } catch (SQLException e) {
        System.err.println("[DBA] beginTransaction 오류: " + e.getMessage());
    }
}

public static void commit() {
    Connection conn = TX_CONN.get();
    if (conn == null) return;
    try {
        conn.commit();
    } catch (SQLException e) {
        System.err.println("[DBA] commit 오류: " + e.getMessage());
    } finally {
        closeTx(conn);
    }
}

public static void rollback() {
    Connection conn = TX_CONN.get();
    if (conn == null) return;
    try {
        conn.rollback();
    } catch (SQLException e) {
        System.err.println("[DBA] rollback 오류: " + e.getMessage());
    } finally {
        closeTx(conn);
    }
}

private static void closeTx(Connection conn) {
    try {
        conn.setAutoCommit(true);
        conn.close();
    } catch (SQLException ignored) {
    } finally {
        TX_CONN.remove();
    }
}
```

---

### 트랜잭션 적용 대상 Runner

단일 테이블만 저장하는 Runner는 기존 그대로. 두 테이블 이상 저장하는 Runner에만 적용.

| Runner | 이유 |
|---|---|
| `EducationExecutionRunner` | education_executions + education_attendances 동시 INSERT |
| `PaymentRunner` | payments + payment_items 동시 INSERT |
| `InsuranceCancellationRunner` | cancellations INSERT + contracts UPDATE |
| `ExpiringContractManagementRunner` | expiring_contract_notices + contracts 동시 UPDATE |
| `RefundCalculationRunner` | refund_calculations 확정 후 RefundPayment 생성 연동 |

**적용 패턴:**

```java
public static void run() {
    DBA.beginTransaction();
    try {
        // ... 기존 로직 (DAO.save 호출들)
        DAO.save(A);
        DAO.save(B);
        DBA.commit();
        ConsoleHelper.printSuccess("저장되었습니다.");
    } catch (Exception e) {
        DBA.rollback();
        ConsoleHelper.printError("처리 중 오류가 발생했습니다. 변경사항이 취소되었습니다.");
        ConsoleHelper.waitEnter();
    }
}
```

---

## 2. FK 제약 추가 (schema.sql)

### 현황과 문제

```sql
-- 현재: 주석으로만 관계 표시 (DB가 무결성 검사 안 함)
customer_id VARCHAR(20),   -- → customers.customer_id
contract_no VARCHAR(20),   -- → contracts.contract_no
```

FK 없이는:
- 존재하지 않는 customer_id를 가진 contract INSERT 가능
- 부모 레코드가 고아(orphan) 자식을 남겨도 DB가 모름
- 데이터 무결성을 앱 코드에만 의존

### 설계 원칙

1. **모두 NULLABLE FK** — 컬럼이 이미 NULL 허용이므로 NULL 삽입은 제약 없이 통과, 비-NULL 값만 참조 무결성 검사
2. **CASCADE 없음 (기본 RESTRICT)** — 부모 삭제 시 자식이 있으면 에러 (실수 방지)
3. **ALTER TABLE을 schema.sql 끝에 추가** — CREATE TABLE은 최소 변경
4. **Docker 볼륨 재생성 필요**: `docker compose down -v && docker compose up -d`

---

### FK 관계 목록 (23개)

**Tier 2 — customers 참조 (6개)**

| 자식 테이블 | FK 컬럼 | 부모 |
|---|---|---|
| contracts | customer_id | customers |
| payments | customer_id | customers |
| accident_reports | customer_id | customers |
| claim_requests | customer_id | customers |
| insurance_applications | customer_id | customers |
| policy_applications | customer_id | customers |

**Tier 2/3 — contracts 참조 (5개)**

| 자식 테이블 | FK 컬럼 | 부모 |
|---|---|---|
| claim_requests | contract_no | contracts |
| revivals | contract_no | contracts |
| payment_records | contract_no | contracts |
| cancellations | contract_no | contracts |
| expiring_contract_notices | contract_no | contracts |

**Tier 3 — 중간 엔터티 참조 (3개)**

| 자식 테이블 | FK 컬럼 | 부모 |
|---|---|---|
| dispatches | accident_no | accident_reports |
| damage_investigations | claim_no | claim_requests |
| education_preparations | plan_no | education_plans |

**Tier 4 — 처리 레코드 참조 (5개)**

| 자식 테이블 | FK 컬럼 | 부모 |
|---|---|---|
| dispatch_records | dispatch_no | dispatches |
| claim_calculations | investigation_no | damage_investigations |
| education_executions | prep_no | education_preparations |
| education_attendances | execution_no | education_executions |
| refund_calculations | cancellation_no | cancellations |
| activity_schedule_items | plan_no | activity_plans |

**Tier 5 — 최종 처리 참조 (3개)**

| 자식 테이블 | FK 컬럼 | 부모 |
|---|---|---|
| claim_payments | calculation_no | claim_calculations |
| refund_payments | refund_no | refund_calculations |
| payment_items | payment_no | payments |

---

### 제외 대상

| 테이블.컬럼 | 제외 이유 |
|---|---|
| `customer_registrations.customer_id` | 판매채널이 고객 DB 등록 전에 registration을 먼저 저장하는 흐름 존재 |

---

### schema.sql에 추가할 코드 형태

```sql
-- ============================================================
-- FK 제약 (전부 NULLABLE — NULL 삽입 허용, 비-NULL만 무결성 검사)
-- ============================================================

-- Tier 2: customers 참조
ALTER TABLE contracts             ADD CONSTRAINT fk_contracts_customer         FOREIGN KEY (customer_id)      REFERENCES customers(customer_id);
ALTER TABLE payments              ADD CONSTRAINT fk_payments_customer           FOREIGN KEY (customer_id)      REFERENCES customers(customer_id);
ALTER TABLE accident_reports      ADD CONSTRAINT fk_accident_reports_customer   FOREIGN KEY (customer_id)      REFERENCES customers(customer_id);
ALTER TABLE claim_requests        ADD CONSTRAINT fk_claim_requests_customer     FOREIGN KEY (customer_id)      REFERENCES customers(customer_id);
ALTER TABLE insurance_applications ADD CONSTRAINT fk_insurance_apps_customer   FOREIGN KEY (customer_id)      REFERENCES customers(customer_id);
ALTER TABLE policy_applications   ADD CONSTRAINT fk_policy_apps_customer        FOREIGN KEY (customer_id)      REFERENCES customers(customer_id);

-- Tier 2/3: contracts 참조
ALTER TABLE claim_requests              ADD CONSTRAINT fk_claim_requests_contract         FOREIGN KEY (contract_no) REFERENCES contracts(contract_no);
ALTER TABLE revivals                    ADD CONSTRAINT fk_revivals_contract               FOREIGN KEY (contract_no) REFERENCES contracts(contract_no);
ALTER TABLE payment_records             ADD CONSTRAINT fk_payment_records_contract        FOREIGN KEY (contract_no) REFERENCES contracts(contract_no);
ALTER TABLE cancellations               ADD CONSTRAINT fk_cancellations_contract          FOREIGN KEY (contract_no) REFERENCES contracts(contract_no);
ALTER TABLE expiring_contract_notices   ADD CONSTRAINT fk_expiring_notices_contract       FOREIGN KEY (contract_no) REFERENCES contracts(contract_no);

-- Tier 3: 중간 엔터티 참조
ALTER TABLE dispatches             ADD CONSTRAINT fk_dispatches_accident        FOREIGN KEY (accident_no)      REFERENCES accident_reports(accident_no);
ALTER TABLE damage_investigations  ADD CONSTRAINT fk_damage_inv_claim           FOREIGN KEY (claim_no)         REFERENCES claim_requests(claim_no);
ALTER TABLE education_preparations ADD CONSTRAINT fk_edu_prep_plan              FOREIGN KEY (plan_no)          REFERENCES education_plans(plan_no);

-- Tier 4: 처리 레코드 참조
ALTER TABLE dispatch_records      ADD CONSTRAINT fk_dispatch_records_dispatch   FOREIGN KEY (dispatch_no)      REFERENCES dispatches(dispatch_no);
ALTER TABLE claim_calculations    ADD CONSTRAINT fk_claim_calc_investigation     FOREIGN KEY (investigation_no) REFERENCES damage_investigations(investigation_no);
ALTER TABLE education_executions  ADD CONSTRAINT fk_edu_exec_prep               FOREIGN KEY (prep_no)          REFERENCES education_preparations(prep_no);
ALTER TABLE education_attendances ADD CONSTRAINT fk_edu_attend_exec             FOREIGN KEY (execution_no)     REFERENCES education_executions(execution_no);
ALTER TABLE refund_calculations   ADD CONSTRAINT fk_refund_calc_cancellation    FOREIGN KEY (cancellation_no)  REFERENCES cancellations(cancellation_no);
ALTER TABLE activity_schedule_items ADD CONSTRAINT fk_schedule_items_plan       FOREIGN KEY (plan_no)          REFERENCES activity_plans(plan_no);

-- Tier 5: 최종 처리 참조
ALTER TABLE claim_payments  ADD CONSTRAINT fk_claim_payments_calc    FOREIGN KEY (calculation_no) REFERENCES claim_calculations(calculation_no);
ALTER TABLE refund_payments ADD CONSTRAINT fk_refund_payments_refund FOREIGN KEY (refund_no)      REFERENCES refund_calculations(refund_no);
ALTER TABLE payment_items   ADD CONSTRAINT fk_payment_items_payment  FOREIGN KEY (payment_no)     REFERENCES payments(payment_no);
```

---

## 3. 작업 순서

```
Step 1  DBA.java 수정
          - ThreadLocal<Connection> TX_CONN 필드 추가
          - beginTransaction() / commit() / rollback() / closeTx() 추가
          - executeUpdate() / executeQuery() / exists() 내부 분기 수정

Step 2  ./gradlew compileJava 확인

Step 3  5개 Runner에 트랜잭션 감싸기
          - EducationExecutionRunner
          - PaymentRunner
          - InsuranceCancellationRunner
          - ExpiringContractManagementRunner
          - RefundCalculationRunner

Step 4  ./gradlew compileJava 확인

Step 5  schema.sql 끝에 ALTER TABLE 23개 추가

Step 6  docker compose down -v && docker compose up -d

Step 7  ./gradlew run 으로 통합 테스트
```

---

## 4. 위험 요소 및 고려사항

| 항목 | 내용 |
|---|---|
| 기존 Runner 영향 | 트랜잭션 미적용 Runner는 기존과 완전 동일하게 동작 |
| HikariCP 호환 | ThreadLocal 방식은 HikariCP와 호환. TX 중 커넥션은 풀로 반납되지 않으므로 poolSize(현재 10) 내에서 관리 |
| FK + NULL 삽입 | NULL 값은 FK 검사 대상 아님 (MySQL 표준). 기존 NULL 삽입 코드 수정 불필요 |
| FK + 삭제 제약 | RESTRICT(기본)이므로 자식 레코드가 있는 부모 삭제 시 에러. 이 앱에서 삭제는 없으므로 영향 없음 |
| 기존 데이터 | FK 추가 시 기존 데이터 무결성 검사. Docker 볼륨 재생성으로 해결 |