# FinalBugReport.md
> 전체 UC 시나리오 대비 코드 검토 결과 (2026-05-28)
> 에이전트 탐색 후 모든 항목을 직접 코드로 검증한 확정 버그만 포함

---

## BUG-EXTRA-STT-01 ✅ 수정 완료
**ContractStatisticsRunner — A1 분기 종료 후 Basic Path로 fall-through**

| 항목 | 내용 |
|---|---|
| 파일 | `dp/runner/usecase/ContractStatisticsRunner.java` |
| 심각도 | HIGH |
| 현상 | `run(Contract contract)` 오버로드에서 `topAction == 2`(전체 계약 통계 조회, A1)를 처리한 뒤 `return`이 없어 `if` 블록이 끝나면 곧바로 아래 Basic Path(특정 계약 납부 이력 조회)까지 실행된다. |
| 원인 | A1 블록 마지막 줄 `ConsoleHelper.printInfo("[A1] Basic Path 2번으로 돌아갑니다.")` 이후 `return;`이 누락됨. |
| 수정 방법 | A1 `if` 블록 끝에 `return;` 추가. |

```java
// ContractStatisticsRunner.java — run(Contract contract) 메서드 내부
if (topAction == 2) {
    // ... A1 전체 통계 처리 ...
    ConsoleHelper.printInfo("[A1] Basic Path 2번으로 돌아갑니다.");
    return;   // ← 이 줄 추가
}
```

---

## BUG-EXTRA-EXP-01 ✅ 수정 완료
**ExpiringContractManagementRunner — A2(해지 희망), A3(추후 결정) 분기에서 DAO.save() 누락**

| 항목 | 내용 |
|---|---|
| 파일 | `dp/runner/usecase/ExpiringContractManagementRunner.java` |
| 심각도 | HIGH |
| 현상 | 고객 응답이 해지(A2) 또는 추후 결정(A3)일 때 `mgmt`의 상태(customerResponse, historyTab)를 변경하지만 `ExpiringContractManagementDAO.save(mgmt)` 호출이 없어 DB에 반영되지 않는다. 갱신(A1) 분기에는 이미 수정됨. |
| 원인 | BUG-NEW-CTR-02 수정 시 A1(갱신) 경로에만 save()를 추가하고 A2/A3 경로를 누락. |
| 수정 방법 | A2 `mgmt.switchToTermination()` 직후와 A3 `mgmt.sendPendingAlert()` 직후에 각각 `ExpiringContractManagementDAO.save(mgmt)` 추가. |

```java
// A2 분기
mgmt.switchToTermination();
mgmt.updateHistoryTab();
ExpiringContractManagementDAO.save(mgmt);   // ← 추가
ConsoleHelper.printStage(...);

// A3 분기
mgmt.sendPendingAlert();
ExpiringContractManagementDAO.save(mgmt);   // ← 추가
ConsoleHelper.printStage(...);
```

---

## BUG-EXTRA-DMG-01 ✅ 수정 완료
**DamageInvestigationRunner — A1(보완 서류), A2(추가 조사) 처리 후 DAO.save() 누락**

| 항목 | 내용 |
|---|---|
| 파일 | `dp/runner/usecase/DamageInvestigationRunner.java` |
| 심각도 | MEDIUM |
| 현상 | 메인 루프에서 case 2(`requestSupplement`) 또는 case 3(`requestAdditional`)를 선택하면 도메인 객체 상태가 변경되지만 루프 반복 중에 `DamageInvestigationDAO.save(inv)` 호출이 없다. 정상 흐름(case 1 `mainFlow`)에서는 save()가 호출되지만, A1/A2 선택 후 `return` 없이 루프를 다시 돌면 변경 내용이 영속화되지 않은 채 다른 작업으로 넘어갈 수 있다. |
| 원인 | `requestSupplement()`, `requestAdditional()` 헬퍼 메서드가 DAO 저장 없이 도메인 로직만 수행. |
| 수정 방법 | `requestSupplement` / `requestAdditional` 호출 직후 `DamageInvestigationDAO.save(investigation)` 추가. |

```java
case 2:
    requestSupplement(investigation);
    DamageInvestigationDAO.save(investigation);   // ← 추가
    break;
case 3:
    requestAdditional(investigation);
    DamageInvestigationDAO.save(investigation);   // ← 추가
    break;
```

---

## BUG-EXTRA-RFC-01 ✅ 수정 완료 (메인 run()만, 오버로드는 BUG-R2-RFC-01 참조)
**RefundCalculationRunner — E1 검증 전에 RefundCalculationDAO.save() 호출**

| 항목 | 내용 |
|---|---|
| 파일 | `dp/runner/usecase/RefundCalculationRunner.java` |
| 심각도 | MEDIUM |
| 현상 | `RefundCalculation` 객체 생성 직후 상태 검증(`CALCULATION_PENDING` 여부) 전에 `RefundCalculationDAO.save(refund)`가 실행된다. E1 조건에 걸려 `return`하면 불완전한(산출 불가) 데이터가 DB에 남는다. |
| 원인 | save() 호출 순서가 잘못됨 — 검증보다 먼저 저장. |
| 수정 방법 | E1 검증 통과 후 save()를 이동하거나, 검증 실패 시 저장된 레코드를 삭제하는 처리 추가. 전자가 더 단순함. |

```java
// 수정 전
RefundCalculation refund = new RefundCalculation(cancellation);
RefundCalculationDAO.save(refund);               // 검증 전 저장 (버그)
if (refund.getStatus() == RefundStatus.CALCULATION_PENDING) {
    ...
    return;
}

// 수정 후
RefundCalculation refund = new RefundCalculation(cancellation);
if (refund.getStatus() == RefundStatus.CALCULATION_PENDING) {
    ...
    return;
}
RefundCalculationDAO.save(refund);               // 검증 통과 후 저장
```

---

## BUG-EXTRA-EVL-01 ✅ 수정 완료
**SalesOrgEvaluationDAO — findAll() evaluated_at 미복원, ON DUPLICATE KEY UPDATE score/evaluated_at 누락**

