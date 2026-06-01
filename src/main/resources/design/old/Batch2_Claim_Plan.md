# 배치 2 상세 계획 — claim 도메인

> **문서 성격**: 2026-05-30~31 배치 2 전환 당시의 상세 기록이다. 당시의 "저장형", "old/ 공존", "Runner" 표현은 진행 이력으로 남긴다.
> **현재 기준 (2026-06-01)**: claim 도메인 전환 완료 ✅ · 최종 수렴 완료 ✅ · `old/` 제거 · 업무번호 저장 컬럼 제거 · FK `id(BIGINT)` 전환 · 업무번호 format-on-read 적용.
> **API 설계 방향**: 단계별 콘솔 절차가 아니라 비즈니스 규칙·검증·분기만 Service에 반영하고, 프론트가 필요한 엔드포인트(GET 조회 / POST 등록·처리)를 호출하는 **클라이언트 주도·무상태** 설계로 만든다.

## 0. claim 파이프라인 개요

claim은 단계가 **앞 단계의 저장 상태를 읽어 잇는 연쇄 흐름**이다:

```
사고접수            출동                출동기록           (보험금) 청구          손해조사              산출                 지급
AccidentReport → Dispatch → DispatchRecord    ClaimRequest → DamageInvestigation → ClaimCalculation → ClaimPayment
   accident_no     dispatch_no   record_no        claim_no       investigation_no     calculation_no      payment_no
```

- 좌측 묶음(사고·출동)과 우측 묶음(보상 청구→지급)은 서로 독립적으로 진행 가능 → **2b / 2a 로 분리**.
- 현재 FK 참조: dispatches→accident_reports, dispatch_records→dispatches, damage_investigations→claim_requests, claim_calculations→damage_investigations, claim_payments→claim_calculations. claim_requests→customers·contracts. 최종 수렴 후 부모 참조는 `*_id BIGINT` 기반이다.

## 1. 대상 테이블 (7) & PK 조치

전환 당시에는 C2(시퀀스형) 테이블을 `id BIGINT AUTO_INCREMENT PK` + 업무키 `UNIQUE`로 바꾸고 업무번호를 id에서 파생 저장했다. 현재는 업무번호 저장 컬럼을 제거했고, 응답 시 `id`에서 format-on-read로 파생한다.

| 테이블 | 업무키(접두) | 부모 FK | 단계 |
|---|---|---|---|
| accident_reports | accident_no (ACC?) | customer_id | 2b |
| dispatches | dispatch_no | accident_no | 2b |
| dispatch_records | record_no | dispatch_no | 2b |
| claim_requests | claim_no | customer_id, contract_no | 2a |
| damage_investigations | investigation_no | claim_no | 2a |
| claim_calculations | calculation_no | investigation_no | 2a |
| claim_payments | payment_no | calculation_no | 2a |

> 현재 접두사·형식은 `id` 기반 format-on-read로 유지한다.

## 2. 서브배치 분할

### 배치 2a — 보상 핵심 (청구 → 조사 → 산출 → 지급)
UC 4개: **ClaimRequest(보험금 요청) · DamageInvestigation(손해 조사) · ClaimCalculation(보험금 산출) · ClaimPayment(보험금 지급)**
- 테이블: claim_requests, damage_investigations, claim_calculations, claim_payments
- 연쇄 조회가 많음(claim_no→investigation_no→calculation_no). 조회 + 처리(등록) 엔드포인트.

### 배치 2b — 사고·출동 (접수 → 출동기록)
UC 2개: **AccidentReport(사고 접수) · DispatchRecord(현장 출동 기록)**
- 테이블: accident_reports, dispatches, dispatch_records
- 사고접수 시 needs_dispatch면 dispatch 생성. 출동요원이 dispatch_record 기록.

> 각 서브배치는 독립적으로 컴파일·`down -v && up -d`·검증 후 다음으로.

## 3. 서브배치별 작업 항목 (공통 절차)

각 UC마다 (메인 §8 패턴):
1. **스키마**: 해당 테이블 `id` PK + 업무키 `UNIQUE` (schema.sql).
2. **엔터티**: `Long id` + getId/setId. 업무번호 세터 없으면 추가.
3. **Repository** (`domain/claim/repository/`): `SqlExecutor` 주입. 최종형 `save()`는 INSERT→`executeInsertReturningKey`→엔터티 업무번호 필드 주입이며 DB UPDATE는 없다.
4. **Service** (`domain/claim/service/`): `@Transactional`(조회 readOnly). 유스케이스 규칙·검증·E1을 처리한다.
5. **DTO** (`domain/claim/dto/`): record 요청/응답.
6. **Controller** (`domain/claim/controller/`): REST 엔드포인트.
7. 검증: 컴파일 → DB 재생성 → 시더 데이터로 흐름 호출(조회·등록·연쇄).

