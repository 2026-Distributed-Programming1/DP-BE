# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

> **현재 상태 (2026-05-31)**: 콘솔(Runner) 구조를 Spring REST API 구조로 전환 완료. 신규 웹 경로와 레거시 콘솔(`old/`)이 **공존**한다.
> - 완료: 패키지 개편 · 파일럿 UC 2개(계약 조회·보험료 납입) · 웹 시더 · surrogate-PK 배치 1(contract·payment) · **배치 2(claim) ✅** · **배치 3(consultation) ✅** · **배치 4(sales) ✅** · **배치 5(education+inquiry+마스터) ✅** — 스키마 컬럼명 통일(education 4개) + PK 파운데이션(13테이블) + 5a(교육계획안 CRUD·승인·반려) + 5b(교육제반 등록·교육진행 완료) + 5c(문의 제출·답변). Controller/Service/Repository/DTO, 흐름·예외 API 검증 완료.
> - **다음 작업**: 최종 수렴 — `old/`(DBA·DAO·Runner) 삭제 + format-on-read 전환, 또는 CORS/인증·인가 추가.
> - **작업 전 반드시 참고**: 전환 계획·현황·결정은 **`src/main/resources/design/ApiMigrationPlan.md`**.

## 빌드 및 실행

```bash
./gradlew compileJava        # 컴파일만 (버그 수정/변경 후 확인 시)
./gradlew bootRun            # Spring Boot 웹 서버 기동 (포트 8080) — 진입점 DpBeApplication
./gradlew build              # 전체 빌드 (test 포함 — 아래 '알려진 한계'의 테스트 에러 주의)
```
- **웹 진입점**: `org.dpbe.DpBeApplication`(@SpringBootApplication). 기동 시 `DataSeeder`가 초기 데이터를 적재한다.
- **콘솔 진입점(레거시)**: `org.dpbe.OldMain.main()` — IDE/`java`로 직접 실행하는 역할 메뉴 시뮬레이터.

**DB 초기화 (Docker)**
```bash
docker compose up -d         # 최초 기동 (schema.sql 자동 실행)
docker compose down -v && docker compose up -d  # 스키마 변경 후 재생성
```
- 컨테이너: `insurance_db` / 접속 정보: `admin:1234@localhost:3306/insurance_db`
- `schema.sql`은 볼륨 첫 초기화 시에만 실행됨 — 컬럼 추가 후엔 반드시 재생성

## 아키텍처

전환 중이라 **두 경로가 공존**한다.

```
신규(웹)   HTTP → Controller → Service(@Transactional) → Repository
                  → SqlExecutor(DataSourceUtils) → Spring DataSource → MySQL
레거시(콘솔) OldMain → Runner(UC 진행자) → Domain → DAO → DBA(자체 풀) → MySQL
```

신규 경로와 레거시 경로는 같은 MySQL을 **각자의 커넥션 풀**로 바라본다. UC를 하나씩 신규 경로로 옮기며, 모두 옮기면 `old/`(DBA·DAO·Runner)를 제거한다(스트랭글러).

### 패키지 구조 (`org.dpbe`)

```
org.dpbe
├─ DpBeApplication(웹 진입점), OldMain(콘솔 진입점)
├─ domain/                          ← 신규 Spring 코드 (package-by-feature)
│   ├─ common/{entity, enums}       BankAccount·Attachment / 열거형 33종
│   ├─ actor/                       Customer, Employee, Designer, Agency, ... (도메인 모델)
│   ├─ contract/                    controller/ service/ repository/ dto/ entity/
│   ├─ payment/                     controller/ service/ repository/ dto/ entity/
│   ├─ customer/                    repository/
│   └─ claim·consultation·sales·education·inquiry/   entity/ (모델만, 미전환)
├─ global/
│   ├─ exception/                   ApiException, ErrorResponse, ApiExceptionHandler(@RestControllerAdvice)
│   ├─ jdbc/                        SqlExecutor (DataSourceUtils 기반 공통 JDBC 헬퍼)
│   └─ seed/                        DataSeeder (웹 기동 시 초기 데이터)
└─ old/                             ← 레거시 (무수정 공존)
    ├─ dao/ (45)   db/ (DBA, SequenceSync)   runner/ (usecase 36 + ConsoleHelper, SampleData)
```
- 컴포넌트 스캔: `@SpringBootApplication`(`org.dpbe`)이 `domain.*`·`global.*`를 스캔. `old.*`는 Spring 애너테이션이 없어 무영향.
- 도메인 모델(엔터티)은 raw JDBC라 JPA `@Entity`가 없으며, 신규/레거시 양쪽이 공유한다.

