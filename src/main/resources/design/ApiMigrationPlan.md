# API 전환 계획 / 진행 현황

> **목적**: 순수 자바 콘솔 구조(Runner 중심)를, 프론트엔드와 REST로 통신하는 Spring 구조(Controller / Service / Repository / DTO)로 전환한다.
> **방식**: 한 번에 바꾸지 않고, 대표 UC 2개를 파일럿으로 완성해 패턴을 확립한 뒤 나머지로 확장한다. 레거시(`old/`)는 무수정으로 공존한다.
> **현재 상태 (2026-05-29)**: 패키지 구조 개편 ✅ · 파일럿 2개(계약 조회·보험료 납입) ✅ · Spring `@Transactional` 도입 ✅ · 기동용 시더 ✅

---

## 1. 배경 — 출발 구조

```
OldMain (역할 메뉴 루프)
  → Runner (UC 진행자)
    → Domain (비즈니스 로직)
      → DAO (static)
        → DBA (static, 자체 HikariCP 풀 + ThreadLocal 트랜잭션)
          → MySQL
```

**Runner 한 클래스에 4가지 책임이 섞여 있던 것**이 분리 대상이었다:
1. UI 입출력 (`ConsoleHelper`)
2. UC 절차 조정 (시나리오 단계 1·A1·E1과 1:1)
3. 트랜잭션 경계 (`DBA.beginTransaction/commit/rollback`)
4. 도메인·DAO 호출

→ **①을 HTTP(Controller+DTO)로, ②③④를 Service/Repository로** 옮긴다. 시나리오 문서(`Usecase_scenario.md`)가 이미 웹 화면 흐름(조회조건→[조회]→테이블→행 클릭→상세→[저장])이라 REST 매핑이 자연스럽다.

---

## 2. 확정 설계 결정

| 항목 | 결정 | 이유 |
|---|---|---|
| **다단계 입력 흐름** | 클라이언트 주도 | 조회 GET + 최종 제출은 완성 DTO 한 번의 POST. 서버 무상태. |
| **패키지 구조** | package-by-feature (도메인형) | 코드 위치만으로 도메인 식별. `domain/`·`global/`·`old/` 3분할 (§3). |
| **DB 접근/트랜잭션** | Spring `DataSource` + `@Transactional` (강의 V3-3 최종형) | 선언적 트랜잭션·추상화 획득. 리포지토리는 raw JDBC + `DataSourceUtils` (§4). |
| **전환 방식** | 스트랭글러 | 레거시 `old/`(DBA·DAO·Runner)는 무수정 공존, 파일럿만 새 Spring 경로. DBA는 전체 전환 완료 시 제거. |
| **진행 단위** | 파일럿 2개 먼저 | 읽기(계약 조회)·쓰기(보험료 납입) 두 유형으로 패턴 확립 후 확장. |
| **PK 동시성** | 파일럿은 현행 유지, 전체 전환은 추후 | `static sequence` PK 그대로. 전체 surrogate-PK 전환은 대규모라 분리 (부록 A). |

---

## 3. 패키지 구조 (현행)

```
org.dpbe
├─ DpBeApplication, OldMain
├─ domain/                          ← 신규 Spring 코드 (피처별)
│   ├─ common/{entity, enums}       BankAccount·Attachment / 열거형 33종
│   ├─ actor/                       Customer, Employee, Designer, ... (도메인 모델)
│   ├─ contract/                    controller/ service/ repository/ dto/ entity/
│   ├─ payment/                     controller/ service/ repository/ dto/ entity/
│   ├─ customer/                    repository/
│   └─ claim·consultation·sales·education·inquiry/   entity/ (모델만, 미전환)
├─ global/
│   ├─ exception/                   ApiException, ErrorResponse, ApiExceptionHandler
│   ├─ jdbc/                        SqlExecutor (DataSourceUtils 기반 공통 JDBC 헬퍼)
│   └─ seed/                        DataSeeder (기동 시 초기 데이터)
└─ old/                             ← 레거시 (무수정 공존)
    ├─ dao/ (45)   db/ (DBA, SequenceSync)   runner/ (usecase 36 + ConsoleHelper, SampleData)
```

