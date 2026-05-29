# API 전환 계획 (Console Runner → Controller/Service/DTO)

> 목적: 순수 자바 콘솔 구조(Runner 중심)를 프론트엔드와 REST API로 통신하는 Spring 구조(Controller + Service + DTO)로 전환한다.
> 작성일: 2026-05-29 · 상태: 파일럿 진행 예정

---

## 1. 배경 / 현재 구조

```
OldMain (역할 메뉴 루프)
  → Runner (UC 진행자, runner/usecase/*)
    → Domain (비즈니스 로직, payment/·contract/ 등)
      → DAO (static 메서드 클래스, dao/*)
        → DBA (static, HikariCP 자체 풀 + ThreadLocal 트랜잭션, db/DBA.java)
          → MySQL
```

- `DpBeApplication`(@SpringBootApplication)와 `OldMain`(실제 콘솔 앱)이 공존. Spring Boot webmvc 의존성은 `build.gradle`에 이미 있으나 아직 미사용.
- **Runner 한 클래스에 4가지 책임이 섞여 있다** — 이것이 분리 대상:
  1. UI 입출력 (`ConsoleHelper.readXxx / printXxx`)
  2. UC 절차 조정 (단계 1,2,…/A1/E1 주석과 1:1)
  3. 트랜잭션 경계 (`DBA.beginTransaction/commit/rollback`)
  4. 도메인·DAO 호출
- 시나리오 문서(`design/Usecase_scenario.md`)는 **이미 웹 기준**(메뉴 클릭 → 조회조건 → [조회] 버튼 → 테이블 → 행 클릭 → 상세 패널 → [저장])이라 REST 매핑이 자연스럽다.

**전환의 핵심**: Runner의 ①UI를 HTTP(Controller+DTO)로, ②③④를 Service로 옮긴다.

---

## 2. 확정된 설계 결정

| 결정 항목 | 선택 | 이유 |
|---|---|---|
| **다단계 입력 흐름** | **클라이언트 주도** | 조회는 GET, 최종 제출은 완성된 DTO 한 번의 POST. 프론트가 중간 상태 보관, 서버는 무상태 유지. REST 정석. |
| **static → Spring 빈 전환 범위** | **(가) DAO·DBA·Domain은 static 그대로, Controller·Service만 빈** | 변경 최소. Spring은 모든 클래스를 빈으로 강제하지 않음 — `@Service` 안에서 `PaymentDAO.save(...)` 같은 static 호출을 그대로 사용. |
| **PK 동시성** | **추후 작업으로 분리** (§9 참조). 파일럿은 기존 `static sequence` PK를 그대로 사용 | 전체 테이블 surrogate PK 전환은 31개+ 테이블·DAO·도메인을 건드리는 대규모 작업이라, Controller/Service/DTO 전환(파일럿)과 분리한다. 파일럿 단계에선 동시성 위험을 감수(시연·학습 단계라 실제 충돌 드묾). |
| **트랜잭션** | 기존 `DBA.beginTransaction/commit/rollback` 유지 (위치만 Runner→Service로 이동) | (가) 방침. 전체 전환 단계에서 Spring `@Transactional`로 수렴 검토. |
| **진행 단위** | **파일럿 1~2개 먼저** (Payment + ContractInfo) | 쓰기 흐름(Payment)·읽기 흐름(ContractInfo) 두 유형으로 패턴 확립 후 나머지 36개에 전파. |

**기존 코드 보존 원칙**: `runner/`, `domain/`, `dao/`, `db/`, `OldMain` 모두 **그대로 둔다.** 새 `api/` 패키지를 얹어 공존시킨다.

---

## 3. PK 방식 — 파일럿은 현행 유지

파일럿(Payment + ContractInfo) 단계에서는 **PK 구조를 변경하지 않는다.** 기존 도메인의 `static int sequence` 기반 업무번호 생성(`"PAY00001"` 등)과 `SequenceSync`를 그대로 둔다. 전체 surrogate-PK 전환은 §9의 추후 작업으로 분리한다.

