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

## 참고: 이동하지 않는 검사 (단순 읽기용)

아래는 응답 DTO 매핑에 사용하는 `getStatus()` 호출이므로 서비스에 그대로 둔다.

- `ContractService` — `statusName()`, `statusLabel()` 변환용
- `CancellationService` — 응답 필드 매핑용
- `ExpiringContractManagementService` — 응답 필드 매핑용
- `PaymentRecordService` — 목록 필터 파라미터 파싱용