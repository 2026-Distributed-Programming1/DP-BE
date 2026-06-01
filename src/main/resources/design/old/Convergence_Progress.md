# 최종 수렴 작업 계획 및 진행 현황

> **브랜치**: `feat/final-convergence`  
> **최종 갱신**: 2026-06-01  
> **목표**: 레거시(`old/`) 제거 + format-on-read + FK를 id로 전환하여 코드베이스를 Spring 단일 경로로 수렴

---

## 1. 배경 — 왜 이 작업인가

배치 1~6으로 전 UC를 Spring REST API로 전환 완료. 전환 기간에는 레거시와 신규가 공존할 수 있도록 두 가지 타협이 남아 있었다.

1. **`old/` 공존**: DBA·DAO·Runner 전체가 그대로 존재. Spring 빌드에 영향은 없지만 불필요한 코드 부담.
2. **업무번호 저장형(INSERT+UPDATE)**: `save()` 마다 `UPDATE table SET xxx_no=? WHERE id=?`가 따라다님. 업무번호는 `id`에서 언제든 파생 가능하므로 저장 자체가 불필요. 또한 FK가 `xxx_no` (문자열)로 걸려 있어 스키마가 무겁고 조인이 복잡.

최종 수렴은 이 두 부채를 청산한다.

---

## 2. 전체 작업 단계

| 단계 | 내용 | 상태 |
|---|---|---|
| **1단계** | `old/`(DBA·DAO·Runner) 전체 삭제 + `OldMain.java` 삭제 | ✅ 완료 |
| **2단계** | 엔터티 정리 — `static sequence` 제거, sequence 생성자 제거, 빈 스텁 메서드 제거 | ✅ 완료 |
| **3단계** | format-on-read — Repository `save()`의 UPDATE 제거, `mapRow`에서 id로 업무번호 파생, `findByXxxNo` → `findById` + Service `parseId()` 패턴 | ✅ 완료 |
| **4단계** | 스키마 정리 — `xxx_no` 컬럼 + UNIQUE 제거, FK를 `id`(BIGINT)로 전환, `schema.sql` 정리 | ✅ 완료 |
| **5단계** | Docker DB 재생성 + 주요 API smoke test | ✅ 완료 |

> 2026-06-01 검증 완료: `docker compose down -v && docker compose up -d`, `bootRun`, DataSeeder, 주요 FK 전환 경로 smoke test 통과.

---

## 3. 단계별 작업 원칙

### 3단계 — format-on-read (도메인당 공통 패턴)

각 도메인 Repository에 대해 아래 4가지 변경을 적용한다.

```
A. save()의 UPDATE 제거
   Before: sql.executeUpdate("UPDATE tbl SET xxx_no=? WHERE id=?", no, id);
   After:  (제거)

B. COLS 상수에서 xxx_no 제거
   Before: "id, xxx_no, col1, col2, ..."
   After:  "id, col1, col2, ..."

C. mapRow에서 id 파생으로 교체
   Before: entity.setXxxNo(rs.getString("xxx_no"));
   After:  entity.setXxxNo("PRE" + String.format("%05d", rs.getLong("id")));

D. findByXxxNo → findById
   Before: sql.queryOne("... WHERE xxx_no=?", mapper, xxxNo);
   After:  sql.queryOne("... WHERE id=?", mapper, id);
```

Service 측 변경:
- `parseId()` 헬퍼 추가 (경로 파라미터 "INQ00001" → Long 1)
- `findByXxxNo(xxxNo)` → `findById(parseId(xxxNo))`
- **Controller 경로 유지** (`/{xxxNo}` 그대로) — 프론트 도입 예정으로 API 표면 보존

```java
private Long parseId(String businessNo) {
    try {
        return Long.parseLong(businessNo.replaceAll("\\D", ""));
    } catch (NumberFormatException e) {
        throw ApiException.badRequest("유효하지 않은 번호: " + businessNo);
    }
}
```

> **FK 컬럼(xxx_no가 아닌 다른 테이블의 참조 컬럼)은 건드리지 않는다.**  
> 예: `education_preparations.plan_no`, `refund_payments.refund_no`, `payment_records.contract_no`  
> 이 컬럼들은 4단계에서 `plan_id`, `refund_id`, `contract_id`로 전환한다.

### 4단계 — 스키마 정리

완료된 순서:
1. 각 테이블의 `xxx_no VARCHAR UNIQUE` 컬럼 DROP
2. FK 컬럼 전환: `xxx_no VARCHAR` → `xxx_id BIGINT` + FK CONSTRAINT 추가
3. `schema.sql` 전체 정리
4. `docker compose down -v && docker compose up -d` 재생성 + DataSeeder 재검증

### 5단계 — 검증 결과

