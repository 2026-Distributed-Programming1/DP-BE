# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

> **현재 상태 (2026-06-01)**: 콘솔(Runner) 구조를 Spring REST API 구조로 전환 완료. 최종 수렴도 완료되어 `old/`/`OldMain` 삭제, 업무번호 format-on-read, FK `id` 전환이 반영됨.
> - 완료: 전 UC Controller/Service/Repository/DTO 전환 · 웹 시더 · `old/` 제거 · `xxx_no` 저장 컬럼 제거 · FK `id(BIGINT)` 전환 · Docker DB 재생성 및 주요 API smoke test ✅
> - **다음 작업**: 문서/주석의 레거시 표현 정리, 선택적으로 slf4j 로깅 확대 · CORS · 인증/인가 · 응답 envelope 검토.
> - **작업 전 반드시 참고**: 전환 계획·현황·결정은 **`src/main/resources/design/ApiMigrationPlan.md`**.

## 빌드 및 실행

```bash
./gradlew compileJava        # 컴파일만 (버그 수정/변경 후 확인 시)
./gradlew bootRun            # Spring Boot 웹 서버 기동 (포트 8080) — 진입점 DpBeApplication
./gradlew build              # 전체 빌드 (test 포함 — 아래 '알려진 한계'의 테스트 에러 주의)
```
- **웹 진입점**: `org.dpbe.DpBeApplication`(@SpringBootApplication). 기동 시 `DataSeeder`가 초기 데이터를 적재한다.
- 콘솔 진입점(`OldMain`)과 레거시 `old/` 계층은 제거됨.

**DB 초기화 (Docker)**
```bash
docker compose up -d         # 최초 기동 (schema.sql 자동 실행)
docker compose down -v && docker compose up -d  # 스키마 변경 후 재생성
```
- 컨테이너: `insurance_db` / 접속 정보: `admin:1234@localhost:3306/insurance_db`
- `schema.sql`은 볼륨 첫 초기화 시에만 실행됨 — 컬럼 추가 후엔 반드시 재생성

## 아키텍처

현재 런타임 경로는 Spring REST API 단일 경로다.

```
신규(웹)   HTTP → Controller → Service(@Transactional) → Repository
                  → SqlExecutor(DataSourceUtils) → Spring DataSource → MySQL
```

레거시 `old/`(DBA·DAO·Runner)와 `OldMain`은 제거되었고, DB 접근은 `SqlExecutor` + Spring `@Transactional`로 통일됐다.

### 패키지 구조 (`org.dpbe`)

```
org.dpbe
├─ DpBeApplication(웹 진입점)
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
```
- 컴포넌트 스캔: `@SpringBootApplication`(`org.dpbe`)이 `domain.*`·`global.*`를 스캔. `old.*`는 Spring 애너테이션이 없어 무영향.
- 도메인 모델(엔터티)은 raw JDBC라 JPA `@Entity`가 없다.

## 신규 웹 계층 패턴

- **Controller** (`domain.<feature>.controller`): `@RestController`. DTO 입출력만, 로직 없음.
- **Service** (`domain.<feature>.service`): `@Service` + `@Transactional`(조회는 `readOnly=true`). UC 절차 조정·검증. `DataSource`/`Connection`을 만지지 않는다(프록시가 경계 관리).
- **Repository** (`domain.<feature>.repository`): `@Repository`. `SqlExecutor` 주입, raw JDBC. 커넥션은 `DataSourceUtils` 경유라 `@Transactional`에 자동 참여.
- **DTO** (`domain.<feature>.dto`): record 기반 요청/응답.
- **예외**: 검증/조회 실패 시 `throw ApiException`(notFound/badRequest) → `ApiExceptionHandler`가 4xx/5xx + `ErrorResponse`로 변환.
- **SqlExecutor** (`global.jdbc`): `executeUpdate`/`executeQuery`/`queryOne`/`executeInsertReturningKey`. SQLException은 런타임 예외로 변환(롤백 유도). 파라미터로 `String/Integer/Long/Double/Boolean/LocalDate/LocalDateTime` 직접 전달.
- **다단계 입력 흐름**은 클라이언트 주도: 조회 `GET` + (선택)`preview POST` + 제출 `POST`(완성 DTO). 서버 무상태.
- **시더** `DataSeeder`(`CommandLineRunner`): 멱등(데이터 있으면 skip), `app.seed.enabled=false`로 비활성화, slf4j 로깅.
- **surrogate-PK / format-on-read**: PK는 DB `id`(AUTO_INCREMENT), 업무번호(`CON00001` 등)는 저장하지 않고 `id`에서 파생한다. FK는 업무번호 문자열이 아니라 부모 `id(BIGINT)`를 참조한다.

