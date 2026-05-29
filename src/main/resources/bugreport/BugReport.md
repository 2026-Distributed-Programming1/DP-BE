# 유스케이스 기반 버그 / 불일치 리포트

> 기준 문서: `Usecase_scenario.md`
> 작성 방식: 도메인별로 순차 추가. 각 항목은 파일·줄 번호와 수정 방향을 포함.

---

## 전체 구조 평가

### 잘 된 부분

| 항목 | 내용 |
|------|------|
| **계층 분리** | `Actor → Runner → Domain → DAO → DB` 흐름이 전 도메인에 걸쳐 일관됨 |
| **유스케이스 1:1 매핑** | 각 Runner가 유스케이스 하나에 대응하고, Basic Path·Alternative·Exception을 주석으로 명시 |
| **도메인 패키지 분리** | `education`, `sales`, `consultation` 등 도메인별로 패키지가 명확하게 분리됨 |
| **Enum 활용** | `ChannelType`, `EvaluationGrade`, `ScreeningStatus` 등 상태값을 enum으로 관리 |
| **ConsoleHelper 중앙화** | 입출력 로직이 한 곳에 모여 있어 각 Runner가 I/O 세부 구현에 의존하지 않음 |

### 반복되는 구조적 문제

| 문제 | 영향 범위 | 설명 |
|------|-----------|------|
| **Domain 메서드 대부분이 빈 껍데기** | 전 도메인 | `loadActivityTable(){}`, `search(){}` 등 실제 로직 없이 구조적 역할만 수행 |
| **DAO 저장 필드 누락 패턴 반복** | 전 도메인 | INSERT 쿼리에 핵심 컬럼이 빠지는 패턴이 도메인마다 반복됨 (BUG-EDU-03, BUG-SAL-02, BUG-SAL-05 등) |
| **Runner 간 컨텍스트 미전달** | 유스케이스 연계 구간 | 유스케이스 간 이동 시 데이터를 파라미터로 넘기지 않고 사용자가 재입력하는 구조 (BUG-SAL-03) |
| **static sequence 카운터로 PK 생성** | 전 도메인 | `private static int sequence = 0` 방식은 JVM 재시작 시 DB의 기존 PK와 충돌 가능 |

### 종합 의견

뼈대 설계는 명확하고 코드 가독성도 좋다. 다만 **"구조는 있는데 살이 덜 붙은" 상태**로, 특히 DAO의 저장 필드 누락이 전 도메인에 걸쳐 반복되는 것이 가장 우선적으로 해결해야 할 패턴 문제이다. 나머지 미검토 도메인(상담/면담, 계약, 보상/청구, 재무/납입, 문의)도 동일 유형의 버그가 존재할 가능성이 높다.

---

## 교육 도메인 (education)

### [BUG-EDU-01] EducationPlan — 교육 목표·내용·교재 목록 필드 없음

| 항목 | 내용 |
|------|------|
| 유스케이스 | 교육 계획안을 작성한다 — Basic Path Step 3 |
| 관련 파일 | `dp/education/EducationPlan.java`, `dp/runner/usecase/EducationPlanRunner.java` |
| 심각도 | **높음** (필수 입력 항목 누락) |

**문제**
유스케이스는 교육 내용 입력 영역에 다음 세 필드를 요구한다.
- 교육 목표 (텍스트, **필수**)
- 교육 내용 (텍스트, **필수**)
- 교재 목록 (텍스트, 선택)

`EducationPlan` 도메인 객체에 이 필드들이 존재하지 않으며, `EducationPlanRunner`에서도 이 항목들을 입력받지 않는다.

**수정 방향**
1. `EducationPlan`에 `educationGoal`, `educationContent`, `textbookList` 필드 추가.
2. `enterPlanInfo()` 또는 별도 메서드로 세 필드를 설정.
3. `validateRequiredFields()`에서 `educationGoal`, `educationContent` null/empty 검증 추가.
4. `EducationPlanRunner`에서 두 필드 입력 프롬프트 추가.
5. `EducationPlanDAO.save()` 쿼리에 해당 컬럼 추가.

---

### [BUG-EDU-02] EducationPlan.reject() — 반려 사유가 저장되지 않음

| 항목 | 내용 |
|------|------|
| 유스케이스 | 교육 계획안을 작성한다 — Alternative A3 |
| 관련 파일 | `dp/education/EducationPlan.java:84-86` |
| 심각도 | **중간** (반려 사유 유실) |

**문제**
```java
public void reject(String reason) {
    this.status = "반려";   // reason이 저장되지 않음
}
```
유스케이스 A3은 "반려 사유와 함께 반려 알림을 영업교육 담당자에게 발송"해야 하는데, `reason` 파라미터를 받고도 버리기 때문에 사유를 추적하거나 알림에 포함시킬 수 없다.

**수정 방향**
1. `EducationPlan`에 `String rejectReason` 필드 추가.
2. `reject(String reason)` 내부에서 `this.rejectReason = reason;` 추가.
3. `EducationPlanDAO.save()` 쿼리에 `reject_reason` 컬럼 업데이트 포함.

---

### [BUG-EDU-03] EducationPlanDAO — endDate·targetCount·budget DB 저장/복원 누락

| 항목 | 내용 |
|------|------|
| 유스케이스 | 교육 계획안을 작성한다 — Basic Path Step 3 (종료일·대상자수·예산 필수) |
| 관련 파일 | `dp/dao/EducationPlanDAO.java:9-44` |
| 심각도 | **높음** (필수 데이터 유실) |

**문제**
`EducationPlanDAO.save()` INSERT 쿼리에 `scheduled_date`(=startDate)만 저장하고 `endDate`, `targetCount`, `budget`은 저장하지 않는다. `findAll()` 조회 시 `EducationPlan.fromDb()`를 통해 복원해도 이 세 값이 0/null로 초기화된다.

그 결과 `EducationPreparationRunner`에서 계획안 목록 출력 시 "대상자수: 0"이 표시된다.

**수정 방향**
1. `education_plans` 테이블에 `end_date DATE`, `target_count INT`, `budget BIGINT` 컬럼 추가(DDL 수정).
2. `EducationPlanDAO.save()` 쿼리에 해당 컬럼·값 추가.
3. `EducationPlanDAO.findAll()` 쿼리에서 해당 컬럼 SELECT.
4. `EducationPlan.fromDb()` 시그니처에 `endDate`, `targetCount`, `budget` 파라미터 추가하여 복원.

---

### [BUG-EDU-04] EducationPreparation.validateRequiredFields() — 교재 현황·대상자 명단 검증 누락

| 항목 | 내용 |
|------|------|
| 유스케이스 | 교육 제반을 등록한다 — Basic Path Step 8, Exception E1 |
| 관련 파일 | `dp/education/EducationPreparation.java:50-53` |
| 심각도 | **중간** (필수 항목 검증 불완전) |

**문제**
유스케이스는 다음을 **필수** 항목으로 정의한다.
- 교육 장소 ✅ (검증 중)
- 강사명 ✅ (검증 중)
- **교재 준비 현황** ❌ (검증 없음)
- **교육 대상자 명단** ❌ (검증 없음)

현재 `validateRequiredFields()`는 `location`과 `instructorName`만 확인한다.

**수정 방향**
```java
public boolean validateRequiredFields() {
    return location != null && !location.isEmpty()
        && instructorName != null && !instructorName.isEmpty()
        && textbookStatus != null && !textbookStatus.isEmpty()   // 추가
        && attendanceList != null && !attendanceList.isEmpty();   // 추가
}
```

---

### [BUG-EDU-05] EducationExecutionRunner — 교육 진행 메모 입력 순서 오류

| 항목 | 내용 |
|------|------|
| 유스케이스 | 교육을 진행한다 — Basic Path Step 5 |
| 관련 파일 | `dp/runner/usecase/EducationExecutionRunner.java:80-102` |
| 심각도 | **낮음** (흐름 순서 불일치) |

**문제**
유스케이스 Step 5: "영업교육 담당자는 교육 진행 메모를 **입력하고** [진행 완료] 버튼을 클릭한다."

현재 구현은 "진행 완료 버튼 선택 → 확인 팝업 → 확인 → 메모 입력" 순서로, 메모 입력이 버튼 클릭 **이후**에 위치한다.

**수정 방향**
메모 입력 프롬프트를 `readMenuChoice()`(진행 완료/취소 선택) **이전**으로 이동한다.

```java
// 올바른 순서
String memo = ConsoleHelper.readLine("  교육 진행 메모 (없으면 엔터): ");
if (!memo.isEmpty()) execution.setMemo(memo);

int action = ConsoleHelper.readMenuChoice("[영업교육담당자] 처리를 선택하세요.", "진행 완료", "취소");
```

