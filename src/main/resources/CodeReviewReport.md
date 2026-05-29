# 코드 리뷰 리포트 (유스케이스 비교)

> 기준 문서: `Usecase_scenario.md`
> 작성일: 2026-05-28
> BugReport.md·AdditionalBugReport.md에 없는 신규 발견 항목만 기록.
> SequenceSync.java로 이미 처리 중인 static sequence PK 문제는 제외.

---

## 교육 도메인 (education)

### [BUG-EDU-05] EducationPreparationDAO — material_ready에 잘못된 값 삽입·ON DUPLICATE KEY 누락

| 항목 | 내용 |
|------|------|
| 관련 파일 | `dp/dao/EducationPreparationDAO.java:26, 20-21` |
| 심각도 | **중간** |

**문제**
`save()` INSERT에서 `material_ready` 컬럼에 `e.getTextbookStatus() != null`(Boolean 식)을 넣는다.
교재 준비 완료 여부를 textbookStatus null 여부로만 판단하므로 실제 준비 상태를 반영하지 못한다.
또한 `ON DUPLICATE KEY UPDATE` 절에 `material_ready`가 누락되어 upsert 시 갱신되지 않는다.

**수정 방향**
1. `EducationPreparation`에 `boolean materialReady` 필드 + getter/setter 추가.
2. `save()` INSERT에서 `e.isTextbookReady()`(또는 별도 boolean 필드)로 교체.
3. `ON DUPLICATE KEY UPDATE`에 `material_ready=VALUES(material_ready)` 추가.

---

### [BUG-EDU-06] EducationPreparationDAO — findAll()에서 material_ready 컬럼 미조회

| 항목 | 내용 |
|------|------|
| 관련 파일 | `dp/dao/EducationPreparationDAO.java:34-35` |
| 심각도 | **낮음** |

**문제**
`findAll()` SELECT 절에 `material_ready` 컬럼이 없어 DB에서 로드 시 항상 null/false로 복원된다.

**수정 방향**
SELECT에 `material_ready` 추가 후 복원 시 해당 값을 setter로 설정.

---

### [BUG-EDU-07] EducationPreparationRunner — 등록 완료 결과에 등록일시·교육명 누락

| 항목 | 내용 |
|------|------|
| 유스케이스 | 교육 제반을 등록한다 — Basic Path Step 10 |
| 관련 파일 | `dp/runner/usecase/EducationPreparationRunner.java:121-123` |
| 심각도 | **중간** |

**문제**
UC Step 10: "등록번호, **등록일시**, **교육명**, 교육장소" 출력 요구.
현재 Runner는 등록번호·교육장소·강사명만 출력하고 **등록일시**(`preparation.getRegisteredAt()`)와 **교육명**(`selectedPlan.getEducationName()`)이 빠져 있다.

**수정 방향**
```java
ConsoleHelper.printInfo("등록번호: " + preparation.getSetupNumber()
    + " | 등록일시: " + preparation.getRegisteredAt()
    + " | 교육명: " + selectedPlan.getEducationName()
    + " | 교육장소: " + preparation.getLocation());
```

---

### [BUG-EDU-08] EducationExecutionRunner — 완료 결과에 완료일시·교육명 누락

| 항목 | 내용 |
|------|------|
| 유스케이스 | 교육을 진행한다 — Basic Path Step 10 |
| 관련 파일 | `dp/runner/usecase/EducationExecutionRunner.java:116-119` |
| 심각도 | **중간** |

**문제**
UC Step 10: "완료번호, **완료일시**, **교육명**, 출석인원" 출력 요구.
현재 완료번호·출석인원/전체인원만 출력. **완료일시**(`execution.getCompletedAt()`)가 누락.
교육명은 `EducationPlan`에만 존재하여 `EducationExecution`→`EducationPreparation` 경로로는 접근 불가 (연결 경로 미구성).

**수정 방향**
1. 단기: 완료일시 출력 추가(`execution.getCompletedAt()`).
2. 장기: `EducationPreparation`에 `educationName` 필드 추가하여 `EducationPlan` 선택 시 복사.

---

### [BUG-EDU-09] EducationPlanRunner — 승인 완료 결과에 승인일시 누락

