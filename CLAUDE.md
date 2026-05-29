# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 빌드 및 실행

```bash
./gradlew compileJava        # 컴파일만 (버그 수정 후 확인 시)
./gradlew build              # 전체 빌드
./gradlew run                # 애플리케이션 실행
```

**DB 초기화 (Docker)**
```bash
docker compose up -d         # 최초 기동 (schema.sql 자동 실행)
docker compose down -v && docker compose up -d  # 스키마 변경 후 재생성
```
- 컨테이너: `insurance_db` / 접속 정보: `admin:1234@localhost:3306/insurance_db`
- `schema.sql`은 볼륨 첫 초기화 시에만 실행됨 — 컬럼 추가 후엔 반드시 재생성

## 아키텍처

**데이터 흐름**
```
Main → Role 메뉴 → Runner (UC 진행자) → Domain (비즈니스 로직) → DAO → DBA → MySQL
```

**패키지 구조**
- `dp/actor/` — 시스템 사용자 역할 클래스 (EducationTrainer, SalesManager 등)
- `dp/runner/usecase/` — 유스케이스별 Runner (38개). 각 Runner는 UC 시나리오 절차(Basic Path / Alternative / Exception)를 그대로 따른다
- `dp/{domain}/` — 도메인 클래스 (claim, consultation, contract, education, sales 등)
- `dp/dao/` — DAO (44개). 테이블당 1개가 원칙이나 보조 데이터는 부모 DAO에서 처리
- `dp/db/DBA.java` — HikariCP 커넥션 풀 + SQL 실행 헬퍼
- `dp/enums/` — 상태/유형 열거형 33종
- `dp/runner/ConsoleHelper.java` — 콘솔 입출력 전담

**Runner 패턴**
- `static void run()` 메서드 하나로 구성
- 코드 내 주석이 UC 단계 번호(1, 2, … / A1, E1 …)와 1:1 대응
- `ConsoleHelper`로 입력을 받고, Domain 메서드로 비즈니스 로직을 처리한 뒤, DAO로 저장

**DAO 패턴**
- `save()` — `INSERT ... ON DUPLICATE KEY UPDATE` upsert
- `findAll()` / `findBy~()` — `DBA.executeQuery(sql, rs -> { ... })` 람다 매핑
- PK는 도메인 클래스의 `static int sequence`로 생성
- **DAO 생성 기준**: 어떤 유스케이스가 저장한 상태를 다른 유스케이스가 나중에 읽어야 할 때만 독립 DAO를 만든다

**DBA 사용법**
```java
DBA.executeUpdate("INSERT INTO ...", param1, param2, ...);
DBA.executeQuery("SELECT ...", rs -> new MyObject(rs.getString("col")));
DBA.queryOne("SELECT ...", rs -> ...);  // 단건 조회
```
파라미터 타입으로 `String`, `Integer`, `Long`, `LocalDate`, `LocalDateTime`, `Boolean` 등을 직접 전달할 수 있다.

**DBA 트랜잭션 API** (2026-05-29 추가)

`ThreadLocal<Connection>` 기반. 트랜잭션 중에는 모든 executeUpdate/executeQuery가 동일 커넥션을 공유한다.

```java
DBA.beginTransaction();
try {
    DAO.save(A);
    DAO.save(B);
    DBA.commit();
} catch (Exception e) {
    DBA.rollback();
}
```

현재 트랜잭션이 적용된 Runner (2개 이상 테이블을 원자적으로 저장):
- `EducationExecutionRunner` — education_executions + education_attendances
- `PaymentRunner` — payments + payment_items
- `InsuranceCancellationRunner` — cancellations + contracts
- `ExpiringContractManagementRunner` — expiring_contract_notices + contracts (A1 갱신 경로)
- `RefundCalculationRunner` — refund_calculations + refund_payments (confirmAndProceed)

**ConsoleHelper 주요 메서드**
- 입력: `readLine`, `readNonEmpty`, `readInt`, `readLong`, `readPositiveInt`, `readYesNo`, `readDate`, `readDateTime`, `readMenuChoice(title, ...options)`, `readMultiChoice`
- 출력: `printStage(actor, msg)`, `printInfo`, `printSuccess`, `printError`, `printWarning`, `printDivider`, `waitEnter`