---

### [BUG-EDU-06] EducationExecutionDAO — 개별 출석 여부(Attendance)가 DB에 저장되지 않음

| 항목 | 내용 |
|------|------|
| 유스케이스 | 교육을 진행한다 — Basic Path Step 8 (수료 알림에 "수료여부" 포함) |
| 관련 파일 | `dp/dao/EducationExecutionDAO.java` |
| 심각도 | **중간** (개인별 수료 이력 미저장) |

**문제**
`education_executions` 테이블에 `attendee_count`(숫자 합계)만 저장되고, 개별 출석자 이름·출석 여부는 어떤 테이블에도 저장되지 않는다. 유스케이스 Step 8은 판매채널에게 "교육명, 교육일시, **수료여부**" 를 포함한 알림을 발송하도록 명시한다. 현재는 수료 여부를 개인별로 추적할 수 없다.

**수정 방향**
1. `education_attendances` 테이블 생성 (`execution_no`, `attendee_name`, `is_attended`).
2. `EducationExecutionDAO.save()` 또는 별도 `AttendanceDAO.saveAll()`에서 `Attendance` 목록을 일괄 INSERT.
3. `sendCompletionNotice()`에서 개인별 수료 여부를 알림에 포함.

---

## 영업 도메인 (sales)

### [BUG-SAL-01] SalesActivityRunner — 목표달성률 70% 미만 강조 시 핵심 출력 항목 누락

| 항목 | 내용 |
|------|------|
| 유스케이스 | 영업 활동을 관리한다 — Basic Path Step 4 |
| 관련 파일 | `dp/runner/usecase/SalesActivityRunner.java:119-129` |
| 심각도 | **중간** (표시 항목 불완전) |

**문제**
유스케이스 Step 4는 "출력 항목: 채널명 / 채널유형 / 방문건수 / 계약건수 / 전환율 / 목표달성률"을 모두 표시하고, 70% 미만 채널은 **셀을 강조**하도록 명시한다. 그러나 현재 코드는 `achievementRate < 70.0` 분기에서 목표달성률만 출력하고 방문건수·계약건수·전환율을 빠뜨린다.

```java
if (achievementRate < 70.0) {
    // 방문건수, 계약건수, 전환율이 모두 누락됨
    ConsoleHelper.printInfo("⚠️  [강조] " + channelName + " | 목표달성률: ...");
}
```

**수정 방향**
두 분기를 합쳐 전체 항목을 항상 출력하고, 70% 미만인 경우에만 강조 접두사를 붙인다.

```java
String prefix = achievementRate < 70.0 ? "⚠️  [강조] " : "";
if (achievementRate < 70.0) activity.highlightLowAchievement();
ConsoleHelper.printInfo(prefix + channelName
    + " | 방문: " + visitCount + "건"
    + " | 계약: " + contractCount + "건"
    + " | 전환율: " + String.format("%.1f", activity.getConversionRate()) + "%"
    + " | 목표달성률: " + achievementRate + "%");
```

---

### [BUG-SAL-02] SalesActivityManagementDAO — 개선 지시 핵심 데이터 미저장

| 항목 | 내용 |
|------|------|
| 유스케이스 | 영업 활동을 관리한다 — Basic Path Step 9~10 |
| 관련 파일 | `dp/dao/SalesActivityManagementDAO.java:9-22` |
| 심각도 | **높음** (업무 핵심 데이터 유실) |

**문제**
`save()` INSERT 쿼리에 `activity_no`, `manager_name`, `channel_name`, `activity_type`, `created_at`만 저장한다. 개선 지시의 핵심 필드가 DB에 저장되지 않는다.

- `improvementContent` (개선 지시 내용)
- `revisedTarget` (수정 목표)
- `visitCount` (방문건수)
- `contractCount` (계약건수)
- `achievementRate` (목표달성률)

**수정 방향**
1. `sales_activity_managements` 테이블에 `improvement_content TEXT`, `revised_target INT`, `visit_count INT`, `contract_count INT`, `achievement_rate DOUBLE` 컬럼 추가.
2. `SalesActivityManagement`에 해당 getter 추가.
3. `save()` 쿼리에 해당 컬럼·값 포함.

---

### [BUG-SAL-03] BonusRequestRunner — 평가 컨텍스트 미전달로 사용자 재입력 필요

| 항목 | 내용 |
|------|------|
| 유스케이스 | 성과급 지급을 요청한다 — Basic Path Step 2 |
| 관련 파일 | `dp/runner/usecase/BonusRequestRunner.java:49-76` |
| 심각도 | **중간** (유스케이스 흐름 불일치) |

**문제**
유스케이스 Step 2: "평가 정보: 평가번호, 채널명, 채널유형, 평가등급이 **자동 표시**"되어야 한다. 그러나 `BonusRequestRunner.run()`이 독립 실행되어 `SalesOrgEvaluationRunner`에서 이미 수집한 채널명·채널유형·평가번호·평가등급을 사용자가 처음부터 다시 입력해야 한다.

특히 평가등급은 S/A임을 확인하고 이동했음에도 `BonusRequestRunner`에서 다시 선택하므로, B/C/D 등급을 잘못 선택할 수 있다.

**수정 방향**
`BonusRequestRunner.run(SalesOrgEvaluation evaluation)` 오버로드를 추가하여 평가 데이터를 파라미터로 전달한다. `SalesOrgEvaluationRunner`에서 호출 시 현재 `evaluation` 객체를 넘긴다.

---

### [BUG-SAL-04] ChannelScreeningDAO — 지원일(applicationDate) 미저장/미복원

| 항목 | 내용 |
|------|------|
| 유스케이스 | 판매채널 채용을 심사한다 — Basic Path Step 2 (테이블 컬럼: 지원일 포함) |
| 관련 파일 | `dp/dao/ChannelScreeningDAO.java:11-28`, `dp/runner/usecase/ChannelScreeningRunner.java:62-70` |
| 심각도 | **중간** (조회 테이블 컬럼 누락) |

**문제**
유스케이스 Step 2 테이블 컬럼: "지원자명 / 채널유형 / **지원일** / 경력 / 자격증 / 심사상태". `ChannelScreeningDAO.save()`에 `application_date` 컬럼이 없고, `findAll()`에도 없어 DB에서 로드 시 지원일이 복원되지 않는다. 목록 출력 시에도 지원일 컬럼이 표시되지 않는다.

**수정 방향**
1. `channel_screenings` 테이블에 `application_date DATE` 컬럼 추가.
2. `ChannelScreeningDAO.save()` 쿼리에 `application_date` 포함.
3. `findAll()` 쿼리에서 `application_date` 조회 후 setter로 복원.
4. `ChannelScreeningRunner` 목록 출력부에 지원일 컬럼 추가.

---

### [BUG-SAL-05] SalesOrgEvaluationDAO — 매출실적·계약건수·평가의견 미저장

| 항목 | 내용 |
|------|------|
| 유스케이스 | 영업조직을 평가한다 — Basic Path Step 10 |
| 관련 파일 | `dp/dao/SalesOrgEvaluationDAO.java:10-21` |
| 심각도 | **중간** (평가 근거 데이터 미저장) |

**문제**
`save()` INSERT 쿼리에 `evaluation_no`, `org_name`, `grade`, `score`(achievementRate), `evaluated_at`만 저장한다. 평가의 근거 데이터가 저장되지 않는다.

- `salesResult` (매출실적)
- `contractCount` (계약건수)
- `evaluationComment` (평가 의견)

**수정 방향**
1. `sales_org_evaluations` 테이블에 `sales_result BIGINT`, `contract_count INT`, `evaluation_comment TEXT` 컬럼 추가.
2. `SalesOrgEvaluation`에 `getSalesResult()`, `getContractCount()`, `getEvaluationComment()` getter 추가.
3. `save()` 쿼리에 해당 컬럼·값 포함.

---

### [BUG-SAL-06] ChannelRecruitmentRunner — E1 필수 항목 오류 시 재입력 불가

| 항목 | 내용 |
|------|------|
| 유스케이스 | 판매채널을 모집한다 — Exception E1 |
| 관련 파일 | `dp/runner/usecase/ChannelRecruitmentRunner.java:148-157` |
| 심각도 | **낮음** (재시도 흐름 누락) |

**문제**
유스케이스 E1: "시스템은 오류 메시지를 출력하고, 영업 관리자는 누락된 항목을 입력한 후 [저장] 버튼을 클릭한다." 재입력이 가능해야 하지만, 현재 코드는 필수 항목 검증 실패 시 `return`으로 즉시 종료되어 처음부터 다시 시작해야 한다.