- `./gradlew compileJava` 통과
- Docker 볼륨 재생성 후 `schema.sql` 초기화 확인
- `bootRun` 기동 및 DataSeeder 성공
- Smoke test 통과: 계약 조회, 보험료 납입/납부내역, 청구-조사-산출-지급, 교육 계획-제반-진행, 영업 활동계획, 부활 신청
- 검증 중 `claim_payments.calculation_id` 누락 버그 수정 완료

---

## 4. 3단계 도메인별 진행 현황

### 진행 순서 및 상태

| # | 도메인 | Repository 수 | 상태 | 완료 일시 |
|---|---|---|---|---|
| 1 | **inquiry** | 1 | ✅ 완료 | 2026-06-01 |
| 2 | **payment** | 4 (Payment·PaymentRecord·RefundCalculation·RefundPayment) | ✅ 완료 | 2026-06-01 |
| 3 | **education** | 3 (EducationPlan·Preparation·Execution) | ✅ 완료 | 2026-06-01 |
| 4 | **sales** | 6 (ChannelRecruitment·Screening·ActivityPlan·SalesActivity·OrgEvaluation·BonusRequest) | ✅ 완료 | 2026-06-01 |
| 5 | **contract** | 4 (Contract·Cancellation·ContractStatistics·ExpiringContractManagement) | ✅ 완료 | 2026-06-01 |
| 6 | **claim** | 7 (AccidentReport·Dispatch·DispatchRecord·ClaimRequest·DamageInvestigation·ClaimCalculation·ClaimPayment) | ✅ 완료 | 2026-06-01 |
| 7 | **consultation** | 8 (ConsultationRequest·InterviewSchedule·InterviewRecord·Proposal·Underwriting·InsuranceApplication·PolicyApplication·Revival) | ✅ 완료 | 2026-06-01 |

**합계**: 33개 Repository (CustomerRegistration·InsuranceProduct·CustomerRepository 제외 — 업무번호 없음)

### 완료 도메인 상세

#### inquiry (1개)
- `InquiryRepository`: COLS `inquiry_no` 제거, `save()` UPDATE 제거, `findByInquiryNo` → `findById`, mapRow `"INQ"+id` 파생
- `InquiryService`: `parseId()` 추가, `findById(parseId(...))` 2곳

#### payment (4개)
- `PaymentRepository`: `save()` UPDATE 1줄 제거
- `PaymentRecordRepository`: COLS `record_no` 제거, `save()` UPDATE 제거, `update()` → `WHERE id=?`, `findByRecordNo` → `findById`, mapRow `"PRC"+id` 파생
- `RefundCalculationRepository`: `save()` UPDATE 제거, `findByRefundNo` → `findById`, 전 SELECT `refund_no` 제거 → COLS 상수화, mapRow `"RFC"+id` 파생
- `RefundPaymentRepository`: `save()` UPDATE 제거, `findByPaymentNo` → `findById`, 전 SELECT `payment_no` 제거 → COLS 상수화, mapRow `"RPY"+id` 파생
- `PaymentRecordService`: `parseId()` 추가, `findById(parseId(...))` 2곳
- `RefundService`: `parseId()` 추가, `findById(parseId(...))` 4곳

---

## 5. 최종 상태 요약

- 모든 업무번호 저장 컬럼(`xxx_no`)은 제거됐다.
- 단건 API 경로는 기존 업무번호 문자열(`CON00001` 등)을 유지하지만, Service에서 id를 파싱해 Repository `findById`로 조회한다.
- 테이블 간 참조는 `*_id BIGINT` FK로 전환됐다.
- Repository `save()`는 `INSERT → executeInsertReturningKey → 엔터티 업무번호 필드 주입` 순서로 동작하며 DB UPDATE를 수행하지 않는다.

---

## 6. 이후 선택 작업

| 항목 | 내용 |
|---|---|
| **로깅 slf4j 확대** | 도메인 서비스 주요 비즈니스 로그를 `@Slf4j`로. DataSeeder는 이미 적용 ✅ |
| **CORS** | 프론트 도입 시 `@CrossOrigin` 또는 글로벌 CorsConfigurer 추가 |
| **인증·인가** | 현재 역할 구분 없음. Spring Security 도입 여부는 별도 판단 |
| **응답 envelope** | `{ data: ..., code: 200 }` 공통 래퍼 채택 여부는 별도 판단 |

---

## 7. 검증 체크리스트

- [x] `./gradlew compileJava` 그린
- [x] `bootRun` 기동 정상
- [x] 신규 저장 후 목록·단건 조회로 업무번호 파생 확인
- [x] 단건 조회 `/{xxxNo}` 경로 정상 작동
- [x] `docker compose down -v && docker compose up -d` 후 DataSeeder 재적재 확인
