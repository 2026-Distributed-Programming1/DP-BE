# 배치 5: education + inquiry + 마스터 도메인 전환 계획

> **목적**: education 도메인 3개 UC(교육 계획안 작성·교육 제반 등록·교육 진행), inquiry 도메인 1개 UC(문의), actor 마스터 8개 테이블 surrogate-PK 적용을 Spring REST API로 전환한다.
> **방식**: 이전 배치와 동일 — 스키마 컬럼명 통일 → PK 파운데이션(스키마+엔터티) → 서브배치 5a(교육계획안) → 5b(교육제반+진행) → 5c(문의) 순 진행.
> **현재 상태 (2026-05-31)**: 컬럼명 통일 ✅ · PK 파운데이션 ✅ · 서브배치 5a ✅ · 서브배치 5b ✅ · 서브배치 5c ✅ — **배치 5 전체 완료**.
>
> **▶ 다음 작업**: 최종 수렴 — `old/`(DBA·DAO·Runner) 삭제 + format-on-read 전환, 또는 CORS/인증·인가 추가 (ApiMigrationPlan.md §9 참조)

---

## 1. 대상 범위

### 1-A. C2 테이블 (업무번호 id 파생)

| 테이블 | 현행 PK | 조치 | 업무번호 파생 |
|---|---|---|---|
| `education_plans` | VARCHAR PK `plan_no` | id BIGINT AUTO_INCREMENT, plan_no UNIQUE | `"PLN"+String.format("%05d",id)` |
| `education_preparations` | VARCHAR PK `prep_no` | id BIGINT AUTO_INCREMENT, prep_no UNIQUE | `"PRP"+String.format("%05d",id)` |
| `education_executions` | VARCHAR PK `execution_no` | id BIGINT AUTO_INCREMENT, execution_no UNIQUE | `"EXC"+String.format("%05d",id)` |
| `education_attendances` | 이미 id 보유 | 불필요 | — |
| `inquiries` | VARCHAR PK `inquiry_no` | id BIGINT AUTO_INCREMENT, inquiry_no UNIQUE | `"INQ"+String.format("%05d",id)` |

### 1-B. C1 테이블 (자연키 보존, id만 추가)

| 테이블 | 자연키 | 조치 |
|---|---|---|
| `education_trainers` | `employee_id VARCHAR(20)` | id BIGINT AUTO_INCREMENT PRIMARY KEY, employee_id UNIQUE NOT NULL |
| `sales_managers` | `manager_id VARCHAR(50)` | id BIGINT AUTO_INCREMENT PRIMARY KEY, manager_id UNIQUE NOT NULL |
| `insurance_reviewers` | `employee_id VARCHAR(20)` | id BIGINT AUTO_INCREMENT PRIMARY KEY, employee_id UNIQUE NOT NULL |
| `claims_handlers` | `employee_id VARCHAR(20)` | id BIGINT AUTO_INCREMENT PRIMARY KEY, employee_id UNIQUE NOT NULL |
| `dispatch_agents` | `employee_id VARCHAR(20)` | id BIGINT AUTO_INCREMENT PRIMARY KEY, employee_id UNIQUE NOT NULL |
| `finance_managers` | `employee_id VARCHAR(20)` | id BIGINT AUTO_INCREMENT PRIMARY KEY, employee_id UNIQUE NOT NULL |
| `designers` | `channel_id VARCHAR(20)` | id BIGINT AUTO_INCREMENT PRIMARY KEY, channel_id UNIQUE NOT NULL |
| `agencies` | `channel_id VARCHAR(20)` | id BIGINT AUTO_INCREMENT PRIMARY KEY, channel_id UNIQUE NOT NULL |

---

## 2. 스키마 컬럼명 통일 (작업 전 선행)

교육 도메인 엔터티와 DB 컬럼명 불일치가 발견돼 PK 파운데이션 전에 먼저 정리했다.

### education_plans 컬럼명 변경