**수정 방향**
유효성 검증 실패 시 해당 입력 단계로 돌아가도록 반복 루프를 적용하거나, 최소한 재시작 안내를 출력한다.

---

---

## 상담/면담 도메인 (consultation)

### [BUG-CON-01] ConsultationRequest — 접수일시·수락일시·예정 상담일시 필드 없음

| 항목 | 내용 |
|------|------|
| 유스케이스 | 상담을 요청한다 — Basic Path Step 6, Step 10 |
| 관련 파일 | `dp/consultation/ConsultationRequest.java`, `dp/runner/usecase/ConsultationRequestRunner.java:83-87, 104-108` |
| 심각도 | **높음** (완료 결과 출력 항목 누락) |

**문제**
유스케이스가 다음 두 결과 화면에서 출력을 요구하는 필드가 도메인 객체에 없다.

- Step 6 "접수번호, **접수일시**, 배정된 담당 설계사 정보" — `receivedAt` 필드 없음, 담당 설계사 정보도 출력 안 함
- Step 10 "수락일시, 담당자명, **예정 상담일시**" — `acceptedAt` 필드 없음, `accept()` 메서드가 일시를 설정하지 않음
- Runner Step 10(line 106-108): `담당자명 + 상태`만 출력; 수락일시·예정 상담일시 모두 누락

**수정 방향**
1. `ConsultationRequest`에 `receivedAt`(접수 시각), `acceptedAt`(수락 시각) 필드 추가.
2. `submit()` 에서 `receivedAt = LocalDateTime.now()` 설정.
3. `accept()` 에서 `acceptedAt = LocalDateTime.now()` 설정.
4. Runner Step 6·10 출력 구문에 해당 datetime 포함.

---

### [BUG-CON-02] ConsultationRequestDAO — scheduledAt을 requested_at으로 저장

| 항목 | 내용 |
|------|------|
| 유스케이스 | 상담을 요청한다 — Basic Path Step 6 |
| 관련 파일 | `dp/dao/ConsultationRequestDAO.java:19` |
| 심각도 | **높음** (전혀 다른 값이 DB에 저장됨) |

**문제**
`save()` INSERT 구문에서 `requested_at` 컬럼에 `r.getScheduledAt()`(고객이 원하는 희망 방문일시)를 넘긴다.
`requested_at`은 신청이 접수된 시각이고 `scheduledAt`은 고객이 원하는 방문일시이므로, 두 값은 완전히 다른 의미이다.
신청 접수 시각을 저장하는 별도 필드·로직이 없는 상태.

**수정 방향**
BUG-CON-01에서 추가하는 `receivedAt`을 `requested_at` 컬럼에 저장하도록 수정한다.

---

### [BUG-CON-03] InterviewScheduleDAO — type·preparation 저장 및 복원 누락

| 항목 | 내용 |
|------|------|
| 유스케이스 | 면담일정을 관리한다 — A1 Step 2·5, Basic Path Step 6 |
| 관련 파일 | `dp/dao/InterviewScheduleDAO.java:10-18, 33-39` |
| 심각도 | **높음** (면담유형이 DB 저장 안 되고 목록에서 null 출력) |

**문제**
`save()` INSERT 쿼리: `schedule_no, customer_name, scheduled_at, location, status` — `type`(면담유형), `preparation`(면담 준비사항) 없음.
`fromDb()` 복원: `type`, `preparation` 모두 복원하지 않음.
Runner 목록 출력(line 49) `s.getType()`이 null → "null" 출력.

**수정 방향**
1. `interview_schedules` 테이블에 `type VARCHAR`, `preparation TEXT` 컬럼 추가.
2. `save()` 쿼리에 포함, `fromDb()`에서 해당 필드 복원.

---

### [BUG-CON-04] InterviewScheduleRunner — 수정·취소 시 항목 선택 없이 항상 마지막 항목 자동 선택

| 항목 | 내용 |
|------|------|
| 유스케이스 | 면담일정을 관리한다 — A4(수정), A5(취소) |
| 관련 파일 | `dp/runner/usecase/InterviewScheduleRunner.java:123, 152` |
| 심각도 | **중간** (의도치 않은 항목 수정·취소 가능) |

**문제**
A4·A5 분기 모두 `interviewSchedules.get(interviewSchedules.size() - 1)`으로 항상 마지막 항목을 선택한다.
유스케이스 Basic Path Step 5는 "목록에서 조회할 면담 항목을 클릭한다"고 명시하므로, 사용자가 원하는 항목을 직접 선택해야 한다.

**수정 방향**
목록을 번호와 함께 출력한 뒤 `ConsoleHelper.readMenuChoice()`로 수정·취소할 항목을 직접 선택하도록 변경한다.

---

### [BUG-CON-05] InterviewSchedule — 등록·수정·취소 완료 결과의 datetime 필드 없음

| 항목 | 내용 |
|------|------|
| 유스케이스 | 면담일정을 관리한다 — A1 Step 5, A4 Step 5, A5 Step 3 |
| 관련 파일 | `dp/consultation/InterviewSchedule.java`, `dp/runner/usecase/InterviewScheduleRunner.java:104-107, 139-140, 155-158` |
| 심각도 | **중간** (완료 결과 필수 출력 항목 누락) |

**문제**
- 등록 완료(A1 Step 5): "면담번호, **등록일시**, 고객명, **면담 담당 설계사명**, 면담일시" 출력 요구. `InterviewSchedule`에 `registeredAt` 필드 없음, 담당 설계사 연결 없음.
- 수정 완료(A4 Step 5): "면담번호, **수정일시**" 출력 요구. `modify()` 메서드에 수정일시 설정 없음.
- 취소 완료(A5 Step 3): "면담번호, **취소일시**" 출력 요구. `cancel()` 메서드에 취소일시 설정 없음.

**수정 방향**
`InterviewSchedule`에 `registeredAt`, `modifiedAt`, `cancelledAt` 필드 추가 후 각 메서드에서 설정, Runner 출력 구문에 포함한다.

---

### [BUG-CON-06] InterviewRecordRunner — 면담일시 입력 누락 + E1 검증 불완전

| 항목 | 내용 |
|------|------|
| 유스케이스 | 면담기록을 관리한다 — A1 Step 2, Exception E1 |
| 관련 파일 | `dp/runner/usecase/InterviewRecordRunner.java:68-82` |
| 심각도 | **중간** (필수 입력값 수동 설정, 검증 불완전) |

**문제**
유스케이스 A1 Step 2: "고객명, **면담일시**, 면담 내용, 고객 반응, 후속 조치를 입력한다."
Runner는 면담일시 입력 단계가 없고 `record.save()` 내부에서 `LocalDateTime.now()`로 자동 세팅한다.
E1 필수 항목: 고객명·면담일시·면담 내용. Runner line 76은 `content`만 체크하며 고객명·면담일시 검증 없음.
저장 완료 결과(A1 Step 5) 출력 구문(line 87-88): "기록번호, **저장일시**, 고객명, **면담일시**" 중 저장일시·면담일시 누락.

**수정 방향**
1. Runner에 `ConsoleHelper.readDateTime("면담일시")` 입력 단계 추가.
2. `InterviewRecord`에 `interviewedAt`을 수동 세터로 받도록 수정.
3. E1 검증에 `customerName` 빈값 여부 추가.
4. 저장 완료 결과 출력에 `record.getInterviewedAt()` 포함.

---

### [BUG-CON-07] InterviewRecordDAO — customer_reaction·follow_up_action 저장 및 복원 누락

| 항목 | 내용 |
|------|------|
| 유스케이스 | 면담기록을 관리한다 — A1 Step 5, Basic Path Step 6 |
| 관련 파일 | `dp/dao/InterviewRecordDAO.java` |
| 심각도 | **높음** (핵심 업무 데이터 유실, 목록에서 interviewedAt null 출력) |

**문제**
`save()` INSERT: `record_no, customer_name, content, recorded_at` — `customer_reaction`(고객 반응), `follow_up_action`(후속 조치) 없음.
`findAll()` SELECT: `recorded_at` 컬럼이 없어서 복원 시 `interviewedAt` = null.
Runner 목록 표시(line 46) `r.getInterviewedAt()` → null 출력.
`fromDb()` 복원: `interviewedAt`, `customerReaction`, `followUpAction` 모두 null.

**수정 방향**
1. `interview_records` 테이블에 `customer_reaction TEXT`, `follow_up_action TEXT` 컬럼 추가.
2. `save()` INSERT에 포함, `findAll()` SELECT·`fromDb()`에서 복원.
3. `findAll()` SELECT에 `recorded_at` 포함.

