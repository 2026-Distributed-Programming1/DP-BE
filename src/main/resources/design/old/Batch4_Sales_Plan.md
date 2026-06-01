# 배치 4: sales 도메인 전환 계획

> **목적**: sales 도메인 7개 UC(모집 공고·채용 심사·활동 계획·고객등록·영업 활동 관리·영업 조직 평가·성과급 요청)를 Spring REST API로 전환한다.
> **방식**: claim·consultation 배치와 동일 — PK 파운데이션(스키마+엔터티) 선행 → 서브배치 4a(채용) → 4b(활동) → 4c(관리·평가·성과급) 순 진행.
> **현재 상태 (2026-05-31)**: PK 파운데이션 ✅ · 서브배치 4a ✅ · 서브배치 4b ✅ · 서브배치 4c ✅ — **배치 4 전체 완료**.
>
> **현재 기준 (2026-06-01)**: 최종 수렴 완료. 업무번호 저장 컬럼은 제거됐고, 업무번호는 `id`에서 format-on-read로 파생한다. FK는 `id(BIGINT)` 기반이다.
>
> **문서 성격**: 배치 4 전환 당시의 상세 기록이다. 레거시/저장형 표현은 당시 이력이며 현재 구현 기준은 `ApiMigrationPlan.md`와 `Convergence_Progress.md`를 따른다.

---

## 1. 대상 범위

| 테이블 | 현행 PK | 조치 | 업무번호 파생 |
|---|---|---|---|
| `channel_recruitments` | VARCHAR PK `recruitment_no` | id BIGINT AUTO_INCREMENT, recruitment_no UNIQUE | `"RCT"+String.format("%05d",id)` |
| `channel_screenings` | VARCHAR PK `screening_no` | id BIGINT AUTO_INCREMENT, screening_no UNIQUE | `"SCN"+String.format("%05d",id)` |
| `activity_plans` | VARCHAR PK `plan_no` | id BIGINT AUTO_INCREMENT, plan_no UNIQUE | `"APL"+String.format("%05d",id)` |
| `sales_activity_managements` | VARCHAR PK `activity_no` | id BIGINT AUTO_INCREMENT, activity_no UNIQUE | `"SAM"+String.format("%05d",id)` |
| `sales_org_evaluations` | VARCHAR PK `evaluation_no` | id BIGINT AUTO_INCREMENT, evaluation_no UNIQUE | `"EVL"+String.format("%05d",id)` |
| `bonus_requests` | VARCHAR PK `request_no` | id BIGINT AUTO_INCREMENT, request_no UNIQUE | `"BNS"+String.format("%05d",id)` |
| `activity_schedule_items` | 이미 id 보유 | 불필요 | — |
| `customer_registrations` | 이미 id 보유 | 불필요 | — |

---

## 2. 엔터티 ↔ DB 컬럼 불일치 정리 (주의)

> 작성 전 반드시 참고. 추측하지 말고 이 표를 기준으로 매핑할 것.