| 변경 전 (DB) | 변경 후 (DB) | 엔터티 필드 | 이유 |
|---|---|---|---|
| `title` | `education_name` | `educationName` | `title`은 범용적, 저장 내용은 명확히 "교육명" |
| `target_audience` | `channel_type` | `channelType` | 실제 저장값이 "설계사/대리점/TM". `target_audience`는 오해 소지 |
| `scheduled_date` | `start_date` | `startDate` | `end_date`와 쌍이므로 `start_date`가 일관됨 |

### education_preparations 컬럼명 변경

| 변경 전 (DB) | 변경 후 (DB) | 엔터티 필드 | 이유 |
|---|---|---|---|
| `trainer_name` | `instructor_name` | `instructorName` | `education_plans`의 `trainer_name`(담당자)과 혼동 방지. 이건 강사(instructor) |

### 엔터티 필드명 변경 (DB를 따름)

| 엔터티 | 변경 전 필드 | 변경 후 필드 | 연관 getter |
|---|---|---|---|
| `EducationPreparation` | `location` | `venue` | `getLocation()` → `getVenue()` |
| `EducationExecution` | `completedAt` | `executedAt` | `getCompletedAt()` → `getExecutedAt()` |

> **레거시 DAO·Runner도 함께 수정 완료**: `EducationPlanDAO`, `EducationPreparationDAO`, `EducationExecutionDAO`, `EducationExecutionRunner`, `EducationPreparationRunner`

---

## 3. 스키마 신규 컬럼 추가

기존에 엔터티에는 있으나 DB에 저장 안 되던 필드, 신규 UC 요구사항을 반영해 추가.

| 테이블 | 추가 컬럼 | 이유 |
|---|---|---|
| `education_preparations` | `additional_notice TEXT` | UC 시나리오의 "기타 준비사항(선택)" 필드. 엔터티에 있었지만 레거시가 저장 안 함 |
| `education_executions` | `total_count INT DEFAULT 0` | 출석/전체 인원을 분리 저장. 레거시는 `attendee_count`만 있어 전체인원 소실 |

---

## 4. 엔터티 추가 내용 (PK 파운데이션)

C2 테이블 엔터티에 추가:
- `Long id` + `getId()` / `setId(Long)`
- 업무번호 String 필드 + getter/setter (레거시 int sequence 필드는 무수정 유지)

| 엔터티 | 추가 필드 | 업무번호 setter |
|---|---|---|
| `EducationPlan` | `Long id`, `String planNo` | `setPlanNo(String)` |
| `EducationPreparation` | `Long id`, `String prepNo` | `setPrepNo(String)` |
| `EducationExecution` | `Long id`, `String executionNo` | `setExecutionNo(String)` |
| `Inquiry` | `Long id` | — (`inquiryNo` 필드+setter 기존에 존재) |

---

## 5. 서브배치 5a — education-plan API

### 5a 엔드포인트

| HTTP | 경로 | 설명 | 예외 |
|---|---|---|---|
| `GET` | `/api/education-plans` | 목록 (status 필터) | — |
| `GET` | `/api/education-plans/{planNo}` | 상세 | 없으면 404 |
| `POST` | `/api/education-plans` | 제출 (action: `TEMP_SAVE` \| `REQUEST_APPROVAL`) | 필수항목 누락 시 400 |
| `POST` | `/api/education-plans/{planNo}/approve` | 승인 | 없으면 404, 승인요청 아닌 상태면 400 |
| `POST` | `/api/education-plans/{planNo}/reject` | 반려 (reason 필수) | 없으면 404, 승인요청 아닌 상태면 400, 사유 없으면 400 |

### 5a 설계 결정

**D1. 임시저장 vs 승인요청 단일 엔드포인트**
요청 바디의 `action` 필드(`TEMP_SAVE` / `REQUEST_APPROVAL`)로 구분. `REQUEST_APPROVAL` 시 필수항목 검증 수행, `TEMP_SAVE`는 통과.