---

### [BUG-CON-08] ProposalRunner — 면담 내용 요약 미전달 + 발송일시 미출력

| 항목 | 내용 |
|------|------|
| 유스케이스 | 보험상품을 제안한다 — Basic Path Step 2, Step 6 |
| 관련 파일 | `dp/runner/usecase/ProposalRunner.java:43-46, 97-99` |
| 심각도 | **중간** (컨텍스트 미전달, 완료 결과 항목 누락) |

**문제**
Step 2: "보험상품 제안 화면(고객 기본 정보, **면담 내용 요약**, 보험상품 목록)" 출력 요구.
면담 내용 요약(고객 반응·후속 조치)은 직전 `InterviewRecordRunner`에서 저장한 데이터이지만, `ProposalRunner.run()`에 파라미터가 없어 `InterviewRecord` 데이터를 전달받지 못한다.
Step 6: "발송 완료 결과(발송일시, 수신고객명)" 중 **발송일시**(`sentAt`) 미출력(line 98-99).

**수정 방향**
1. `ProposalRunner.run(InterviewRecord record)`으로 파라미터 추가하여 면담 내용 요약을 화면에 표시.
2. 발송 완료 결과 출력에 `proposal.getSentAt()` 포함.

---

---

## 계약 도메인 (contract)

### [BUG-CTR-01] PolicyApplicationRunner — 파일 업로드 일시 미출력 + E1(업로드 실패) 미구현

| 항목 | 내용 |
|------|------|
| 유스케이스 | 청약서를 작성한다 — Basic Path Step 8, Exception E1 |
| 관련 파일 | `dp/runner/usecase/PolicyApplicationRunner.java:112-122` |
| 심각도 | **중간** (완료 결과 항목 누락, 예외 흐름 미구현) |

**문제**
Step 8 "첨부 파일 업로드 완료 결과(**파일명, 업로드 일시**)" 중 업로드 일시가 출력되지 않는다. Runner line 115는 파일명만 하드코딩으로 출력한다. `PolicyApplication`에 `uploadedAt` 필드가 없어 일시 자체를 저장할 수 없다.
E1(파일 업로드 실패) 분기가 존재하지 않아 항상 업로드 성공으로 처리된다.

**수정 방향**
1. `PolicyApplication`에 `uploadedAt` 필드 추가, `attachSignature()` 호출 시 `LocalDateTime.now()` 세팅.
2. Runner Step 8 출력에 `application.getUploadedAt()` 포함.
3. 업로드 성공/실패 시뮬레이션 메뉴 추가 후 E1 분기 구현.

---

### [BUG-CTR-02] ContractInfoRunner — 목록·상세 패널 필수 항목 누락

| 항목 | 내용 |
|------|------|
| 유스케이스 | 계약 정보를 조회한다 — Basic Path Step 2, Step 6 |
| 관련 파일 | `dp/runner/usecase/ContractInfoRunner.java:97-108, 124-131`, `dp/dao/ContractDAO.java` |
| 심각도 | **높음** (유스케이스 핵심 출력 항목 다수 누락) |

**문제**
목록 출력(Step 2) 유스케이스 요구 컬럼: 계약번호/계약자명/보험종류/계약기간/**보험료**/납입현황/계약상태. Runner line 97 헤더·103-108 데이터에 **보험료, 납입현황** 없음.
상세 패널(Step 6) 유스케이스 요구 항목: 이름, **연락처**, 계약번호, 보험종류, 기간, 월보험료, 상태, **납입현황**(전체납입횟수/정상납입횟수/최근납입일), 특약정보. Runner line 124-131에 연락처·납입현황 미출력.
`Contract` DB 로딩 생성자 및 `ContractDAO.SELECT`에 `totalPayCount`, `paidCount`, `lastPaymentDate` 컬럼이 없어서 복원 자체가 불가능하다.

**수정 방향**
1. `contracts` 테이블에 `total_pay_count INT`, `paid_count INT`, `last_payment_date DATE` 컬럼 추가.
2. `ContractDAO.save()`, `findAll()`, `mapRow()`에 해당 컬럼 포함.
3. Runner 목록·상세 출력에 보험료·납입현황·연락처 추가.

---

### [BUG-CTR-03] ContractInfoRunner — 상세 패널 계약 상태 출력 오류

| 항목 | 내용 |
|------|------|
| 유스케이스 | 계약 정보를 조회한다 — Basic Path Step 6 |
| 관련 파일 | `dp/runner/usecase/ContractInfoRunner.java:130`, `dp/enums/ContractStatus.java` |
| 심각도 | **중간** (상태 표시 오류) |

**문제**
유스케이스 Step 6 계약 상태: "정상/**만기임박**/실효/해지". 상세 패널 출력 line 130:
```java
(contract.getStatus() == ContractStatus.NORMAL ? "정상" : "만기")
```
`ContractStatus.CANCELLED`(해지) 도 "만기"로 출력되고, 실효(LAPSED) 상태는 enum에도 없다.
목록 출력(line 101-102)은 NORMAL→정상, EXPIRED→만기, else→해지 로 처리하여 상세와 일관성도 없다.

**수정 방향**
1. `ContractStatus` enum에 `LAPSED`(실효) 추가.
2. `Contract.updateStatus()`에 "실효" 매핑 추가.
3. 상세 패널 출력(line 130)을 switch-case로 NORMAL/EXPIRED/CANCELLED/LAPSED 각각 처리.

---

### [BUG-CTR-04] ActivityPlanDAO — 핵심 필드 대부분 저장 누락 + activity_type null 전달

| 항목 | 내용 |
|------|------|
| 유스케이스 | 활동 계획을 작성한다 — Basic Path Step 3~8 |
| 관련 파일 | `dp/dao/ActivityPlanDAO.java:12-24` |
| 심각도 | **높음** (활동 계획 핵심 데이터 유실) |

**문제**
INSERT 저장 컬럼: `plan_no, author_name, activity_type, scheduled_date, target, proposed_insurance_type, status`.
저장되지 않는 핵심 필드:
- `plan_name` (계획명, **필수**)
- `end_date` (종료일, **필수**)
- `target_contract_count` (목표 계약 건수, **필수**)
- `target_contract_amount` (목표 계약 금액, **필수**)
- `target_new_customer` (목표 신규 고객 수)
- `memo`, `proposal_reason`

`activity_type` 파라미터가 `null`로 하드코딩(line 21). `ScheduleItem` 목록 전혀 저장 안 됨. `findAll()` 없어서 기존 계획 조회 불가.

**수정 방향**
1. `activity_plans` 테이블 컬럼 확장.
2. `save()` INSERT에 모든 필드 포함, `activity_type` null 제거.
3. `ScheduleItem` 저장용 별도 테이블 및 DAO 추가.
4. `findAll()` 구현.

---

### [BUG-CTR-05] ActivityPlanRunner — 스케줄 활동 일시 입력 단계 없음

| 항목 | 내용 |
|------|------|
| 유스케이스 | 활동 계획을 작성한다 — Basic Path Step 4 |
| 관련 파일 | `dp/runner/usecase/ActivityPlanRunner.java:158` |
| 심각도 | **중간** (필수 입력 항목 누락) |

**문제**
유스케이스 Step 4 일정 입력 필수 항목: 대상고객, 활동유형, **활동일시(YYYY.MM.DD HH:MM)**, 활동장소(선택), 메모(선택).
Runner line 158:
```java
ScheduleItem item = new ScheduleItem(scheduleCustomerId, actType, scheduleLocation, scheduleMemo);
```
활동 일시(`activityDateTime`)를 입력받는 단계가 없고 `ScheduleItem` 생성자에도 전달하지 않는다.

**수정 방향**
1. `ConsoleHelper.readDateTime("활동 일시")` 입력 단계 추가.
2. `ScheduleItem` 생성자에 `activityDateTime` 파라미터 추가하여 전달.

---

### [BUG-CTR-06] CustomerRegistrationRunner — addSpecialClause() 빈 값 중복 추가 + E1/E2 재시도 불가

| 항목 | 내용 |
|------|------|
| 유스케이스 | 고객 정보를 등록한다 — Basic Path Step 5(A2), Step 7(E1), Step 8(E2) |
| 관련 파일 | `dp/runner/usecase/CustomerRegistrationRunner.java:121-124, 132-160` |
| 심각도 | **중간** (데이터 오염, 재시도 흐름 누락) |

