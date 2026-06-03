# 상태 전이 검증 리팩터링 진행 현황

## 목표

서비스 계층에 흩어진 상태 검사(`if (entity.getStatus() != XYZ)`)를 엔터티 메서드 안으로 이동한다.
엔터티가 스스로 전이 가능 여부를 검증하고 불가능하면 `ApiException.badRequest()`를 직접 던진다.
서비스는 중복 검사를 제거하고 메서드를 호출만 한다.

### 이 방식을 선택한 이유
- `ApiException`은 Spring `HttpStatus`를 사용하는 `RuntimeException`이고, 이 프로젝트 엔터티는 JPA `@Entity`가 아니라 순수 POJO이므로 도메인 레이어에서 직접 사용해도 무방하다고 합의됨.
- `System.out.println`은 제거 대상이며, 엔터티 메서드 수정 시 함께 제거한다.
- 서비스에서 중복 상태 검사를 제거해도 동일한 `ApiException`이 전파되므로 클라이언트 응답은 동일하다.

---

## 완료된 작업

### 엔터티

| 파일 | 변경 내용 |
|---|---|
| `claim/entity/ClaimCalculation.java` | `approve()` — `CALCULATED` 아니면 `ApiException`. `requireApproved()` 신규 추가. `submitForApproval()`, `closeAsExceeded()` println 제거. |
| `claim/entity/ClaimPayment.java` | `execute()` — `COMPLETED`면 `ApiException`. `schedule()`, `handleTransferFailure()`, `sendCompletionNotice()`, `close()` println 제거. |
| `claim/entity/AccidentReport.java` | `receive()` — 검증 실패 시 `ApiException`. println 제거. |
| `claim/entity/ClaimRequest.java` | `submit()` — 검증 실패 시 `ApiException`. println 제거. |
| `claim/entity/DispatchRecord.java` | `transmit()` — 검증 실패 시 `ApiException`. `uploadPhoto()` println 제거. |
| `payment/entity/PaymentRecord.java` | `confirm()`, `reject()` — `WAITING` 아니면 `ApiException`. println 제거. |
| `payment/entity/RefundPayment.java` | `execute()` — `COMPLETED`/`LOCKED`면 `ApiException`. println 제거. |
| `payment/entity/RefundCalculation.java` | 생성자 — 데이터 누락 시 `ApiException`. `confirm()` — `CALCULATED` 아니면 `ApiException`. println 제거. |
| `sales/entity/ChannelScreening.java` | `approve()`, `reject()` — `PENDING` 아니면 `ApiException`. |
| `inquiry/entity/Inquiry.java` | `answer(String content)` 신규 추가 — `PENDING` 아니면 `ApiException`. |
| `consultation/entity/ConsultationRequest.java` | `accept()` — `"접수"` 아니면 `ApiException`. |
| `consultation/entity/InterviewSchedule.java` | `modify()`, `cancel()` — `"취소"` 상태 검사 추가. |
| `education/entity/EducationPlan.java` | `approve()`, `reject()` — `"승인요청"` 아니면 `ApiException`. |

### 서비스

| 파일 | 제거한 중복 검사 |
|---|---|
| `claim/service/AccidentReportService.java` | `report.getStatus() != RECEIVED` 사후 검사 제거. `AccidentReportStatus` import 제거. 미사용 `canAccessCustomer()` 삭제. |
| `claim/service/ClaimRequestService.java` | `claim.getStatus() != RECEIVED` 사후 검사 제거. `ClaimRequestStatus` import 제거. 미사용 `canAccessCustomer()` 삭제. |
| `claim/service/DispatchRecordService.java` | `rec.getStatus() != TRANSMITTED` 사후 검사 제거. `DispatchRecordStatus` import 제거. |
| `claim/service/ClaimCalculationService.java` | `calc.getStatus() != CALCULATED` 제거. `CalculationStatus` import 제거. |
| `claim/service/ClaimPaymentService.java` | `calc.getStatus() != APPROVED` → `calc.requireApproved()` 교체. `payment.getStatus() == COMPLETED` 제거. `CalculationStatus` import 제거. |
| `payment/service/PaymentRecordService.java` | `record.getStatus() != WAITING` (confirm/reject 각각) 제거. |
| `payment/service/RefundService.java` | `CALCULATION_PENDING` 사후 검사 제거. `CALCULATED` 사전 검사 제거. `refund.confirm()` 호출로 교체. `RefundStatus` import 제거. |
| `sales/service/ChannelScreeningService.java` | `s.getScreeningStatus() != PENDING` (approve/reject 각각) 제거. |
| `inquiry/service/InquiryService.java` | `InquiryStatus.ANSWERED` 중복 검사 제거. `inquiry.answer()` 호출로 교체. |
| `consultation/service/ConsultationService.java` | `!"접수".equals(r.getStatus())` 사전 검사 제거. |
| `consultation/service/InterviewScheduleService.java` | `"취소".equals(s.getStatus())` 사전 검사 2개 제거. |
| `education/service/EducationPlanService.java` | `!"승인요청".equals(plan.getStatus())` 사전 검사 2개 제거. |

