# 배치 3: consultation 도메인 전환 계획

> **목적**: consultation 도메인 8 UC(상담요청·면담일정·면담기록·제안·인수심사·보험가입신청·청약서·부활)를 Spring REST API로 전환한다.
> **방식**: claim 배치와 동일 — PK 파운데이션(스키마+엔터티) 선행 → 서브배치 3a(상담·면담·제안) → 3b(심사·신청·청약·부활) 순으로 진행.
> **현재 상태 (2026-05-31)**: PK 파운데이션 ✅ · 서브배치 3a ✅ · 서브배치 3b ✅ — **배치 3 전체 완료**.
>
> **▶ 다음 작업**: 배치 4 — sales 도메인 (ApiMigrationPlan.md §9 참조)

---

## 1. 대상 범위

| 테이블 | PK 타입 | 조치 | 업무번호 파생 |
|---|---|---|---|
| `consultation_requests` | VARCHAR PK | id BIGINT AUTO_INCREMENT, consult_no UNIQUE | `"CSL"+String.format("%05d",id)` |
| `interview_schedules` | VARCHAR PK | id BIGINT AUTO_INCREMENT, schedule_no UNIQUE | `"SCH"+String.format("%05d",id)` |
| `interview_records` | VARCHAR PK | id BIGINT AUTO_INCREMENT, record_no UNIQUE | `"REC"+String.format("%05d",id)` |
| `proposals` | VARCHAR PK | id BIGINT AUTO_INCREMENT, proposal_no UNIQUE | `"PRO"+String.format("%05d",id)` |
| `underwritings` | VARCHAR PK | id BIGINT AUTO_INCREMENT, underwriting_no UNIQUE | `"UDW"+String.format("%05d",id)` |
| `insurance_applications` | INT PK | id BIGINT AUTO_INCREMENT, application_no VARCHAR(20) UNIQUE | `"APP"+String.format("%05d",id)` |
| `policy_applications` | INT PK | id BIGINT AUTO_INCREMENT, application_no VARCHAR(20) UNIQUE | `"POL"+String.format("%05d",id)` |
| `revivals` | VARCHAR PK | id BIGINT AUTO_INCREMENT, revival_no UNIQUE | `"REV"+String.format("%05d",id)` |
| `insurance_products` | VARCHAR PK(자연키) | id BIGINT AUTO_INCREMENT만 추가, product_name 유지 | id 파생 안 함 (C1타입) |

---

## 2. 엔터티 ↔ DB 컬럼 불일치 정리 (주의)

> 작성 전 반드시 참고. 추측하지 말고 이 표를 기준으로 매핑할 것.

| 엔터티 클래스 | 엔터티 필드 | DB 컬럼 (확정) | 비고 |
|---|---|---|---|
| ConsultationRequest | `type` | `consultation_type` ✅ 컬럼명 통일 | 구 `channel` 에서 변경 |
| ConsultationRequest | `receivedAt` | `received_at` ✅ 컬럼명 통일 | 구 `requested_at` 에서 변경 |
| ConsultationRequest | `consultationNumber`(int) | `consult_no VARCHAR(20)` | 신규: `consultNo`(String) 추가 |
| InterviewSchedule | `type` | `interview_type` ✅ 컬럼명 통일 | 구 `type` 에서 변경 |
| InterviewSchedule | `interviewNumber`(int) | `schedule_no VARCHAR(20)` | 신규: `scheduleNo`(String) 추가 |
| InterviewRecord | `recordNumber`(int) | `record_no VARCHAR(20)` | 신규: `recordNo`(String) 추가 |
| InterviewRecord | `modifiedAt` | `modified_at` ✅ 컬럼 추가 | 구 스키마 누락 → 추가됨 |
| Proposal | `proposalId`(int) | `proposal_no VARCHAR(20)` | 신규: `proposalNo`(String) 추가 |
| Proposal | `sentAt` | `sent_at` ✅ 컬럼명 통일 | 구 `created_at` 에서 변경 |
| Underwriting | `reviewNumber`(int) | `underwriting_no VARCHAR(20)` | 신규: `underwritingNo`(String) 추가 |
| Underwriting | `reviewType` | `review_type` ✅ 컬럼명 통일 | 구 `app_type` 에서 변경 |
| Underwriting | `reviewResult.result` | `result` | |
| Underwriting | `reviewResult.condition` | `result_condition` ✅ 컬럼 추가 | 구 스키마 누락 → 추가됨 |
| Underwriting | `reviewResult.rejectionReason` | `rejection_reason` ✅ 컬럼 추가 | 구 스키마 누락 → 추가됨 |
| InsuranceApplication | `applicationNumber`(int) | `application_no VARCHAR(20)` | 구 INT → VARCHAR 변경 |
| InsuranceApplication | `selectedSpecialTerms` | *컬럼 없음* | DB 저장 안 함 |
| PolicyApplication | `applicationNumber`(int) | `application_no VARCHAR(20)` | 구 INT → VARCHAR 변경 |
| Revival | `revivalNumber`(int) | `revival_no VARCHAR(20)` | 신규: `revivalNo`(String) 추가 |
| Revival | `appliedAt` | `revived_at` | schema.sql 원본 유지 (변경 미반영) |
| InsuranceProduct | `type` | `category` | 자연키 테이블, 컬럼명 유지 |
| InsuranceProduct | `coverage` | `coverage_summary` | |
| InsuranceProduct | `specialTerms` | `exclusion_summary` | |