**문제**
특약 추가(line 121-124): `registration.addSpecialClause()` (인자 없는 호출)이 `registration.addSpecialClause(clause)` 바로 앞에 있어서 빈 특약이 먼저 추가된 뒤 실제 특약이 추가되는 이중 추가 버그.
E1(필수항목 누락) 및 E2(중복감지) 처리 시 `return`으로 즉시 종료 — 유스케이스는 "basic path 7번으로 돌아간다"고 명시하여 재시도가 가능해야 한다.

**수정 방향**
1. line 121의 `registration.addSpecialClause()` (무인자 호출) 삭제.
2. E1/E2 분기에서 `return` 대신 반복 루프로 재입력 가능하게 변경.

---

### [BUG-CTR-07] CustomerRegistrationDAO — SSN 원본 미저장으로 중복 검증 NPE 발생

| 항목 | 내용 |
|------|------|
| 유스케이스 | 고객 정보를 등록한다 — Basic Path Step 8, Exception E2 |
| 관련 파일 | `dp/dao/CustomerRegistrationDAO.java`, `dp/runner/usecase/CustomerRegistrationRunner.java:152-154` |
| 심각도 | **높음** (런타임 NPE로 E2 중복 검증 불가) |

**문제**
`save()` INSERT에 `ssn_masked`(마스킹 SSN)만 저장, 원본 SSN 컬럼 없음.
`findAll()` 복원 시 SSN 원본 자리에 `null` 전달 (`new CustomerRegistration(..., null, ...)` line 41).
Runner line 152-154에서 `r.getSsn().equals(...)` — `r.getSsn()`이 null이므로 **NullPointerException** 발생하거나 E2 검증이 항상 통과된다.
`save()`에 `ON DUPLICATE KEY UPDATE`도 없어서 동일 고객 재저장 시 PK 충돌 오류 발생.

**수정 방향**
1. `customer_registrations` 테이블에 `ssn VARCHAR` 컬럼 추가 (원본 또는 암호화 저장).
2. `findAll()` 복원 시 SSN 원본을 복원하여 `getSsn()` null 방지.
3. `save()`에 `ON DUPLICATE KEY UPDATE` 절 추가.

---

### [BUG-CTR-08] ContractStatisticsRunner — 계약 컨텍스트 미전달로 실제 통계 데이터 조회 불가

| 항목 | 내용 |
|------|------|
| 유스케이스 | 계약 통계 정보를 관리한다 — Basic Path Step 2~4 |
| 관련 파일 | `dp/runner/usecase/ContractStatisticsRunner.java:89-94`, `dp/runner/usecase/ContractInfoRunner.java:158-162` |
| 심각도 | **중간** (실제 계약 데이터 없이 수동 입력에 의존) |

**문제**
유스케이스는 `계약 정보를 조회한다`에서 선택한 특정 계약의 통계를 보여주는 흐름이다. 그러나 `ContractInfoRunner`가 `ContractStatisticsRunner.run()`을 파라미터 없이 호출(line 162)하여 선택된 계약 정보가 전달되지 않는다.
Runner line 91-94에서 계약번호·계약자명을 **수동 입력**받아야 하며, 실제 DB에서 납부 이력·청구 이력을 조회하지도 않는다 (동일한 컨텍스트 미전달 패턴).

**수정 방향**
`ContractStatisticsRunner.run(Contract contract)`으로 파라미터 추가, `ContractInfoRunner`에서 선택한 계약을 넘겨 계약번호·계약자명을 자동 표시하고 관련 납부·청구 이력을 DB에서 직접 조회.

---

---

## 보상/청구 도메인 (claim)

### [BUG-CLM-01] AccidentReportRunner — A1 인명사고 분기 전혀 미구현

| 항목 | 내용 |
|------|------|
| 유스케이스 | 사고를 접수한다 — Alternative A1 |
| 관련 파일 | `dp/runner/usecase/AccidentReportRunner.java` |
| 심각도 | **높음** (유스케이스 분기 전체 누락) |

**문제**
유스케이스 A1: 사고 유형이 "인명사고"인 경우 추가 입력 항목이 필요하다.
- 부상자 수
- 부상자 구분 (운전자/동승자/보행자 등)
- 부상 정도 (경상/중상/사망)
- 119 신고 여부

현재 Runner는 `selectAccidentType(AccidentType.PERSON, ...)` 선택 후 아무 추가 입력 없이 동일한 흐름을 계속 진행한다. 인명사고와 물적사고를 같은 흐름으로 처리하므로 A1 경로가 전혀 구현되지 않았다.

**수정 방향**
사고 유형 선택 후 `AccidentType.PERSON`인 경우 분기하여 위 항목들을 입력받고, `AccidentReport`에 해당 필드 추가(부상자수: `casualtyCount`, 부상정도: `injurySeverity`, 119신고: `emergencyReported`) 및 저장 로직 추가.

---

### [BUG-CLM-02] AccidentReportRunner — Step 6 현장출동 신청 완료 결과 항목 누락

| 항목 | 내용 |
|------|------|
| 유스케이스 | 사고를 접수한다 — Basic Path Step 6 |
| 관련 파일 | `dp/runner/usecase/AccidentReportRunner.java` |
| 심각도 | **중간** (완료 결과 출력 항목 불완전) |

**문제**
유스케이스 Step 6 "현장출동 신청 완료 결과" 출력 요구 항목:
- 출동번호 ✅ (`dispatch.getDispatchNo()`)
- **신청 시간** ❌ (미출력)
- **사고 위치** ❌ (미출력)
- **사고 상황** ❌ (미출력)
- **휴대폰 번호** ❌ (미출력)
- **차량번호** ❌ (미출력)

현재 Runner는 `dispatch.getDispatchNo()`만 출력하고 나머지 5개 항목을 모두 생략한다. `AccidentReport`에는 해당 필드가 모두 있으므로 출력 구문 추가만 필요하다.

**수정 방향**
현장출동 신청 완료 결과 출력 구문을 다음과 같이 확장:
```java
ConsoleHelper.printInfo("출동번호: " + dispatch.getDispatchNo()
    + " | 신청시간: " + report.getReportedAt()
    + " | 사고위치: " + report.getLocation()
    + " | 사고상황: " + report.getDamageType()
    + " | 휴대폰: " + report.getPhoneNo()
    + " | 차량번호: " + report.getVehicleNo());
```

---

### [BUG-CLM-03] AccidentReportDAO — 차량정보·사고위치 등 핵심 필드 미저장 + findAll() 없음

| 항목 | 내용 |
|------|------|
| 유스케이스 | 사고를 접수한다 — 전반 |
| 관련 파일 | `dp/dao/AccidentReportDAO.java` |
| 심각도 | **높음** (핵심 사고 데이터 유실, 연계 조회 불가) |

**문제**
`save()` INSERT 저장 컬럼: `accident_no, customer_id, customer_name, accident_type, reported_at, status`.
저장되지 않는 핵심 필드:
- `vehicle_no` (차량번호)
- `owner_name` (소유자명/피보험자명)
- `phone_no` (휴대폰 번호)
- `damage_type` (피해 유형)
- `location` (사고 위치)
- `needs_dispatch` (현장출동 필요 여부)

또한 `findAll()` 메서드가 존재하지 않아 다른 Runner(예: `DispatchRecordRunner`)에서 접수 건을 DB로부터 조회할 수 없다. 현재는 메모리 내 생성 객체만 사용 가능하여 JVM 재시작 후 데이터가 유실된다.

**수정 방향**
1. `accident_reports` 테이블에 `vehicle_no VARCHAR`, `owner_name VARCHAR`, `phone_no VARCHAR`, `damage_type VARCHAR`, `location VARCHAR`, `needs_dispatch BOOLEAN` 컬럼 추가.
2. `save()` INSERT에 해당 컬럼·값 포함.
3. `findAll()` 구현 — `AccidentReport` 쉘 객체 복원, 필요한 필드 setter로 복원.

---

### [BUG-CLM-04] DispatchRecordRunner — A1/A2 분기 미구현 + A3/A4 레이블 불일치

| 항목 | 내용 |
|------|------|
| 유스케이스 | 현장 출동 정보를 기록한다 — Alternative A1, A2, A3, A4 |
| 관련 파일 | `dp/runner/usecase/DispatchRecordRunner.java` |
| 심각도 | **중간** (대안 흐름 2개 미구현, 레이블 오류 2개) |