## 4. 엔드포인트 초안 (전환 당시 설계)

### 2a
| UC | HTTP | 비고 |
|---|---|---|
| 보험금 요청 | `GET /api/customers/{id}/contracts` (재사용) · `POST /api/claims` | 청구 등록(계약·진단·사유·계좌) |
| | `GET /api/claims`, `GET /api/claims/{claimNo}` | 목록·상세 |
| 손해 조사 | `GET /api/claims/{claimNo}/investigation` · `POST /api/claims/{claimNo}/investigation` | 조사 등록(과실비율·인정손해·결과) |
| 보험금 산출 | `POST /api/investigations/{no}/calculation` · `GET .../calculation` | 산출(공제·조정) |
| 보험금 지급 | `POST /api/calculations/{no}/payment` · `GET .../payment` | 지급(수령계좌·예정/완료) |

### 2b
| UC | HTTP | 비고 |
|---|---|---|
| 사고 접수 | `POST /api/accidents`, `GET /api/accidents`, `GET /api/accidents/{no}` | 접수 시 needs_dispatch면 dispatch 생성 |
| 출동 기록 | `GET /api/dispatches`, `POST /api/dispatches/{no}/record` | 출동요원 기록 |

> 세부 필드·분기(A/E)는 유스케이스 시나리오와 도메인 규칙을 기준으로 Service에 반영했다. 위 경로는 당시 초안이며, 완료된 엔드포인트는 §8·§9 기준이다.

## 5. 진행 순서 / 현재 위치

- **PK 파운데이션 ✅ 완료** — claim 7테이블 id PK 적용.
- **배치 2a ✅ 완료** — 보상 핵심(청구→조사→산출→지급) API 전환.
- **배치 2b ✅ 완료** — 사고·출동 API 전환.
- **최종 수렴 ✅ 완료** — 업무번호 저장 컬럼 제거, FK `id(BIGINT)` 전환, 업무번호 format-on-read 적용.

## 6. 주의

- 현재 런타임 경로는 Spring REST API 단일 경로다.
- claim_requests의 `claim_reasons`는 CSV로 저장한다.
- 검증용 시드: 현재 `DataSeeder`는 고객·계약만 적재. claim 흐름 검증엔 사고/청구 최초 등록을 API로 직접 호출해 생성(별도 시드 불필요).

---

## 7. ⚠️ 정석 재작성 가이드라인 (2026-05-30)

> **경위**: 1차 시도의 claim 2a 코드(dto 7 + repository 4)가 **실제 엔터티/스키마를 확인하지 않고** 작성돼 컴파일 9에러로 깨졌고, 전량 삭제했다(PK 파운데이션·배치 1은 정상이라 유지). 재작성 시 아래를 **반드시** 지킬 것.

### 7.1 절대 규칙 — "추측 금지, 실물 확인"

코드 작성 전 **반드시** 해당 엔터티(`domain/claim/entity/*`)·스키마(`schema.sql`)·현재 Repository/Service/DTO를 열어 시그니처를 확인한다. 1차 실패는 전부 *존재하지 않는 타입/메서드/컬럼을 가정*해서 발생했다:
- ❌ `BankAccountDto` — **없음**. 계좌는 엔터티 `BankAccount` 하나뿐.
- ❌ `enums.ClaimReason` — **없음**. 청구 사유는 `List<String>`.
- ❌ `new ClaimRequest()`, `getReceiptAccount()`, `getContractNo()`, `setCustomerId(int)`, `getDiagnosisName()`, `getAccidentDate()`, `getRequestAmount()`, `BankAccount.getAccountNumber()`, `new BankAccount(b,n,h)` — **전부 없음**.

### 7.2 실제 도메인 API (확인 완료 — 이 표를 기준으로 작성)

**`BankAccount`** (`domain/common/entity`): 기본 생성자 + `enter(bank, no, holder)` / `verify()` / `isVerified()` / `getBankName()` `getAccountNo()` `getAccountHolder()`. ※ `getAccountNumber()` 아님.

**`ClaimRequest`**: 생성자 `(Customer, Contract)` 또는 DB로딩용 `(claimNo, Customer, Contract, ClaimRequestStatus)`. 계좌=`getBankAccount()`/`selectExistingAccount`/`registerNewAccount`. 계약=`getContract().getContractNo()`. 고객=`getCustomer()`(customer_id는 **String**). 사유=`getClaimReasons():List<String>`/`selectClaimReasons(List<String>)`. 진단=`getDiagnosis()`/`enterDiagnosis`. 유형=`getClaimType():ClaimType`. 그 외 `getId/setId`, `getClaimNo/setClaimNo`, `getRequestedAt/setRequestedAt`, `getStatus`. ⚠️ accidentDate·requestAmount 필드는 **존재하지 않음**(스키마에도 없음).

