# API 전환 계획 / 진행 현황

> **목적**: 순수 자바 콘솔 구조(Runner 중심)를, 프론트엔드와 REST로 통신하는 Spring 구조(Controller / Service / Repository / DTO)로 전환한다.
> **방식**: 파일럿 2개로 패턴을 확립한 뒤 배치 단위로 전체 UC를 Spring REST API로 전환했고, 최종 수렴에서 레거시(`old/`)를 제거했다.
> **현재 상태 (2026-06-01)**: 전 UC 전환 완료 ✅ · `old/`/`OldMain` 제거 ✅ · format-on-read ✅ · `xxx_no` 저장 컬럼 제거 ✅ · FK `id(BIGINT)` 전환 ✅ · Docker DB 재생성 및 주요 API smoke test 완료 ✅
>
> **▶ 다음 작업 (RESUME HERE)**: 선택 작업 — slf4j 로깅 확대, CORS, 인증·인가, 응답 envelope 검토.
>
> **문서 읽기 기준**: 이 문서는 전환 이력을 함께 보존한다. 현재 구현 기준은 상단 상태와 §9, `Convergence_Progress.md`이며, 배치별 문서의 "저장형", "old/ 공존", "Runner 은퇴" 표현은 당시 진행 기록이다.

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
| **패키지 구조** | package-by-feature (도메인형) | 코드 위치만으로 도메인 식별. 현재는 `domain/`·`global/` 중심의 Spring 단일 경로. |
| **DB 접근/트랜잭션** | Spring `DataSource` + `@Transactional` (강의 V3-3 최종형) | 선언적 트랜잭션·추상화 획득. 리포지토리는 raw JDBC + `DataSourceUtils` (§4). |
| **전환 방식** | 스트랭글러 완료 | 전 UC를 Spring 경로로 옮긴 뒤 `old/`와 DBA 제거 완료. |
| **진행 단위** | 파일럿 2개 먼저 | 읽기(계약 조회)·쓰기(보험료 납입) 두 유형으로 패턴 확립 후 확장. |
| **PK/FK 방식** | surrogate-PK + format-on-read 완료 | 업무번호는 저장하지 않고 `id`에서 파생. FK는 `*_id BIGINT`로 부모 `id` 참조. |

---

## 3. 패키지 구조 (현행)

```
org.dpbe
├─ DpBeApplication
├─ domain/                          ← 신규 Spring 코드 (피처별)
│   ├─ common/{entity, enums}       BankAccount·Attachment / 열거형 33종
│   ├─ actor/                       Customer, Employee, Designer, ... (도메인 모델)
│   ├─ contract/                    controller/ service/ repository/ dto/ entity/
│   ├─ payment/                     controller/ service/ repository/ dto/ entity/
│   ├─ customer/                    repository/
│   └─ claim·consultation·sales·education·inquiry/   controller/ service/ repository/ dto/ entity/
├─ global/
│   ├─ exception/                   ApiException, ErrorResponse, ApiExceptionHandler
│   ├─ jdbc/                        SqlExecutor (DataSourceUtils 기반 공통 JDBC 헬퍼)
│   └─ seed/                        DataSeeder (기동 시 초기 데이터)
```

- DTO·도메인 모델은 `domain.<feature>` 아래. 엔터티는 raw JDBC라 JPA `@Entity`는 없고 영속 모델 역할만 한다.
- 컴포넌트 스캔: `@SpringBootApplication`(`org.dpbe`)이 `domain.*`·`global.*`를 스캔한다.

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
- `global/jdbc/SqlExecutor`: `executeUpdate`/`executeQuery`/`queryOne`/`executeInsertReturningKey`. SQLException은 런타임 예외로 변환(롤백 유도).
- 리포지토리: `ContractRepository`, `CustomerRepository`, `PaymentRepository`, `PaymentRecordRepository`, `CancellationRepository`, `RefundCalculationRepository`, `RefundPaymentRepository` (raw JDBC, SqlExecutor 경유)
- 검증·에러 → `throw ApiException` → `@RestControllerAdvice`(global.exception)에서 4xx/5xx + `ErrorResponse`