---

## 3. 설계 결정 필요 사항

### D1. underwritings 테이블 — condition/rejection_reason 컬럼 추가 ✅ 완료
`result_condition TEXT NULL`, `rejection_reason TEXT NULL` 컬럼 추가 확정 및 schema.sql 반영 완료.

### D2. insurance_applications/policy_applications — application_no 타입 변경 ✅ 완료
`INT PRIMARY KEY` → `id BIGINT AUTO_INCREMENT PK` + `application_no VARCHAR(20) UNIQUE` 변경 완료. 레거시 Runner 은퇴 처리.

### D3. 부활(Revival) 미납금 산출 ✅ 결정
클라이언트가 요청 바디에 `unpaid_amount`를 직접 전달. 서버는 저장만 담당.

### D4. 인수심사 UnderwritingRequest — applicationType 명시 ✅ 결정
심사 대기 목록 응답의 `applicationType`("청약"/"보험신청")을 클라이언트가 보관하여 POST 시 함께 전송. prefix 기반 암묵적 분기 대신 명시적 라우팅 채택 (클라이언트 주도 무상태 패턴 일관성).

### D5. 컬럼명 통일 ✅ 완료
엔터티 필드명 기준으로 DB 컬럼명 통일 및 누락 컬럼 추가 (§2 표 참조). 레거시 DAO는 런타임에만 영향 (컴파일 유지, Runner 은퇴로 허용).

---

## 4. 엔드포인트 설계 (서브배치 3a/3b)

### 서브배치 3a — 상담요청·면담일정·면담기록·제안

| HTTP | 경로 | 설명 | 시나리오 |
|---|---|---|---|
| POST | `/api/consultations` | 상담 신청 | Basic 5 |
| GET | `/api/consultations` | 상담 목록 | Basic 2 |
| GET | `/api/consultations/{consultNo}` | 상담 상세 | Basic 8 |
| POST | `/api/consultations/{consultNo}/accept` | 상담 수락 | Basic 9 |
| GET | `/api/interview-schedules` | 면담일정 목록 | Basic 2 |
| POST | `/api/interview-schedules` | 면담 등록 | A1 |
| GET | `/api/interview-schedules/{scheduleNo}` | 면담 상세 | Basic 6 |
| PUT | `/api/interview-schedules/{scheduleNo}` | 면담 수정 | A4 |
| POST | `/api/interview-schedules/{scheduleNo}/cancel` | 면담 취소 | A5 |
| GET | `/api/interview-records` | 면담기록 목록 | Basic 2 |
| POST | `/api/interview-records` | 면담기록 등록 | A1 |
| PUT | `/api/interview-records/{recordNo}` | 면담기록 수정 | A3 |
| GET | `/api/insurance-products` | 보험상품 목록 | Proposal/Application 공용 |
| POST | `/api/proposals` | 제안서 발송 | Basic 5 |

### 서브배치 3b — 인수심사·보험가입신청·청약서·부활