**D2. 상태 값 (한글 문자열)**
엔터티의 상태 메서드(`tempSave()`, `requestApproval()`, `approve()`, `reject()`)가 한글 상태값("임시저장", "승인요청", "승인", "반려") 사용. 신규 경로도 동일 문자열 유지. 상태 필터 쿼리 파라미터는 URL 인코딩 필요.

### 5a 검증 결과

```
POST /api/education-plans (TEMP_SAVE)   → PLN00001, status: 임시저장 ✅
POST /api/education-plans (REQUEST_APPROVAL) → PLN00002, status: 승인요청 ✅
GET  /api/education-plans               → 전체 목록 2건 ✅
GET  /api/education-plans?status=승인요청 → 1건 필터 ✅
POST /api/education-plans/PLN00002/approve → status: 승인, approvedAt: 설정됨 ✅
POST /api/education-plans/PLN00003/reject (reason) → status: 반려, rejectReason 저장 ✅
POST /api/education-plans/PLN00001/approve (임시저장 상태) → 400 ✅
GET  /api/education-plans/PLN99999      → 404 ✅
```

---

## 6. 서브배치 5b — education-preparation + execution API

### 5b 엔드포인트

| HTTP | 경로 | 설명 | 예외 |
|---|---|---|---|
| `GET` | `/api/education-preparations` | 목록 (planNo 필터) | — |
| `GET` | `/api/education-preparations/{prepNo}` | 상세 | 없으면 404 |
| `POST` | `/api/education-preparations` | 제반 등록 | planNo 없으면 404, 필수항목 누락 400 |
| `GET` | `/api/education-executions` | 목록 (prepNo 필터) | — |
| `GET` | `/api/education-executions/{executionNo}` | 상세 (출석 포함) | 없으면 404 |
| `POST` | `/api/education-executions` | 교육 진행 완료 | prepNo 없으면 404, attendances 없으면 400 |

### 5b 설계 결정

**D3. attendance_list 저장 방식**
`education_preparations.attendance_list` 컬럼은 레거시와 동일하게 쉼표 구분 TEXT로 저장. API 요청/응답은 `List<String>`으로 변환.

**D4. education_attendances 테이블 활용**
`education_executions` 저장 시 각 출석자를 `education_attendances` 테이블에 행 단위로 INSERT. 상세 조회 시 JOIN 없이 별도 쿼리로 로드.

**D5. complete() 메서드 활용**
서비스에서 `EducationPreparation` 셸 객체에 출석 목록(`Attendance(name, attended)`)을 채운 뒤 `EducationExecution.complete()` 호출. `calculateAttendanceCount()`가 자동으로 출석/전체 인원 계산.

**D6. total_count 저장**
레거시에는 없던 `total_count` 컬럼을 추가해 전체 인원수를 분리 저장. `attendee_count`는 출석 인원, `total_count`는 전체 인원.

**D7. additional_notice 컬럼 추가**
UC 시나리오의 "기타 준비사항(선택)" 필드. 엔터티에는 있었으나 레거시가 저장하지 않아 `additional_notice TEXT` 컬럼 신규 추가.

### 5b 검증 결과

```
POST /api/education-preparations (planNo=PLN00001) → PRP00001, attendees: [김교육, 이교육, 박교육] ✅
GET  /api/education-preparations                   → 목록 1건 ✅
GET  /api/education-preparations?planNo=PLN00001   → 필터 1건 ✅
POST /api/education-executions (3명, 2명 출석)      → EXC00001, attendeeCount:2/totalCount:3 ✅
GET  /api/education-executions/EXC00001            → 출석 상세 포함 ✅
POST /api/education-executions (없는 prepNo)        → 404 ✅
POST /api/education-preparations (없는 planNo)      → 404 ✅
```

---

## 7. 서브배치 5c — inquiry API

### 5c 엔드포인트