---

## 4. 목표 패키지 구조 — package-by-feature (도메인형)

레이어로 최상위를 가르지 않고 **도메인별 폴더 안에 레이어**를 둔다(package-by-feature). 최상위는 `domain/`(신규 Spring 코드 + 엔터티) · `global/`(전역 공통) · `old/`(전환 전 레거시)로 3분할. (2026-05-29 실제 적용 완료)

```
org.dpbe
├─ DpBeApplication, OldMain
├─ domain/                          ← 신규 Spring 코드 (피처별)
│   ├─ common/
│   │   ├─ entity/   BankAccount, Attachment
│   │   └─ enums/    상태/유형 열거형 33종
│   ├─ actor/        Customer, Employee, Designer, Agency, ... (도메인 모델 14)
│   ├─ contract/     controller/ service/ repository/ dto/ entity/(Contract, Cancellation, ...)
│   ├─ payment/      controller/ service/ repository/ dto/ entity/(Payment, PaymentItem, PaymentRecord, ...)
│   ├─ customer/     repository/(CustomerRepository)
│   └─ claim/ consultation/ sales/ education/ inquiry/   각 entity/ (도메인 모델만, 아직 미전환)
├─ global/
│   ├─ exception/    ApiException, ErrorResponse, ApiExceptionHandler(@RestControllerAdvice)
│   └─ jdbc/         SqlExecutor  (DataSourceUtils 기반 공통 JDBC 헬퍼)
└─ old/                             ← 레거시 (전환 전, 무수정 공존)
    ├─ dao/          DAO 45
    ├─ db/           DBA, SequenceSync
    └─ runner/       usecase/ Runner 36 + ConsoleHelper, SampleData
```

- DTO는 record 기반.
- **엔터티 폴더**: 도메인 모델 클래스를 `domain.<feature>.entity`로 이동 완료. P4는 raw JDBC라 JPA `@Entity` 애너테이션은 없으며, 영속 모델 역할을 그대로 한다(레거시 `old/dao`·`old/runner`와 공유).
- 컴포넌트 스캔: `@SpringBootApplication`(`org.dpbe`)이 `domain.*`·`global.*`를 모두 스캔. `old.*`는 Spring 애너테이션이 없어 무영향.

---

## 5. 엔드포인트 매핑

### ContractInfo (읽기 흐름) — 단순한 쪽 먼저

| HTTP | 설명 | 시나리오 대응 |
|---|---|---|
| `GET /api/contracts?type=&page=` | 필터·페이징 계약 목록 | Basic 2, A1(필터 없음 전체조회), A2(결과 없음) |
| `GET /api/contracts/{contractNo}` | 상세 + 만기임박(D-day) 계산 + 특약 | Basic 4, A3(만기 30일 이내), A5(특약 없음) |

- E1(페이지 출력 오류)·UC 간 이동(만기관리/통계)은 후속 Runner 전환 시 연계.

### Payment (다단계 쓰기 흐름) — 클라이언트 주도

| HTTP | 설명 | 시나리오 대응 |
|---|---|---|
| `GET /api/customers/{id}/contracts` | 납입 가능 계약 목록 | 계약 선택 화면 |
| `POST /api/payments/preview` | 계약·횟수·방법 받아 총액/선납할인 계산 (저장 X) | 총액 산출 단계 |
| `POST /api/payments` | 완성 DTO 검증 → 트랜잭션 저장 (payments + payment_items + payment_records) | 제출, E1 |

- 프론트가 중간 상태(선택 계약·횟수·계좌)를 보관 → `preview`로 금액 확인 → 최종 `POST`. 서버 무상태.
- 검증 실패·계좌 인증 실패는 HTTP 4xx + 에러 DTO (콘솔의 E1/재입력 루프 대체).
- 저장은 기존 `PaymentRunner`의 트랜잭션 블록(payments + items + records 원자적 저장) 로직을 Service로 이관.

---

## 6. 작업 순서 (파일럿)