| 항목 | 내용 |
|---|---|
| 파일 | `dp/dao/SalesOrgEvaluationDAO.java` |
| 심각도 | MEDIUM |
| 현상 1 | `findAll()` SELECT 절에 `evaluated_at`이 포함되어 있으나 mapRow 람다에서 `e.setEvaluatedAt(...)` 호출이 없어 DB에서 읽어온 평가일시가 버려진다. `SalesOrgEvaluationRunner`에서 `evaluation.getEvaluatedAt()`를 출력할 때 항상 null이 반환된다. |
| 현상 2 | `ON DUPLICATE KEY UPDATE` 절에 `score=VALUES(score)`와 `evaluated_at=VALUES(evaluated_at)`가 없어 동일 PK로 update 시 점수와 평가일시가 갱신되지 않는다. |
| 원인 | mapRow 작성 시 evaluated_at 복원 라인 누락; ON DUPLICATE KEY UPDATE 절 작성 시 score·evaluated_at 누락. |
| 수정 방법 | mapRow에 `e.setEvaluatedAt(rs.getTimestamp("evaluated_at") != null ? rs.getTimestamp("evaluated_at").toLocalDateTime() : null)` 추가; ON DUPLICATE KEY UPDATE에 `score=VALUES(score), evaluated_at=VALUES(evaluated_at)` 추가. |

```java
// findAll() mapRow — evaluated_at 복원 추가
e.setEvaluatedAt(rs.getTimestamp("evaluated_at") != null
    ? rs.getTimestamp("evaluated_at").toLocalDateTime() : null);

// save() ON DUPLICATE KEY UPDATE — score/evaluated_at 추가
+ "  evaluation_comment=VALUES(evaluation_comment),"
+ "  score=VALUES(score), evaluated_at=VALUES(evaluated_at)"   // ← 추가
```

---

## BUG-EXTRA-EDU-04 ✅ 수정 완료
**EducationPreparationDAO + schema.sql — registered_at 컬럼 누락으로 항상 null**

| 항목 | 내용 |
|---|---|
| 파일 | `dp/dao/EducationPreparationDAO.java`, `src/main/resources/schema.sql` |
| 심각도 | MEDIUM |
| 현상 | `education_preparations` 테이블에 `registered_at` 컬럼이 없다. DAO INSERT에도 포함되지 않고, `findAll()`도 복원하지 않는다. `EducationPreparationRunner` 라인 122에서 `preparation.getRegisteredAt()`를 출력하면 항상 null이 된다. |
| 원인 | schema.sql 테이블 DDL 작성 시 `registered_at` 컬럼 누락; DAO도 동일하게 누락. |
| 수정 방법 | schema.sql `education_preparations` 테이블에 `registered_at TIMESTAMP NULL` 컬럼 추가 후 Docker 볼륨 재생성; DAO INSERT·ON DUPLICATE KEY UPDATE·findAll()에 `registered_at` 추가. |

```sql
-- schema.sql education_preparations 테이블
CREATE TABLE IF NOT EXISTS education_preparations (
    prep_no         VARCHAR(20)  PRIMARY KEY,
    ...
    status          VARCHAR(20),
    registered_at   TIMESTAMP    NULL          -- ← 추가
);
```

```java
// DAO INSERT 컬럼 목록에 registered_at 추가
"INSERT INTO education_preparations"
+ " (prep_no, plan_no, trainer_name, venue, material_ready,"
+ "  textbook_status, attendance_list, status, registered_at)"   // ← registered_at 추가
+ " VALUES (?,?,?,?,?,?,?,?,?)"
// 파라미터에 e.getRegisteredAt() 추가

// ON DUPLICATE KEY UPDATE에 registered_at 추가
+ "  status=VALUES(status), registered_at=VALUES(registered_at)"  // ← 추가

// findAll() mapRow에 복원 추가
e.setRegisteredAt(rs.getTimestamp("registered_at") != null
    ? rs.getTimestamp("registered_at").toLocalDateTime() : null);
```

---

---

## BUG-R2-DMG-01 ✅ 수정 완료
**DamageInvestigationDAO — findAll() opinion/result 미복원 + ON DUPLICATE KEY UPDATE 누락**

| 항목 | 내용 |
|---|---|
| 파일 | `dp/dao/DamageInvestigationDAO.java` |
| 심각도 | HIGH |
| 현상 1 | `findAll()` mapRow(lines 57-68): SELECT에 `opinion`, `result`가 포함되어 있지만 복원 코드 없음. DB에서 불러온 조사 건의 opinion/result가 null → `validateRequired()`가 항상 실패하고 `getResult() == APPROVED` 체크도 깨짐. |
| 현상 2 | ON DUPLICATE KEY UPDATE(lines 29-30): `opinion`, `reject_reason`, `our_fault_ratio`, `counter_ratio` 누락. 조사 진행 중 이 필드들을 변경해도 save() 시 DB에 반영 안 됨. |
| 수정 방법 1 | mapRow에 `String op = rs.getString("opinion"); if(op!=null) inv.enterOpinion(op);` 및 result 열거형 복원 추가. `DamageInvestigation`에 `setResult()` setter 필요. |
| 수정 방법 2 | ON DUPLICATE KEY UPDATE에 `opinion=VALUES(opinion), reject_reason=VALUES(reject_reason), our_fault_ratio=VALUES(our_fault_ratio), counter_ratio=VALUES(counter_ratio)` 추가. |

---

## BUG-R2-RFC-01 ✅ 수정 완료
**RefundCalculationRunner — 오버로드 run(Cancellation)에서 save() 위치 버그**

| 항목 | 내용 |
|---|---|
| 파일 | `dp/runner/usecase/RefundCalculationRunner.java` |
| 심각도 | MEDIUM |
| 현상 | BUG-EXTRA-RFC-01이 메인 `run()`만 수정하고 `run(Cancellation)` 오버로드(line 76-112)를 누락. line 82에서 `RefundCalculationDAO.save(refund)`가 line 84의 E1 검증보다 먼저 실행됨. |
| 수정 방법 | `run(Cancellation cancellation)` 메서드에서 `RefundCalculationDAO.save(refund)` 호출을 E1 검증 블록(lines 84-88) 이후로 이동. |