### 풀 구성
Spring `HikariPool` 단일 경로를 사용한다. 레거시 `DBA` 풀은 최종 수렴에서 제거됐다.

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

### claim 도메인 (배치 2 — 전체 11개 엔드포인트)
위 두 흐름은 파일럿 매핑이다. claim(청구·조사·산출·승인·지급·사고접수·출동기록)의 엔드포인트·설계 결정·검증 결과는 **`Batch2_Claim_Plan.md` §8(2a)·§9(2b)** 에 정리돼 있다(여기 중복 게재하지 않음).

### consultation 도메인 (배치 3 — 3a 검증 완료, 3b 검증 대기)
엔드포인트·설계 결정·검증 결과는 **`Batch3_Consultation_Plan.md`** 에 정리돼 있다.

| HTTP | 경로 | 설명 |
|---|---|---|
| POST | `/api/consultations` | 상담 신청 |
| GET | `/api/consultations` | 상담 목록 |
| GET | `/api/consultations/{consultNo}` | 상담 상세 |
| POST | `/api/consultations/{consultNo}/accept` | 상담 수락 |
| GET | `/api/interview-schedules` | 면담일정 목록 |
| POST | `/api/interview-schedules` | 면담 등록 |
| GET | `/api/interview-schedules/{scheduleNo}` | 면담 상세 |
| PUT | `/api/interview-schedules/{scheduleNo}` | 면담 수정 |
| POST | `/api/interview-schedules/{scheduleNo}/cancel` | 면담 취소 |
| GET | `/api/interview-records` | 면담기록 목록 |
| POST | `/api/interview-records` | 면담기록 등록 |
| PUT | `/api/interview-records/{recordNo}` | 면담기록 수정 |
| GET | `/api/insurance-products` | 보험상품 목록 |
| POST | `/api/proposals` | 제안서 발송 |
| GET | `/api/underwriting/pending` | 심사 대기 목록(청약+보험신청 통합) |
| POST | `/api/underwriting` | 인수심사 완료 |
| POST | `/api/insurance-applications` | 보험 가입 신청 |
| POST | `/api/policy-applications` | 청약서 제출 |
| POST | `/api/revivals` | 부활 신청 |

### 환급 도메인 (배치 6 일부 — 7개 엔드포인트, 검증 완료)

| HTTP | 경로 | 설명 |
|---|---|---|
| POST | `/api/cancellations/{cancellationNo}/refund-calculation` | 환급금 산출 |
| GET | `/api/refund-calculations` | 산출 목록 |
| GET | `/api/refund-calculations/{refundNo}` | 산출 단건 |
| POST | `/api/refund-calculations/{refundNo}/confirm` | 확정 + 지급 이관 |
| POST | `/api/refund-payments/{paymentNo}/execute` | OTP 인증 후 이체 실행 |
| GET | `/api/refund-payments` | 지급 목록 |
| GET | `/api/refund-payments/{paymentNo}` | 지급 단건 |

### education + inquiry 도메인 (배치 5 — 전체 14개 엔드포인트, 검증 완료)

| HTTP | 경로 | 설명 |
|---|---|---|
| GET | `/api/education-plans` | 교육 계획안 목록 (status 필터) |
| GET | `/api/education-plans/{planNo}` | 교육 계획안 상세 |
| POST | `/api/education-plans` | 교육 계획안 제출 (action: TEMP_SAVE \| REQUEST_APPROVAL) |
| POST | `/api/education-plans/{planNo}/approve` | 교육 계획안 승인 |
| POST | `/api/education-plans/{planNo}/reject` | 교육 계획안 반려 (reason 필수) |
| GET | `/api/education-preparations` | 교육 제반 목록 (planNo 필터) |
| GET | `/api/education-preparations/{prepNo}` | 교육 제반 상세 |
| POST | `/api/education-preparations` | 교육 제반 등록 |
| GET | `/api/education-executions` | 교육 진행 목록 (prepNo 필터) |
| GET | `/api/education-executions/{executionNo}` | 교육 진행 상세 (출석 포함) |
| POST | `/api/education-executions` | 교육 진행 완료 기록 |
| GET | `/api/inquiries` | 문의 목록 (customerName·status 필터) |
| GET | `/api/inquiries/{inquiryNo}` | 문의 상세 |
| POST | `/api/inquiries` | 문의 제출 |
| POST | `/api/inquiries/{inquiryNo}/answer` | 문의 답변 등록 |