## 전체 파일 구성

### 도메인별 클래스 ↔ DAO ↔ 테이블 매핑

| 도메인 패키지 | 주요 도메인 클래스 | DAO | DB 테이블 |
|---|---|---|---|
| `dp/education` | EducationPlan | EducationPlanDAO | education_plans |
| | EducationPreparation | EducationPreparationDAO | education_preparations |
| | EducationExecution | EducationExecutionDAO | education_executions, education_attendances |
| `dp/sales` | ChannelRecruitment | ChannelRecruitmentDAO | channel_recruitments |
| | ChannelScreening | ChannelScreeningDAO | channel_screenings |
| | ActivityPlan, ScheduleItem | ActivityPlanDAO | activity_plans, activity_schedule_items |
| | SalesActivityManagement | SalesActivityManagementDAO | sales_activity_managements |
| | SalesOrgEvaluation | SalesOrgEvaluationDAO | sales_org_evaluations |
| | BonusRequest | BonusRequestDAO | bonus_requests |
| | CustomerRegistration | CustomerRegistrationDAO | customer_registrations |
| `dp/consultation` | ConsultationRequest | ConsultationRequestDAO | consultation_requests |
| | InterviewSchedule | InterviewScheduleDAO | interview_schedules |
| | InterviewRecord | InterviewRecordDAO | interview_records |
| | Proposal | ProposalDAO | proposals |
| | Underwriting | UnderwritingDAO | underwritings |
| | InsuranceApplication | InsuranceApplicationDAO | insurance_applications |
| | PolicyApplication | PolicyApplicationDAO | policy_applications |
| | Revival | RevivalDAO | revivals |
| `dp/contract` | Contract | ContractDAO | contracts |
| | Cancellation | CancellationDAO | cancellations |
| | ContractStatistics | ContractStatisticsDAO | contract_statistics |
| | ExpiringContractManagement | ExpiringContractManagementDAO | expiring_contract_notices |
| `dp/claim` | AccidentReport | AccidentReportDAO | accident_reports |
| | Dispatch | DispatchDAO | dispatches |
| | DispatchRecord | DispatchRecordDAO | dispatch_records |
| | ClaimRequest | ClaimRequestDAO | claim_requests |
| | DamageInvestigation | DamageInvestigationDAO | damage_investigations |
| | ClaimCalculation | ClaimCalculationDAO | claim_calculations |
| | ClaimPayment | ClaimPaymentDAO | claim_payments |
| `dp/payment` | Payment, PaymentItem | PaymentDAO | payments, payment_items |
| | PaymentRecord | PaymentRecordDAO | payment_records |
| | RefundCalculation | RefundCalculationDAO | refund_calculations |
| | RefundPayment | RefundPaymentDAO | refund_payments |
| | OverdueNoticeSetting | OverdueNoticeSettingDAO | overdue_notice_settings |
| `dp/inquiry` | Inquiry | InquiryDAO | inquiries |
| `dp/actor` | Customer | CustomerDAO | customers |
| | Designer | DesignerDAO | designers |
| | Agency | AgencyDAO | agencies |
| | EducationTrainer | EducationTrainerDAO | education_trainers |
| | SalesManager | SalesManagerDAO | sales_managers |
| | InsuranceReviewer | InsuranceReviewerDAO | insurance_reviewers |
| | ClaimsHandler | ClaimsHandlerDAO | claims_handlers |
| | DispatchAgent | DispatchAgentDAO | dispatch_agents |
| | FinanceManager | FinanceManagerDAO | finance_managers |

### Runner 목록 (36개)

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

### SequenceSync 메커니즘

JVM 재시작 시 도메인 클래스의 `private static int sequence`가 0으로 리셋되어 기존 DB PK와 충돌하는 문제를 방지한다.

`SequenceSync.sync()`는 `Main.main()` 초기에 호출되며, 리플렉션으로 각 도메인 클래스의 `sequence` 필드에 `SELECT MAX(pk)` 결과를 주입한다. PK 형식이 숫자가 아닌 경우(예: `CS-홍길동-2024-01-01`) 정규식으로 숫자만 추출하여 파싱한다.

