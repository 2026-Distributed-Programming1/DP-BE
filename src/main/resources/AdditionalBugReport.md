# 추가 버그 리포트 (BugReport.md 미포함 신규 발견)

> 기준: BugReport.md(BUG-EDU-01~06, BUG-SAL-01~06, BUG-CON-01~08, BUG-CTR-01~08, BUG-CLM-01~10, BUG-FIN-01~06, BUG-INQ-01~04)에 없는 문제만 기록.
> 작성일: 2026-05-21

---

## 교육 도메인 (education)

### [BUG-NEW-EDU-01] EducationPreparationDAO — status 컬럼 항상 NULL로 저장·미복원

| 항목 | 내용 |
|------|------|
| 유스케이스 | 교육 제반을 등록한다 |
| 관련 파일 | `dp/dao/EducationPreparationDAO.java:28`, `dp/dao/EducationPreparationDAO.java:33-56` |
| 심각도 | **높음** (상태 관리 완전 무력화) |

**문제**

`save()` INSERT 구문의 마지막 파라미터가 `null`로 하드코딩되어 있다.

```java
// EducationPreparationDAO.java:22-29
DBA.executeUpdate(
    "INSERT INTO education_preparations"
    + " (prep_no, plan_no, trainer_name, venue, material_ready,"
    + "  textbook_status, attendance_list, status)"
    + " VALUES (?,?,?,?,?,?,?,?)"
    + " ON DUPLICATE KEY UPDATE ..."
    + "  attendance_list=VALUES(attendance_list), status=VALUES(status)",
    ...
    null);   // ← status 항상 NULL 저장
```

`findAll()` SELECT 쿼리에도 `status` 컬럼이 포함되지 않아, DB에서 로드 시 상태 값이 복원되지 않는다.

**수정 방향**

1. `save()` 마지막 파라미터를 `e.getStatus()`(또는 `e.getStatus() != null ? e.getStatus().name() : null`)로 수정.
2. `findAll()` SELECT에 `status` 추가, 복원 시 setter 호출.

---

### [BUG-NEW-EDU-02] EducationExecutionDAO — status 항상 NULL, memo 필드 스키마 없음·미저장

| 항목 | 내용 |
|------|------|
| 유스케이스 | 교육을 진행한다 — Basic Path Step 5 |
| 관련 파일 | `dp/dao/EducationExecutionDAO.java:21`, `dp/runner/usecase/EducationExecutionRunner.java:82` |
| 심각도 | **중간** (실행 상태 미저장, 메모 유실) |

**문제**

`save()` INSERT 구문에서 `status` 파라미터가 `null`로 하드코딩되어 있다.

```java
// EducationExecutionDAO.java:9-21
DBA.executeUpdate(
    "INSERT INTO education_executions (execution_no, prep_no, trainer_name,"
    + " executed_at, attendee_count, status)"
    + " VALUES (?,?,?,?,?,?)"
    + " ON DUPLICATE KEY UPDATE attendee_count=VALUES(attendee_count)",
    execNo,
    ...,
    e.getAttendanceCount(),
    null);  // ← status 항상 NULL
```

또한 Runner에서 `execution.setMemo(memo)` 로 설정하지만 `education_executions` 테이블에 `memo` 컬럼이 없고 `save()`에서도 저장하지 않아 교육 진행 메모가 DB에 전혀 기록되지 않는다.

**수정 방향**

1. `save()` 마지막 파라미터를 `e.getStatus() != null ? e.getStatus().name() : null`로 수정.
2. `education_executions` 테이블에 `memo TEXT` 컬럼 추가.
3. `save()` INSERT에 `memo` 컬럼·값 포함.

---

### [BUG-NEW-EDU-03] EducationExecutionRunner — 제반 항목 선택 없이 항상 마지막 제반을 자동 선택

| 항목 | 내용 |
|------|------|
| 유스케이스 | 교육을 진행한다 — Basic Path Step 1 |
| 관련 파일 | `dp/runner/usecase/EducationExecutionRunner.java:49` |
| 심각도 | **중간** (의도치 않은 제반 자동 선택) |