---

## 6. 초기 데이터 시더

`global/seed/DataSeeder` (`CommandLineRunner`):
- 레거시 `SampleData`는 콘솔 `OldMain`에서만 실행 → Spring 기동 시 DB가 비는 문제를 대체.
- **신규 Spring 경로**(Repository + `@Transactional` + DataSource)로 **실제 MySQL에 적재**.
- 멱등: 고객 데이터가 있으면 건너뜀. 토글: `app.seed.enabled=false`로 비활성화.
- 적재: 고객 3 + 계약 4 (레거시 SampleData와 동일 데이터). 로깅은 slf4j(`@Slf4j`).

---

## 7. 검증 결과 (2026-05-30, 완료)

`docker compose up -d` + `./gradlew bootRun`(8080) 실호출:
- 기동 시 Spring `HikariPool-1` 생성 확인.
- 시더: `[seed] 완료 — 고객 3명, 계약 4건` → DB 적재 + `GET /api/contracts` 4건 서빙 확인.
- 읽기: `GET /api/contracts`, `/{no}`(D-day 포함) → 200.
- 쓰기/커밋: `POST /api/payments` → PAY00001(id 파생) 생성, 3개 테이블 저장 확인.
- **롤백**: `payment_records.method`를 일시 `VARCHAR(3)`로 축소 → 제출 시 2번째 INSERT "Data too long" 실패 → 먼저 들어간 payments·payment_items까지 **전부 롤백**(0건), 500. 다중 리포지토리 원자성 확인. (이후 원복)
- 에러 경로: 없는 계약 404 / 빈 items 400 / 계좌 인증 실패 400 (표준 에러 DTO).
- 배치 1 surrogate-PK: 시더가 `CON00001`(=id 1)~ 적재, 제출 → `PAY00001`·`PRC00001`(id 파생) + id PK 저장 확인.

**수정한 기존 버그 — BUG-API-01**: 과거 콘솔 DAO의 `CustomerDAO.findById` SELECT에 `registered_at` 누락(매퍼는 읽음) → 조회 실패가 null로 처리되어 submit "고객 없음" 404. SELECT에 컬럼 추가로 수정했다. 해당 DAO 계층은 최종 수렴에서 삭제됐다.

---

## 8. 후속 전환 시 적용할 패턴

- **읽기 UC** → `GET` + 응답 DTO. 필터는 쿼리 파라미터, 상세는 path variable. `@Transactional(readOnly=true)`.
- **쓰기 UC(다단계)** → 조회 `GET` + (선택)계산 `preview POST` + 제출 `POST`. 제출 메서드에 `@Transactional`.
- **UC 간 이동**(예: 계약조회→만기관리) → Runner 직접 호출 대신 프론트 라우팅 + 독립 엔드포인트.
- **E1/검증 실패** → `ApiException` throw → `@RestControllerAdvice`가 4xx + `ErrorResponse`로 변환.
- **리포지토리** → `SqlExecutor` 주입, raw JDBC. 업무번호는 DB에 저장하지 않고 `id`에서 format-on-read로 파생한다.

---

## 9. 남은 작업 (To-Do)

우선순위 순. surrogate-PK 전환과 UC 신규 전환을 **도메인 배치 단위로 함께** 진행한다. (공통 선행 `SqlExecutor.executeInsertReturningKey` ✅ 완료)