**`DamageInvestigation`·`ClaimCalculation`·`ClaimPayment`**: 현재 Repository의 컬럼·게터·셸 객체 생성·연관 복원 방식을 기준으로 한다.

### 7.3 스키마 컬럼명 (claim_requests 예 — 실제와 1:1)

`id, claim_no, customer_id, customer_name, contract_no, claim_type, diagnosis, claim_reasons(CSV), bank_name, account_no, account_holder, requested_at, status`. ⚠️ `diagnosis_name`·`accident_date`·`request_amount`·`account_number` 같은 컬럼은 **없다**. 나머지 6테이블도 `schema.sql`의 실제 컬럼명을 그대로 쓸 것.

### 7.4 리포지토리 패턴 — 배치 1을 복제

`domain/contract/repository/ContractRepository`를 템플릿으로:
- `@Repository` + 생성자 주입 `SqlExecutor sql`.
- `save()`: INSERT → `sql.executeInsertReturningKey(...)`로 id 회수 → `entity.setId(id)` + `entity.setXxxNo(접두+String.format("%05d", id))`. DB에 업무번호를 저장하거나 UPDATE하지 않는다.
- 접두: claim_no=`CLM`, investigation_no=`INV`, calculation_no=`CAL`, claim_payments.payment_no=`CPY`, accident_no=`ACC`, dispatch_no=`DSP`, dispatch_records.record_no=`DRC` (각 엔터티 생성자의 기존 접두와 일치시킬 것 — 엔터티에서 확인).
- finder: `mapRow`는 `RowMapper<T>`(행 1개→객체) 시그니처. ⚠️ 1차 실패처럼 `executeQuery` 안에서 `while(rs.next())`로 List를 만들지 말 것 — `executeQuery`가 내부에서 행을 순회하며 `mapRow`를 행마다 호출한다. `SELECT id, ...` 로 id 포함, `c.setId(rs.getLong("id"))` 매핑 추가.
- 매핑 로직·셸 객체 복원은 현재 Repository 구현과 `schema.sql`을 기준으로 한다.

### 7.5 Service/DTO/Controller

- **Service** `@Transactional`(조회 `readOnly=true`), 검증 실패→`ApiException`(global.exception). 절차가 아니라 규칙만 이관(§상단 API 설계 방향).
- **DTO**: record. 계좌는 `BankAccountDto`를 만들 거면 **새로 정의**(bankName/accountNo/accountHolder 3필드)하되, 굳이 없어도 요청 record에 3필드를 평면으로 받아도 됨. 1차처럼 *정의 없이 참조*하지 말 것.
- **Controller** `@RestController`, 엔드포인트는 §4 초안 기준.

### 7.6 완료 기준

`./gradlew compileJava` 그린 → `docker compose down -v && up -d` → 기동 → 2a 흐름(청구 등록→조사→산출→지급) API 호출 검증 → 본 문서·메인 A.6에 ✅.

---

## 8. ✅ 배치 2a 완료 (2026-05-31)

claim 보상 핵심 4개 UC를 신규 웹 경로로 전환 완료. `compileJava` 그린 → `down -v && up -d` 재생성 → 기동 → 전체 흐름·예외 분기 API 검증 통과.

### 8.1 엔드포인트 (구현·검증 완료)

| UC | HTTP |
|---|---|
| 보험금 요청 | `POST /api/claims` · `GET /api/claims` · `GET /api/claims/{claimNo}` |
| 손해 조사 | `POST·GET /api/claims/{claimNo}/investigation` |
| 보험금 산출 | `POST·GET /api/investigations/{no}/calculation` |
| 산출 승인 | `POST /api/calculations/{no}/approve` (CALCULATED→APPROVED) |
| 보험금 지급 | `POST·GET /api/calculations/{no}/payment` · `POST /api/payments/{no}/execute` |

### 8.2 설계 결정 (1차 실패 교훈 반영)