## 신규 웹 계층 패턴

- **Controller** (`domain.<feature>.controller`): `@RestController`. DTO 입출력만, 로직 없음.
- **Service** (`domain.<feature>.service`): `@Service` + `@Transactional`(조회는 `readOnly=true`). UC 절차 조정·검증. `DataSource`/`Connection`을 만지지 않는다(프록시가 경계 관리).
- **Repository** (`domain.<feature>.repository`): `@Repository`. `SqlExecutor` 주입, raw JDBC. 커넥션은 `DataSourceUtils` 경유라 `@Transactional`에 자동 참여.
- **DTO** (`domain.<feature>.dto`): record 기반 요청/응답.
- **예외**: 검증/조회 실패 시 `throw ApiException`(notFound/badRequest) → `ApiExceptionHandler`가 4xx/5xx + `ErrorResponse`로 변환.
- **SqlExecutor** (`global.jdbc`): `executeUpdate`/`executeQuery`/`queryOne`/`executeInsertReturningKey`. SQLException은 런타임 예외로 변환(롤백 유도). 파라미터로 `String/Integer/Long/Double/Boolean/LocalDate/LocalDateTime` 직접 전달.
- **다단계 입력 흐름**은 클라이언트 주도: 조회 `GET` + (선택)`preview POST` + 제출 `POST`(완성 DTO). 서버 무상태.
- **시더** `DataSeeder`(`CommandLineRunner`): 멱등(데이터 있으면 skip), `app.seed.enabled=false`로 비활성화, slf4j 로깅.
- **surrogate-PK(전환된 도메인)**: PK는 DB `id`(AUTO_INCREMENT), 업무번호(contract_no 등)는 `id`에서 파생해 저장(`save()`: INSERT→`executeInsertReturningKey`→파생 UPDATE). 엔터티에 `Long id` 보유.

### 전환(마이그레이션) 작업 원칙 — 반드시 준수

DAO/Runner를 API로 옮기는 작업은 **기계적 이식이 아니다.** 아래를 지킨다.

1. **DAO를 통째로 옮기지 않는다.** 레거시 DAO·Runner의 로직을 한 뭉탱이로 복붙해 한 메서드/엔드포인트에 몰아넣지 말 것. 책임을 **적절히 나눈다** — 조회는 조회대로, 검증은 검증대로, 단계는 단계대로 Service/Repository/DTO로 분해한다. (DAO의 `mapRow`·SQL은 *컬럼·매핑의 정답지*로 참고하되, 구조까지 베끼는 게 아니다.)
2. **실제 운용에서의 API 사용을 먼저 따진다.** "콘솔이 이렇게 했으니"가 아니라 "프론트/클라이언트가 이 자원을 실제로 어떻게 호출하나"를 기준으로 엔드포인트를 가른다. 무상태·클라이언트 주도 설계(§위 다단계 흐름), 자원의 실제 성격(예: OTP·예약은 생성/실행 분리)을 우선한다. 콘솔의 단계 순서를 그대로 옮기지 않는다.
3. **예상치 못한 변수가 생기면 임의로 단순화하지 말고 사용자와 대화한다.** 엔터티/스키마/DAO가 가정과 다르거나, 상태 전이가 막히거나, 저장 위치·계좌 출처 같은 설계 선택지가 생기면 — 혼자 "되는 쪽으로" 우회·축소하지 말고 **멈추고 선택지를 제시해 합의**한 뒤 진행한다. (과거 1차 실패는 추측으로 밀어붙여 발생했다.)

### 전환 구현 체크리스트 (모든 UC 공통)