| 항목 | 내용 |
|------|------|
| 유스케이스 | 교육 계획안을 작성한다 — Basic Path Step 10 |
| 관련 파일 | `dp/runner/usecase/EducationPlanRunner.java:140-143` |
| 심각도 | **중간** |

**문제**
UC Step 10: "계획번호, **승인일시**, 교육명" 출력 요구.
현재 계획번호·교육명·status만 출력. `EducationPlan`에 `approvedAt` 필드 자체가 없다.

**수정 방향**
BUG-EDU-10과 함께 수정.

---

### [BUG-EDU-10] EducationPlan — approvedAt 필드·DB 컬럼 부재

| 항목 | 내용 |
|------|------|
| 유스케이스 | 교육 계획안을 작성한다 — Basic Path Step 10 |
| 관련 파일 | `dp/education/EducationPlan.java`, `dp/dao/EducationPlanDAO.java`, `schema.sql` |
| 심각도 | **중간** |

**문제**
`EducationPlan.approve()` 호출 시 status만 "승인"으로 변경하고 승인 일시를 기록하지 않는다.
`education_plans` 테이블에도 `approved_at` 컬럼이 없다. `reject_reason` 컬럼은 있으나 `approved_at`이 누락.

**수정 방향**
1. `EducationPlan`에 `LocalDateTime approvedAt` 필드 추가, `approve()`에서 `this.approvedAt = LocalDateTime.now()` 설정.
2. `education_plans` 테이블에 `approved_at TIMESTAMP NULL` 컬럼 추가.
3. `EducationPlanDAO.save()`·`findAll()` 수정.

---

### [BUG-EDU-11] EducationPreparationRunner — 임시저장 계획안 재편집 경로 없음

| 항목 | 내용 |
|------|------|
| 유스케이스 | 교육 계획안을 작성한다 — Alternative A1 (임시저장) |
| 관련 파일 | `dp/runner/usecase/EducationPreparationRunner.java:47-49`, `dp/runner/usecase/EducationPlanRunner.java` |
| 심각도 | **중간** |

**문제**
UC A1: 임시저장 후 나중에 다시 불러와 승인 요청할 수 있어야 한다.
`EducationPreparationRunner`는 status가 "승인"인 계획안만 필터링하여 보여주므로, 임시저장("임시저장") 상태의 계획안은 제반 등록 목록에 노출되지 않는다.
`EducationPlanRunner`에도 임시저장된 계획안을 불러와 재편집하는 분기가 없어, 임시저장은 저장만 되고 후속 처리가 불가능하다.

**수정 방향**
`EducationPlanRunner`에 "임시저장 계획안 불러오기" 메뉴 추가 후 이어서 승인 요청할 수 있는 흐름 구현.

---

### [BUG-EDU-12] EducationExecutionDAO — findAll() 없어서 교육 실시 이력 조회 불가

| 항목 | 내용 |
|------|------|
| 관련 파일 | `dp/dao/EducationExecutionDAO.java` |
| 심각도 | **낮음** |

**문제**
`EducationExecutionDAO`에 `save()`만 있고 `findAll()` 또는 `findByPrepNo()` 등 조회 메서드가 없다.
교육 실시 이력을 다른 유스케이스(예: 수료 이력 확인)에서 조회할 방법이 없다.

**수정 방향**
```java
public static List<EducationExecution> findAll() {
    return DBA.executeQuery(
        "SELECT execution_no, prep_no, trainer_name, executed_at,"
        + " attendee_count, memo, status FROM education_executions",
        rs -> { /* 복원 로직 */ });
}
```

---

## 영업 도메인 (sales)

### [BUG-SAL-07] SalesActivityRunner — 초기 테이블 출력 컬럼 누락

| 항목 | 내용 |
|------|------|
| 관련 파일 | `dp/runner/usecase/SalesActivityRunner.java:72-75` |
| 심각도 | **중간** |

**문제**
UC step 2는 테이블 컬럼으로 `채널명 / 방문건수 / 계약건수 / 전환율 / 목표달성률`을 요구하지만, 실제 출력은 `번호 | 채널명 | 등록일시`만 출력한다. DB에서 복원된 데이터에 visitCount·contractCount·achievementRate가 있음에도 표시하지 않는다.

**수정 방향**
출력 헤더와 행 포맷을 UC 명세에 맞게 수정.

---

### [BUG-SAL-08] SalesActivityManagementDAO — start_date·end_date·channel_type 미저장