| 엔터티 클래스 | 엔터티 필드/게터 | DB 컬럼 | 비고 |
|---|---|---|---|
| ChannelRecruitment | `condition` / `getCondition()` | `condition_text` | 컬럼명 다름 |
| ChannelRecruitment | `registeredAt` / `getRegisteredAt()` | `created_at` | 컬럼명 다름 |
| ChannelRecruitment | `startDate` / **`getLocalStartDate()`** | `start_date` | getter 이름 주의 |
| ChannelRecruitment | `endDate` / **`getLocalEndDate()`** | `end_date` | getter 이름 주의 |
| ChannelRecruitment | (없음) | `status` | DB에 컬럼 있음, 엔터티 미보유 → INSERT 시 NULL |
| ChannelScreening | `approvalNo` / `getApprovalNo()` | `screening_no` (전환 당시 업무키) | 전환 당시 엔터티에 screeningNo 필드 추가 필요 |
| ChannelScreening | `applicantName` / `getApplicantName()` | `candidate_name` | 컬럼명 다름 |
| ChannelScreening | `career` / `getCareer()` | `qualification` | 컬럼명 다름 |
| ChannelScreening | `certifications` (List&lt;String&gt;) | `certifications TEXT` | DB는 콤마 구분 문자열 |
| ChannelScreening | `approvedAt` / `getApprovedAt()` | `reviewed_at` | 컬럼명 다름 |
| ActivityPlan | `planId` / `getPlanId()` | `plan_no` | 필드명 다름 (내용은 같음) |
| ActivityPlan | `author` / `getAuthor()` | `author_name` | 컬럼명 다름 |
| SalesActivityManagement | `managementNo` / `getManagementNo()` | `activity_no` | 필드명·컬럼명 모두 다름 |
| SalesActivityManagement | `channelType` / **`getActivityType()`** | `activity_type` | getActivityType()이 channelType.name() 반환 |
| SalesActivityManagement | `registeredAt` / `getRegisteredAt()` | `created_at` | 컬럼명 다름 |
| SalesOrgEvaluation | `channelName` / `getChannelName()` | `org_name` | 컬럼명 다름 |
| SalesOrgEvaluation | `achievementRate` / `getAchievementRate()` | `score` | 컬럼명 다름 |
| BonusRequest | `channelName` / `getChannelName()` | `requester` | 컬럼명 다름 |
| BonusRequest | `bonusAmount` (Double) | `amount BIGINT` | 저장 시 정수 금액으로 변환 |

---

## 3. 설계 결정 사항

### D1. ChannelScreening — screeningNo 필드 추가
전환 당시 엔터티에 `screeningNo` 필드가 없어 surrogate-PK 패턴에 따라 `screeningNo = "SCN" + id`로 파생하도록 추가했다.
→ **확정**: 엔터티에 `String screeningNo` + getter/setter 추가.

### D2. ActivityPlan — 임시저장 vs 제출 흐름
REST 설계에서는 단일 `POST /api/activity-plans`에 `status` 필드(`TEMP_SAVE` / `UNDER_REVIEW`)를 포함해 한 번에 처리.
→ **확정**: 클라이언트가 status를 명시해서 전송. 서버는 저장 + 경우에 따라 manager 알림만 담당.

### D3. ActivityPlan — ScheduleItem 자식 저장
`activity_schedule_items`는 `activity_plans`의 자식 테이블. 활동 계획 POST 시 일정 목록을 함께 받아 트랜잭션 내에서 한 번에 INSERT.
→ **확정**: 요청 DTO에 `List<ScheduleItemRequest>` 포함, Service에서 plan 저장 후 items 순차 INSERT.

### D4. BonusRequest — baseSalary 출처
REST 환경에서는 기본급을 클라이언트가 입력값으로 전달.
→ **확정**: 요청 바디에 `baseSalary` 포함. 서버는 `bonusAmount = baseSalary * bonusRatio` 계산 후 저장.

### D5. CustomerRegistration — 배치 4 포함 여부
`customer_registrations`는 이미 id 보유(PK 파운데이션 불필요). UC 전환만 필요.
→ **확정**: 서브배치 4a에 포함 (채용 흐름과 같은 판매채널 액터).

### D6. SalesOrgEvaluation — score vs achievementRate 컬럼명
`score` 컬럼을 `achievementRate`에 매핑한다.
→ **확정**: 신규 Repository는 `score` 컬럼 그대로 사용, 매핑 시 achievementRate에 바인딩.

---

## 4. 엔드포인트 설계

### 서브배치 4a — 모집 공고 · 채용 심사 · 고객 정보 등록

| HTTP | 경로 | 설명 |
|---|---|---|
| POST | `/api/channel-recruitments` | 모집 공고 등록 |
| GET | `/api/channel-recruitments` | 모집 공고 목록 |
| GET | `/api/channel-screenings` | 지원자(심사) 목록 |
| POST | `/api/channel-screenings` | 지원자 등록 |
| POST | `/api/channel-screenings/{screeningNo}/approve` | 지원자 승인 |
| POST | `/api/channel-screenings/{screeningNo}/reject` | 지원자 거절 |
| POST | `/api/customer-registrations` | 고객 정보 등록 |
| GET | `/api/customer-registrations` | 등록 고객 목록 |