- [x] **배치 1: contract + payment** — surrogate-PK + 파일럿 UC (✅ 부록 A.5)
- [x] **배치 2: claim** — PK 파운데이션 ✅ + 2a 보상 UC(요청·조사·산출·지급) ✅ + 2b 사고·출동 UC(접수·출동기록) ✅. 세부·검증: `Batch2_Claim_Plan.md` §8·§9
- [x] **배치 3: consultation** — PK 파운데이션 ✅ · 3a ✅ · 3b ✅. 세부·검증: `Batch3_Consultation_Plan.md` §8·§9
- [x] **배치 4: sales** — PK 파운데이션 ✅ · 4a(채용·고객등록) ✅ · 4b(활동계획) ✅ · 4c(영업관리·평가·성과급) ✅. 세부·검증: `Batch4_Sales_Plan.md`
- [x] **배치 5: education+inquiry+마스터** — 스키마 컬럼명 통일(education_plans 3개·education_preparations 1개) + PK 파운데이션(13테이블: education 3+inquiry+actor 8+α) + 5a(교육계획안 CRUD+승인·반려) ✅ + 5b(교육제반 등록+교육진행 완료) ✅ + 5c(문의 제출+답변) ✅. 검증 완료 (부록 A.6)
- [x] **배치 6: 해지·환급·계약통계·만기관리** — PK 파운데이션(5테이블) ✅ · 환급 3종 ✅ · 해지 ✅ · 계약통계 ✅ · 만기관리 ✅. 세부: `Batch6_ContractClosure_Plan.md` **→ 전 UC 전환 완료**
- [ ] **로깅 slf4j 확대** — 도메인/서비스 비즈니스 로그를 slf4j로. (`DataSeeder` 적용 완료)
- [x] **최종 수렴** — `old/`(DBA·DAO·Runner) 삭제 + 업무번호 저장 폐기 → format-on-read + FK id 전환 → 생성 시 UPDATE 제거.
- [ ] **그 외** — CORS/오리진, 인증·인가(현재 콘솔은 역할 메뉴로만 구분), 응답 공통 envelope 채택 여부.

---

## 부록 A. surrogate-PK 전환 (배치 진행)

> **확정 결정**: 모든 주요 업무 테이블에 대리키 `id BIGINT AUTO_INCREMENT PRIMARY KEY` 도입. 최종 수렴 후에는 Spring 단일 경로만 남았다.

### A.1 업무번호 생성 — 최종형 "format-on-read"

`payment_no = "PAY"+id`처럼 **DB가 매긴 id에서 파생**한다. 최종 수렴 후 업무번호 컬럼은 저장하지 않는다.

- MySQL 생성 컬럼은 AUTO_INCREMENT 참조 금지(*"An AUTO_INCREMENT column cannot be used as a base column in a generated column definition."*) → 단문 불가.
- 따라서 `save()` 절차: **① INSERT → ② 생성 id 회수(`executeInsertReturningKey`) → ③ 엔터티 업무번호 필드에 `PREFIX + id` 주입**. DB UPDATE는 없다.
- FK는 업무번호 문자열이 아니라 부모 `id(BIGINT)`를 저장한다.
- 자연키 테이블(customers, 임직원, product_name 등)은 **id 파생 안 함** — 앱이 주는 업무키 그대로, surrogate `id`만 추가.

### A.2 최종 수렴 규칙

- `old/dao`·`old/runner`·`DBA`·`OldMain`은 삭제됐다.
- 엔터티에는 DB PK인 `Long id`와 응답용 업무번호 필드를 둔다.
- 리포지토리 `save()`는 INSERT 후 생성 id를 회수하고, 엔터티 필드에만 업무번호를 파생 주입한다.
- FK는 부모 업무번호 문자열이 아니라 부모 `id(BIGINT)`를 저장한다.

### A.3 변경 항목 (배치마다)