| 항목 | 내용 |
|------|------|
| 관련 파일 | `dp/dao/SalesActivityManagementDAO.java:9-33`, `schema.sql` |
| 심각도 | **높음** |

**문제**
Runner에서 `setStartDate`, `setEndDate`, `setChannelType`을 호출해 값을 입력받지만, DAO `save()`와 스키마 모두 이 세 컬럼이 없다. 재시작 후 조회 기간·채널 유형 필터 기반 재조회가 불가능하다.

**수정 방향**
`sales_activity_managements` 테이블에 `start_date DATE`, `end_date DATE`, `channel_type VARCHAR(50)` 컬럼 추가 후 DAO `save()`·`findAll()`에 반영.

---

### [BUG-SAL-09] ChannelScreeningRunner — 자격증 컬럼 출력 누락

| 항목 | 내용 |
|------|------|
| 관련 파일 | `dp/runner/usecase/ChannelScreeningRunner.java:57, 62-71` |
| 심각도 | **중간** |

**문제**
UC step 2 테이블 컬럼: `지원자명 / 채널유형 / 지원일 / 경력 / **자격증** / 심사상태`. 출력 헤더 및 각 행에 자격증 컬럼이 누락되어 있다.

**수정 방향**
헤더 및 행 출력에 `자격증` 항목 추가.

---

### [BUG-SAL-10] ChannelScreeningRunner — 조회 조건에 채널유형·심사상태 필터 누락

| 항목 | 내용 |
|------|------|
| 관련 파일 | `dp/runner/usecase/ChannelScreeningRunner.java:75-79` |
| 심각도 | **중간** |

**문제**
UC step 3 조회 조건은 `모집 기간 / 채널 유형 / 심사 상태` 세 가지이지만, Runner는 시작일·종료일만 입력받고 채널 유형과 심사 상태 필터를 입력받지 않는다.

**수정 방향**
`readMenuChoice`로 채널 유형·심사 상태 입력 추가 후 `ChannelScreeningDAO`에 필터용 `findBy~` 메서드 구현.

---

### [BUG-SAL-11] ChannelScreeningDAO / schema — 자격증(certifications) 미저장

| 항목 | 내용 |
|------|------|
| 관련 파일 | `dp/dao/ChannelScreeningDAO.java:11-31`, `schema.sql` |
| 심각도 | **높음** |

**문제**
`ChannelScreening.certifications` 필드는 Runner에서 입력받고 상세 패널에서 출력하지만, DAO `save()`에서 저장하지 않고 스키마에도 해당 컬럼이 없다. 재시작 후 자격증 목록이 항상 빈 값으로 복원된다.

**수정 방향**
`channel_screenings` 테이블에 `certifications TEXT` 컬럼 추가 후 DAO `save()`·`findAll()` 수정. 복수 자격증은 쉼표 구분 문자열로 직렬화/역직렬화.

---

### [BUG-SAL-12] SalesOrgEvaluationDAO.findAll() — evaluation_no 미복원

| 항목 | 내용 |
|------|------|
| 관련 파일 | `dp/dao/SalesOrgEvaluationDAO.java:31-49` |
| 심각도 | **중간** |

**문제**
`findAll()`은 `evaluation_no` 컬럼을 SELECT하지만 ResultSet에서 읽어 객체에 설정하지 않는다 (`setEvaluationNo` setter도 없음). DB 재로드 시 `evaluationNo`가 항상 null.

**수정 방향**
`SalesOrgEvaluation`에 `setEvaluationNo(String)` 추가 후 `findAll()` 매퍼에서 복원.

---

### [BUG-SAL-13] SalesOrgEvaluationDAO / schema — channel_type 미저장

| 항목 | 내용 |
|------|------|
| 관련 파일 | `dp/dao/SalesOrgEvaluationDAO.java:10-29`, `schema.sql` |
| 심각도 | **중간** |

**문제**
Runner에서 `evaluation.setChannelType(...)`으로 채널 유형을 입력받지만, DAO `save()`와 `sales_org_evaluations` 스키마 모두 `channel_type` 컬럼이 없다. 재시작 후 채널 유형 복원 불가.

**수정 방향**
`sales_org_evaluations` 테이블에 `channel_type VARCHAR(50)` 추가 후 DAO 반영.