### 서브배치 4b — 활동 계획

| HTTP | 경로 | 설명 |
|---|---|---|
| GET | `/api/activity-plans` | 활동 계획 목록 |
| POST | `/api/activity-plans` | 활동 계획 생성 (임시저장·제출 status로 구분) |
| GET | `/api/activity-plans/{planNo}` | 활동 계획 상세 (일정 목록 포함) |

### 서브배치 4c — 영업 활동 관리 · 평가 · 성과급

| HTTP | 경로 | 설명 |
|---|---|---|
| GET | `/api/sales-activity-managements` | 영업 활동 현황 목록 |
| POST | `/api/sales-activity-managements` | 개선 지시 + 수정 목표 저장 |
| GET | `/api/sales-org-evaluations` | 영업 조직 평가 목록 |
| POST | `/api/sales-org-evaluations` | 평가 등록 |
| POST | `/api/bonus-requests` | 성과급 요청 |

---

## 5. PK 파운데이션 schema.sql 변경 명세

> **실제 반영은 schema.sql CREATE TABLE에 직접 반영한다.** ALTER는 설명용.
> 변경 후 `docker compose down -v && docker compose up -d` 필수.

```sql
-- channel_recruitments
ALTER TABLE channel_recruitments
  DROP PRIMARY KEY,
  ADD id BIGINT AUTO_INCREMENT PRIMARY KEY FIRST,
  ADD UNIQUE KEY uk_recruitment_no (recruitment_no);

-- channel_screenings
ALTER TABLE channel_screenings
  DROP PRIMARY KEY,
  ADD id BIGINT AUTO_INCREMENT PRIMARY KEY FIRST,
  ADD UNIQUE KEY uk_screening_no (screening_no);

-- activity_plans
ALTER TABLE activity_plans
  DROP PRIMARY KEY,
  ADD id BIGINT AUTO_INCREMENT PRIMARY KEY FIRST,
  ADD UNIQUE KEY uk_plan_no (plan_no);

-- sales_activity_managements
ALTER TABLE sales_activity_managements
  DROP PRIMARY KEY,
  ADD id BIGINT AUTO_INCREMENT PRIMARY KEY FIRST,
  ADD UNIQUE KEY uk_activity_no (activity_no);

-- sales_org_evaluations
ALTER TABLE sales_org_evaluations
  DROP PRIMARY KEY,
  ADD id BIGINT AUTO_INCREMENT PRIMARY KEY FIRST,
  ADD UNIQUE KEY uk_evaluation_no (evaluation_no);

-- bonus_requests
ALTER TABLE bonus_requests
  DROP PRIMARY KEY,
  ADD id BIGINT AUTO_INCREMENT PRIMARY KEY FIRST,
  ADD UNIQUE KEY uk_request_no (request_no);

-- activity_schedule_items, customer_registrations: 이미 id 보유 → 불필요
```

---

## 6. 엔터티 수정 명세 (PK 파운데이션)

공통 추가: `private Long id;` + `getId()` + `setId(Long)`

| 엔터티 | 추가 필드 | setter명 | 비고 |
|---|---|---|---|
| ChannelRecruitment | `String recruitmentNo` (필드는 있음) | `setRecruitmentNo(String)` 추가 | getter `getRecruitmentNo()` 이미 있음 |
| ChannelScreening | `String screeningNo` 신규 | `setScreeningNo(String)` 추가 | — |
| ActivityPlan | `planId` 필드 있음 | `setPlanId(String)` 이미 있음 | DB 컬럼명은 `plan_no` |
| SalesActivityManagement | `managementNo` 필드 있음 | `setManagementNo(String)` 이미 있음 | DB 컬럼명은 `activity_no` |
| SalesOrgEvaluation | `evaluationNo` 필드 있음 | `setEvaluationNo(String)` 이미 있음 | — |
| BonusRequest | `requestNo` 필드 있음 | `setRequestNo(String)` 추가 | getter `getRequestNo()` 이미 있음 |

