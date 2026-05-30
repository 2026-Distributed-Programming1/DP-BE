# 배치 2 상세 계획 — claim 도메인

> 메인 문서 `ApiMigrationPlan.md`의 §9·부록 A(배치 분할)를 보완하는 **배치 2 전용 세부 계획**. 메인 문서는 변경하지 않는다.
> 원칙(메인 부록 A 그대로): 도메인 단위로 **UC 신규 전환(Controller/Service/Repository/DTO) + surrogate-PK(id) + 업무번호 id-파생(저장형)** 을 함께. 레거시 `old/`는 무수정 공존.
> **API 설계 방향**: 콘솔의 단계별 절차를 베끼지 않는다. Runner의 **비즈니스 규칙·검증·분기(Alt/Exception)** 만 Service로 이관하고, 프론트가 필요한 엔드포인트(GET 조회 / POST 등록·처리)를 골라 호출하는 **클라이언트 주도·무상태** 설계로 만든다. 화면 흐름의 순서는 프론트가 정한다.
> **실행 순서(확정)**: ① claim 7개 테이블 스키마(id PK+UNIQUE) + 엔터티 `Long id` **일괄 선행** (✅ 2026-05-30 완료 — 컴파일·DB 재생성·기동 검증, 7개 테이블 PK=id, 레거시 무수정) → ② UC를 서브배치(2a 보상 → 2b 사고·출동)로 신규 전환.

## 0. claim 파이프라인 개요

claim은 단계가 **앞 단계의 저장 상태를 읽어 잇는 연쇄 흐름**이다:

```
사고접수            출동                출동기록           (보험금) 청구          손해조사              산출                 지급
AccidentReport → Dispatch → DispatchRecord    ClaimRequest → DamageInvestigation → ClaimCalculation → ClaimPayment
   accident_no     dispatch_no   record_no        claim_no       investigation_no     calculation_no      payment_no
```

- 좌측 묶음(사고·출동)과 우측 묶음(보상 청구→지급)은 서로 독립적으로 진행 가능 → **2b / 2a 로 분리**.
- FK 참조: dispatches→accident_reports, dispatch_records→dispatches, damage_investigations→claim_requests, claim_calculations→damage_investigations, claim_payments→claim_calculations. (claim_requests→customers·contracts) — 모두 업무키 문자열 참조 → 업무키 UNIQUE 유지로 그대로 동작.

## 1. 대상 테이블 (7) & PK 조치

전부 C2(시퀀스형) → `id BIGINT AUTO_INCREMENT PK` + 업무키 `UNIQUE`, 업무번호 id-파생(저장형: INSERT→회수→UPDATE).

| 테이블 | 업무키(접두) | 부모 FK | 단계 |
|---|---|---|---|
| accident_reports | accident_no (ACC?) | customer_id | 2b |
| dispatches | dispatch_no | accident_no | 2b |
| dispatch_records | record_no | dispatch_no | 2b |
| claim_requests | claim_no | customer_id, contract_no | 2a |
| damage_investigations | investigation_no | claim_no | 2a |
| claim_calculations | calculation_no | investigation_no | 2a |
| claim_payments | payment_no | calculation_no | 2a |

> 접두사·형식은 기존 도메인 생성자(`static sequence`)의 접두를 따른다(엔터티 확인 후 확정).

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
3. **Repository** (`domain/claim/repository/`): `SqlExecutor` 주입. finder(기존 DAO 매핑 복제 + id), `save()`는 INSERT→`executeInsertReturningKey`→업무번호 파생 UPDATE.
4. **Service** (`domain/claim/service/`): `@Transactional`(조회 readOnly). 기존 Runner 절차/검증/E1을 이관. 다단계 처리 결과는 트랜잭션 저장.
5. **DTO** (`domain/claim/dto/`): record 요청/응답.
6. **Controller** (`domain/claim/controller/`): REST 엔드포인트.
7. 검증: 컴파일 → DB 재생성 → 시더 데이터로 흐름 호출(조회·등록·연쇄).