| HTTP | 경로 | 설명 | 예외 |
|---|---|---|---|
| `GET` | `/api/inquiries` | 목록 (customerName·status 복합 필터) | — |
| `GET` | `/api/inquiries/{inquiryNo}` | 상세 | 없으면 404 |
| `POST` | `/api/inquiries` | 문의 제출 | 필수항목 400, 파일 10MB 초과 400 |
| `POST` | `/api/inquiries/{inquiryNo}/answer` | 답변 등록 | 없으면 404, 이미 답변이면 400, 내용 없으면 400 |

### 5c 설계 결정

**D8. inquiry_no 파생 방식 변경**
레거시 `submit()` 메서드는 `"INQ-" + 타임스탬프` 방식으로 생성. 신규 경로는 surrogate-PK 패턴으로 `"INQ" + String.format("%05d", id)` 파생. `submit()` 메서드는 레거시용으로 유지(무수정).

**D9. status/inquiryType 저장 형식**
enum `.name()` 문자열로 저장 (`PENDING`, `ANSWERED`, `INSURANCE`, `CLAIM` 등). 응답 DTO에서도 enum 이름 그대로 노출.

**D10. 복합 필터**
`customerName`·`status` 각각 단독 또는 조합 필터 지원. 서비스 레이어에서 제공된 파라미터 조합에 따라 repository 메서드 분기.

### 5c 검증 결과

```
POST /api/inquiries (CLAIM 유형)         → INQ00001, status: PENDING ✅
GET  /api/inquiries                      → 목록 1건 ✅
GET  /api/inquiries?customerName=김고객  → 필터 1건 ✅
GET  /api/inquiries?status=ANSWERED      → 답변완료 1건 ✅
POST /api/inquiries/INQ00001/answer      → status: ANSWERED, answerContent 저장 ✅
POST /api/inquiries/INQ00001/answer (재) → 400 (이미 답변완료) ✅
GET  /api/inquiries/INQ99999             → 404 ✅
POST /api/inquiries (유형·제목 누락)      → 400 ✅
```

---

## 8. 신규 파일 목록

```
domain/education/
├─ dto/
│   ├─ EducationPlanRequest.java
│   ├─ EducationPlanRejectRequest.java
│   ├─ EducationPlanResponse.java
│   ├─ EducationPreparationRequest.java
│   ├─ EducationPreparationResponse.java
│   ├─ EducationExecutionRequest.java       (nested: AttendanceRecord)
│   └─ EducationExecutionResponse.java      (nested: AttendanceDetail)
├─ repository/
│   ├─ EducationPlanRepository.java
│   ├─ EducationPreparationRepository.java
│   └─ EducationExecutionRepository.java
├─ service/
│   ├─ EducationPlanService.java
│   ├─ EducationPreparationService.java
│   └─ EducationExecutionService.java
└─ controller/
    ├─ EducationPlanController.java
    ├─ EducationPreparationController.java
    └─ EducationExecutionController.java

domain/inquiry/
├─ dto/
│   ├─ InquiryRequest.java
│   ├─ InquiryAnswerRequest.java
│   └─ InquiryResponse.java
├─ repository/
│   └─ InquiryRepository.java
├─ service/
│   └─ InquiryService.java
└─ controller/
    └─ InquiryController.java
```

---

## 9. 스키마 변경 이력 (배치 5)

| 날짜 | 변경 내용 |
|---|---|
| 2026-05-31 | `education_plans`: `title`→`education_name`, `target_audience`→`channel_type`, `scheduled_date`→`start_date` |
| 2026-05-31 | `education_preparations`: `trainer_name`→`instructor_name` |
| 2026-05-31 | C2 테이블 5개에 `id BIGINT AUTO_INCREMENT PRIMARY KEY` 추가, 업무키 UNIQUE 강등 |
| 2026-05-31 | C1 테이블 8개에 `id BIGINT AUTO_INCREMENT PRIMARY KEY` 추가, 자연키 `UNIQUE NOT NULL` 강등 |
| 2026-05-31 | `education_preparations`: `additional_notice TEXT` 신규 추가 |
| 2026-05-31 | `education_executions`: `total_count INT DEFAULT 0` 신규 추가 |