1. DTO 작성 (record 기반).
2. ContractService / ContractController → `./gradlew compileJava` + 기동 확인.
3. PaymentService / PaymentController → 컴파일 + 기동 확인.
4. `@ControllerAdvice` 전역 예외 핸들러.
5. 전체 `./gradlew compileJava` 검증.

> PK 변경 없음 — 기존 `static sequence`/DAO/도메인 그대로 사용.

### 6.1 파일럿 검증 결과 (2026-05-29, 완료)

`docker compose up -d` + `./gradlew bootRun`(포트 8080) 기동 후 실제 호출로 전 경로 확인:
- `GET /api/contracts`, `GET /api/contracts/{no}`(D-day 계산 포함), `GET /api/customers/{id}/contracts` → 200
- `POST /api/payments/preview` → 총액/선납할인 계산 정상
- `POST /api/payments` → PAY00001 생성, payments+payment_items+payment_records **트랜잭션 원자 저장** 확인
- 에러 경로: 없는 계약 404, 빈 items 400, 계좌 인증 실패 400 — `@RestControllerAdvice` 표준 에러 DTO 반환
- **발견·수정한 기존 버그 — BUG-API-01**: `CustomerDAO.findById`의 SELECT에 `registered_at` 컬럼 누락(매퍼는 해당 컬럼을 읽음) → SQLException을 DBA가 삼켜 null 반환 → submit이 "고객 없음" 404. SELECT에 `registered_at` 추가로 수정. (다른 DAO의 `findBy~`에도 동일 패턴 존재 가능 — 추후 점검 대상)
- 주의: 샘플 데이터(`SampleData`)는 콘솔 진입점 `OldMain`에서만 주입됨. Spring 진입점(`DpBeApplication`)으로 띄우면 빈 DB → 별도 시드 필요.

---

## 7. 후속 전환 시 적용할 패턴 (파일럿 확립 목표)

- **읽기 UC** → `GET` 1~2개 + 응답 DTO. 필터는 쿼리 파라미터, 상세는 path variable.
- **쓰기 UC(다단계)** → 조회 `GET` + 계산 `preview POST`(선택) + 제출 `POST`. 트랜잭션은 Service의 제출 메서드에 한정.
- **UC 간 이동**(예: 계약조회→만기관리) → 콘솔의 Runner 직접 호출 대신, 프론트 라우팅 + 각 UC 독립 엔드포인트로 분리.
- **E1/검증 실패** → 예외 던지고 `@ControllerAdvice`에서 4xx + 표준 에러 DTO로 변환.

---

## 10. P4 — Spring 트랜잭션(@Transactional) 전환 (파일럿)

### 10.1 확정 결정

- DB 접근 방식: **P4 최종형** — Spring `DataSource` + `DataSourceTransactionManager` + `@Transactional`. 범위는 **파일럿(Payment·Contract)만**.
- 리포지토리 구현: **raw JDBC + `DataSourceUtils`** (JdbcTemplate 미사용). 보일러플레이트는 공통 헬퍼 `SqlExecutor`로 억제.
- 구조: **스트랭글러** — 기존 `DBA`·`dao/*`·`runner/*`·`OldMain`은 무수정 공존. 파일럿만 새 Spring 리포지토리 경로 사용. DBA는 전체 전환 완료 시점에 제거.
- 패키지: package-by-feature (§4).

### 10.2 핵심 원리 (왜 이렇게)

`@Transactional`이 JDBC 커넥션을 통제하는 조건은 ① `DataSourceTransactionManager` 빈, ② DAO가 커넥션을 **`DataSourceUtils.getConnection(ds)`** 로 획득(= `TransactionSynchronizationManager` ThreadLocal에 바인딩된 트랜잭션 커넥션을 꺼냄). JdbcTemplate은 ②를 자동화할 뿐 **필수가 아님**. 현재 `DBA`가 `@Transactional`과 안 맞는 이유는 *자체 풀에서 독립 커넥션을 직접 열기 때문* → `DataSourceUtils` 경유로 바꾸면 해결.