## 4. 엔드포인트 초안 (기존 Runner 흐름 기준)

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

> 세부 필드·분기(A/E)는 각 Runner를 보고 Service에 1:1 이관. 위 경로는 초안이며 구현 시 확정.

## 5. 진행 순서 / 현재 위치

- **① PK 파운데이션 ✅ 완료** — claim 7테이블 id PK + 업무키 UNIQUE(schema.sql), 7엔터티 `Long id`+getId/setId. → **§3의 1·2단계(스키마·엔터티)는 7테이블 모두 이미 끝남. 2a/2b는 재작업 불필요.**
- **▶ 다음 = 배치 2a** (보상 핵심). 각 UC에 대해 §3의 **3~6단계만**: Repository(`domain/claim/repository/`) → Service(`domain/claim/service/`) → DTO(`domain/claim/dto/`) → Controller(`domain/claim/controller/`).
  - 리포지토리 `save()`는 부록 A.1 절차(INSERT→`executeInsertReturningKey`→업무번호 파생 UPDATE). 접두: claim_no=`CLM`, investigation_no=`INV`, calculation_no=`CAL`, claim_payment.payment_no=`CPY` (엔터티 생성자 기준).
  - 참고 구현: `domain/payment/**`(쓰기 흐름), `domain/contract/**`(읽기 흐름) — 동일 패턴.
- 그다음 **2b**(사고·출동). 접두: accident_no=`ACC`, dispatch_no=`DSP`, dispatch_record.record_no=`DRC`.
- 각 서브배치: 코드 → `./gradlew compileJava` → `docker compose down -v && up -d` → 기동·흐름 호출 검증 → 본 문서/메인 A.6에 ✅.

## 6. 주의

- **한 테이블 한 writer**: claim UC를 신규로 옮기면 해당 레거시 Runner는 은퇴(콘솔 실행 금지) — 업무번호 형식 충돌 방지(메인 A.2).
- 레거시 `old/dao`·`old/runner`·도메인 `static sequence` 무수정.
- claim_requests의 `claim_reasons`는 CSV로 저장(기존 DAO 방식 유지).
- 검증용 시드: 현재 `DataSeeder`는 고객·계약만 적재. claim 흐름 검증엔 사고/청구 최초 등록을 API로 직접 호출해 생성(별도 시드 불필요).

---

## 7. ⚠️ 정석 재작성 가이드라인 (2026-05-30)

> **경위**: 1차 시도의 claim 2a 코드(dto 7 + repository 4)가 **실제 엔터티/스키마를 확인하지 않고** 작성돼 컴파일 9에러로 깨졌고, 전량 삭제했다(PK 파운데이션·배치 1은 정상이라 유지). 재작성 시 아래를 **반드시** 지킬 것.

### 7.1 절대 규칙 — "추측 금지, 실물 확인"

코드 작성 전 **반드시** 해당 엔터티(`domain/claim/entity/*`)·스키마(`schema.sql`)·기존 DAO(`old/dao/*DAO`)를 열어 시그니처를 확인한다. 1차 실패는 전부 *존재하지 않는 타입/메서드/컬럼을 가정*해서 발생했다:
- ❌ `BankAccountDto` — **없음**. 계좌는 엔터티 `BankAccount` 하나뿐.
- ❌ `enums.ClaimReason` — **없음**. 청구 사유는 `List<String>`.
- ❌ `new ClaimRequest()`, `getReceiptAccount()`, `getContractNo()`, `setCustomerId(int)`, `getDiagnosisName()`, `getAccidentDate()`, `getRequestAmount()`, `BankAccount.getAccountNumber()`, `new BankAccount(b,n,h)` — **전부 없음**.

### 7.2 실제 도메인 API (확인 완료 — 이 표를 기준으로 작성)

**`BankAccount`** (`domain/common/entity`): 기본 생성자 + `enter(bank, no, holder)` / `verify()` / `isVerified()` / `getBankName()` `getAccountNo()` `getAccountHolder()`. ※ `getAccountNumber()` 아님.