---

### [BUG-SAL-14] BonusRequestDAO / schema — channel_type·evaluation_no·evaluation_grade 미저장

| 항목 | 내용 |
|------|------|
| 관련 파일 | `dp/dao/BonusRequestDAO.java:8-18`, `schema.sql` |
| 심각도 | **중간** |

**문제**
`bonus_requests` 테이블에 `channel_type`, `evaluation_no`, `evaluation_grade` 컬럼이 없고 DAO도 저장하지 않는다. UC step 2는 성과급 요청 화면에 "평가번호, 채널명, 채널 유형, 평가등급"을 표시하도록 요구하며, 이력 재조회 시 해당 값이 손실된다.

**수정 방향**
`bonus_requests` 테이블에 세 컬럼 추가 후 DAO `save()`·`findAll()` 수정.

---

### [BUG-SAL-15] CustomerRegistrationDAO — address 컬럼 미저장

| 항목 | 내용 |
|------|------|
| 관련 파일 | `dp/dao/CustomerRegistrationDAO.java:10-28`, `schema.sql` |
| 심각도 | **중간** |

**문제**
Runner에서 `registration.setAddress(address)`로 주소를 입력받고 도메인에 `address` 필드가 존재하지만, DAO `save()`와 `customer_registrations` 스키마에 `address` 컬럼이 없다.

**수정 방향**
`customer_registrations` 테이블에 `address VARCHAR(255)` 추가 후 DAO 반영.

---

### [BUG-SAL-16] CustomerRegistrationRunner — 등록 완료 출력에 고객번호·계약번호 누락

| 항목 | 내용 |
|------|------|
| 관련 파일 | `dp/runner/usecase/CustomerRegistrationRunner.java:187-202` |
| 심각도 | **중간** |

**문제**
UC step 10은 "고객 번호, 계약 번호가 부여된 고객 정보 화면"을 등록 완료 시 출력하도록 요구하지만, step 10 출력에 이름·연락처·보험종류·월보험료만 있고 고객번호·계약번호가 포함되지 않는다.

**수정 방향**
step 10 출력에 `registration.getCustomerNo()`, `registration.getContractNo()` 추가.

---

## 상담/면담 도메인 (consultation)

### [BUG-CON-01] ConsultationRequestRunner — step 6 출력에 담당 설계사 정보 누락

| 항목 | 내용 |
|------|------|
| 관련 파일 | `dp/runner/usecase/ConsultationRequestRunner.java:84-87` |
| 심각도 | **중간** |

**문제**
UC step 6 요구: "접수번호, 접수일시, **배정된 담당 설계사 정보**"를 출력. 현재 `접수번호 | 접수일시 | 상담유형 | 상태`만 출력하고 담당 설계사 이름이 빠져 있다.

**수정 방향**
설계사 배정 후 step 6 출력에 `designer.getName()` 추가.

---

### [BUG-CON-02] ConsultationRequestRunner — scheduledAt에 LocalDateTime.now() 전달

| 항목 | 내용 |
|------|------|
| 관련 파일 | `dp/runner/usecase/ConsultationRequestRunner.java:68, 110` |
| 심각도 | **중간** |

**문제**
`enterConsultationInfo()` 호출 시 `scheduledAt` 파라미터로 `LocalDateTime.now()`를 전달하므로, UC에서 요구하는 "희망 방문일시"(고객이 직접 입력)가 아닌 접수 시각이 들어간다. step 10 출력의 "예정 상담일시"가 잘못된 값이 된다.

**수정 방향**
```java
LocalDateTime scheduledAt = ConsoleHelper.readDateTime("  희망 방문일시");
request.enterConsultationInfo(scheduledAt, location, contact, content);
```

---

### [BUG-CON-03] ConsultationRequestDAO — scheduled_at 컬럼 저장 누락

| 항목 | 내용 |
|------|------|
| 관련 파일 | `dp/dao/ConsultationRequestDAO.java:11-23` |
| 심각도 | **중간** |

**문제**
`ConsultationRequest`에 `scheduledAt` 필드가 있으나 `save()`의 INSERT/UPDATE 구문에 포함되지 않는다. UC에서 "희망 방문일시"는 이후 상세 조회 시 필요한 항목이다.