레이어 책임 (V2 수동 방식과의 차이):
```
Controller → Service(@Transactional, Repository 주입; 커넥션 모름)
           → Repository(DataSource 주입, DataSourceUtils로 동기화 커넥션 획득) → DB
```
서비스는 `DataSource`/`Connection`을 만지지 않는다(프록시가 경계 관리). DataSource를 아는 건 리포지토리뿐.

### 10.3 작업 순서

1. `build.gradle`: `spring-boot-starter-jdbc` 추가.
2. `application.yaml`: `spring.datasource.{url,username,password,driver-class-name}` (DBA 하드코딩 값 이전).
3. `global/jdbc/SqlExecutor`: `DataSourceUtils` 기반 `executeUpdate`/`executeQuery`/`queryOne` 헬퍼(DBA API와 유사하나 커넥션 출처가 Spring DataSource).
4. 리포지토리: `ContractRepository`, `CustomerRepository`, `PaymentRepository`, `PaymentRecordRepository` (raw JDBC, SqlExecutor 경유).
5. 서비스: `DBA.beginTransaction/commit/rollback` 제거 → `@Transactional`(읽기 전용은 `readOnly=true`), 리포지토리 주입.
6. `./gradlew compileJava` + 기동·엔드포인트 재검증.

### 10.3.1 P4 검증 결과 (2026-05-29, 완료)

`spring-boot-starter-jdbc` 추가 후 기동 시 Spring이 `HikariPool-1`(자체 DataSource) 생성 확인 — DBA 풀과 공존.
- 읽기 경로: `GET /api/contracts`, `GET /api/contracts/{no}` → 200 (`@Transactional(readOnly=true)` + SqlExecutor + DataSourceUtils 경유)
- 쓰기/커밋: `POST /api/payments` → PAY00001 생성, payments+payment_items+payment_records 저장 확인
- **롤백 검증**: `payment_records.method`를 일시적으로 `VARCHAR(3)`로 축소 → 제출 시 두 번째 INSERT가 "Data too long"으로 실패 → `@Transactional`이 먼저 들어간 payments·payment_items까지 **전부 롤백**(3개 테이블 0건), HTTP 500. 다중 리포지토리 원자성 보장 확인. (이후 컬럼 원복)
- 트리거 방식 롤백 테스트는 admin 계정의 SUPER 권한 부재(ERROR 1419)로 불가 → 컬럼 축소 방식으로 대체.

### 10.4 추후 작업 (별도)

- **웹 기동용 기본/시드 데이터 클래스**: `SampleData.initialize()`는 콘솔 `OldMain`에서만 호출됨. Spring 기동 시 기본값을 주입할 클래스(예: `CommandLineRunner`/`@PostConstruct` 시더) 신설 필요. ← 파일럿 검증 시 빈 DB 문제로 확인됨.
- 전체 전환 완료 시 `DBA` 제거 + DAO들을 리포지토리로 흡수.

## 8. 미해결 / 후속 검토 항목

- DBA 자체 HikariCP 풀 ↔ Spring DataSource 이원화 — 전체 전환 시 (다) 완전 통합(@Transactional + DataSource 빈)으로 수렴 검토.
- `SequenceSync` ↔ `seq_counters` 초기값 정합.
- CORS / 프론트엔드 오리진 설정.
- 인증·인가(역할 기반) — 현재 콘솔은 역할 메뉴로만 구분. API에서는 별도 설계 필요.
- 응답 DTO 표준 포맷(성공/에러 공통 envelope) 채택 여부.

---

## 9. [추후 작업] 전체 테이블 surrogate-PK 전환

> 파일럿 완료 후 별도 단계로 진행. 결정은 확정됨, 실행만 보류.

### 9.1 목표 / 확정 결정

- **모든 테이블에 DB가 자동 채번하는 대리키 `id BIGINT AUTO_INCREMENT PRIMARY KEY`를 둔다.** (범위: C1 자연키 + C2 시퀀스형 **전부**)
- 시퀀스형 업무번호(`payment_no` 등)는 **방법 2 (INSERT 후 회수)**로 생성한다.
- `static int sequence` 기반 PK 생성과 `SequenceSync`는 제거/축소한다.