**문제**

Runner line 49:

```java
EducationPreparation preparation = educationPreparations.get(educationPreparations.size() - 1);
```

등록된 교육 제반이 여러 건인 경우 사용자가 진행할 교육을 선택할 수 없고, 항상 가장 마지막에 등록된 제반이 자동으로 선택된다. 유스케이스는 담당자가 진행할 교육 제반을 직접 선택하는 흐름을 명시한다.

**수정 방향**

```java
// 목록 출력 후 선택
String[] prepOptions = educationPreparations.stream()
        .map(p -> "#" + p.getSetupNumber() + " - " + p.getLocation()
                + " / 강사: " + p.getInstructorName())
        .toArray(String[]::new);
int prepChoice = ConsoleHelper.readMenuChoice(
        "[영업교육담당자] 진행할 교육 제반을 선택하세요:", prepOptions);
EducationPreparation preparation = educationPreparations.get(prepChoice - 1);
```

---

## 영업 도메인 (sales)

### [BUG-NEW-SAL-01] ActivityPlanDAO — findAll()에서 plan_no 미복원으로 ID 덮어쓰기

| 항목 | 내용 |
|------|------|
| 유스케이스 | 활동 계획을 작성한다 — 목록 조회 후 수정·참조 |
| 관련 파일 | `dp/dao/ActivityPlanDAO.java:61` |
| 심각도 | **높음** (DB 복원 객체 PK 손실, 수정 시 엉뚱한 레코드 업데이트) |

**문제**

`findAll()` 복원 블록에서 `new ActivityPlan()`으로 객체를 생성하면 `ActivityPlan` 생성자가 내부 `static sequence` 카운터를 증가시키며 새 `planId`를 자동 할당한다. DB에서 읽어 온 `plan_no` 값을 `p.setPlanId(rs.getString("plan_no"))`로 설정하는 코드가 없으므로, 복원된 객체의 `planId`는 JVM 메모리 시퀀스 기반의 새 ID가 된다.

이후 해당 객체를 수정하여 `save()`를 호출하면 `ON DUPLICATE KEY UPDATE`가 엉뚱한 키로 실행되어 다른 레코드가 오염되거나 신규 레코드가 중복 생성된다.

**수정 방향**

`findAll()` 복원 블록에 다음 한 줄 추가:

```java
p.setPlanId(rs.getString("plan_no"));
```

---

### [BUG-NEW-SAL-02] ChannelScreeningDAO — rejection_reason 컬럼 없음으로 거절 사유 영구 유실

| 항목 | 내용 |
|------|------|
| 유스케이스 | 판매채널 채용을 심사한다 — Alternative A1 (거절) |
| 관련 파일 | `dp/dao/ChannelScreeningDAO.java:17-29`, `dp/sales/ChannelScreening.java:60-62` |
| 심각도 | **중간** (A1 핵심 데이터 유실) |

**문제**

`ChannelScreening.reject()` 호출 시 `rejectionReason` 필드에 거절 사유가 설정된다(Runner에서 `setRejectionReason()` 호출). 그러나 `ChannelScreeningDAO.save()` INSERT에 `rejection_reason` 컬럼이 없고, 스키마 `channel_screenings` 테이블에도 해당 컬럼이 존재하지 않는다. `findAll()` 복원 시에도 거절 사유가 복원되지 않는다.

거절된 심사 건을 재조회하면 `getRejectionReason()`이 항상 null을 반환한다.

**수정 방향**

1. `channel_screenings` 테이블에 `rejection_reason TEXT` 컬럼 추가.
2. `ChannelScreeningDAO.save()` INSERT에 `rejection_reason` 컬럼과 `s.getRejectionReason()` 값 포함.
3. `findAll()` SELECT에 `rejection_reason` 추가, 복원 시 `setRejectionReason()` 호출.

---

## 상담/면담 도메인 (consultation)