**수정 방향**
`consultation_requests` 테이블에 `scheduled_at TIMESTAMP NULL` 컬럼 추가 후 DAO `save()`·`findAll()` 수정.

---

### [BUG-CON-04] ConsultationRequestDAO — findAll()에서 scheduledAt 복원 불가

| 항목 | 내용 |
|------|------|
| 관련 파일 | `dp/dao/ConsultationRequestDAO.java:41-51` |
| 심각도 | **중간** |

**문제**
BUG-CON-03과 연동. `scheduled_at` 컬럼이 스키마에 없으므로 `findAll()` 복원 시에도 항상 null이 된다.

**수정 방향**
BUG-CON-03 수정 후 `findAll()` 매퍼에 `scheduled_at` 복원 추가.

---

### [BUG-SCH-01] InterviewScheduleRunner — A1 step 5 출력에 담당 설계사명 누락

| 항목 | 내용 |
|------|------|
| 관련 파일 | `dp/runner/usecase/InterviewScheduleRunner.java:105-108` |
| 심각도 | **중간** |

**문제**
UC A1 step 5 요구: "면담번호, 등록일시, 고객명, **면담 담당 설계사명**, 면담일시". Runner에 설계사 개념이 없어 담당 설계사명이 출력에서 완전히 빠져 있다.

**수정 방향**
면담 등록 시 담당 설계사를 배정하는 입력 단계를 추가하고, step 5 출력에 포함.

---

### [BUG-SCH-02] InterviewScheduleRunner — Basic Path step 5-6 상세 정보 출력 단계 미구현

| 항목 | 내용 |
|------|------|
| 관련 파일 | `dp/runner/usecase/InterviewScheduleRunner.java` |
| 심각도 | **중간** |

**문제**
UC Basic Path step 5-6: 목록에서 항목을 선택하면 "고객명/면담유형/면담일시/면담장소/면담준비사항/면담상태" 상세 정보를 출력한 뒤 [수정]/[취소]/[닫기] 버튼을 제공해야 한다. 현재 구현은 메뉴에서 바로 작업을 선택하는 방식이라 상세 정보 조회 단계가 통째로 없다.

**수정 방향**
목록 선택 후 상세 출력 단계를 거쳐 작업 메뉴를 제공하는 흐름으로 재구성.

---

### [BUG-SCH-03] InterviewScheduleRunner — 수정 시 E2 검증에 면담유형 변경 입력 누락

| 항목 | 내용 |
|------|------|
| 관련 파일 | `dp/runner/usecase/InterviewScheduleRunner.java:136-140` |
| 심각도 | **낮음** |

**문제**
UC E2: "수정 시 필수 항목(면담유형, 면담일시)이 입력되지 않은 경우". 구현은 `scheduledAt == null`만 체크하고 면담유형 재선택 입력 자체가 없어, 유형 변경이 불가하고 E2 검증도 부분 미구현이다.

**수정 방향**
수정 화면에 면담유형 재선택 옵션 추가 후 E2 검증에 포함.

---

### [BUG-REC-01] InterviewRecordRunner — A3 수정 시 사용자 선택 없이 마지막 항목 자동 선택

| 항목 | 내용 |
|------|------|
| 관련 파일 | `dp/runner/usecase/InterviewRecordRunner.java:113` |
| 심각도 | **높음** |

**문제**
수정 작업에서 `interviewRecords.get(interviewRecords.size() - 1)`로 자동 선택. UC A3 step 1에서 사용자가 목록에서 클릭하여 선택하는 단계가 없어, 기록이 여러 건일 때 의도치 않은 항목이 수정된다.

**수정 방향**
`ConsoleHelper.readMenuChoice()`로 사용자가 직접 수정할 기록을 선택하도록 변경.

---

### [BUG-REC-02] InterviewRecordRunner — 저장일시 필드 없어 interviewedAt 중복 출력

| 항목 | 내용 |
|------|------|
| 관련 파일 | `dp/runner/usecase/InterviewRecordRunner.java:91-94` |
| 심각도 | **중간** |

**문제**
UC A1 step 5: "기록번호, **저장일시**, 고객명, 면담일시". `InterviewRecord`에 `recordedAt`(저장일시) 별도 필드가 없어서 `interviewedAt`을 두 번 출력한다. DB의 `recorded_at` 컬럼이 이 목적임에도 도메인 객체에 필드가 없다.