```java
// 수정 전 (line 81-88)
RefundCalculation refund = new RefundCalculation(cancellation);
RefundCalculationDAO.save(refund);               // E1 검증 전 저장 (버그)
if (refund.getStatus() == RefundStatus.CALCULATION_PENDING) { ... return; }

// 수정 후
RefundCalculation refund = new RefundCalculation(cancellation);
if (refund.getStatus() == RefundStatus.CALCULATION_PENDING) { ... return; }
RefundCalculationDAO.save(refund);               // 검증 통과 후 저장
```

---

## BUG-R2-DSP-01 ✅ 수정 완료
**DispatchRecordRunner — A3/A1/A4/배정 후 DispatchDAO.save() 누락**

| 항목 | 내용 |
|---|---|
| 파일 | `dp/runner/usecase/DispatchRecordRunner.java` |
| 심각도 | HIGH |
| 현상 | `DispatchDAO.save(dispatch)`는 정상 흐름 완료 시(writeRecord 내부 line 138)에만 호출됨. 다음 4가지 상태 변경 후 save 없음: ① 직원 배정(lines 42-47), ② A1 위치 갱신(line 66), ③ A3 취소(lines 78-81), ④ A4 위치 갱신(line 85). A3 취소 후 return하면 CANCELED 상태가 영속화되지 않아 재시작 시 취소된 출동이 다시 표시됨. |
| 수정 방법 | ① assignAgent 블록 끝(line 47)에 `DispatchDAO.save(dispatch)` 추가. ② A1 updateLocation(line 67) 직후 추가. ③ A3 cancel(line 80) waitEnter 전 추가. ④ A4 updateLocation(line 86) 직후 추가. |

---

## BUG-R2-EDU-01 ✅ 수정 완료
**EducationExecutionDAO — ON DUPLICATE KEY UPDATE에 executed_at 누락**

| 항목 | 내용 |
|---|---|
| 파일 | `dp/dao/EducationExecutionDAO.java` |
| 심각도 | LOW |
| 현상 | ON DUPLICATE KEY UPDATE(lines 18-19): `attendee_count`, `memo`, `status`만 포함. INSERT에는 `executed_at`이 있지만(line 16) UPDATE에서 누락. 같은 PK로 재저장 시 완료 일시가 갱신되지 않음. |
| 수정 방법 | ON DUPLICATE KEY UPDATE에 `executed_at=VALUES(executed_at)` 추가. |

---

## BUG-R2-EDU-02 ✅ 수정 완료
**EducationPlanDAO — ON DUPLICATE KEY UPDATE에 trainer_name, title 누락**

| 항목 | 내용 |
|---|---|
| 파일 | `dp/dao/EducationPlanDAO.java` |
| 심각도 | LOW |
| 현상 | ON DUPLICATE KEY UPDATE(lines 16-23): `trainer_name`과 `title`이 누락됨. 교육 계획 수정 시 강사명과 교육명이 DB에 반영되지 않음. |
| 수정 방법 | ON DUPLICATE KEY UPDATE에 `trainer_name=VALUES(trainer_name), title=VALUES(title)` 추가. |

---

## BUG-R2-SAL-01 ✅ 수정 완료
**ChannelScreeningDAO — findAll() mapRow에서 screening_no(approvalNo) 미복원**

| 항목 | 내용 |
|---|---|
| 파일 | `dp/dao/ChannelScreeningDAO.java` |
| 심각도 | MEDIUM |
| 현상 | `findAll()` mapRow: `screening_no`를 SELECT하지만 반환된 `ChannelScreening` 객체에 설정하지 않음. `ChannelScreening`에 `setApprovalNo()` setter 없음. 로드된 객체의 `approvalNo == null` → 해당 객체를 `save()`하면 PK가 새로 생성되어 기존 레코드를 UPDATE 대신 신규 INSERT함. |
| 수정 방법 | `ChannelScreening`에 `setApprovalNo(String)` 추가. mapRow에 `s.setApprovalNo(rs.getString("screening_no"))` 추가. |

---

## BUG-R2-PAY-01 ✅ 수정 완료
**PaymentRecordDAO — confirmed_at/rejected_at/rejectCategory/rejectReason 미저장 (schema 포함)**

| 항목 | 내용 |
|---|---|
| 파일 | `dp/dao/PaymentRecordDAO.java`, `schema.sql` |
| 심각도 | MEDIUM |
| 현상 | `PaymentRecord.confirm()`은 `confirmedAt`, `reject()`는 `rejectedAt`, `rejectCategory`, `rejectReason`을 설정하지만 `payment_records` 테이블에 이 컬럼들이 없음. DAO INSERT에도 포함 안 됨. `PaymentRecordRunner`에서 confirm/reject 후 save해도 해당 데이터는 소실됨. |
| 수정 방법 | `schema.sql` `payment_records` 테이블에 `confirmed_at TIMESTAMP NULL`, `rejected_at TIMESTAMP NULL`, `reject_category VARCHAR(50)`, `reject_reason VARCHAR(500)` 추가. DAO INSERT/UPDATE에 동일 반영. Docker 볼륨 재생성 필요. |

---

## BUG-R2-EXP-01 ✅ 수정 완료
**ExpiringContractManagementRunner — A2 해지 희망 시 InsuranceCancellationRunner.run() 미호출**

| 항목 | 내용 |
|---|---|
| 파일 | `dp/runner/usecase/ExpiringContractManagementRunner.java` |
| 심각도 | HIGH |
| 현상 | A2 분기(lines 251-255): `mgmt.switchToTermination()` + `DAO.save()` 후 "[A2] 해지 처리 유스케이스로 전환합니다." 메시지만 출력하고 종료. UC 시나리오 "해지 처리 유스케이스로 전환"의 실제 실행(`InsuranceCancellationRunner.run()`)이 누락됨. `InsuranceCancellationRunner`가 import조차 되어 있지 않음. |
| 수정 방법 | import 추가 후 line 255 직후 `InsuranceCancellationRunner.run(selectedContract);` 호출. |

---

## 검토 중 오탐으로 제외한 항목