- DTO·도메인 모델은 `domain.<feature>` 아래. 엔터티는 raw JDBC라 JPA `@Entity`는 없고 영속 모델 역할만 한다(레거시와 공유).
- 컴포넌트 스캔: `@SpringBootApplication`(`org.dpbe`)이 `domain.*`·`global.*`를 스캔. `old.*`는 Spring 애너테이션이 없어 무영향.

---

## 4. DB 접근 & 트랜잭션 (P4)

### 동작 원리
`@Transactional`이 JDBC 커넥션을 통제하는 조건은 ① `DataSourceTransactionManager` 빈(`spring-boot-starter-jdbc`가 자동 등록), ② 데이터 접근 코드가 커넥션을 **`DataSourceUtils.getConnection(ds)`** 로 획득(= `TransactionSynchronizationManager`에 바인딩된 트랜잭션 커넥션을 꺼냄). JdbcTemplate은 ②를 자동화할 뿐 필수가 아니다. 레거시 `DBA`가 `@Transactional`과 안 맞았던 이유는 *자체 풀에서 독립 커넥션을 직접 열기 때문*이다.

### 레이어 책임
```
Controller → Service(@Transactional; Repository 주입, 커넥션 모름)
           → Repository(SqlExecutor 주입) → SqlExecutor(DataSource 주입, DataSourceUtils로 동기화 커넥션) → DB
```
서비스는 `DataSource`/`Connection`을 만지지 않는다(프록시가 경계 관리). 조회 메서드는 `@Transactional(readOnly=true)`.

### 구성 요소
- `build.gradle`: `spring-boot-starter-jdbc`
- `application.yaml`: `spring.datasource.*` (접속정보 외부화 — DBA 하드코딩 탈출)
- `global/jdbc/SqlExecutor`: `executeUpdate`/`executeQuery`/`queryOne`. SQLException은 런타임 예외로 변환(롤백 유도).
- 리포지토리: `ContractRepository`, `CustomerRepository`, `PaymentRepository`, `PaymentRecordRepository` (raw JDBC, SqlExecutor 경유)
- 검증·에러 → `throw ApiException` → `@RestControllerAdvice`(global.exception)에서 4xx/5xx + `ErrorResponse`

### 풀 공존
Spring 자체 `HikariPool`과 레거시 `DBA` 풀이 같은 MySQL을 각자 바라본다. 파일럿 경로만 Spring 풀, 레거시는 DBA. **전체 전환 완료 시 DBA 제거**.

---

## 5. 엔드포인트 매핑

### 계약 정보 조회 (읽기 흐름)
| HTTP | 설명 | 시나리오 |
|---|---|---|
| `GET /api/contracts?type=&page=&size=` | 필터·페이징 목록 | Basic 2 / A1(전체) / A2(없음) |
| `GET /api/contracts/{contractNo}` | 상세 + 만기 D-day + 특약 | Basic 4 / A3(만기임박) / A5(특약없음) |

### 보험료 납입 (다단계 쓰기 흐름, 클라이언트 주도)
| HTTP | 설명 | 시나리오 |
|---|---|---|
| `GET /api/customers/{id}/contracts` | 납입 가능 계약 목록 | 계약 선택 |
| `POST /api/payments/preview` | 총액·선납할인 계산 (저장 X) | 총액 산출 |
| `POST /api/payments` | 검증 → 트랜잭션 저장(payments+payment_items+payment_records) | 제출 / E1 |

프론트가 중간 상태(선택 계약·횟수·계좌)를 보관 → `preview`로 금액 확인 → 최종 `POST`. 검증/계좌 인증 실패는 4xx + 에러 DTO.

---

## 6. 초기 데이터 시더

`global/seed/DataSeeder` (`CommandLineRunner`):
- 레거시 `SampleData`는 콘솔 `OldMain`에서만 실행 → Spring 기동 시 DB가 비는 문제를 대체.
- **신규 Spring 경로**(Repository + `@Transactional` + DataSource)로 **실제 MySQL에 적재**.
- 멱등: 고객 데이터가 있으면 건너뜀. 토글: `app.seed.enabled=false`로 비활성화.
- 적재: 고객 3 + 계약 4 (레거시 SampleData와 동일 데이터). 로깅은 slf4j(`@Slf4j`).