> 배치 2(claim)에서 확립한 절차를 일반화한 것. 어떤 도메인을 전환하든 그대로 적용한다.

**A. 추측 금지 — 코드 전 실물 확인 (가장 자주 깨진 규칙).** 작성 전 반드시 ① 대상 엔터티(`domain/<feature>/entity/*`) ② `schema.sql`의 실제 컬럼명 ③ 대응 `old/dao/*DAO`의 `save()`/`findAll()` 을 열어 **타입·메서드명·컬럼명을 눈으로 확인**한다. 메서드/필드/컬럼이 있을 거라 가정하고 쓰지 않는다. 자주 어긋나는 지점: 게터명(`getAccountNo` vs `getAccountNumber`), 업무번호 게터(`getReportNo`인데 컬럼은 `accident_no`), 필드 존재 여부(스키마에 없는 컬럼 가정), 컬렉션 타입(`List<String>` vs enum), 생성자 시그니처.

**B. 엔터티는 rich 도메인 모델 — 행위 메서드를 쓴다.** 엔터티는 세터 더미가 아니라 검증·상태전이 메서드를 가진다(`submit()`, `validateXxx()`, `approve()` 등). Service에서 이 메서드로 규칙을 태우고, **업무번호 세터(`setXxxNo`)·DB복원용 세터·셸 주입 세터가 없으면 추가**한다(다른 전환 도메인의 관습과 일치시킬 것). 같은 행 컬럼은 셸 주입으로 나르고(조인 아님), 다른 테이블 값만 조인한다.

**C. Repository = surrogate-PK 패턴(배치 1·2 복제).** `domain/contract/repository/ContractRepository`가 템플릿. `@Repository` + 생성자 주입 `SqlExecutor`. `save()`는 업무번호 컬럼 제외 INSERT → `executeInsertReturningKey`로 id 회수 → `setId` + 업무번호 파생(`접두+String.format("%05d", id)`) → `UPDATE ... SET xxx_no=? WHERE id=?`. `mapRow`는 `RowMapper<T>`(행 1개→객체) 시그니처 — **`executeQuery` 안에서 `while(rs.next())`로 List를 만들지 말 것**(executeQuery가 행마다 mapRow를 호출). finder는 `SELECT id, ...`로 id 포함해 `setId` 매핑. 매핑·셸 복원 로직은 대응 DAO를 그대로 옮기되 `DBA`→`SqlExecutor` 치환.

**D. Service/DTO/Controller.** Service `@Transactional`(조회 `readOnly=true`, 쓰기 메서드만 재선언), 검증 실패→`ApiException`(notFound/badRequest), 콘솔 절차가 아닌 **규칙만** 이관. DTO는 record, *정의 없는 타입을 참조하지 말 것*(필요하면 새로 정의하거나 요청 record에 평면 필드로). Controller `@RestController`, DTO 입출력만.

**E. 완료 기준(매 UC/배치).** `./gradlew compileJava` 그린 → 스키마 변경 시 `docker compose down -v && up -d` → `bootRun` 기동 → 정상 흐름 + 예외 분기(E1 등)를 실제 API 호출로 검증 → 설계 문서·이 파일에 ✅ 기록. 검증 없이 "완료"로 보고하지 않는다.

## 레거시 콘솔 계층 (`old/`)

**Runner 패턴** (`old/runner/usecase/`)
- `static void run()` 하나로 구성. 코드 주석이 UC 단계 번호(1,2,… / A1, E1)와 1:1 대응.
- `ConsoleHelper`로 입력 → Domain으로 로직 처리 → DAO로 저장.

**DAO 패턴** (`old/dao/`)
- `save()` — `INSERT ... ON DUPLICATE KEY UPDATE` upsert.
- `findAll()`/`findBy~()` — `DBA.executeQuery(sql, rs -> {...})` 람다 매핑.
- PK는 도메인 클래스의 `static int sequence`로 생성(미전환 도메인 한정).