### [BUG-NEW-CON-01] ConsultationRequestDAO — findAll()에서 accepted_at 미복원 (setter 미호출)

| 항목 | 내용 |
|------|------|
| 유스케이스 | 상담을 요청한다 — Basic Path Step 10 (수락일시 출력) |
| 관련 파일 | `dp/dao/ConsultationRequestDAO.java:39-50` |
| 심각도 | **중간** (수락일시 DB 저장은 되나 로드 후 null) |

**문제**

`findAll()` 복원 블록에서 `accepted_at`을 DB에서 읽어 지역 변수 `acceptedAt`에 담지만, `ConsultationRequest` 객체에 설정하는 setter 호출이 없다.

```java
// ConsultationRequestDAO.java:39-50
java.sql.Timestamp acceptedTs = rs.getTimestamp("accepted_at");
java.time.LocalDateTime acceptedAt = acceptedTs != null ? acceptedTs.toLocalDateTime() : null;
ConsultationRequest cr = new ConsultationRequest(...);
cr.setReceivedAt(receivedAt);
// ← setAcceptedAt(acceptedAt) 호출 없음
return cr;
```

또한 `ConsultationRequest` 도메인 클래스에 `setAcceptedAt()` setter 자체가 없어, 읽어 온 값을 설정할 방법이 없다.

**수정 방향**

1. `ConsultationRequest`에 `setAcceptedAt(LocalDateTime acceptedAt)` setter 추가.
2. `findAll()` 복원 블록에 `cr.setAcceptedAt(acceptedAt)` 호출 추가.

---

### [BUG-NEW-CON-02] UnderwritingDAO — riskGrade·reviewOpinion 스키마 컬럼 없음·미저장

| 항목 | 내용 |
|------|------|
| 유스케이스 | 인수 심사를 한다 — Basic Path Step 9 (수동 심사 A1 포함) |
| 관련 파일 | `dp/dao/UnderwritingDAO.java`, `dp/consultation/Underwriting.java` |
| 심각도 | **중간** (수동 심사 상세 데이터 영구 유실) |

**문제**

`UnderwritingDAO.save()` INSERT에 저장되는 컬럼은 `underwriting_no, app_type, app_no, customer_name, result, reviewed_at` 6개뿐이다.

`Underwriting` 도메인 객체에 존재하는 `riskGrade`(위험등급), `reviewOpinion`(심사 의견) 필드가 저장되지 않고, `underwritings` 테이블 스키마에도 해당 컬럼이 없다.

특히 수동 심사(A1) 경로에서 담당자가 입력한 심사 유형, 필요 서류 첨부 여부, 심사 의견(`manualReview()` 호출 시 전달)이 DB에 전혀 기록되지 않아 심사 이력 감사(audit)가 불가능하다.

**수정 방향**

1. `underwritings` 테이블에 `risk_grade VARCHAR`, `review_opinion TEXT` 컬럼 추가.
2. `UnderwritingDAO.save()` INSERT에 해당 컬럼과 `u.getRiskGrade()`, `u.getReviewOpinion()` 포함.
3. `findAll()` 구현 시 해당 컬럼 복원.

---

## 계약 도메인 (contract)

### [BUG-NEW-CTR-01] ExpiringContractManagementRunner — DAO·테이블 없어 모든 처리 이력 JVM 재시작 시 유실

| 항목 | 내용 |
|------|------|
| 유스케이스 | 만기 계약을 관리한다 — 전반 |
| 관련 파일 | `dp/runner/usecase/ExpiringContractManagementRunner.java`, `dp/contract/ExpiringContractManagement.java` |
| 심각도 | **높음** (업무 이력 전체 미지속) |

**문제**

Runner에서 `ExpiringContractManagement` 객체에 안내 메모(`setNoticeMemo()`), 안내 일자(`setNoticeDate()`), 갱신 보험료(`setRenewalPremium()`), 고객 반응(`setCustomerResponse()`) 등을 설정하지만, 이 도메인에 대응하는 DAO 클래스와 DB 테이블이 존재하지 않는다.