**수정 방향**
`InterviewRecord`에 `recordedAt` 필드 추가, `save()` 호출 시점(`LocalDateTime.now()`)을 기록하고 `recorded_at` 컬럼에 저장.

---

### [BUG-REC-03] InterviewRecordRunner — A3 수정 완료 출력에 수정일시 누락

| 항목 | 내용 |
|------|------|
| 관련 파일 | `dp/runner/usecase/InterviewRecordRunner.java:128-129` |
| 심각도 | **낮음** |

**문제**
UC A3 step 5: "기록번호, **수정일시**" 출력 요구. 구현은 기록번호만 출력. `InterviewRecord.modify()`가 `modifiedAt`을 set하지 않으며 해당 필드 자체가 도메인·DAO 모두에 없다.

**수정 방향**
`InterviewRecord`에 `modifiedAt` 필드 추가, `modify()` 호출 시 `LocalDateTime.now()` 설정 후 출력.

---

### [BUG-REC-04] InterviewRecordDAO — recorded_at에 면담일시(interviewedAt) 오저장

| 항목 | 내용 |
|------|------|
| 관련 파일 | `dp/dao/InterviewRecordDAO.java:23` |
| 심각도 | **중간** |

**문제**
`recorded_at` 파라미터에 `r.getInterviewedAt()`을 전달. `recorded_at`은 "저장한 시각"이어야 하는데, 면담 일시(사용자 입력값)가 들어가 면담일시와 저장일시가 동일 값으로 저장된다.

**수정 방향**
BUG-REC-02 수정 후 `r.getRecordedAt()`으로 교체.

---

### [BUG-PRP-01] ProposalDAO — findAll() 미구현

| 항목 | 내용 |
|------|------|
| 관련 파일 | `dp/dao/ProposalDAO.java` |
| 심각도 | **낮음** |

**문제**
`save()`만 있고 `findAll()`이 없다. 기존 제안 이력 조회나 다른 UC에서 제안을 참조할 방법이 없다.

**수정 방향**
`proposals` 테이블 전체 조회 `findAll()` 메서드 추가.

---

### [BUG-UDW-01] UnderwritingRunner — "심사 결과를 전달한다" step 6 처리번호 미출력

| 항목 | 내용 |
|------|------|
| 관련 파일 | `dp/runner/usecase/UnderwritingRunner.java:225-227` |
| 심각도 | **낮음** |

**문제**
UC "심사 결과를 전달한다" step 6: "확인 완료 처리 결과(확인일시, **처리번호**)". `result.confirm()` 후 처리번호 출력이 없으며 `ReviewResult`에 처리번호 필드가 없을 수 있다.

**수정 방향**
`ReviewResult`에 처리번호 필드 추가 후 step 6 출력에 포함.

---

### [BUG-REV-01] RevivalRunner — E2 납입 처리 실패 분기 미구현

| 항목 | 내용 |
|------|------|
| 관련 파일 | `dp/runner/usecase/RevivalRunner.java:78-83` |
| 심각도 | **중간** |

**문제**
UC step 6 `(E2)`: 납입 처리 실패 시 "납입 처리에 실패했습니다." 출력 요구. `revival.pay(paymentMethod)` 반환값이나 실패 분기 처리 없이 곧바로 성공 결과만 출력한다.

**수정 방향**
`pay()` 반환값(또는 status 체크)으로 성공/실패 분기 처리 추가.

---

### [BUG-REV-02] RevivalRunner — step 3에서 납입방법 revival에 미전달

| 항목 | 내용 |
|------|------|
| 관련 파일 | `dp/runner/usecase/RevivalRunner.java:69` |
| 심각도 | **중간** |

**문제**
UC step 3: "고객은 부활 신청 정보(연락처, 납입방법)를 입력한다." Step 3에서 `contact`만 `revival`에 set하고 납입방법을 set하지 않아(`revival.setPaymentMethod(...)` 호출 없음), 입력값이 유실된다.

**수정 방향**
step 3에서 납입방법 입력 후 `revival.setPaymentMethod(paymentMethod)` 호출 추가.

---

## 계약/보상·청구 도메인 (contract / claim)

### [BUG-NEW-CTR-02] ExpiringContractManagementRunner — 안내·갱신 처리 후 DAO.save() 누락