**문제**
1. **A1 미구현**: 유스케이스 A1은 "출동 요원이 현장 이동 중 고객 위치 업데이트를 받아 알림을 확인하는" 분기이다. Runner에 해당 분기 자체가 없다.
2. **A2 미구현**: 유스케이스 A2는 "현장 도착 전 고객과의 연락(전화/메시지) 처리" 분기이다. Runner에 해당 분기가 없다.
3. **A3 레이블 불일치**: 코드의 A3는 "사고 위치 갱신"으로 표기되나 유스케이스 A3는 "사고 상황 종료로 출동 취소"이다.
4. **A4 레이블 불일치**: 코드의 A4는 "출동 취소"로 표기되나 유스케이스 A4는 "정확한 위치 반영"이다.

A3/A4 코드 로직 자체도 유스케이스 의도와 반대로 구현되어 있을 가능성이 높다.

**수정 방향**
1. A1: 고객 위치 업데이트 알림 수신 분기 추가 (위치 정보 갱신 → 화면 표시).
2. A2: 현장 도착 전 연락 처리 분기 추가 (연락 방법 선택 → 메모 입력).
3. A3: 레이블 및 로직을 "사고 상황 종료 → 출동 취소" 로 수정.
4. A4: 레이블 및 로직을 "정확한 위치 정보 갱신" 으로 수정.

---

### [BUG-CLM-05] DispatchRecordDAO — 경찰출동·견인·특이사항·전송일시 미저장 + findAll() 없음

| 항목 | 내용 |
|------|------|
| 유스케이스 | 현장 출동 정보를 기록한다 — Basic Path Step 7~8 |
| 관련 파일 | `dp/dao/DispatchRecordDAO.java` |
| 심각도 | **높음** (현장 기록 핵심 데이터 유실) |

**문제**
`save()` INSERT 저장 컬럼: `record_no, dispatch_no, agent_name, status` — 4개 필드뿐.
저장되지 않는 핵심 필드:
- `police_required` (경찰 출동 여부)
- `towing_required` (견인 필요 여부)
- `notes` (현장 특이사항 및 요원 소견)
- `transmitted_at` (전송 일시)

사진 목록(`photos`)도 어떤 테이블에도 저장되지 않는다. `findAll()` 메서드도 없어 저장 후 조회가 불가능하다.

**수정 방향**
1. `dispatch_records` 테이블에 `police_required BOOLEAN`, `towing_required BOOLEAN`, `notes TEXT`, `transmitted_at DATETIME` 컬럼 추가.
2. `save()` INSERT에 해당 컬럼·값 포함.
3. 사진 저장용 `dispatch_record_photos` 테이블 생성 및 별도 저장 로직 추가(또는 파일명 목록을 VARCHAR로 직렬화).
4. `findAll()` 구현.

---

### [BUG-CLM-06] ClaimRequestDAO — findAll()에서 계좌 정보 복원 누락

| 항목 | 내용 |
|------|------|
| 유스케이스 | 보험금을 요청한다 — A4(등록 계좌 확인), A5(계좌 불일치 E1) |
| 관련 파일 | `dp/dao/ClaimRequestDAO.java:36-72` |
| 심각도 | **중간** (계좌 정보 로드 후 null, 지급 단계에서 오류 유발) |

**문제**
`save()` INSERT에는 `bank_name, account_no, account_holder`가 포함되어 저장은 된다.
그러나 `findAll()` SELECT 쿼리에 `bank_name, account_no, account_holder`가 포함되지 않아 DB에서 `ClaimRequest`를 로드하면 `getBankAccount()`가 null을 반환한다.

이후 `ClaimPaymentRunner`에서 DB 로드된 청구 건의 계좌 정보를 확인하려 할 때 NullPointerException이 발생하거나 계좌 정보가 비어있는 상태로 지급이 진행된다.

**수정 방향**
`findAll()` SELECT 쿼리에 `bank_name, account_no, account_holder` 추가, 복원 시 `BankAccount` 객체를 생성하여 `ClaimRequest`에 설정:
```java
String bank = rs.getString("bank_name");
String accNo = rs.getString("account_no");
String holder = rs.getString("account_holder");
if (bank != null) r.registerBankAccount(new BankAccount(bank, accNo, holder));
```

---

### [BUG-CLM-07] ClaimRequestRunner — 피보험자 선택이 항상 청구 고객 본인으로 고정

| 항목 | 내용 |
|------|------|
| 유스케이스 | 보험금을 요청한다 — Basic Path Step 3 |
| 관련 파일 | `dp/runner/usecase/ClaimRequestRunner.java:79` |
| 심각도 | **낮음** (유스케이스 흐름 불일치) |

**문제**
유스케이스 Step 3: "고객은 보험금 청구 대상 피보험자를 **선택**한다." 실손·자동차 보험에서 피보험자가 계약자와 다를 수 있으나 Runner line 79:
```java
claim.selectInsured(customer);  // 항상 청구 고객 자신으로 고정
```
피보험자 선택 UI 없이 무조건 청구 고객 본인으로 설정된다.

**수정 방향**
피보험자와 청구 고객이 동일한지 여부를 물어보고, 다를 경우 피보험자 이름·관계를 별도 입력받는 분기 추가:
```java
boolean sameAsCustomer = ConsoleHelper.readYesNo("[고객] 피보험자가 본인입니까?");
if (sameAsCustomer) {
    claim.selectInsured(customer);
} else {
    String insuredName = ConsoleHelper.readNonEmpty("  피보험자 이름: ");
    // 별도 피보험자 설정
}
```

---

### [BUG-CLM-08] DamageInvestigationDAO — findAll()에서 reject_reason·investigated_at 복원 누락

| 항목 | 내용 |
|------|------|
| 유스케이스 | 손해를 조사한다 — A3(면책 종결), Basic Path Step 7 |
| 관련 파일 | `dp/dao/DamageInvestigationDAO.java:38-66` |
| 심각도 | **중간** (면책 사유·조사일시 조회 시 null 출력) |

**문제**
`save()` INSERT에는 `reject_reason`과 `investigated_at`이 포함되어 저장은 된다.
그러나 `findAll()` SELECT 쿼리에서 두 컬럼을 조회하지 않는다:
```sql
SELECT investigation_no, claim_no, claim_customer, customer_id, handler_name,
       our_fault_ratio, counter_ratio, recognized_damage, opinion,
       result, status FROM damage_investigations
-- reject_reason, investigated_at 누락
```
DB에서 로드된 `DamageInvestigation` 객체는 `getRejectReason()` → null, `getInvestigatedAt()` → null 반환.
다음 화면(보험금 산출 등)에서 조사 결과 요약 시 면책 사유와 조사일시가 공백으로 출력된다.

**수정 방향**
`findAll()` SELECT에 `reject_reason, investigated_at` 추가하고, `DamageInvestigation` 복원 시 해당 필드 setter 호출.

---

### [BUG-CLM-09] ClaimPaymentDAO — 지급일시·예약일시·수령인·계좌 정보 미저장

| 항목 | 내용 |
|------|------|
| 유스케이스 | 보험금을 지급한다 — Basic Path Step 7, A1(예약 지급) |
| 관련 파일 | `dp/dao/ClaimPaymentDAO.java` |
| 심각도 | **높음** (지급 완료 이력 핵심 데이터 유실) |

**문제**
`save()` INSERT 저장 컬럼: `payment_no, calculation_no, final_amount, status` — 4개 필드뿐.
저장되지 않는 핵심 필드:
- `paid_at` (실제 지급일시 — Step 7 출력 요구)
- `scheduled_at` (예약 일시 — A1 분기)
- `payment_type` (즉시/예약 구분)
- `failure_reason` (이체 실패 사유 — E2)

또한 `recipient`(수령인), `account`(수령 계좌) 정보가 전혀 저장되지 않아 `findAll()` 로드 후 수령인명·계좌번호가 null이 된다. 지급 이력 조회 화면에서 수령인과 계좌 정보를 표시할 수 없다.

**수정 방향**
1. `claim_payments` 테이블에 `paid_at DATETIME`, `scheduled_at DATETIME`, `payment_type VARCHAR`, `recipient_name VARCHAR`, `account_no VARCHAR`, `failure_reason TEXT` 컬럼 추가.
2. `save()` INSERT에 해당 컬럼·값 포함.
3. `findAll()` SELECT·복원에 해당 컬럼 추가.

---

### [BUG-CLM-10] ClaimPaymentRunner — 지급 상세 화면에 접수번호·사고일자 미출력

| 항목 | 내용 |
|------|------|
| 유스케이스 | 보험금을 지급한다 — Basic Path Step 2 |
| 관련 파일 | `dp/runner/usecase/ClaimPaymentRunner.java` |
| 심각도 | **중간** (지급 화면 필수 표시 항목 누락) |