1. `schema.sql`: 대상 테이블에 `id BIGINT AUTO_INCREMENT PRIMARY KEY` 추가, 기존 업무키 `UNIQUE` 강등. → `docker compose down -v && up -d` 재생성(시더가 재적재).
2. 엔터티: `Long id` 필드 + getId/setId (+ 업무번호 세터 없으면 추가).
3. 리포지토리 `save()`: A.1 절차(INSERT→회수→엔터티 업무번호 주입). finder는 `id` 매핑 추가.
4. `SqlExecutor.executeInsertReturningKey(sql, params)` (공통 선행, ✅ 완료).
5. FK는 부모 `id(BIGINT)`를 참조한다.

### A.4 PK 감사 (44 테이블)

| 분류 | 테이블 | 조치 |
|---|---|---|
| A. 이미 DB 자동 (4) | customer_registrations, activity_schedule_items, education_attendances, payment_items | 불필요 |
| B. 정수, 앱 부여 (2) | insurance_applications, policy_applications | AUTO_INCREMENT화 |
| B'. 싱글톤 (1) | overdue_notice_settings | 그대로 |
| C1. 자연키 (10) | customers, *_trainers/managers/reviewers/handlers/agents, designers, agencies, insurance_products | surrogate id 추가, 업무키 UNIQUE (id 파생 안 함) |
| C2. 업무번호 파생형 (27+) | contracts, payments, payment_records, accident_reports, claim_requests, ... | surrogate id + 업무번호 format-on-read |

### A.5 배치 1 — contract + payment ✅ (2026-05-30 완료)

대상 테이블: `contracts`, `payments`, `payment_records`, `customers`(C1, id만), `payment_items`(이미 id 보유).
- 엔터티: `Contract`(contract_no·policy_no 파생), `Payment`(payment_no), `PaymentRecord`(record_no), `Customer`(id만) — 모두 `Long id` + getId/setId. (`Contract.setPolicyNo`, `PaymentRecord.setRecordNo` 세터 추가)
- 공통: `SqlExecutor.executeInsertReturningKey` 추가.
- 리포지토리: 최종 수렴 후 `ContractRepository`·`PaymentRepository`·`PaymentRecordRepository.save()`는 INSERT→id 회수→엔터티 업무번호 주입으로 동작한다. `CustomerRepository`는 자연키 upsert 유지 + 조회 id 매핑.
- 검증: `down -v && up -d` 재생성 → 시더가 `CON00001`(=id 1)~ 적재, `POST /api/payments` → `PAY00001`·`PRC00001`(id 파생) 저장 확인.

### A.6 배치 분할 계획

도메인 단위로, 각 배치에서 **그 도메인 UC의 신규 전환(Controller/Service/Repository)** 과 **그 테이블의 surrogate-PK 적용**을 함께 한다(새 리포지토리가 준비될 때 그 테이블 스키마를 바꿔 이중 writer/어중간 상태 방지).

| 배치 | 도메인 | 대상 테이블 |
|---|---|---|
| 1 ✅ | contract + payment | contracts, payments, payment_records, customers(id만) (payment_items=이미 id) |
| 2 ✅ | claim (PK + UC 전환 완료) | accident_reports, dispatches, dispatch_records, claim_requests, damage_investigations, claim_calculations, claim_payments (+ dispatch_photos 신규) |
| 3 ✅ | consultation | consultation_requests, interview_schedules, interview_records, proposals, underwritings, insurance_applications·policy_applications(INT→AI), revivals, insurance_products(id만) |
| 4 ✅ | sales | channel_recruitments, channel_screenings, activity_plans, sales_activity_managements, sales_org_evaluations, bonus_requests (activity_schedule_items·customer_registrations=이미 id) |
| 5 ✅ | education + inquiry + 마스터 | education_plans/preparations/executions(attendances=이미 id), inquiries, actor 마스터(designers·agencies·임직원 8종, id만). 스키마 컬럼명 통일(education_plans 3개·education_preparations 1개) + additional_notice·total_count 신규 컬럼 추가 포함. |

배치마다: 스키마 → 엔터티 `Long id` → 신규 리포지토리 save() id-파생 → 컴파일 → `down -v && up -d` → 검증. 최종 수렴에서 업무번호 저장 컬럼과 문자열 FK는 제거됐다.