**DBA** (`old/db/DBA.java`) — 자체 HikariCP 풀 + `ThreadLocal<Connection>` 트랜잭션. **레거시 전용**. 신규 코드는 DBA 대신 `SqlExecutor`+`@Transactional`을 쓴다.
```java
DBA.executeUpdate("INSERT INTO ...", p1, p2);
DBA.beginTransaction(); try { ...; DBA.commit(); } catch(Exception e){ DBA.rollback(); }
```
DBA 트랜잭션이 적용된 Runner: `EducationExecutionRunner`, `PaymentRunner`, `InsuranceCancellationRunner`, `ExpiringContractManagementRunner`, `RefundCalculationRunner`.

**ConsoleHelper** (`old/runner/`)
- 입력: `readLine/readNonEmpty/readInt/readLong/readPositiveInt/readYesNo/readDate/readDateTime/readMenuChoice/readMultiChoice`
- 출력: `printStage/printInfo/printSuccess/printError/printWarning/printDivider/waitEnter`

## 전체 파일 구성

### 도메인 모델 ↔ DAO ↔ 테이블 매핑

> 모델(엔터티)은 `org.dpbe.domain.<feature>.entity`(actor 계열은 `domain.actor`), DAO는 `org.dpbe.old.dao`, Runner는 `org.dpbe.old.runner.usecase`. 아래 표의 도메인 열은 feature 이름이다.

| 도메인(feature) | 주요 도메인 클래스 | DAO | DB 테이블 |
|---|---|---|---|
| education | EducationPlan | EducationPlanDAO | education_plans |
| | EducationPreparation | EducationPreparationDAO | education_preparations |
| | EducationExecution | EducationExecutionDAO | education_executions, education_attendances |
| sales | ChannelRecruitment | ChannelRecruitmentDAO | channel_recruitments |
| | ChannelScreening | ChannelScreeningDAO | channel_screenings |
| | ActivityPlan, ScheduleItem | ActivityPlanDAO | activity_plans, activity_schedule_items |
| | SalesActivityManagement | SalesActivityManagementDAO | sales_activity_managements |
| | SalesOrgEvaluation | SalesOrgEvaluationDAO | sales_org_evaluations |
| | BonusRequest | BonusRequestDAO | bonus_requests |
| | CustomerRegistration | CustomerRegistrationDAO | customer_registrations |
| consultation | ConsultationRequest | ConsultationRequestDAO | consultation_requests |
| | InterviewSchedule | InterviewScheduleDAO | interview_schedules |
| | InterviewRecord | InterviewRecordDAO | interview_records |
| | Proposal | ProposalDAO | proposals |
| | Underwriting | UnderwritingDAO | underwritings |
| | InsuranceApplication | InsuranceApplicationDAO | insurance_applications |
| | PolicyApplication | PolicyApplicationDAO | policy_applications |
| | Revival | RevivalDAO | revivals |
| contract | Contract | ContractDAO | contracts |
| | Cancellation | CancellationDAO | cancellations |
| | ContractStatistics | ContractStatisticsDAO | contract_statistics |
| | ExpiringContractManagement | ExpiringContractManagementDAO | expiring_contract_notices |
| claim | AccidentReport | AccidentReportDAO | accident_reports |
| | Dispatch | DispatchDAO | dispatches |
| | DispatchRecord | DispatchRecordDAO | dispatch_records |
| | ClaimRequest | ClaimRequestDAO | claim_requests |
| | DamageInvestigation | DamageInvestigationDAO | damage_investigations |
| | ClaimCalculation | ClaimCalculationDAO | claim_calculations |
| | ClaimPayment | ClaimPaymentDAO | claim_payments |
| payment | Payment, PaymentItem | PaymentDAO | payments, payment_items |
| | PaymentRecord | PaymentRecordDAO | payment_records |
| | RefundCalculation | RefundCalculationDAO | refund_calculations |
| | RefundPayment | RefundPaymentDAO | refund_payments |
| | OverdueNoticeSetting | OverdueNoticeSettingDAO | overdue_notice_settings |
| inquiry | Inquiry | InquiryDAO | inquiries |
| actor | Customer | CustomerDAO | customers |
| | Designer | DesignerDAO | designers |
| | Agency | AgencyDAO | agencies |
| | EducationTrainer | EducationTrainerDAO | education_trainers |
| | SalesManager | SalesManagerDAO | sales_managers |
| | InsuranceReviewer | InsuranceReviewerDAO | insurance_reviewers |
| | ClaimsHandler | ClaimsHandlerDAO | claims_handlers |
| | DispatchAgent | DispatchAgentDAO | dispatch_agents |
| | FinanceManager | FinanceManagerDAO | finance_managers |