**문제**
유스케이스 Step 2 "보험금 지급 화면" 출력 요구 항목:
- 지급번호 ✅
- 산출번호 ✅
- 수령인 ✅
- 계좌 ✅
- 지급액 ✅
- **접수번호** ❌ (미출력)
- **사고 일자** ❌ (미출력)

`showPayment()` 호출 구문이 접수번호와 사고 일자를 표시하지 않는다. 접수번호와 사고 일자는 `ClaimPayment → ClaimCalculation → DamageInvestigation → ClaimRequest → AccidentReport` 체인을 따라가면 접근 가능하나, DB 로드 객체는 이 체인이 쉘 객체로만 구성되어 null이 된다.

**수정 방향**
1. `ClaimPaymentRunner`의 지급 정보 출력 구문에 `calculation.getInvestigation().getClaim().getClaimNo()`(접수번호 역할) 및 사고 접수 일자 표시 추가.
2. BUG-CLM-03·06·09 수정을 통해 체인 데이터가 DB에서 실제로 복원된 후에야 null 없이 출력 가능.

---

---

## 재무/납입 도메인 (payment / finance)

### [BUG-FIN-01] PaymentRunner — 납입 완료 결과 화면 미출력 (납입번호·납입일시 누락)

| 항목 | 내용 |
|------|------|
| 유스케이스 | 보험료를 납입한다 — Basic Path 마지막 단계 |
| 관련 파일 | `dp/runner/usecase/PaymentRunner.java:87-103` |
| 심각도 | **중간** (완료 결과 화면 전체 누락) |

**문제**
`payment.submit()` 호출 후 Runner는 `PaymentDAO.save()`와 `PaymentRecord` 생성 로그만 출력하고 납입 완료 결과 화면을 출력하지 않는다. 유스케이스는 납입 완료 시 다음을 출력하도록 요구한다:
- 납입 신청번호 (`payment.getPaymentNo()`)
- 납입 일시 (`payment.getRequestedAt()`)
- 최종 결제액 (이미 출력됨 — 다만 `submit()` **이전**에 출력되어 신청번호 없이 표시됨)
- 납입 방법

`run(Customer)` 오버로드(MyInsuranceViewRunner에서 호출) 역시 동일하게 완료 결과 화면이 없다.

**수정 방향**
`payment.submit()` 호출 직후 완료 결과 출력 구문 추가:
```java
ConsoleHelper.printSuccess("납입 완료");
ConsoleHelper.printInfo("납입번호: " + payment.getPaymentNo()
    + " | 납입일시: " + payment.getRequestedAt()
    + " | 최종결제액: " + payment.getDiscountedAmount() + "원"
    + " | 납입방법: " + payment.getPaymentMethod());
```

---

### [BUG-FIN-02] PaymentDAO — PaymentItem 계약별 납입 항목 미저장 + findAll() 없음

| 항목 | 내용 |
|------|------|
| 유스케이스 | 보험료를 납입한다 — 전반 / 납부 내역을 관리한다 — 이력 조회 |
| 관련 파일 | `dp/dao/PaymentDAO.java` |
| 심각도 | **높음** (납입 항목 단위 이력 유실, 이력 조회 불가) |

**문제**
`PaymentDAO.save()` INSERT 저장 컬럼: `payment_no, customer_id, customer_name, total_amount, payment_method, requested_at, status`. 한 번의 납입 신청(Payment)에 포함된 `PaymentItem` 목록(계약별 납입 횟수·소계)이 어떤 테이블에도 저장되지 않는다.

재무회계 담당자가 어떤 계약에 몇 회, 얼마가 납입되었는지 DB에서 조회할 수 없다. 또한 `findAll()` 메서드가 없어 납입 신청 이력 자체를 조회하는 화면이 존재할 경우 구현이 불가능하다.

**수정 방향**
1. `payment_items` 테이블 생성 (`payment_no, contract_no, count, subtotal`).
2. `PaymentDAO.save()` 후 PaymentItem 목록을 `payment_items` 테이블에 일괄 INSERT.
3. `findAll()` 구현 (헤더 Payment + 항목 join 또는 별도 조회).

---

### [BUG-FIN-03] InsuranceCancellationRunner — 해약 완료 결과에 해지번호·해지일시 미출력

| 항목 | 내용 |
|------|------|
| 유스케이스 | 보험을 해지한다 — Basic Path Step 7 |
| 관련 파일 | `dp/runner/usecase/InsuranceCancellationRunner.java:138` |
| 심각도 | **중간** (완료 결과 필수 항목 누락) |

**문제**
유스케이스 Step 7 완료 시 "해약 신청이 완료되었습니다" 메시지와 함께 **해지번호**, **해지 처리 일시**를 출력해야 한다. 현재 Runner line 138:
```java
ConsoleHelper.printSuccess("[시스템] 보험 해약이 완료되었습니다. 환급금은 추후 별도 안내 드리겠습니다.");
```
`cancellation.getCancellationNo()`와 `cancellation.getCanceledAt()`이 출력되지 않는다.

**수정 방향**
```java
ConsoleHelper.printSuccess("보험 해약이 완료되었습니다.");
ConsoleHelper.printInfo("해지번호: " + cancellation.getCancellationNo()
    + " | 해지일시: " + cancellation.getCanceledAt());
ConsoleHelper.printInfo("환급금은 추후 별도 안내 드리겠습니다.");
```

---

### [BUG-FIN-04] CancellationDAO — 상세 사유(detail_reason) 미저장, cancelled_at 미복원

| 항목 | 내용 |
|------|------|
| 유스케이스 | 보험을 해지한다 — Alternative A1, Basic Path Step 7 |
| 관련 파일 | `dp/dao/CancellationDAO.java` |
| 심각도 | **중간** (A1 기타 상세사유 유실, 해지일시 미복원) |

**문제**
`save()` INSERT 쿼리에 `detail_reason`(기타 선택 시 필수 입력 상세 사유) 컬럼이 없어 `Cancellation.detailReason` 값이 저장되지 않는다. A1 분기로 입력한 상세 사유가 DB 재조회 시 유실된다.

`findAll()` SELECT 쿼리에 `cancelled_at`이 없어 해지일시가 복원되지 않는다. `RefundListRunner.showDetail()`에서 `cancellation.getCanceledAt()`을 출력하면 null이 된다.

**수정 방향**
1. `cancellations` 테이블에 `detail_reason TEXT` 컬럼 추가.
2. `save()` INSERT에 `detail_reason` 포함.
3. `findAll()` SELECT에 `cancelled_at` 추가, 복원 시 setter 호출.

---

### [BUG-FIN-05] RefundListRunner — 해약 상세 화면에 해지사유·해지일시 미출력

| 항목 | 내용 |
|------|------|
| 유스케이스 | 해약 환급 내역을 조회한다 — Basic Path Step 3 |
| 관련 파일 | `dp/runner/usecase/RefundListRunner.java:107-125` |
| 심각도 | **중간** (상세 화면 필수 항목 누락) |

**문제**
`showDetail()` 메서드가 출력하는 항목: `해지번호, 계약번호, 고객명, 진행상태, 기본환급금, 실지급환급금`.
유스케이스 상세 화면 요구 항목 중 **해지 사유**(`reason`)와 **해지 일시**(`canceledAt`)가 누락되어 있다.

또한 `reason`은 `CancellationDAO.findAll()`에서 복원되나 `canceledAt`은 BUG-FIN-04로 인해 null이다.

**수정 방향**
`showDetail()` 출력 구문에 추가:
```java
System.out.println("  해지 사유 : " + (cancellation.getReason() != null ? cancellation.getReason() : "-"));
System.out.println("  해지 일시 : " + (cancellation.getCanceledAt() != null ? cancellation.getCanceledAt() : "-"));
```
BUG-FIN-04 수정 후 `canceledAt`이 복원되면 정상 출력 가능.

---

### [BUG-FIN-06] RefundPaymentDAO — 이체일시·알림발송여부 미저장 + findAll()에서 Cancellation 체인 단절로 DB 로드 건 이체 항상 실패

| 항목 | 내용 |
|------|------|
| 유스케이스 | 해약 환급금을 지급한다 — Basic Path, E2 시뮬레이션 이후 |
| 관련 파일 | `dp/dao/RefundPaymentDAO.java` |
| 심각도 | **높음** (DB 로드된 지급 건 이체 항상 실패, 핵심 이력 데이터 미저장) |

**문제 1 — 핵심 필드 미저장**
`save()` INSERT 저장 컬럼: `payment_no, refund_no, cancellation_no, final_amount, status`.
저장되지 않는 필드:
- `transferred_at` (이체 완료 일시)
- `notice_sent` (알림톡 발송 여부)
- `notice_failure_message` (E3 발송 실패 메시지)
- `otp_fail_count` (E1 실패 횟수)