### System.out.println 전체 제거 (2026-06-03)

엔터티 전 파일의 `System.out.println` 26건을 모두 제거했다. 대상 파일:
`actor/SalesChannel`, `actor/SalesManager`, `claim/DamageInvestigation`, `claim/SupplementRequest`,
`common/Attachment`, `consultation/InsuranceApplication`, `consultation/PolicyApplication`,
`consultation/Proposal`, `consultation/ReviewResult`, `consultation/Revival`, `consultation/Underwriting`,
`contract/ContractFilter`, `education/EducationExecution`, `payment/DeductionAdjustment`,
`payment/OverdueNoticeSetting`, `payment/Payment`.

---

---

## 코드 점검 결과 (2026-06-03) — 수정 완료

리팩터링 완료 후 전 도메인 엔터티·서비스를 재점검한 결과. 아래 항목 모두 수정 완료.

### A. 실제 버그 (동작 오류)

**`payment/entity/RefundCalculation.java:156`**
`confirm()` 메서드가 확정 처리 후에도 상태를 `CALCULATED → CALCULATED`로 유지한다. `PAID`로 바꿔야 한다.

```java
// 현재 (버그)
this.status = RefundStatus.CALCULATED;
// 수정
this.status = RefundStatus.PAID;
```

추가로 `payment/service/RefundService.java`의 `confirm()` 메서드가 `refundCalculationRepository.updateStatus(refund)`를 호출하지 않아, confirm 후에도 DB의 `RefundCalculation` 상태가 `CALCULATED`로 남는다. 두 곳을 함께 수정해야 한다.

---

### B. Dead Code (호출되지 않아 의미 없는 코드)

| 위치 | 문제 |
|---|---|
| `contract/entity/Contract.java:99-109` `updateStatus(String)` | 한글 문자열("해지", "만기" 등)로 상태를 비교하는 메서드. `CancellationService`는 `contractRepository.updateStatus(id, enum)`으로 직접 우회하므로 이 메서드는 아무데서도 호출되지 않는다. 삭제 가능. |
| `claim/entity/ClaimCalculation.java:108-113` `submitForApproval()` | `APPROVAL_PENDING` 상태로 전이하는 메서드. 서비스에서 호출하지 않고 `approve()`가 `CALCULATED→APPROVED`를 직접 처리한다. 삭제 가능. |
| `domain/common/enums/PlanStatus.java` | `EducationPlan`은 `String status` 필드에 "작성중"/"임시저장"/"승인요청"/"승인"/"반려" 한글 문자열을 직접 사용한다. `PlanStatus` enum(TEMP_SAVE, UNDER_REVIEW)은 어디서도 import되지 않는다. enum을 쓰려면 "승인"/"반려" 상태 추가와 DB 마이그레이션이 필요해 별도 작업으로 판단, 우선 삭제 후 String 방식 유지하거나 전환 계획을 세운다. |

---

### C. Guard Check 누락

| 위치 | 문제 |
|---|---|
| `claim/entity/AccidentReport.java:112-114` `cancel()` | 현재 상태 확인 없이 바로 `CANCELED`로 전이. 문서 기준(RECEIVED 상태에서만 취소 가능)과 불일치. `status != RECEIVED`면 `badRequest` 추가 필요. |
| `claim/entity/Dispatch.java` `assignAgent()`, `depart()`, `arrive()`, `complete()`, `cancel()` | 전이 전 상태 검증이 없어 어떤 순서로도 전이 가능. 단, 현재 서비스에서 REQUESTED 이후 API가 미구현이므로 실제 호출 경로가 없다. API 추가 시 함께 guard check를 넣는다. |

---

## 참고: 이동하지 않는 검사 (단순 읽기용)

아래는 응답 DTO 매핑에 사용하는 `getStatus()` 호출이므로 서비스에 그대로 둔다.

- `ContractService` — `statusName()`, `statusLabel()` 변환용
- `CancellationService` — 응답 필드 매핑용
- `ExpiringContractManagementService` — 응답 필드 매핑용
- `PaymentRecordService` — 목록 필터 파라미터 파싱용