| 항목 | 제외 이유 |
|---|---|
| ChannelScreeningRunner 필터 미설정 | `typeFilter`/`statusFilter`를 스트림 `.filter()`에 직접 사용 — 도메인 set 불필요 |
| ContractInfoRunner save() | `contract.setIsExpiringSoon()` 계산 결과를 의도적으로 영속화 |
| RefundListRunner NPE | CancellationDAO가 항상 셸 Customer/Contract를 생성함 |
| PaymentRunner selectPaymentMethod 미반영 | `Payment.selectPaymentMethod()`가 직접 `this.paymentMethod` 설정 |
| EducationPlanRunner E2 | UC 주석에 E2가 없음 (E1만 명시) |
| SalesActivityRunner 목록 미선택 | findAll()은 기존 데이터 참고용 표시이고 입력은 신규 데이터 — 의도적 |
| AccidentReportDAO.findAll() 필드 누락 | 어떤 Runner에서도 AccidentReportDAO.findAll() 호출 없음 — 런타임 영향 없음 |
| DispatchRecordDAO.findAll() 필드 누락 | 어떤 Runner에서도 DispatchRecordDAO.findAll() 호출 없음 — 런타임 영향 없음 |
| PaymentDAO payment_items ON DUPLICATE 누락 | payment당 save() 1회만 호출 — 중복 INSERT 발생하지 않음 |
| CustomerRegistrationDAO ON DUPLICATE 일부 누락 | customer_id가 타임스탬프 기반 생성 — 실제 PK 충돌 없음 |
| RefundCalculationDAO confirmed_at 미저장 | schema에 confirmed_at 컬럼 없음 + Runner 표시에 불필요 |
| UnderwritingRunner — startReview() 미호출 오탐 | InsuranceReviewer.startUnderwriting() 내부에서 startReview() 호출됨 |
| InsuranceApplicationDAO — applied_at/status SELECT 누락 오탐 | WHERE status='신청' 필터로 pending만 조회, 저장 시 status 명시적으로 설정함 |
| PolicyApplicationDAO — submitted_at/status SELECT 누락 오탐 | 동일 이유 |
| RefundCalculationDAO ON DUPLICATE 일부 누락 오탐 | existsByCancellationNo 선행 체크로 중복 save 발생하지 않음 |
| ProposalDAO.findAll() product 미복원 오탐 | 어떤 Runner도 ProposalDAO.findAll() 호출 없음 |
| BonusRequestDAO ON DUPLICATE reason/status 누락 오탐 | status가 SUBMITTED 하드코딩, 재저장 흐름 없음 |
| InterviewScheduleRunner A4 장소 항상 필수 오탐 | 수정 시 장소 변경 필요성 있음 — 의도적 |

---

## BUG-R3-UDW-01 ✅ 수정 완료
**UnderwritingDAO — ON DUPLICATE KEY UPDATE에 reviewed_at 누락**

| 항목 | 내용 |
|---|---|
| 파일 | `dp/dao/UnderwritingDAO.java` |
| 심각도 | LOW |
| 현상 | `save()` ON DUPLICATE KEY UPDATE에 `result`, `risk_grade`, `review_opinion`만 포함. `reviewed_at`이 누락되어 동일 PK로 재저장 시 심사 일시가 갱신되지 않음. INSERT에는 포함됨. |
| 수정 방법 | ON DUPLICATE KEY UPDATE에 `reviewed_at=VALUES(reviewed_at)` 추가. |

---

## BUG-R3-RPY-01 ✅ 수정 완료
**RefundPaymentDAO — findAll() SELECT에 transferred_at/notice_sent/otp_fail_count 누락**

| 항목 | 내용 |
|---|---|
| 파일 | `dp/dao/RefundPaymentDAO.java` |
| 심각도 | MEDIUM |
| 현상 | `findAll()` SELECT 절에 `transferred_at`, `notice_sent`, `otp_fail_count`가 빠져 있어 DB에서 불러온 `RefundPayment` 객체에서 이체일시·통보여부·OTP실패횟수가 항상 기본값(null/false/0)으로 표시됨. |
| 수정 방법 | SELECT에 세 컬럼 추가 후 mapRow에서 복원 코드 추가. |

---

## BUG-R3-CAN-01 ✅ 수정 완료
**CancellationDAO — ON DUPLICATE KEY UPDATE에 reason/detail_reason/expected_refund 누락**

| 항목 | 내용 |
|---|---|
| 파일 | `dp/dao/CancellationDAO.java` |
| 심각도 | LOW |
| 현상 | ON DUPLICATE KEY UPDATE에 `status`와 `cancelled_at`만 포함. `reason`, `detail_reason`, `expected_refund`가 누락되어 동일 PK로 재저장 시(JVM 재시작 후 SequenceSync 충돌 시) 해지 사유·예상환급금이 유지되지 않음. |
| 수정 방법 | ON DUPLICATE KEY UPDATE에 `reason=VALUES(reason), detail_reason=VALUES(detail_reason), expected_refund=VALUES(expected_refund)` 추가. |

---

## BUG-R3-SAL-01 ✅ 수정 완료
**SalesActivityRunner — showSaveSuccess() 호출이 DAO.save() 보다 앞에 위치**

| 항목 | 내용 |
|---|---|
| 파일 | `dp/runner/usecase/SalesActivityRunner.java` |
| 심각도 | MEDIUM |
| 현상 | line 207에서 `activity.showSaveSuccess()`(저장 완료 팝업)를 출력한 뒤 line 208에서 `SalesActivityManagementDAO.save(activity)`를 호출. DB 저장 실패 시에도 이미 성공 메시지가 출력된 상태. |
| 수정 방법 | 두 줄 순서를 바꿔 `DAO.save(activity)` 이후 `showSaveSuccess()` 호출. |

---

## BUG-R3-SCR-01 ✅ 수정 완료
**ChannelScreeningDAO — findAll() mapRow에 reviewed_at 미복원**

| 항목 | 내용 |
|---|---|
| 파일 | `dp/dao/ChannelScreeningDAO.java` |
| 심각도 | LOW |
| 현상 | INSERT 절에 `reviewed_at`이 포함되지만 `findAll()` SELECT·mapRow에는 없음. DB에서 불러온 `ChannelScreening` 객체의 `approvedAt`(심사일시)이 항상 null. |
| 수정 방법 | `findAll()` SELECT에 `reviewed_at` 추가 후 mapRow에서 `s.setApprovedAt(...)` 호출. |

---

## BUG-R3-REV-01 ✅ 수정 완료
**RevivalDAO — contract_no를 null로 하드코딩**