이체 완료 일시를 저장하지 않아 환급금 지급 이력 화면에서 이체일시를 표시할 수 없다.

**문제 2 — findAll()의 Cancellation 체인 단절로 런타임 이체 실패**
`findAll()`에서 `RefundCalculation` 쉘 객체의 `cancellation`이 null로 생성된다:
```java
RefundCalculation refundShell = new RefundCalculation(
    rno != null ? rno : "?", null, 0, null, 0, 0, 0, 0, 0, null);
//                             ^^^^ cancellation = null
```
`RefundPaymentRunner`에서 계좌 자동 로드 시 `payment.getRefund().getCancellation()` → null → 등록 계좌 fallback 탐색 실패 → `account = null` 상태로 `payment.execute()` 호출 → `RefundPayment.execute()`의 `if (account == null || !account.isVerified())` 조건 충족 → `handleTransferFailure()` 강제 실행 → **E2 오류로 이체 실패**.

**수정 방향**
1. `refund_payments` 테이블에 `transferred_at DATETIME`, `notice_sent BOOLEAN`, `otp_fail_count INT` 컬럼 추가.
2. `save()` INSERT에 해당 컬럼·값 포함.
3. `findAll()` 복원 시 `cancellation_no`를 이용해 `CancellationDAO`에서 해당 해지 건을 조회하여 `refundShell`의 `cancellation`을 올바르게 설정.

---

---

## 문의 도메인 (inquiry)

### [BUG-INQ-01] InquiryDAO — title·첨부파일 미저장 + findAll() / findByCustomer() 없음

| 항목 | 내용 |
|------|------|
| 유스케이스 | 문의한다 — Basic Path Step 7, Alternative A2 |
| 관련 파일 | `dp/dao/InquiryDAO.java` |
| 심각도 | **높음** (제목 미저장, 첨부파일 미저장, 이력 조회 불가) |

**문제**
`save()` INSERT 저장 컬럼: `inquiry_no, customer_name, inquiry_type, content, status, created_at`.
유스케이스 Step 7은 "문의 유형, **제목**, 내용, **첨부 파일**을 저장"하도록 명시하나 두 필드 모두 누락:
- `title` (제목, **필수** — 최대 50자)
- `attachment_file_name` (첨부 파일명)
- `attachment_file_size` (첨부 파일 크기)

또한 A2 분기(문의 내역 조회)에서 고객의 과거 문의 목록을 조회해야 하지만 `findAll()` 또는 `findByCustomerName()` 메서드가 존재하지 않아 DB 조회 자체가 불가능하다.

**수정 방향**
1. `inquiries` 테이블에 `title VARCHAR(50)`, `attachment_file_name VARCHAR`, `attachment_file_size BIGINT` 컬럼 추가.
2. `save()` INSERT에 해당 컬럼·값 포함.
3. `findAll()` 또는 `findByCustomerName(String name)` 구현.

---

### [BUG-INQ-02] InquiryRunner — A2 문의 내역 조회가 실제 DB 조회 없이 빈 메서드만 호출

| 항목 | 내용 |
|------|------|
| 유스케이스 | 문의한다 — Alternative A2 |
| 관련 파일 | `dp/runner/usecase/InquiryRunner.java:107-118` |
| 심각도 | **높음** (A2 전체 기능 미구현) |

**문제**
A2 분기(문의 내역 조회) 코드:
```java
Inquiry historyInquiry = new Inquiry();
page.switchTab();
historyInquiry.getHistoryList();   // 빈 메서드 {}
historyInquiry.getDetail();        // 빈 메서드 {}
ConsoleHelper.printStage("시스템", "문의 상세 페이지를 표시합니다.");
```
`getHistoryList()`와 `getDetail()`이 모두 빈 메서드로, 실제로는 아무 문의 데이터도 화면에 출력되지 않는다. `InquiryDAO.findAll()` 자체도 없어 DB 조회가 불가능하다.

유스케이스 A2 요구 사항:
- DB에서 현재 로그인 고객의 문의 목록 조회 및 출력 (번호·유형·제목·접수일시·상태)
- 고객이 항목을 선택하면 상세 조회 출력 (내용·첨부파일·답변 내용·답변일시)

**수정 방향**
BUG-INQ-01에서 `findByCustomerName()` 구현 후, A2 분기를 다음과 같이 수정:
```java
List<Inquiry> histories = InquiryDAO.findByCustomerName(customer.getName());
if (histories.isEmpty()) {
    ConsoleHelper.printInfo("문의 내역이 없습니다.");
} else {
    // 목록 출력 후 선택 → 상세 출력
}
```

---

### [BUG-INQ-03] InquiryRunner — Step 11 A4 분기 로직 오류: PENDING 문의를 "답변 완료 확인"으로 표시

| 항목 | 내용 |
|------|------|
| 유스케이스 | 문의한다 — Step 11, Alternative A4 |
| 관련 파일 | `dp/runner/usecase/InquiryRunner.java:205-230` |
| 심각도 | **중간** (A4 분기가 의미 없이 구현됨) |

**문제**
Step 11에서 `detailChoice` 값에 관계없이 항상 방금 접수한 PENDING 상태의 `inquiry` 객체 상세 정보를 출력한다:
```java
int detailChoice = ConsoleHelper.readMenuChoice(
        "[고객] 처리를 선택하세요.",
        "문의 상세 조회", "답변 완료 문의 확인 (A4)");

// detailChoice 분기 없이 항상 실행됨
inquiry.getDetail();
ConsoleHelper.printStage("시스템", "문의 상세 페이지를 표시합니다.");
ConsoleHelper.printInfo("... 처리 상태: " + (status == PENDING ? "답변 대기" : "답변 완료"));

if (detailChoice == 2) {
    ConsoleHelper.printInfo("답변 완료된 문의 내용을 표시합니다.");  // 내용 없음
}
```
A4 분기(답변 완료 문의 확인)는 방금 접수된 PENDING 문의가 아니라 DB에서 `ANSWERED` 상태의 다른 문의를 별도로 조회해야 한다. 현재는 동일한 PENDING 문의를 보여주면서 "답변 완료 내용 표시" 메시지만 추가하므로 실제로는 아무 답변도 출력되지 않는다.

**수정 방향**
`detailChoice == 2`일 때 현재 문의 대신 DB에서 `ANSWERED` 상태의 문의 목록을 조회하여 선택 후 `answerContent`·`answeredAt`을 출력:
```java
if (detailChoice == 2) {
    List<Inquiry> answered = InquiryDAO.findByCustomerNameAndStatus(
            customer.getName(), InquiryStatus.ANSWERED);
    // 목록 표시 → 선택 → 답변 내용·답변일시 출력
}
```

---

### [BUG-INQ-04] InquiryRunner — E1 필수 항목 검증 실패 시 재입력 불가, A3 E2 후 재첨부 루프 없음

| 항목 | 내용 |
|------|------|
| 유스케이스 | 문의한다 — Exception E1, Alternative A3 (E2) |
| 관련 파일 | `dp/runner/usecase/InquiryRunner.java:176-184, 158-165` |
| 심각도 | **중간** (예외 후 재시도 흐름 누락) |

**문제 1 — E1 재입력 불가**
유스케이스 E1: "누락된 항목을 입력한 후 [제출] 버튼을 다시 클릭한다" — 재입력이 가능해야 한다.
Runner line 183에서 `return`으로 즉시 종료, 처음부터 다시 실행해야 한다.

**문제 2 — A3 E2 후 재첨부 불가**
유스케이스 A3 E2: 파일 크기 초과 시 "파일 첨부 버튼을 다시 클릭하여 다른 파일을 선택해주세요."라는 안내를 출력하지만, Runner는 다시 파일 첨부 여부를 묻는 루프 없이 그대로 진행한다. 사용자가 다른 파일로 재시도하려면 처음부터 다시 실행해야 한다.

**수정 방향**
1. E1: 제목/내용/유형 입력 단계를 `while(!inquiry.validateRequired())` 루프로 감싸거나, 검증 실패 시 해당 입력 단계로 돌아가도록 구조 변경.
2. A3 E2: 파일 첨부 블록을 루프로 감싸, E2 발생 시 다시 첨부 여부를 물어보도록 변경:
```java
boolean attached = false;
while (ConsoleHelper.readYesNo("  [A3] 파일을 첨부하시겠습니까?")) {
    // 파일 입력 및 E2 검증
    if (inquiry.validateFileSize()) { attached = true; break; }
    ConsoleHelper.printError("[E2] 10MB 이하 파일을 선택해주세요.");
}
```

---