### Runner 목록 (36개, `old/runner/usecase/`)

| Runner | 유스케이스 | 호출 역할 |
|---|---|---|
| EducationPlanRunner | 교육 계획안 작성 | 영업교육담당자 |
| EducationPreparationRunner | 교육 제반 등록 | 영업교육담당자 |
| EducationExecutionRunner | 교육 진행 | 영업교육담당자 |
| ChannelRecruitmentRunner | 판매채널 모집 | 영업관리자 |
| ChannelScreeningRunner | 판매채널 채용 심사 | 영업관리자 |
| ActivityPlanRunner | 활동 계획 작성 | 판매채널 |
| SalesActivityRunner | 영업 활동 관리 | 영업관리자 |
| SalesOrgEvaluationRunner | 영업조직 평가 | 영업관리자 |
| BonusRequestRunner | 성과급 지급 요청 | 영업관리자 |
| CustomerRegistrationRunner | 고객 정보 등록 | 판매채널 |
| ConsultationRequestRunner | 상담 요청 | 고객 |
| InterviewScheduleRunner | 면담일정 관리 | 설계사 |
| InterviewRecordRunner | 면담기록 관리 | 설계사 |
| ProposalRunner | 보험상품 제안 | 설계사 |
| UnderwritingRunner | 인수 심사 | 보험심사자 |
| InsuranceApplicationRunner | 보험 가입 신청 | 고객 |
| PolicyApplicationRunner | 청약서 작성 | 설계사 |
| RevivalRunner | 보험 부활 | 설계사 |
| ContractInfoRunner | 계약 정보 조회 | 계약관리자 |
| ContractStatisticsRunner | 계약 통계 관리 | 계약관리자 |
| ExpiringContractManagementRunner | 만기 계약 관리 | 계약관리자 |
| InsuranceCancellationRunner | 보험 해지 | 계약관리자 |
| AccidentReportRunner | 사고 접수 | 고객 |
| DispatchRecordRunner | 현장 출동 기록 | 출동요원 |
| ClaimRequestRunner | 보험금 요청 | 고객 |
| DamageInvestigationRunner | 손해 조사 | 보상담당자 |
| ClaimCalculationRunner | 보험금 산출 | 보상담당자 |
| ClaimPaymentRunner | 보험금 지급 | 보상담당자 |
| PaymentRunner | 보험료 납입 | 고객 |
| PaymentRecordRunner | 납부 내역 관리 | 재무회계담당자 |
| RefundCalculationRunner | 해약 환급금 산출 | 재무회계담당자 |
| RefundListRunner | 해약 환급 내역 조회 | 재무회계담당자 |
| RefundPaymentRunner | 해약 환급금 지급 | 재무회계담당자 |
| InquiryRunner | 문의 | 고객 |
| InsuranceProductInquiryRunner | 보험상품 조회 | 고객 |
| MyInsuranceViewRunner | 내 보험 조회 | 고객 |

> 신규 웹 전환 완료: 계약 정보 조회(`GET /api/contracts`, `/{contractNo}`), 보험료 납입(`GET /api/customers/{id}/contracts`, `POST /api/payments/preview`, `POST /api/payments`). 엔드포인트 상세는 ApiMigrationPlan.md §5.

### SequenceSync 메커니즘 (레거시)

JVM 재시작 시 도메인 클래스의 `static int sequence`가 0으로 리셋되어 기존 DB PK와 충돌하는 문제를 방지한다. `SequenceSync.sync()`는 콘솔 `SampleData.initialize()`에서 호출되며, 리플렉션으로 각 도메인의 `sequence`에 `SELECT MAX(pk)`를 주입한다.
- Employee 서브클래스는 `Employee.sequence`를 공유 → 여러 테이블 MAX를 UNION ALL로 조회.
- 신규 웹 경로의 시더(`DataSeeder`)는 SequenceSync를 호출하지 않는다(멱등 가드로 중복만 방지).