| 항목 | 내용 |
|------|------|
| 관련 파일 | `dp/runner/usecase/ExpiringContractManagementRunner.java:173, 219` |
| 심각도 | **높음** |

**문제**
Line 173에서 `mgmt.saveNoticeRecord()` 호출 후, Line 219에서 `mgmt.saveRenewalContract()` 호출 후 각각 `ExpiringContractManagementDAO.save(mgmt)`를 호출하지 않는다. DAO 클래스는 존재하므로 단순 호출 누락이다. JVM 재시작 시 안내 이력과 갱신 계약 정보가 모두 손실된다.

**수정 방향**
```java
// Line 173 이후
ExpiringContractManagementDAO.save(mgmt);

// Line 219 이후
ExpiringContractManagementDAO.save(mgmt);
```

---

### [BUG-NEW-CLM-02] ClaimPaymentRunner — OTP 시도 횟수 5회 기준 미충족 (3회로 구현)

| 항목 | 내용 |
|------|------|
| 관련 파일 | `dp/runner/usecase/ClaimPaymentRunner.java:172-184` |
| 심각도 | **중간** |

**문제**
UC Exception E1: "OTP 인증을 **5회** 연속 실패한 경우 비활성화." 코드는 `while (attempt < 3)`으로 3회까지만 허용한다.

**수정 방향**
```java
while (attempt < 5) {
    attempt++;
    String otp = ConsoleHelper.readNonEmpty("[보상담당자] OTP 6자리 입력 (시도 " + attempt + "/5): ");
    ...
}
```

---

### [BUG-NEW-CLM-03] ClaimCalculationDAO — deductible·coverage_limit·calculated_at 컬럼 미저장

| 항목 | 내용 |
|------|------|
| 관련 파일 | `dp/dao/ClaimCalculationDAO.java:14-21`, `schema.sql` |
| 심각도 | **중간** |

**문제**
`ClaimCalculation` 도메인에 `deductible`, `coverageLimit`, `calculatedAt` 필드가 있으나, DAO `save()`와 `claim_calculations` 테이블 스키마 모두에 해당 컬럼이 없다. 산출 시 적용된 자기부담금·보장한도·산출일시가 DB에 저장되지 않아 감사 및 재조회 시 손실된다.

**수정 방향**
1. `claim_calculations` 테이블에 `deductible BIGINT DEFAULT 0`, `coverage_limit BIGINT DEFAULT 0`, `calculated_at TIMESTAMP NULL` 컬럼 추가.
2. DAO `save()`에 해당 파라미터 추가.
3. `findAll()` 매퍼에서 복원.

---

### [BUG-NEW-CLM-04] ClaimPaymentRunner — UC step 10 안내 메시지 발송 후 확인 단계 누락

| 항목 | 내용 |
|------|------|
| 관련 파일 | `dp/runner/usecase/ClaimPaymentRunner.java:91-100` |
| 심각도 | **낮음** |

**문제**
UC Basic Path step 10: "보험금 지급이 완료되었다는 안내 메시지를 발송하고 팝업을 출력한 뒤 확인 버튼을 제공." 현재 `sendCompletionNotice()` → `close()` 순서로만 실행되며, step 10 안내 팝업과 step 11 확인 프롬프트가 없다.

**수정 방향**
```java
payment.sendCompletionNotice();
ConsoleHelper.printInfo("고객의 휴대폰으로 보험금 지급 완료 안내 메시지를 발송했습니다.");
ConsoleHelper.waitEnter();
payment.close();
```

---

### [BUG-NEW-CLM-05] ClaimPaymentDAO — ON DUPLICATE KEY UPDATE에서 scheduled_at·payment_type 미업데이트

| 항목 | 내용 |
|------|------|
| 관련 파일 | `dp/dao/ClaimPaymentDAO.java:17-25` |
| 심각도 | **중간** |

**문제**
`ON DUPLICATE KEY UPDATE` 절에 `status`, `paid_at`, `failure_reason`만 있고 `scheduled_at`(예약 지급 일시)과 `payment_type`(즉시/예약)이 없다. A1 예약 지급 경로에서 상태 변경 후 `save()` 재호출 시 예약 정보가 DB에 반영되지 않는다.