| HTTP | 경로 | 설명 | 시나리오 |
|---|---|---|---|
| GET | `/api/underwriting/pending` | 심사 대기 목록(청약+신청 통합) | Basic 2·E1 |
| POST | `/api/underwriting` | 심사 완료 저장 | Basic 7~9 |
| GET | `/api/insurance-products` | 상품 목록 (공용) | — |
| POST | `/api/insurance-applications` | 보험 신청 | Basic 7~10 |
| POST | `/api/policy-applications` | 청약서 제출 | Basic 9 |
| GET | `/api/customers/{id}/contracts` | 부활 대상 계약 목록 | (기존 엔드포인트 활용) |
| POST | `/api/revivals` | 부활 신청 | Basic 7~10 |

---

## 5. PK 파운데이션 schema.sql 변경 명세

아래 각 테이블에 대해:
1. 기존 PK 선언 제거 (DROP PRIMARY KEY)
2. `id BIGINT AUTO_INCREMENT PRIMARY KEY` 추가
3. 기존 업무키 `UNIQUE` 강등

```sql
-- consultation_requests
ALTER TABLE consultation_requests
  DROP PRIMARY KEY,
  ADD id BIGINT AUTO_INCREMENT PRIMARY KEY FIRST,
  ADD UNIQUE KEY uk_consult_no (consult_no);

-- interview_schedules
ALTER TABLE interview_schedules
  DROP PRIMARY KEY,
  ADD id BIGINT AUTO_INCREMENT PRIMARY KEY FIRST,
  ADD UNIQUE KEY uk_schedule_no (schedule_no);

-- interview_records
ALTER TABLE interview_records
  DROP PRIMARY KEY,
  ADD id BIGINT AUTO_INCREMENT PRIMARY KEY FIRST,
  ADD UNIQUE KEY uk_record_no (record_no);

-- proposals
ALTER TABLE proposals
  DROP PRIMARY KEY,
  ADD id BIGINT AUTO_INCREMENT PRIMARY KEY FIRST,
  ADD UNIQUE KEY uk_proposal_no (proposal_no);

-- underwritings: result_condition, rejection_reason 컬럼 추가 (D1 결정)
ALTER TABLE underwritings
  DROP PRIMARY KEY,
  ADD id BIGINT AUTO_INCREMENT PRIMARY KEY FIRST,
  ADD UNIQUE KEY uk_underwriting_no (underwriting_no),
  ADD COLUMN result_condition TEXT NULL,
  ADD COLUMN rejection_reason TEXT NULL;

-- insurance_applications: application_no INT → VARCHAR(20) (D2 결정)
ALTER TABLE insurance_applications
  DROP PRIMARY KEY,
  MODIFY application_no VARCHAR(20),
  ADD id BIGINT AUTO_INCREMENT PRIMARY KEY FIRST,
  ADD UNIQUE KEY uk_insurance_app_no (application_no);

-- policy_applications: application_no INT → VARCHAR(20) (D2 결정)
ALTER TABLE policy_applications
  DROP PRIMARY KEY,
  MODIFY application_no VARCHAR(20),
  ADD id BIGINT AUTO_INCREMENT PRIMARY KEY FIRST,
  ADD UNIQUE KEY uk_policy_app_no (application_no);

-- revivals
ALTER TABLE revivals
  DROP PRIMARY KEY,
  ADD id BIGINT AUTO_INCREMENT PRIMARY KEY FIRST,
  ADD UNIQUE KEY uk_revival_no (revival_no);

-- insurance_products: 자연키 유지, id만 추가 (C1타입)
ALTER TABLE insurance_products
  ADD id BIGINT AUTO_INCREMENT UNIQUE KEY FIRST;
```

> **실제 반영은 schema.sql CREATE TABLE에 직접 반영한다.** ALTER는 설명용.
> 변경 후 `docker compose down -v && docker compose up -d` 필수.

---

## 6. 엔터티 수정 명세 (PK 파운데이션)

각 엔터티에 공통으로 추가:
- `private Long id;`
- `public Long getId() { return id; }`
- `public void setId(Long id) { this.id = id; }`
- 업무번호 String 필드 + getter + setter (기존 int 필드는 레거시용으로 유지)