### 설계 제약 및 알려진 한계

| 항목 | 현황 | 비고 |
|---|---|---|
| **신규/레거시 공존** | 웹(Service+@Transactional)과 콘솔(Runner+DBA)이 같은 MySQL을 각자 풀로 사용 | 파일럿 외 UC는 아직 레거시 경로. 전체 전환 시 DBA 제거 |
| **Service 레이어** | 파일럿(contract·payment)에 도입 완료. 나머지는 Runner가 UC 조정 겸함 | §ApiMigrationPlan §8 패턴으로 확장 |
| **트랜잭션** | 신규: Spring `@Transactional`. 레거시: DBA `ThreadLocal` TX(5개 Runner) | |
| **PK 방식 (전환 중)** | 신규 전환 도메인(contract·payment)은 DB `id`(AUTO_INCREMENT) PK + 업무키 UNIQUE, 업무번호는 id 파생(저장형). claim은 PK만 완료. 미전환 도메인은 `static sequence` 잔존 | 배치 단위 진행 (ApiMigrationPlan 부록 A) |
| **FK 제약** | schema.sql에 NULLABLE FK 23개 (`customer_registrations.customer_id` 제외) | 비-NULL 값만 무결성 검사 |
| **테스트 컴파일 에러** | `ActivityPlanTest.java` — ScheduleItem 생성자 시그니처 불일치 | `compileJava`(main)는 정상, `build`(test 포함)는 실패 |

---

## 버그 수정 규칙

1. **수정 전 BUG ID를 먼저 언급한다** — Edit/Write 직전에 `BUG-XXX-NN: 무엇을 왜 고치는지` 한 줄 이상 명시.
2. **하나씩 수정하고 컴파일 확인** — 버그 하나 수정 → `./gradlew compileJava` 통과 → 다음.
3. **레포트 범위만 수정한다** — 보고에 없는 리팩토링/기능은 넣지 않는다.
4. **트랜잭션 블록 안에서 만든 변수를 밖에서 참조하지 않는다** — 블록 밖에 컬렉션을 선언해 채운 뒤 커밋 후 사용.

## 설계/이력 문서

- **`src/main/resources/design/ApiMigrationPlan.md`** — API 전환 계획·현황·결정 근거 (주력 문서, 상단에 RESUME 포인터)
- `design/Batch2_Claim_Plan.md` — 배치 2(claim) 세부 계획(서브배치 2a/2b·엔드포인트 초안·진행 현황)
- `design/Batch5_Education_Plan.md` — 배치 5(education+inquiry+마스터) 세부 계획·스키마 컬럼명 통일·검증 결과
- `design/Usecase_scenario.md` — UC 시나리오 (Basic/Alternative/Exception). 웹·신규 흐름의 기준
- `design/Class_Diagram_Domain.md`, `design/Class_Diagram_Mermaid.md`, `design/DAOStructure.md` — 클래스/DAO 구조
- 버그 이력 (`src/main/resources/bugreport/`): `BugReport.md`, `AdditionalBugReport.md`, `CodeReviewReport.md`, `FinalBugReport.md` — 발견·수정 버그 전체 내역(모두 ✅)

## 스키마 변경 이력

- **2026-05-28**: `expiring_contract_notices` 신규, `interview_records.interviewed_at` 추가
- **2026-05-29**: 버그 수정으로 컬럼 6개 추가, 전체 23개 테이블 NULLABLE FK 추가
- **2026-05-30**: surrogate-PK 배치 1·2 — customers·contracts·payments·payment_records + claim 7테이블에 `id BIGINT AUTO_INCREMENT PK` 추가, 기존 업무키 `UNIQUE` 강등
- **2026-05-31**: 배치 5 — education 컬럼명 통일(4개 변경) + education_plans·preparations·executions·inquiries + actor 마스터 8테이블에 `id AUTO_INCREMENT` 추가 + `additional_notice`, `total_count` 신규 컬럼 추가

> **주의**: 스키마 변경 후엔 반드시 `docker compose down -v && docker compose up -d` 실행