**수정 방향**
```java
+ " ON DUPLICATE KEY UPDATE status=VALUES(status),"
+ " paid_at=VALUES(paid_at), scheduled_at=VALUES(scheduled_at),"
+ " payment_type=VALUES(payment_type), failure_reason=VALUES(failure_reason)",
```

---

## 재무/납입/문의 도메인 (payment / inquiry)

### [BUG-FIN-08] RefundCalculationDAO.findAll() — Cancellation 쉘 객체로 복원 시 NPE 위험

| 항목 | 내용 |
|------|------|
| 관련 파일 | `dp/dao/RefundCalculationDAO.java:27-55` |
| 심각도 | **중간** |

**문제**
`findAll()`에서 `Cancellation`을 껍데기 객체로만 생성한다. 이후 `RefundPaymentRunner`에서 `payment.getRefund().getCancellation().getContract().getCustomer().getRegisteredAccounts()` 체인 호출 시 `getContract()`나 `getCustomer()`가 null이면 NPE가 발생한다.

**수정 방향**
`cancellation_no`로 `CancellationDAO.findAll()`에서 실제 해지 건을 조회하여 교차 복원하거나, 최소한 체인 각 단계에서 null 체크를 추가.

---

### [BUG-INQ-05] InquiryDAO.mapRow() — inquiry_no 미복원

| 항목 | 내용 |
|------|------|
| 관련 파일 | `dp/dao/InquiryDAO.java:54-78` |
| 심각도 | **중간** |

**문제**
SELECT 쿼리에 `inquiry_no`가 포함되지만 `mapRow()`에서 객체에 설정하지 않아(`setInquiryNo()` 미호출), DB 로드 후 `getInquiryNo()`가 항상 null을 반환한다. `InquiryRunner`에서 문의 번호 출력 시 null로 표시된다.

**수정 방향**
1. `Inquiry` 도메인 클래스에 `setInquiryNo(String)` 추가.
2. `mapRow()` 첫 줄에 `i.setInquiryNo(rs.getString("inquiry_no"))` 추가.

---

### [BUG-INQ-06] InquiryDAO.save() — answer_content·answered_at 저장 경로 없음

| 항목 | 내용 |
|------|------|
| 관련 파일 | `dp/dao/InquiryDAO.java:11-28` |
| 심각도 | **높음** |

**문제**
`save()` INSERT에 `answer_content`, `answered_at`이 없고 `ON DUPLICATE KEY UPDATE`에서 `status`만 업데이트한다. 관리자가 `Inquiry.addAnswer()`로 답변을 등록한 뒤 `save()`를 호출해도 답변이 DB에 영속화되지 않는다. AdditionalBugReport의 BUG-NEW-INQ-01은 기존 답변이 덮어쓰이는 문제를 해결했지만, 답변 자체의 저장 경로는 여전히 없다.

**수정 방향**
```java
"INSERT INTO inquiries (inquiry_no, customer_name, inquiry_type, title, content,"
+ " attachment_file_name, attachment_file_size, answer_content, answered_at, status, created_at)"
+ " VALUES (?,?,?,?,?,?,?,?,?,?,?)"
+ " ON DUPLICATE KEY UPDATE status=VALUES(status),"
+ " answer_content=VALUES(answer_content), answered_at=VALUES(answered_at)",
```
파라미터에 `i.getAnswerContent()`, `i.getAnsweredAt()` 추가.

---

### [BUG-REF-01] RefundPaymentRunner — 지급 완료 결과 출력(팝업) 누락

| 항목 | 내용 |
|------|------|
| 관련 파일 | `dp/runner/usecase/RefundPaymentRunner.java:75-96, 139-159` |
| 심각도 | **중간** |

**문제**
UC step 10 "지급 완료 팝업": 이체 완료 후 수령인명·은행명·계좌번호·이체금액을 출력해야 한다. 현재 `payment.execute()` 호출 후 바로 알림톡 발송 로직으로 진행하며, 완료 결과 팝업이 없다.

**수정 방향**
```java
if (payment.getStatus() == RefundPaymentStatus.COMPLETED) {
    ConsoleHelper.printSuccess("환급금 지급을 완료하였습니다.");
    ConsoleHelper.printInfo("수령인명: " + ... + " | 은행명: " + ... 
        + " | 계좌번호: " + ... + " | 이체금액: " + payment.getFinalAmount() + "원");
    ConsoleHelper.waitEnter();
}
```