| 항목 | 내용 |
|---|---|
| 파일 | `dp/dao/RevivalDAO.java`, `dp/consultation/Revival.java`, `dp/runner/usecase/RevivalRunner.java` |
| 심각도 | MEDIUM |
| 현상 | `RevivalDAO.save()` line 18: `contract_no` 파라미터가 리터럴 `null`. `revivals` 테이블에 `contract_no` 컬럼이 있으나 항상 null로 저장됨. Revival 도메인 클래스에 `contractNo` 필드 없음. |
| 수정 방법 | `Revival`에 `contractNo` 필드(getter/setter) 추가 → `RevivalRunner`에서 ContractDAO로 계약 목록 조회 후 선택 → `revival.setContractNo(...)` → DAO에서 `null` 대신 `r.getContractNo()` 사용. |

---

## BUG-R4-EDU-01 ✅ 수정 완료
**EducationPlanDAO — ON DUPLICATE KEY UPDATE에 target_audience/scheduled_date 누락**

| 항목 | 내용 |
|---|---|
| 파일 | `dp/dao/EducationPlanDAO.java` |
| 심각도 | MEDIUM |
| 현상 | `save()` INSERT에 `target_audience`, `scheduled_date`가 포함되지만 ON DUPLICATE KEY UPDATE(lines 16-24)에서 누락. 동일 PK로 재저장 시 대상 채널과 예정 날짜가 갱신되지 않음. |
| 수정 방법 | ON DUPLICATE KEY UPDATE에 `target_audience=VALUES(target_audience), scheduled_date=VALUES(scheduled_date)` 추가. |

```java
// ON DUPLICATE KEY UPDATE에 추가
+ "  target_audience=VALUES(target_audience), scheduled_date=VALUES(scheduled_date),"
```

---

## BUG-R4-EDU-02 ✅ 수정 완료
**EducationPreparationDAO — ON DUPLICATE KEY UPDATE에 plan_no/trainer_name 누락**

| 항목 | 내용 |
|---|---|
| 파일 | `dp/dao/EducationPreparationDAO.java` |
| 심각도 | LOW |
| 현상 | `save()` INSERT에 `plan_no`, `trainer_name`이 포함되지만 ON DUPLICATE KEY UPDATE(lines 19-23)에서 누락. 동일 PK로 재저장 시 계획 번호와 강사명이 갱신되지 않음. |
| 수정 방법 | ON DUPLICATE KEY UPDATE에 `plan_no=VALUES(plan_no), trainer_name=VALUES(trainer_name)` 추가. |

```java
// ON DUPLICATE KEY UPDATE에 추가
+ "  plan_no=VALUES(plan_no), trainer_name=VALUES(trainer_name),"
```

---

## BUG-R4-EDU-03 ✅ 수정 완료
**EducationExecutionDAO — ON DUPLICATE KEY UPDATE에 trainer_name 누락**

| 항목 | 내용 |
|---|---|
| 파일 | `dp/dao/EducationExecutionDAO.java` |
| 심각도 | LOW |
| 현상 | `save()` INSERT에 `trainer_name`이 포함되지만 ON DUPLICATE KEY UPDATE(lines 18-19)에서 누락. 동일 PK로 재저장 시 강사명이 갱신되지 않음. |
| 수정 방법 | ON DUPLICATE KEY UPDATE에 `trainer_name=VALUES(trainer_name)` 추가. |

```java
// ON DUPLICATE KEY UPDATE에 추가
" ON DUPLICATE KEY UPDATE trainer_name=VALUES(trainer_name),"
+ " attendee_count=VALUES(attendee_count),"
+ " memo=VALUES(memo), status=VALUES(status), executed_at=VALUES(executed_at)"
```

---

## BUG-R4-SAL-01 ✅ 수정 완료
**ChannelRecruitmentRunner — showSaveSuccess() 호출이 DAO.save() 보다 앞에 위치**

| 항목 | 내용 |
|---|---|
| 파일 | `dp/runner/usecase/ChannelRecruitmentRunner.java` |
| 심각도 | MEDIUM |
| 현상 | line 171에서 `recruitment.showSaveSuccess()`("모집 공고가 등록되었습니다." 팝업)를 출력한 뒤 line 179에서 `ChannelRecruitmentDAO.save(recruitment)` 호출. DB 저장 실패 시에도 성공 메시지가 먼저 출력된 상태. BUG-R3-SAL-01과 동일 패턴. |
| 수정 방법 | `ChannelRecruitmentDAO.save(recruitment)` 호출을 `showSaveSuccess()` 이전으로 이동. |

```java
// 수정 후 순서
ChannelRecruitmentDAO.save(recruitment);   // 먼저 저장
recruitment.showSaveSuccess();             // 그 다음 성공 팝업
```

---

## BUG-R4-SAL-02 ✅ 수정 완료
**ActivityPlanDAO — ON DUPLICATE KEY UPDATE에 target_new_customer/proposal_reason/memo 누락**

| 항목 | 내용 |
|---|---|
| 파일 | `dp/dao/ActivityPlanDAO.java` |
| 심각도 | MEDIUM |
| 현상 | INSERT에 `target_new_customer`, `proposal_reason`, `memo`가 포함되지만 ON DUPLICATE KEY UPDATE(lines 21-25)에서 누락. 동일 PK로 재저장 시 세 필드가 갱신되지 않음. |
| 수정 방법 | ON DUPLICATE KEY UPDATE에 세 필드 추가. |

```java
// ON DUPLICATE KEY UPDATE에 추가
+ " target_new_customer=VALUES(target_new_customer),"
+ " proposal_reason=VALUES(proposal_reason), memo=VALUES(memo)"
```

---

## BUG-R4-SAL-03 ✅ 수정 완료
**SalesActivityManagementDAO — ON DUPLICATE KEY UPDATE에 activity_type 누락**

| 항목 | 내용 |
|---|---|
| 파일 | `dp/dao/SalesActivityManagementDAO.java` |
| 심각도 | LOW |
| 현상 | INSERT에 `activity_type`이 포함되지만 ON DUPLICATE KEY UPDATE(lines 18-25)에서 누락. 동일 PK로 재저장 시 활동 유형이 갱신되지 않음. |
| 수정 방법 | ON DUPLICATE KEY UPDATE에 `activity_type=VALUES(activity_type)` 추가. |

---

## BUG-R4-SAL-04 ✅ 수정 완료
**SalesOrgEvaluationDAO — ON DUPLICATE KEY UPDATE에 org_name 누락**