### 전환 이력 참고

DAO/Runner → Spring REST API 전환은 완료됐다. 아래 원칙은 과거 전환 이력 확인이나 유사 작업을 추가할 때의 참고용이다.

1. **기계적 이식 금지.** 콘솔 흐름을 그대로 복사하지 않고, 조회·검증·상태전이·저장을 Controller/Service/Repository/DTO 책임으로 나눈다.
2. **API 사용 방식 우선.** "콘솔이 이렇게 했다"가 아니라 "클라이언트가 이 자원을 어떻게 호출하나"를 기준으로 엔드포인트와 요청/응답 DTO를 정한다.
3. **설계 선택지는 합의.** 엔터티/스키마/상태 전이가 가정과 다르면 임의로 축소하지 않고 선택지를 정리한 뒤 진행한다.

### 현재 구현 체크리스트

**A. 실물 확인.** 작성 전 대상 엔터티, `schema.sql`, 현재 Repository/Service/DTO를 확인한다. `old/dao`는 제거됐으므로 더 이상 정답지로 사용할 수 없다.

**B. 엔터티는 rich 도메인 모델.** Service는 엔터티의 검증·상태전이 메서드(`submit()`, `approve()` 등)를 호출한다. DB 복원용 셸 주입은 기존 도메인 관습에 맞춘다.

**C. Repository = 최종 format-on-read 패턴.** `save()`는 업무번호 저장 컬럼 없이 INSERT → `executeInsertReturningKey` → `setId` → 업무번호 필드 파생 주입 순서로 처리한다. DB에 `xxx_no`를 저장하거나 UPDATE하지 않는다. FK는 부모 업무번호가 아니라 부모 `id(BIGINT)`를 저장한다.

**D. Service/DTO/Controller.** Service는 `@Transactional`을 사용하고, 검증 실패는 `ApiException`으로 표현한다. DTO는 record 중심으로 유지하고 Controller는 DTO 입출력만 담당한다.

**E. 완료 기준.** `./gradlew compileJava` → 스키마 변경 시 Docker DB 재생성 → `bootRun` → 주요 정상 흐름과 예외 분기 API smoke test → 설계 문서 갱신 순서로 검증한다.

## 레거시 콘솔 계층

`old/`, `OldMain`, `DBA`, `SequenceSync`, Runner/DAO 계층은 2026-06-01 최종 수렴에서 삭제됐다. 현재 런타임 경로는 Spring REST API 단일 경로이며, DB 접근은 `SqlExecutor` + Spring `@Transactional`로 통일됐다.

## 전체 파일 구성

### 도메인 모델 ↔ 테이블 매핑

> 모델(엔터티)은 `org.dpbe.domain.<feature>.entity`(actor 계열은 `domain.actor`). 아래 표의 도메인 열은 feature 이름이다.