---

## 7. Repository save() 패턴

claim·consultation 방식과 동일:

```
① INSERT (업무번호 컬럼 제외)
② executeInsertReturningKey → id 회수
③ setId(id) + setXxxNo("PREFIX" + String.format("%05d", id))
④ 최종 수렴 후 DB UPDATE 없음 — 업무번호는 엔터티/응답에서만 파생
```

- `activity_schedule_items`: plan 저장 후 plan_no 확정 시점에 일반 INSERT (id 파생 없음, plan_no FK만 바인딩)
- `customer_registrations`: id 이미 보유, upsert 방식 유지

---

## 8. 서브배치 4a 진행 현황

| UC | Controller | Service | Repository | DTO | 검증 |
|---|---|---|---|---|---|
| 모집 공고 등록/목록 | ✅ | ✅ | ✅ | ✅ | ✅ |
| 채용 심사(등록/승인/거절) | ✅ | ✅ | ✅ | ✅ | ✅ |
| 고객 정보 등록/목록 | ✅ | ✅ | ✅ | ✅ | ✅ |

**4a 검증 결과 (2026-05-31)**
- `POST /api/channel-recruitments` → RCT00001 생성, registeredAt 기록
- `GET /api/channel-recruitments` 목록 확인
- 필수항목 누락 → 400 에러 DTO 정상
- `POST /api/channel-screenings` → SCN00001 생성, status=PENDING, certifications 콤마 변환
- `POST /api/channel-screenings/SCN00001/approve` → APPROVED, approvalNo·reviewedAt 기록
- 이중 승인 → 400, 없는 번호 → 404
- `POST /api/channel-screenings/SCN00002/reject` → REJECTED, rejectionReason·reviewedAt 기록
- `POST /api/customer-registrations` → CRG00001, SSN 마스킹 정상(하이픈 제거 후 저장)
- `GET /api/customer-registrations` 목록 확인

---

## 9. 서브배치 4b 진행 현황

| UC | Controller | Service | Repository | DTO | 검증 |
|---|---|---|---|---|---|
| 활동 계획 생성/목록/상세 | ✅ | ✅ | ✅ | ✅ | ✅ |

**4b 검증 결과 (2026-05-31)**
- `POST /api/activity-plans` → APL00001 생성, ScheduleItem 2개 트랜잭션 내 함께 저장
- `GET /api/activity-plans/APL00001` → 상세 + 일정 목록 포함
- 없는 번호 → 404, 필수항목 누락 → 400

---

## 10. 서브배치 4c 진행 현황

| UC | Controller | Service | Repository | DTO | 검증 |
|---|---|---|---|---|---|
| 영업 활동 관리 | ✅ | ✅ | ✅ | ✅ | ✅ |
| 영업 조직 평가 | ✅ | ✅ | ✅ | ✅ | ✅ |
| 성과급 요청 | ✅ | ✅ | ✅ | ✅ | ✅ |

**4c 검증 결과 (2026-05-31)**
- `POST /api/sales-activity-managements` → SAM00001 생성, conversionRate 자동 계산
- `GET /api/sales-activity-managements` 목록 확인
- `POST /api/sales-org-evaluations` → EVL00001 생성, S등급 기록
- `GET /api/sales-org-evaluations` 목록 확인
- `POST /api/bonus-requests` S등급 → BNS00001, bonusAmount=baseSalary×1.5 계산 정상
- B등급 이하 요청 → 400

---

## 11. 완료 기준

각 서브배치마다:
1. `./gradlew compileJava` 그린
2. `docker compose down -v && docker compose up -d` (스키마 변경 시)
3. `./gradlew bootRun` 기동
4. 정상 흐름 + 예외 분기 실제 API 호출로 검증
5. 이 문서 진행 현황 ✅ 업데이트