- **Employee 서브클래스** (`ClaimsHandler`, `FinanceManager` 등)는 `Employee.sequence`를 공유하므로 여러 테이블의 MAX를 UNION ALL로 한꺼번에 조회한다.
- **SequenceSync가 등록하지 않은 DAO** (Sales 계열 등): 별도 PK 동기화가 없어 JVM 재시작 시 충돌 가능성이 있다.

### 설계 제약 및 알려진 한계

| 항목 | 현황 | 이유 |
|---|---|---|
| **FK 제약** | schema.sql에 NULLABLE FK 23개 선언 (2026-05-29) | 비-NULL 값만 무결성 검사. `customer_registrations.customer_id` 1개만 제외 (등록 순서 문제) |
| **트랜잭션** | DBA가 `ThreadLocal<Connection>` 기반 TX 지원 (2026-05-29). 5개 Runner에 적용 | 나머지 Runner는 단일 테이블 저장이라 원자성 보장. 크로스-DAO TX는 추후 Service 레이어 도입 시 이전 예정 |
| **Service 레이어 부재** | Runner가 UC 조정 + 트랜잭션 경계 역할을 겸함 | AOP 없는 순수 Java 구조. 추후 Service 레이어 추가 시 `DBA.beginTransaction()` 호출을 Runner → Service로 이전 |
| **static sequence PK** | 모든 도메인이 JVM 내 카운터로 PK 생성 | SequenceSync로 재시작 후 MAX 동기화하지만, 동시 실행 환경에서는 충돌 가능 |
| **테스트 컴파일 에러** | `ActivityPlanTest.java:123` — ScheduleItem 생성자 시그니처 불일치 | 테스트 코드 미수정 상태; `./gradlew compileJava`(main)는 정상, `./gradlew build`(test 포함)는 실패 |

---

## 버그 수정 규칙

1. **수정 전 BUG ID를 먼저 언급한다** — Edit/Write 호출 직전에 `BUG-XXX-NN: 무엇을 왜 고치는지` 한 줄 이상 명시
2. **하나씩 수정하고 컴파일 확인** — 버그 하나 수정 → `./gradlew compileJava` 통과 → 다음 버그
3. **레포트 범위만 수정한다** — 버그 레포트에 없는 리팩토링이나 추가 기능은 넣지 않는다
4. **트랜잭션 블록 안에서 생성한 변수를 밖에서 참조하지 않는다** — 필요한 경우 블록 밖에 컬렉션을 선언하고 블록 안에서 채운 뒤 커밋 후 사용

## 추가 설계 문서

- `StructureDesign.md` — 트랜잭션 지원(DBA ThreadLocal 방식) 및 FK 제약 추가에 대한 설계 결정 근거와 구현 방법 기술

## 스키마 변경 이력

컬럼 추가 상세 내역은 각 BUG ID(`FinalBugReport.md`)를 참조. 구조적 변경 요약:

- **2026-05-28**: `expiring_contract_notices` 테이블 신규 생성, `interview_records.interviewed_at` 컬럼 추가
- **2026-05-29**: 버그 수정 과정에서 컬럼 6개 추가 (payment_records, education_preparations, overdue_notice_settings, policy_applications 등)
- **2026-05-29**: 전체 23개 테이블에 NULLABLE FK 제약 추가 (`StructureDesign.md` 참조)

> **주의**: 스키마 변경 후엔 반드시 `docker compose down -v && docker compose up -d` 실행

---


## 버그 수정 이력 문서

아래 파일에 발견·수정된 버그 전체 내역이 기록되어 있다. 모든 항목 ✅ 수정 완료.

| 파일 | 내용 |
|---|---|
| `AdditionalBugReport.md` | 초기 DB 마이그레이션 후 발견된 버그 (BUG-NEW-*) |
| `CodeReviewReport.md` | 코드 리뷰 단계 버그 (BUG-EDU-*, BUG-SAL-*, BUG-CON-*, BUG-SCH-*, BUG-REC-*, 기타) |
| `FinalBugReport.md` | 멀티 에이전트 전수 탐색으로 발견된 버그 (BUG-EXTRA-*, BUG-R2-* ~ BUG-R9-*) |