| 도메인(feature) | 주요 도메인 클래스 | DB 테이블 |
|---|---|---|
| education | EducationPlan | education_plans |
| | EducationPreparation | education_preparations |
| | EducationExecution | education_executions, education_attendances |
| sales | ChannelRecruitment | channel_recruitments |
| | ChannelScreening | channel_screenings |
| | ActivityPlan, ScheduleItem | activity_plans, activity_schedule_items |
| | SalesActivityManagement | sales_activity_managements |
| | SalesOrgEvaluation | sales_org_evaluations |
| | BonusRequest | bonus_requests |
| | CustomerRegistration | customer_registrations |
| consultation | ConsultationRequest | consultation_requests |
| | InterviewSchedule | interview_schedules |
| | InterviewRecord | interview_records |
| | Proposal | proposals |
| | Underwriting | underwritings |
| | InsuranceApplication | insurance_applications |
| | PolicyApplication | policy_applications |
| | Revival | revivals |
| contract | Contract | contracts |
| | Cancellation | cancellations |
| | ContractStatistics | contract_statistics |
| | ExpiringContractManagement | expiring_contract_notices |
| claim | AccidentReport | accident_reports |
| | Dispatch | dispatches |
| | DispatchRecord | dispatch_records, dispatch_photos |
| | ClaimRequest | claim_requests |
| | DamageInvestigation | damage_investigations |
| | ClaimCalculation | claim_calculations |
| | ClaimPayment | claim_payments |
| payment | Payment, PaymentItem | payments, payment_items |
| | PaymentRecord | payment_records |
| | RefundCalculation | refund_calculations |
| | RefundPayment | refund_payments |
| | OverdueNoticeSetting | overdue_notice_settings |
| inquiry | Inquiry | inquiries |
| actor | Customer | customers |
| | Designer | designers |
| | Agency | agencies |
| | EducationTrainer | education_trainers |
| | SalesManager | sales_managers |
| | InsuranceReviewer | insurance_reviewers |
| | ClaimsHandler | claims_handlers |
| | DispatchAgent | dispatch_agents |
| | FinanceManager | finance_managers |

### 유스케이스 전환 상태

전 UC는 Controller/Service/Repository/DTO 구조로 전환 완료됐다. 세부 엔드포인트와 남은 운영 개선 과제는 `src/main/resources/design/ApiMigrationPlan.md`를 기준으로 본다.

### SequenceSync

삭제됨. 업무번호는 DB `AUTO_INCREMENT id` 기반 format-on-read로 파생하며, `DataSeeder`는 멱등 가드로 중복 시드를 방지한다.

### 설계 제약 및 알려진 한계

| 항목 | 현황 | 비고 |
|---|---|---|
| **런타임 경로** | Spring REST API 단일 경로 | `old/`, `OldMain`, DBA 제거 완료 |
| **Service 레이어** | 전 UC Controller/Service/Repository/DTO 전환 완료 | |
| **트랜잭션** | Spring `@Transactional` | Repository는 `SqlExecutor` 경유 |
| **PK/FK 방식** | 모든 주요 업무 테이블은 `id` PK, 업무번호는 format-on-read, FK는 `*_id BIGINT` | |
| **검증** | Docker DB 재생성 + 주요 API smoke test 통과 | 계약, 납입, 청구-지급, 교육, 영업, 부활 |
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
- `design/Class_Diagram_Domain.md`, `design/Class_Diagram_Mermaid.md`, `design/DAOStructure.md` — 클래스/구조 참고 문서
- 버그 이력 (`src/main/resources/bugreport/`): `BugReport.md`, `AdditionalBugReport.md`, `CodeReviewReport.md`, `FinalBugReport.md` — 발견·수정 버그 전체 내역(모두 ✅)

## 스키마 변경 이력

- **2026-05-28**: `expiring_contract_notices` 신규, `interview_records.interviewed_at` 추가
- **2026-05-29**: 버그 수정으로 컬럼 6개 추가, 전체 23개 테이블 NULLABLE FK 추가
- **2026-05-30**: surrogate-PK 배치 1·2 — customers·contracts·payments·payment_records + claim 7테이블에 `id BIGINT AUTO_INCREMENT PK` 추가, 기존 업무키 `UNIQUE` 강등
- **2026-05-31**: 배치 5 — education 컬럼명 통일(4개 변경) + education_plans·preparations·executions·inquiries + actor 마스터 8테이블에 `id AUTO_INCREMENT` 추가 + `additional_notice`, `total_count` 신규 컬럼 추가
- **2026-06-01**: 최종 수렴 — `old/`/`OldMain` 제거, `xxx_no` 저장 컬럼 제거, FK `id(BIGINT)` 전환, format-on-read 적용, Docker DB 재생성 및 smoke test 완료

> **주의**: 스키마 변경 후엔 반드시 `docker compose down -v && docker compose up -d` 실행