| 항목 | 내용 |
|---|---|
| 파일 | `dp/dao/SalesOrgEvaluationDAO.java` |
| 심각도 | LOW |
| 현상 | INSERT에 `org_name`이 포함되지만 ON DUPLICATE KEY UPDATE(lines 18-23)에서 누락. 동일 PK로 재저장 시 채널(조직)명이 갱신되지 않음. |
| 수정 방법 | ON DUPLICATE KEY UPDATE에 `org_name=VALUES(org_name)` 추가. |

---

## BUG-R4-CTR-01 ✅ 수정 완료
**CancellationDAO — findAll() SELECT에 detail_reason 누락**

| 항목 | 내용 |
|---|---|
| 파일 | `dp/dao/CancellationDAO.java` |
| 심각도 | MEDIUM |
| 현상 | INSERT에 `detail_reason`이 포함되지만 `findAll()` SELECT(lines 28-29)에 없음. DB에서 불러온 `Cancellation` 객체의 `detailReason`이 항상 null. |
| 수정 방법 | SELECT 절에 `detail_reason` 추가 후 mapRow에서 복원 코드 추가. |

```java
// findAll() SELECT에 추가
"SELECT cancellation_no, contract_no, customer_name, monthly_premium,"
+ " reason, detail_reason, expected_refund, status, cancelled_at FROM cancellations"

// mapRow에 복원 추가
c.setDetailReason(rs.getString("detail_reason"));
```

---

## BUG-R4-CON-01 ✅ 수정 완료
**ProposalDAO — ON DUPLICATE KEY UPDATE에 monthly_premium 누락**

| 항목 | 내용 |
|---|---|
| 파일 | `dp/dao/ProposalDAO.java` |
| 심각도 | LOW |
| 현상 | INSERT에 `monthly_premium`이 포함되지만 ON DUPLICATE KEY UPDATE(line 17)에 `product_name`만 있고 `monthly_premium`이 누락. 동일 PK로 재저장 시 월 보험료가 갱신되지 않음. |
| 수정 방법 | ON DUPLICATE KEY UPDATE에 `monthly_premium=VALUES(monthly_premium)` 추가. |

```java
+ " ON DUPLICATE KEY UPDATE product_name=VALUES(product_name),"
+ " monthly_premium=VALUES(monthly_premium)"
```

---

## BUG-R4-CLM-01 ✅ 수정 완료
**ClaimPaymentDAO — findAll() SELECT에 failure_reason 누락**

| 항목 | 내용 |
|---|---|
| 파일 | `dp/dao/ClaimPaymentDAO.java` |
| 심각도 | LOW |
| 현상 | INSERT에 `failure_reason`이 포함되지만 `findAll()` SELECT(lines 31-32)에 없음. DB에서 불러온 `ClaimPayment` 객체의 실패 사유가 항상 null. |
| 수정 방법 | SELECT 절에 `failure_reason` 추가 후 mapRow에서 복원 코드 추가. |

---

## BUG-R4-CLM-02 ✅ 수정 완료
**ClaimPaymentDAO — ON DUPLICATE KEY UPDATE에 recipient_name/account_no 누락**

| 항목 | 내용 |
|---|---|
| 파일 | `dp/dao/ClaimPaymentDAO.java` |
| 심각도 | MEDIUM |
| 현상 | INSERT에 `recipient_name`, `account_no`가 포함되지만 ON DUPLICATE KEY UPDATE(lines 21-23)에서 누락. 동일 PK로 재저장 시 수령인명과 계좌번호가 갱신되지 않음. |
| 수정 방법 | ON DUPLICATE KEY UPDATE에 `recipient_name=VALUES(recipient_name), account_no=VALUES(account_no)` 추가. |

---

## BUG-R5-ACT-01 ✅ 수정 완료
**DesignerDAO — ON DUPLICATE KEY UPDATE에 location 누락**

| 항목 | 내용 |
|---|---|
| 파일 | `dp/dao/DesignerDAO.java` |
| 심각도 | LOW |
| 현상 | INSERT에 `location`이 포함되지만 ON DUPLICATE KEY UPDATE(line 13)에 `name`만 있음. 동일 PK로 재저장 시 location이 갱신되지 않음. |
| 수정 방법 | ON DUPLICATE KEY UPDATE에 `location=VALUES(location)` 추가. |

---

## BUG-R5-ACT-02 ✅ 수정 완료
**AgencyDAO — ON DUPLICATE KEY UPDATE에 location 누락**

| 항목 | 내용 |
|---|---|
| 파일 | `dp/dao/AgencyDAO.java` |
| 심각도 | LOW |
| 현상 | INSERT에 `location`이 포함되지만 ON DUPLICATE KEY UPDATE(line 13)에 `name`만 있음. 동일 PK로 재저장 시 location이 갱신되지 않음. |
| 수정 방법 | ON DUPLICATE KEY UPDATE에 `location=VALUES(location)` 추가. |

---

## BUG-R5-ACT-04 ✅ 수정 완료
**DispatchAgentDAO — ON DUPLICATE KEY UPDATE에 department/position/vehicle_no 누락**

| 항목 | 내용 |
|---|---|
| 파일 | `dp/dao/DispatchAgentDAO.java` |
| 심각도 | MEDIUM |
| 현상 | INSERT에 `department`, `position`, `vehicle_no`가 포함되지만 ON DUPLICATE KEY UPDATE(line 13)에 `name`, `region`만 있음. 동일 PK로 재저장 시 부서·직급·차량번호가 갱신되지 않음. |
| 수정 방법 | ON DUPLICATE KEY UPDATE에 `department=VALUES(department), position=VALUES(position), vehicle_no=VALUES(vehicle_no)` 추가. |

---

## BUG-R5-CLM-01 ✅ 수정 완료
**ClaimRequestDAO — findAll() SELECT에 requested_at 누락**