---

## 7. 검증 결과 (2026-05-29, 완료)

`docker compose up -d` + `./gradlew bootRun`(8080) 실호출:
- 기동 시 Spring `HikariPool-1` 생성 확인 (DBA 풀과 공존).
- 시더: `[seed] 완료 — 고객 3명, 계약 4건` → DB 적재 + `GET /api/contracts` 4건 서빙 확인.
- 읽기: `GET /api/contracts`, `/{no}`(D-day 포함) → 200.
- 쓰기/커밋: `POST /api/payments` → PAY00001 생성, 3개 테이블 저장 확인.
- **롤백**: `payment_records.method`를 일시 `VARCHAR(3)`로 축소 → 제출 시 2번째 INSERT "Data too long" 실패 → 먼저 들어간 payments·payment_items까지 **전부 롤백**(0건), 500. 다중 리포지토리 원자성 확인. (이후 원복)
- 에러 경로: 없는 계약 404 / 빈 items 400 / 계좌 인증 실패 400 (표준 에러 DTO).

**수정한 기존 버그 — BUG-API-01**: `old/dao/CustomerDAO.findById` SELECT에 `registered_at` 누락(매퍼는 읽음) → SQLException을 DBA가 삼켜 null → submit "고객 없음" 404. SELECT에 컬럼 추가로 수정. (다른 DAO `findBy~`에도 동일 패턴 가능 — 점검 대상)

---

## 8. 후속 전환 시 적용할 패턴

- **읽기 UC** → `GET` + 응답 DTO. 필터는 쿼리 파라미터, 상세는 path variable. `@Transactional(readOnly=true)`.
- **쓰기 UC(다단계)** → 조회 `GET` + (선택)계산 `preview POST` + 제출 `POST`. 제출 메서드에 `@Transactional`.
- **UC 간 이동**(예: 계약조회→만기관리) → Runner 직접 호출 대신 프론트 라우팅 + 독립 엔드포인트.
- **E1/검증 실패** → `ApiException` throw → `@RestControllerAdvice`가 4xx + `ErrorResponse`로 변환.
- **리포지토리** → `SqlExecutor` 주입, raw JDBC. 레거시 DAO와 SQL이 일시 중복되더라도 새 경로로 작성(스트랭글러).

---

## 9. 남은 작업 (To-Do)

우선순위 순. surrogate-PK 전환과 UC 신규 전환을 **도메인 배치 단위로 함께** 진행한다.

- [ ] **surrogate-PK 전환 (배치 진행)** — 부록 A. 공통 선행으로 `SqlExecutor.executeInsertReturningKey` 추가.
  - [ ] 배치 1: **contract + payment** (이미 신규 전환된 UC)
  - [ ] 배치 2~: claim / sales / consultation / education / inquiry — UC 신규 전환과 함께
- [ ] **나머지 UC 신규 경로 전환** — §8 패턴(Controller/Service/Repository/DTO). 배치별로 surrogate-PK와 동시 진행.
- [ ] **로깅 slf4j 확대** — 도메인/서비스 비즈니스 로그를 slf4j로. (`DataSeeder` 적용 완료. `ConsoleHelper` UI 출력 제외)
- [ ] **최종 수렴 (레거시 제거 후)** — 전 UC 전환되면 `old/`(DBA·DAO·Runner) 삭제 + 업무번호 저장 폐기 → **format-on-read**(읽을 때 `"PAY"+id`) + FK를 id로 전환 → 생성 시 UPDATE 제거.
- [ ] **그 외** — CORS/오리진, 인증·인가(현재 콘솔은 역할 메뉴로만 구분), 응답 공통 envelope 채택 여부.

---

## 부록 A. surrogate-PK 전환 (배치 진행)

> **확정 결정**: 모든 테이블에 대리키 `id BIGINT AUTO_INCREMENT PRIMARY KEY` 도입. 한 번에가 아니라 **도메인 배치로** 진행. 레거시 `old/`는 **무수정**.