**`ClaimRequest`**: 생성자 `(Customer, Contract)` 또는 DB로딩용 `(claimNo, Customer, Contract, ClaimRequestStatus)`. 계좌=`getBankAccount()`/`selectExistingAccount`/`registerNewAccount`. 계약=`getContract().getContractNo()`. 고객=`getCustomer()`(customer_id는 **String**). 사유=`getClaimReasons():List<String>`/`selectClaimReasons(List<String>)`. 진단=`getDiagnosis()`/`enterDiagnosis`. 유형=`getClaimType():ClaimType`. 그 외 `getId/setId`, `getClaimNo/setClaimNo`, `getRequestedAt/setRequestedAt`, `getStatus`. ⚠️ accidentDate·requestAmount 필드는 **존재하지 않음**(스키마에도 없음).

**`DamageInvestigation`·`ClaimCalculation`·`ClaimPayment`**: 각 `old/dao`의 `save()`/`mapRow()` 컬럼·게터를 그대로 따른다(해당 DAO가 정답지). 셸 객체 생성·연관 복원 방식도 DAO와 동일하게.

### 7.3 스키마 컬럼명 (claim_requests 예 — 실제와 1:1)

`id, claim_no, customer_id, customer_name, contract_no, claim_type, diagnosis, claim_reasons(CSV), bank_name, account_no, account_holder, requested_at, status`. ⚠️ `diagnosis_name`·`accident_date`·`request_amount`·`account_number` 같은 컬럼은 **없다**. 나머지 6테이블도 `schema.sql`의 실제 컬럼명을 그대로 쓸 것.

### 7.4 리포지토리 패턴 — 배치 1을 복제

`domain/contract/repository/ContractRepository`를 템플릿으로:
- `@Repository` + 생성자 주입 `SqlExecutor sql`.
- `save()`: 업무번호 컬럼 제외하고 INSERT → `sql.executeInsertReturningKey(...)`로 id 회수 → `entity.setId(id)` + `entity.setXxxNo(접두+String.format("%05d", id))` → `UPDATE ... SET xxx_no=? WHERE id=?`.
- 접두: claim_no=`CLM`, investigation_no=`INV`, calculation_no=`CAL`, claim_payments.payment_no=`CPY`, accident_no=`ACC`, dispatch_no=`DSP`, dispatch_records.record_no=`DRC` (각 엔터티 생성자의 기존 접두와 일치시킬 것 — 엔터티에서 확인).
- finder: `mapRow`는 `RowMapper<T>`(행 1개→객체) 시그니처. ⚠️ 1차 실패처럼 `executeQuery` 안에서 `while(rs.next())`로 List를 만들지 말 것 — `executeQuery`가 내부에서 행을 순회하며 `mapRow`를 행마다 호출한다. `SELECT id, ...` 로 id 포함, `c.setId(rs.getLong("id"))` 매핑 추가.
- 매핑 로직·셸 객체 복원은 대응되는 `old/dao/*DAO.findAll()`을 그대로 옮기되 `DBA`→`sql`(SqlExecutor)로 치환.

### 7.5 Service/DTO/Controller

- **Service** `@Transactional`(조회 `readOnly=true`), 검증 실패→`ApiException`(global.exception). 절차가 아니라 규칙만 이관(§상단 API 설계 방향).
- **DTO**: record. 계좌는 `BankAccountDto`를 만들 거면 **새로 정의**(bankName/accountNo/accountHolder 3필드)하되, 굳이 없어도 요청 record에 3필드를 평면으로 받아도 됨. 1차처럼 *정의 없이 참조*하지 말 것.
- **Controller** `@RestController`, 엔드포인트는 §4 초안 기준.

### 7.6 완료 기준

`./gradlew compileJava` 그린 → `docker compose down -v && up -d` → 기동 → 2a 흐름(청구 등록→조사→산출→지급) API 호출 검증 → 본 문서·메인 A.6에 ✅.