| 항목 | 내용 |
|---|---|
| 파일 | `dp/dao/ClaimRequestDAO.java`, `dp/claim/ClaimRequest.java` |
| 심각도 | MEDIUM |
| 현상 | INSERT에 `requested_at`이 포함되고 ON DUPLICATE KEY UPDATE에도 있지만 `findAll()` SELECT(lines 38-39)에 누락. DB에서 불러온 `ClaimRequest` 객체의 `requestedAt`이 항상 null. `ClaimRequest`에 `setRequestedAt()` setter 없음. |
| 수정 방법 | `ClaimRequest`에 `setRequestedAt(LocalDateTime)` 추가 → `findAll()` SELECT에 `requested_at` 추가 → mapRow에서 복원. |

---

## BUG-R5-PAY-01 ✅ 수정 완료
**PaymentRecordDAO — findAll() SELECT에 confirmed_at/rejected_at/reject_category/reject_reason 누락**

| 항목 | 내용 |
|---|---|
| 파일 | `dp/dao/PaymentRecordDAO.java`, `dp/payment/PaymentRecord.java` |
| 심각도 | MEDIUM |
| 현상 | BUG-R2-PAY-01에서 INSERT/UPDATE에는 4개 컬럼이 추가됐지만 `findAll()` SELECT(line 32)에는 반영되지 않음. DB에서 불러온 `PaymentRecord`의 확정일시·거절일시·거절사유가 항상 null. `PaymentRecord`에 setter도 없음. |
| 수정 방법 | `PaymentRecord`에 setter 4개 추가 → `findAll()` SELECT에 4개 컬럼 추가 → mapRow에서 복원. |

---

## BUG-R5-PAY-02 ✅ 수정 완료
**PaymentRecordDAO — findByContractNo() SELECT에 동일 4개 필드 누락**

| 항목 | 내용 |
|---|---|
| 파일 | `dp/dao/PaymentRecordDAO.java` |
| 심각도 | MEDIUM |
| 현상 | BUG-R5-PAY-01과 동일. `findByContractNo()` SELECT(line 54)에도 `confirmed_at`, `rejected_at`, `reject_category`, `reject_reason` 누락. |
| 수정 방법 | `findByContractNo()` SELECT에 4개 컬럼 추가 → mapRow에서 복원. |

---

## BUG-R5-CLC-01 ✅ 수정 완료
**ClaimCalculationDAO — ON DUPLICATE KEY UPDATE에 recognized_damage/fault_ratio/exceeded_deductible/adjusted 누락**

| 항목 | 내용 |
|---|---|
| 파일 | `dp/dao/ClaimCalculationDAO.java` |
| 심각도 | MEDIUM |
| 현상 | ON DUPLICATE KEY UPDATE(line 18)에 `status`, `final_amount`만 포함. INSERT에 있는 `recognized_damage`, `fault_ratio`, `exceeded_deductible`, `adjusted`가 누락. 동일 PK로 재저장 시 손해인정액·과실비율·공제금 초과여부·조정여부가 갱신되지 않음. |
| 수정 방법 | ON DUPLICATE KEY UPDATE에 4개 필드 추가. |

---

## BUG-R6-EMP-01 ✅ 수정 완료
**EducationTrainerDAO — ON DUPLICATE KEY UPDATE에 department/position 누락**

| 항목 | 내용 |
|---|---|
| 파일 | `dp/dao/EducationTrainerDAO.java` |
| 심각도 | LOW |
| 현상 | INSERT에 `department`, `position`이 포함되지만 ON DUPLICATE KEY UPDATE(line 13)에 `name`만 있음. 동일 PK 재저장 시 부서·직급이 갱신되지 않음. |
| 수정 방법 | `department=VALUES(department), position=VALUES(position)` 추가. |

---

## BUG-R6-EMP-02 ✅ 수정 완료
**InsuranceReviewerDAO — ON DUPLICATE KEY UPDATE에 department/position 누락**

| 항목 | 내용 |
|---|---|
| 파일 | `dp/dao/InsuranceReviewerDAO.java` |
| 심각도 | LOW |
| 현상 | INSERT에 `department`, `position`이 포함되지만 ON DUPLICATE KEY UPDATE에 `name`만 있음. |
| 수정 방법 | `department=VALUES(department), position=VALUES(position)` 추가. |

---

## BUG-R6-EMP-03 ✅ 수정 완료
**FinanceManagerDAO — ON DUPLICATE KEY UPDATE에 department/position 누락**

| 항목 | 내용 |
|---|---|
| 파일 | `dp/dao/FinanceManagerDAO.java` |
| 심각도 | LOW |
| 현상 | INSERT에 `department`, `position`이 포함되지만 ON DUPLICATE KEY UPDATE에 `name`만 있음. |
| 수정 방법 | `department=VALUES(department), position=VALUES(position)` 추가. |

---

## BUG-R6-EMP-04 ✅ 수정 완료
**SalesManagerDAO — ON DUPLICATE KEY UPDATE에 department 누락**

| 항목 | 내용 |
|---|---|
| 파일 | `dp/dao/SalesManagerDAO.java` |
| 심각도 | LOW |
| 현상 | INSERT에 `department`가 포함되지만 ON DUPLICATE KEY UPDATE에 `name`만 있음. |
| 수정 방법 | `department=VALUES(department)` 추가. |

---

## BUG-R6-EMP-05 ✅ 수정 완료
**ClaimsHandlerDAO — ON DUPLICATE KEY UPDATE에 department/position 누락**

| 항목 | 내용 |
|---|---|
| 파일 | `dp/dao/ClaimsHandlerDAO.java` |
| 심각도 | LOW |
| 현상 | INSERT에 `department`, `position`이 포함되지만 ON DUPLICATE KEY UPDATE에 `name`, `transfer_limit`만 있음. |
| 수정 방법 | `department=VALUES(department), position=VALUES(position)` 추가. |

---

## BUG-R6-CHN-01 ✅ 수정 완료
**ChannelRecruitmentDAO — ON DUPLICATE KEY UPDATE에 manager_name 누락**

| 항목 | 내용 |
|---|---|
| 파일 | `dp/dao/ChannelRecruitmentDAO.java` |
| 심각도 | MEDIUM |
| 현상 | INSERT에 `manager_name`이 포함되지만 ON DUPLICATE KEY UPDATE(lines 16-18)에서 누락. 동일 PK 재저장 시 담당 관리자명이 갱신되지 않음. |
| 수정 방법 | ON DUPLICATE KEY UPDATE에 `manager_name=VALUES(manager_name)` 추가. |

---