### A.1 업무번호 생성 — 전환기 "저장형" (id 파생, INSERT+UPDATE)

`payment_no = "PAY"+id`처럼 **DB가 매긴 id에서 파생**하되, 전환기에는 컬럼으로 **저장**한다(레거시·FK가 문자열 키를 계속 사용하므로).

- MySQL 생성 컬럼은 AUTO_INCREMENT 참조 금지(*"An AUTO_INCREMENT column cannot be used as a base column in a generated column definition."*) → 단문 불가.
- 따라서 `save()` 절차: **① INSERT(업무번호 비움) → ② 생성 id 회수(`executeInsertReturningKey`) → ③ `id`·`paymentNo="PAY"+id` set 후 UPDATE.**
- 생성 시 +1 쿼리(UPDATE) 비용은 한시적. **최종 수렴 단계**에서 저장 폐기 → format-on-read + FK를 id로 옮기면 UPDATE가 사라진다(§9).
- 자연키 테이블(customers, 임직원, product_name 등)은 **id 파생 안 함** — 앱이 주는 업무키 그대로, surrogate `id`만 추가.

### A.2 레거시 공존 규칙 (중요)

엔터티 클래스는 신규 리포지토리와 레거시 `old/dao`·`old/runner`가 **공유**한다.
- 도메인의 `static int sequence`/기존 생성자는 **그대로 둔다**(레거시 전용). 신규 경로는 안 쓰므로 죽은 코드가 되고, 레거시 제거 시 함께 사라진다.
- 엔터티에 `Long id` + **번호 미할당 생성자/팩토리** 추가 → 신규 경로는 이걸 사용(sequence 미사용).
- **한 테이블의 쓰기 경로는 하나만**: 어떤 UC를 신규로 옮기면 그 UC의 레거시 Runner는 *은퇴*로 간주(콘솔 실행 금지). 두 경로가 같은 테이블에 INSERT하면 업무번호(sequence vs id파생) 형식이 겹쳐 UNIQUE 충돌 가능.

### A.3 변경 항목 (배치마다)

1. `schema.sql`: 대상 테이블에 `id BIGINT AUTO_INCREMENT PRIMARY KEY` 추가, 기존 업무키 `UNIQUE` 강등. → `docker compose down -v && up -d` 재생성(시더가 재적재).
2. 엔터티: `Long id` 필드 + getter/setter + 미할당 생성자/팩토리.
3. 리포지토리 `save()`: A.1 절차(INSERT→회수→UPDATE). finder는 `id` 매핑 추가.
4. `SqlExecutor.executeInsertReturningKey(sql, params)` (공통 선행, 1회).
5. FK는 업무키(UNIQUE)를 계속 참조 → 재배선 불필요.

### A.4 PK 감사 (44 테이블)

| 분류 | 테이블 | 조치 |
|---|---|---|
| A. 이미 DB 자동 (4) | customer_registrations, activity_schedule_items, education_attendances, payment_items | 불필요 |
| B. 정수, 앱 부여 (2) | insurance_applications, policy_applications | AUTO_INCREMENT화 |
| B'. 싱글톤 (1) | overdue_notice_settings | 그대로 |
| C1. 자연키 (10) | customers, *_trainers/managers/reviewers/handlers/agents, designers, agencies, insurance_products | surrogate id 추가, 업무키 UNIQUE (id 파생 안 함) |
| C2. 시퀀스형 (27+) | contracts(contract_no·policy_no), payments, payment_records, accident_reports, claim_requests, ... | surrogate id + 업무번호 id 파생(저장형) |

### A.5 배치 1 — contract + payment

대상 테이블: `contracts`, `payments`, `payment_records`, `customers`(C1, id만), `payment_items`(이미 id 보유).
- 엔터티: `Contract`(contract_no·policy_no 파생), `Payment`(payment_no), `PaymentRecord`(record_no), `Customer`(id만).
- 리포지토리: `ContractRepository`·`PaymentRepository`·`PaymentRecordRepository`의 `save()`를 A.1로, `CustomerRepository.save()`는 id 컬럼만 반영.
- 재생성 후 §7 검증(읽기·커밋·롤백) 재실행.