JVM 프로세스가 종료되면 만기 도래 안내 발송 이력, 갱신 협의 결과, 고객 반응 데이터가 모두 유실된다.

**수정 방향**

1. `expiring_contract_managements` 테이블 생성 (`mgmt_no, contract_no, notice_memo, notice_date, renewal_premium, customer_response, status, created_at` 등).
2. `ExpiringContractManagementDAO` 클래스 생성 (`save()`, `findAll()`).
3. Runner에서 처리 단계마다 `ExpiringContractManagementDAO.save()` 호출.

---

## 보상/청구 도메인 (claim)

### [BUG-NEW-CLM-01] ClaimPaymentDAO — findAll()에서 recipient_name·account_no·paid_at·scheduled_at·payment_type 미복원

| 항목 | 내용 |
|------|------|
| 유스케이스 | 보험금을 지급한다 — 지급 이력 조회 |
| 관련 파일 | `dp/dao/ClaimPaymentDAO.java:28-47` |
| 심각도 | **중간** (지급 이력 핵심 필드 복원 누락) |

**문제**

`save()`는 BUG-CLM-09 수정 후 `paid_at, scheduled_at, payment_type, recipient_name, account_no, failure_reason`을 모두 저장한다. 그러나 `findAll()`의 복원 코드는 다음과 같이 4개 필드만 생성한다.

```java
// ClaimPaymentDAO.java:43-46
return new ClaimPayment(
    rs.getString("payment_no"), calcShell,
    rs.getLong("final_amount"), status);
```

SELECT 쿼리에서 읽어 온 `recipient_name, account_no, paid_at, scheduled_at, payment_type`이 `ClaimPayment` 객체에 설정되지 않는다. DB 로드 후 조회 화면에서 수령인·계좌·지급일시가 모두 null로 표시된다.

**수정 방향**

`findAll()` 복원 블록에서 생성자 호출 후 setter로 나머지 필드를 복원한다.

```java
ClaimPayment cp = new ClaimPayment(
        rs.getString("payment_no"), calcShell,
        rs.getLong("final_amount"), status);
cp.setRecipientName(rs.getString("recipient_name"));
cp.setAccountNo(rs.getString("account_no"));
java.sql.Timestamp pat = rs.getTimestamp("paid_at");
if (pat != null) cp.setPaidAt(pat.toLocalDateTime());
java.sql.Timestamp sat = rs.getTimestamp("scheduled_at");
if (sat != null) cp.setScheduledAt(sat.toLocalDateTime());
String pt = rs.getString("payment_type");
if (pt != null) { /* PaymentType.valueOf(pt) 설정 */ }
return cp;
```

---

## 재무/납입 도메인 (payment / finance)

### [BUG-NEW-FIN-01] PaymentDAO — findAll()에서 payment_no·payment_method·requestedAt·금액 미복원

| 항목 | 내용 |
|------|------|
| 유스케이스 | 보험료를 납입한다 — 납입 이력 조회 |
| 관련 파일 | `dp/dao/PaymentDAO.java:34-51` |
| 심각도 | **중간** (납입 이력 로드 시 핵심 필드 전부 null) |

**문제**

`save()`는 `payment_no, customer_id, customer_name, total_amount, payment_method, requested_at, status`를 모두 저장한다. 그러나 `findAll()` 복원 코드는 `new Payment(custShell)`로 생성한 뒤 `status`만 설정한다.

```java
// PaymentDAO.java:43-49
Payment pay = new Payment(custShell);
String st = rs.getString("status");
if (st != null) {
    try { pay.setStatus(PaymentStatus.valueOf(st)); }
    catch (IllegalArgumentException ignored) {}
}
return pay;
```

SELECT에서 읽어 온 `payment_no`, `payment_method`, `requested_at`, `total_amount`가 객체에 전혀 설정되지 않는다. DB 로드 후 납입번호·납입방법·신청일시·금액이 모두 null/0이 된다.