| 엔터티 | 추가 필드 | setter명 |
|---|---|---|
| ConsultationRequest | `String consultNo` | `setConsultNo(String)` |
| InterviewSchedule | `String scheduleNo` | `setScheduleNo(String)` |
| InterviewRecord | `String recordNo` | `setRecordNo(String)` |
| Proposal | `String proposalNo` | `setProposalNo(String)` |
| Underwriting | `String underwritingNo` | `setUnderwritingNo(String)` |
| InsuranceApplication | `String applicationNo` | `setApplicationNo(String)` |
| PolicyApplication | `String applicationNo` | `setApplicationNo(String)` |
| Revival | `String revivalNo` | `setRevivalNo(String)` |
| InsuranceProduct | (id만, 업무번호 파생 없음) | — |

---

## 7. Repository save() 패턴 (claim 방식 동일)

```
① INSERT (업무번호 컬럼 제외)
② executeInsertReturningKey → id 회수
③ setId(id) + setXxxNo("PREFIX" + String.format("%05d", id))
④ UPDATE SET xxx_no=? WHERE id=?
```

insurance_products는 자연키이므로 upsert (INSERT ON DUPLICATE KEY UPDATE) 유지, id 파생 없음.

---

## 8. 서브배치 3a 진행 현황 ✅ 완료 (2026-05-31)

| UC | Controller | Service | Repository | DTO | 검증 |
|---|---|---|---|---|---|
| 상담 요청/수락 | ✅ | ✅ | ✅ | ✅ | ✅ |
| 면담일정 관리 | ✅ | ✅ | ✅ | ✅ | ✅ |
| 면담기록 관리 | ✅ | ✅ | ✅ | ✅ | ✅ |
| 보험상품 조회 | ✅ | ✅ | ✅ | ✅ | ✅ |
| 제안서 발송 | ✅ | ✅ | ✅ | ✅ | ⏳ 시더 추가 후 검증 예정 |

**3a 검증 결과 (2026-05-31)**
- `POST /api/consultations` → CSL00001 생성, status=접수
- `POST /api/consultations/CSL00001/accept` → status=수락, acceptedAt 기록
- E1 필수항목 누락 → 400 에러 DTO 정상
- `POST /api/interview-schedules` → SCH00001 생성
- `PUT /api/interview-schedules/SCH00001` → modifiedAt 기록
- `POST /api/interview-schedules/SCH00001/cancel` → status=취소, cancelledAt 기록
- 이중 취소 방지 → 400
- `POST /api/interview-records` → REC00001 생성
- 없는 자원 → 404

---

## 9. 서브배치 3b 진행 현황 ✅ 완료 (2026-05-31)

| UC | Controller | Service | Repository | DTO | 검증 |
|---|---|---|---|---|---|
| 인수심사 | ✅ | ✅ | ✅ | ✅ | ✅ |
| 보험가입신청 | ✅ | ✅ | ✅ | ✅ | ✅ |
| 청약서 제출 | ✅ | ✅ | ✅ | ✅ | ✅ |
| 부활 신청 | ✅ | ✅ | ✅ | ✅ | ✅ |

**3b 검증 결과 (2026-05-31)**
- `POST /api/insurance-applications` → APP00001 생성, status=신청
- `POST /api/policy-applications` → POL00001 생성, status=신청
- `GET /api/underwriting/pending` → 청약+보험신청 통합 목록 (applicationType 포함)
- `POST /api/underwriting` (청약·승인) → UDW00001, pending에서 제거 확인
- `POST /api/underwriting` (보험신청·조건부승인) → UDW00002, resultCondition 저장 확인
- `POST /api/underwriting` (applicationType 누락) → 400 에러 DTO 정상
- `POST /api/revivals` → REV00001 생성, appliedAt 기록
- 없는 계약으로 부활 신청 → 404
- DataSeeder 보험상품 3개(실손의료보험·종신보험·자동차보험) 추가 완료

**버그 수정**: `RevivalRepository` INSERT에서 `applied_at` → `revived_at` (schema.sql 컬럼명 불일치)

---

## 10. 완료 기준

각 서브배치마다:
1. `./gradlew compileJava` 그린
2. `docker compose down -v && docker compose up -d` (스키마 변경 시)
3. `./gradlew bootRun` 기동
4. 정상 흐름 + 예외 분기 실제 API 호출로 검증
5. 이 문서 진행 현황 ✅ 업데이트