## BUG-R6-DAO-01 ✅ 수정 완료
**InterviewRecordDAO — ON DUPLICATE KEY UPDATE에 customer_name 누락**

| 항목 | 내용 |
|---|---|
| 파일 | `dp/dao/InterviewRecordDAO.java` |
| 심각도 | LOW |
| 현상 | INSERT에 `customer_name`이 포함되지만 ON DUPLICATE KEY UPDATE(lines 14-18)에서 누락. 동일 PK 재저장 시 고객명이 갱신되지 않음. |
| 수정 방법 | ON DUPLICATE KEY UPDATE에 `customer_name=VALUES(customer_name)` 추가. |

---

## BUG-R7-DIN-01 ✅ 수정 완료
**DamageInvestigationDAO — findAll() SELECT에 handler_emp_id 누락**

| 항목 | 내용 |
|---|---|
| 파일 | `dp/dao/DamageInvestigationDAO.java` |
| 심각도 | LOW |
| 현상 | INSERT에 `handler_emp_id`(line 26)가 포함되지만 `findAll()` SELECT(line 42)에 없음. `handler_name`만 복원되고 담당자 ID는 항상 null. |
| 수정 방법 | SELECT에 `handler_emp_id` 추가 후 mapRow에서 shell ClaimsHandler 생성 시 ID 설정. |

---

## BUG-R7-DOM-01 ✅ 수정 완료
**RefundCalculation — confirm() 호출 시 상태를 PAID로 즉시 설정**

| 항목 | 내용 |
|---|---|
| 파일 | `dp/payment/RefundCalculation.java` |
| 심각도 | MEDIUM |
| 현상 | `confirm()` 메서드(line 159)에서 `this.status = RefundStatus.PAID`로 설정. 이 시점은 RefundPayment 객체를 생성하는 단계로, 실제 이체는 아직 발생하지 않았음. RefundListRunner 등에서 `CALCULATED` 상태의 산출 건을 목록에 표시하는 경우, confirm() 직후 PAID로 바뀌어 실제 지급 전에 목록에서 사라질 수 있음. |
| 수정 방법 | `confirm()` 내 상태를 `RefundStatus.CALCULATED`로 유지하거나, 실제 이체 완료 후 `RefundStatus.PAID`로 전이하도록 `RefundPayment.execute()` 완료 시점으로 이동. |

```java
// 수정 전
this.status = RefundStatus.PAID;   // confirm 시점에 PAID 설정 (버그)

// 수정 후
// status는 CALCULATED 유지 — 실제 이체 완료(RefundPayment.execute()) 후 PAID로 전이
```

---

## BUG-R7-RUN-01 ✅ 수정 완료
**ChannelScreeningRunner — showApprovalResult() 호출이 DAO.save() 보다 앞에 위치**

| 항목 | 내용 |
|---|---|
| 파일 | `dp/runner/usecase/ChannelScreeningRunner.java` |
| 심각도 | MEDIUM |
| 현상 | line 214에서 `screening.showApprovalResult()`(승인 결과 팝업)를 출력한 뒤 line 215에서 `ChannelScreeningDAO.save(screening)` 호출. DB 저장 실패 시에도 이미 성공 결과가 출력된 상태. BUG-R3-SAL-01, BUG-R4-SAL-01과 동일 패턴. |
| 수정 방법 | `ChannelScreeningDAO.save(screening)`을 `showApprovalResult()` 이전으로 이동. |

```java
// 수정 후
ChannelScreeningDAO.save(screening);   // 먼저 저장
screening.showApprovalResult();        // 그 다음 결과 출력
```
---

## BUG-R8-DAO-01 ✅ 수정 완료
**CustomerDAO — findAll()/findById() SELECT에 registered_at 누락 + Customer 생성 시 현재시각으로 덮어씀**

| 항목 | 내용 |
|---|---|
| 파일 | `dp/dao/CustomerDAO.java`, `dp/actor/Customer.java` |
| 심각도 | LOW |
| 현상 | `Customer` 생성자에서 `this.registeredAt = LocalDateTime.now()`를 설정하지만, `findAll()`·`findById()` SELECT에 `registered_at`이 없어 DB 실제 가입일시를 복원하지 못함. 로드된 Customer 객체의 `registeredAt`은 항상 로딩 시각이 됨. |
| 수정 방법 | `Customer`에 `setRegisteredAt()` 추가 → SELECT에 `registered_at` 추가 → mapRow에서 복원. |

---

## BUG-R8-PA-01 ✅ 수정 완료
**PolicyApplicationDAO — uploaded_at 저장 누락 (schema 포함)**

| 항목 | 내용 |
|---|---|
| 파일 | `dp/dao/PolicyApplicationDAO.java`, `dp/consultation/PolicyApplication.java`, `schema.sql` |
| 심각도 | LOW |
| 현상 | `PolicyApplication.attachSignature()` 호출 시 `uploadedAt = LocalDateTime.now()`가 설정되지만 `policy_applications` 테이블에 `uploaded_at` 컬럼이 없고 DAO INSERT에도 없음. JVM 재시작 후 서명 업로드 시각이 유실됨. |
| 수정 방법 | `schema.sql`에 `uploaded_at TIMESTAMP NULL` 추가 → `PolicyApplication`에 `setUploadedAt()` 추가 → DAO INSERT·UPDATE·findAll() 반영. Docker 볼륨 재생성 필요. |

---

## BUG-R8-ONS-01 ✅ 수정 완료
**OverdueNoticeSettingDAO — saved_at 저장 누락 (schema 포함)**

| 항목 | 내용 |
|---|---|
| 파일 | `dp/dao/OverdueNoticeSettingDAO.java`, `dp/payment/OverdueNoticeSetting.java`, `schema.sql` |
| 심각도 | LOW |
| 현상 | `OverdueNoticeSetting.save()` 호출 시 `savedAt = LocalDateTime.now()`가 설정되지만 `overdue_notice_settings` 테이블에 `saved_at` 컬럼이 없고 DAO INSERT에도 없음. 설정 저장 일시를 추적할 수 없음. |
| 수정 방법 | `schema.sql`에 `saved_at TIMESTAMP NULL` 추가 → `OverdueNoticeSetting`에 `setSavedAt()` 추가 → DAO INSERT·UPDATE·find() 반영. Docker 볼륨 재생성 필요. |