### 9.2 ⚠️ 중요 제약 — MySQL 생성 컬럼은 AUTO_INCREMENT를 참조 못 함

처음엔 `payment_no VARCHAR AS (CONCAT('PAY', LPAD(id,5,'0'))) STORED`(생성 컬럼, "방법 1")로 하려 했으나, **MySQL은 생성 컬럼이 `AUTO_INCREMENT` 컬럼을 base column으로 참조하는 것을 금지**한다 (*"An AUTO_INCREMENT column cannot be used as a base column in a generated column definition."*). → 방법 1 폐기, **방법 2 채택**.

**방법 2 절차** (DAO `save()`):
1. `id` 없이 INSERT (업무번호 컬럼은 비우거나 임시값).
2. 생성된 `id` 회수 — DBA에 생성키 반환 헬퍼 추가 필요 (`Statement.RETURN_GENERATED_KEYS` 또는 `SELECT LAST_INSERT_ID()`).
3. `payment_no = "PAY" + format(id)`로 UPDATE.
- C1 자연키(customer_id, employee_id, product_name 등)는 id에서 파생하지 않음 — 앱이 주는 값을 그대로 두고 surrogate `id`만 추가, 업무키 컬럼은 `UNIQUE`로 유지.

### 9.3 FK 영향

기존 FK는 PK가 아닌 **업무키 문자열**(`contract_no`, `payment_no`, `customer_id` 등)을 참조한다. 업무키 컬럼을 `UNIQUE`로 유지하면 PK를 surrogate id로 바꿔도 FK는 그대로 동작한다(FK 대상은 PK가 아니어도 UNIQUE면 됨). 따라서 FK 재배선은 불필요, 대신 모든 업무키 컬럼에 `UNIQUE` 보장 필요.

### 9.4 PK 감사 결과 (44개 테이블)

| 분류 | 테이블 | 조치 |
|---|---|---|
| **A. 이미 DB 자동 PK** (4) | customer_registrations, activity_schedule_items, education_attendances, payment_items | 변경 불필요 |
| **B. 정수 PK, 앱이 부여** (2) | insurance_applications, policy_applications (`application_no INT`) | surrogate id 추가 또는 AUTO_INCREMENT화 |
| **B'. 싱글톤** (1) | overdue_notice_settings (`id INT DEFAULT 1`) | 그대로 둠 |
| **C1. 자연키/마스터** (10) | customers, education_trainers, sales_managers, insurance_reviewers, claims_handlers, dispatch_agents, finance_managers, designers, agencies, insurance_products | surrogate id 추가 + 업무키 UNIQUE 유지 (업무키는 앱 생성값 그대로) |
| **C2. 시퀀스형 업무키** (27+) | contracts, payments, accident_reports, claim_requests, education_plans, consultation_requests, interview_schedules, interview_records, proposals, underwritings, revivals, channel_recruitments, channel_screenings, activity_plans, bonus_requests, sales_activity_managements, sales_org_evaluations, inquiries, expiring_contract_notices, payment_records, cancellations, contract_statistics, dispatches, damage_investigations, education_preparations, refund_calculations, claim_calculations, dispatch_records, education_executions, refund_payments, claim_payments | surrogate id 추가 + 업무번호 방법 2로 파생 + 도메인 `static sequence` 제거 |

### 9.5 작업 범위 요약

- `schema.sql`: 31개+ 테이블에 `id` 추가, 업무키 `UNIQUE`화, 스키마 재생성(`docker compose down -v && up -d`).
- `DBA`: 생성키 반환 헬퍼 추가.
- DAO ~31개: `save()` upsert → INSERT 후 회수 패턴으로 변경.
- 도메인 ~31개: `static int sequence`/생성자 PK 부여 제거.
- `SequenceSync`: 해당 엔티티 등록 제거/축소.