**수정 방향**

복원 블록에 setter 호출 추가:

```java
pay.setPaymentNo(rs.getString("payment_no"));
String method = rs.getString("payment_method");
if (method != null) { /* PaymentMethod.valueOf(method) 설정 */ }
java.sql.Timestamp rat = rs.getTimestamp("requested_at");
if (rat != null) pay.setRequestedAt(rat.toLocalDateTime());
pay.setDiscountedAmount(rs.getLong("total_amount"));
```

---

### [BUG-NEW-FIN-02] RefundCalculationRunner — selectOrCreateCancellation()에서 c.getCustomer() NPE 위험

| 항목 | 내용 |
|------|------|
| 유스케이스 | 해약 환급금을 산출한다 — 해지 건 선택 단계 |
| 관련 파일 | `dp/runner/usecase/RefundCalculationRunner.java:171` |
| 심각도 | **중간** (NullPointerException으로 Runner 강제 종료) |

**문제**

`selectOrCreateCancellation()` 내부의 `contracts` 목록 레이블 생성 코드:

```java
// RefundCalculationRunner.java:185-188
String[] options = contracts.stream()
        .map(c -> c.getContractNo() + " - " + c.getCustomer().getName()
                + " (월 " + c.getMonthlyPremium() + "원)")
        .toArray(String[]::new);
```

`ContractDAO.findAll()`에서 복원된 `Contract` 객체의 `customer` 필드가 null인 경우(예: `customer_id`가 DB에 NULL로 저장된 계약), `c.getCustomer().getName()` 호출 시 `NullPointerException`이 발생하여 목록 생성 자체가 실패한다.

**수정 방향**

null 안전 접근으로 변경:

```java
.map(c -> c.getContractNo() + " - "
        + (c.getCustomer() != null ? c.getCustomer().getName() : "미상")
        + " (월 " + c.getMonthlyPremium() + "원)")
```

---

## 문의 도메인 (inquiry)

### [BUG-NEW-INQ-01] InquiryDAO — ON DUPLICATE KEY UPDATE answer_content=VALUES(answer_content)로 기존 답변 삭제

| 항목 | 내용 |
|------|------|
| 유스케이스 | 문의를 처리한다 (관리자 답변 후 상태 재저장) |
| 관련 파일 | `dp/dao/InquiryDAO.java:14-28` |
| 심각도 | **높음** (관리자가 입력한 답변이 상태 업데이트 시 DB에서 삭제됨) |

**문제**

`save()` INSERT 쿼리에 `answer_content` 컬럼이 포함되지 않지만, `ON DUPLICATE KEY UPDATE` 절에는 `answer_content=VALUES(answer_content)`가 있다.

```java
// InquiryDAO.java:14-28
DBA.executeUpdate(
    "INSERT INTO inquiries (inquiry_no, customer_name, inquiry_type, title, content,"
    + " attachment_file_name, attachment_file_size, status, created_at)"
    + " VALUES (?,?,?,?,?,?,?,?,?)"
    + " ON DUPLICATE KEY UPDATE status=VALUES(status), answer_content=VALUES(answer_content)",
    ...);
```

`answer_content`가 INSERT 컬럼 목록에 없으므로 `VALUES(answer_content)`는 NULL을 반환한다. 따라서 관리자가 답변을 저장한 뒤 문의 상태만 변경하여 `save()`를 재호출하면 `ON DUPLICATE KEY UPDATE`가 실행되어 `answer_content`가 NULL로 덮어써진다. 관리자의 답변이 영구적으로 삭제된다.

**수정 방향**

`ON DUPLICATE KEY UPDATE` 절에서 `answer_content=VALUES(answer_content)` 제거, 답변 저장이 필요한 경우 별도 UPDATE 쿼리 사용:

```java
" ON DUPLICATE KEY UPDATE status=VALUES(status)"
// 답변 저장은 별도 메서드로 분리:
// UPDATE inquiries SET answer_content=?, answered_at=? WHERE inquiry_no=?
```

---