- **무상태·단계분리**: 각 단계 1 POST = 자기 테이블 1행 저장. 부모는 URL 업무키로 DB에서 읽어 잇음(서버 무상태).
- **지급 생성/실행 2단계 분리**: OTP·예약 대응. `payment`(WAITING/SCHEDULED 생성)와 `execute`(OTP 검증→이체)를 분리. **실제 OTP 도입 시 execute의 검증(현재 더미 6자리)만 교체.** 이체 실패(FAILED)는 정상 업무결과라 예외를 던지지 않고 저장(롤백 방지).
- **지급 계좌 = 조인 1쿼리 승계**: `claim_calculations→damage_investigations→claim_requests` 조인으로 산출액+청구 인증계좌+수령인 로드(셸 체인 우회 안 함).
- **산출 승인 별도 엔드포인트**: 엔터티 `approve()`(CALCULATED→APPROVED)를 `POST /api/calculations/{no}/approve`로 노출. 승인된 산출만 지급 생성 가능.

### 8.3 스키마 변경 (§7.3 갱신)

`down -v && up -d` 필요. 추가 컬럼:
- `claim_requests.personal_info_agreed BOOLEAN` — 개인정보 동의(보존가치 있는 기록). authMethod·authenticated는 검증용 휘발(미저장).
- `claim_calculations.deductible BIGINT`, `coverage_limit BIGINT` — 산출 파라미터(행이 자기 산출결과를 설명하도록 영속화). 미저장 시 GET에서 0 반환되던 결함 해소.

### 8.4 검증 시나리오 결과 (모두 통과)

- 정상 연쇄: 청구(CLM00001)→조사(INV00001, 합100·APPROVED)→산출(CAL00001, 5,000,000×60%−100,000=2,900,000)→승인→지급(CPY00001)→이체(OTP 6자리)=COMPLETED.
- 산출 GET이 저장된 deductible(100000)·coverage_limit(100000000) 정상 반환.
- 예외: 미동의 400 · 없는 고객 404 · 과실합≠100 400(E1) · 중복조사 400 · OTP 5자리 400 · 면책(REJECTED) 건 산출 시도 400.

> **다음**: 배치 2b(사고·출동). 메인 A.6에도 2a ✅ 반영 필요.

---

## 9. ✅ 배치 2b 완료 (2026-05-31)

claim 사고·출동 UC 2개를 신규 웹 경로로 전환 완료. `compileJava` 그린 → `down -v && up -d` → 기동 → 흐름·예외 API 검증 통과. **claim 도메인 7테이블 전부 전환 완료.**

### 9.1 엔드포인트 (구현·검증 완료)

| UC | HTTP |
|---|---|
| 사고 접수 | `POST /api/accidents` · `GET /api/accidents` · `GET /api/accidents/{no}` |
| 출동 기록 | `GET /api/dispatches` · `POST /api/dispatches/{no}/record`(multipart) · `GET /api/dispatches/{no}/record` |

### 9.2 설계 결정

- **사고 접수 시 출동 동반 생성**: needs_dispatch=true면 `requestDispatch()`로 Dispatch를 같은 트랜잭션에서 생성(`dispatches` 행). 응답에 dispatchNo 포함.
- **셸 주입으로 customerName 노출**: accident_reports 자기 행의 customer_id·customer_name을 `AccidentReport.setCustomer`(신규)로 셸 주입(조인 아님 — 같은 행 컬럼). claim 도메인 공통 셸 패턴과 일관.
- **출동 기록 사진 = multipart 직접 수신 + 로컬 파일시스템 저장**: `POST .../record`는 `multipart/form-data`로 실제 파일 수신 → `uploads/dispatch/{recordNo}/`에 저장 → 메타는 `dispatch_photos`(신규 1:N 테이블)에 기록. `photos`는 `required=false`로 받아 빈 경우 Service가 E1(400)로 처리(필수로 두면 500). record_no(DRC) 확정 후 디렉터리·FK 키로 사용.

### 9.3 스키마 변경

`down -v && up -d` 필요. 신규:
- `dispatch_photos`(id, record_no FK→dispatch_records, file_name, file_path, file_size, mime_type, uploaded_at) — 출동 기록 사진 1:N. 실제 파일은 `uploads/`(gitignore).

### 9.4 검증 결과 (모두 통과)

- 정상: 사고접수(ACC00001, customerName 노출, needs_dispatch→DSP00001 동반)→출동목록→출동기록(multipart 사진 2장→DRC00001, TRANSMITTED) — 파일 저장·dispatch_photos 메타·조회 복원 확인.
- 예외: 약관 미동의 400 · 사진 없음 E1 400 · 중복 기록 400.
- 알려진 사소사항: 사고접수 GET 상세에서 reportedAt이 null(DB로딩 생성자가 reportedAt 미복원) — 기능 영향 없음, 후속 정리 가능.

> **claim 도메인 마이그레이션 완료.** 다음 도메인(education/sales/consultation 등)은 PK 파운데이션부터 별도 배치 필요. 메인 A.6에 2a·2b ✅ 반영.
