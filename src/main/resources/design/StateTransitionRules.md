# 도메인 상태 전이 규칙

코드 기준으로 정리한 각 도메인의 상태값(status enum)과 전이 조건·트리거 API.
상태 전이를 일으키는 서비스 메서드와 호출 엔드포인트를 함께 기재한다.

---

## 1. Contract — 계약 (`ContractStatus`)

| 상태 | 설명 |
|---|---|
| `NORMAL` | 정상 계약 (초기값) |
| `CANCELLED` | 해지 완료 |
| `EXPIRED` | 만기 |
| `LAPSED` | 실효 |

**전이 규칙**

```
(계약 생성) ──────────────────────────────────▶ NORMAL
NORMAL ─── POST /api/contracts/{contractNo}/cancel ──▶ CANCELLED
NORMAL ─── (시스템/배치) ──────────────────────────▶ EXPIRED
NORMAL ─── (시스템/배치) ──────────────────────────▶ LAPSED
```

- `CANCELLED` 전이: `CancellationService.cancel()`. NORMAL 이외의 상태에서 요청 시 `ApiException.badRequest`.
- `EXPIRED`, `LAPSED`: 현재 서비스 코드에서 직접 전이하는 경로 없음 — 별도 배치 또는 수동 처리 대상.
- 해지 후 Contract에 연결된 `ContractPaymentStatus`(NORMAL/OVERDUE/UNPAID)는 납입 기록과 별도로 관리됨.

---

## 2. AccidentReport — 사고 접수 (`AccidentReportStatus`)

| 상태 | 설명 |
|---|---|
| `DRAFT` | 작성중 (enum 정의만 있음, 서비스에서 직접 사용 안 함) |
| `RECEIVED` | 접수완료 |
| `CANCELED` | 취소 |

**전이 규칙**

```
POST /api/accidents ──────▶ RECEIVED   (report.receive() 호출)
RECEIVED ─── (취소 API) ──▶ CANCELED
```

- 생성 시 `report.receive()` 직후 `RECEIVED`가 아니면 `badRequest`로 막혀 DRAFT 상태로 저장되지 않음.
- 사고 접수와 동시에 `needsDispatch=true`면 같은 트랜잭션에서 `Dispatch`(상태 `REQUESTED`)가 자동 생성됨.

---

## 3. Dispatch — 출동 (`DispatchStatus`)

| 상태 | 설명 |
|---|---|
| `REQUESTED` | 신청 (사고 접수 시 자동 생성) |
| `ASSIGNED` | 배정 |
| `DEPARTED` | 출발 |
| `ARRIVED` | 도착 |
| `COMPLETED` | 완료 |
| `CANCELED` | 취소 |

**전이 규칙**

```
POST /api/accidents (needsDispatch=true) ──▶ REQUESTED
REQUESTED ──▶ ASSIGNED ──▶ DEPARTED ──▶ ARRIVED ──▶ COMPLETED
                                                  └─▶ CANCELED
```

- `REQUESTED` 이후 전이(`ASSIGNED`, `DEPARTED`, 등)는 현재 별도 상태변경 API가 구현되어 있지 않음 — 향후 추가 대상.

---

## 4. DispatchRecord — 출동 기록 (`DispatchRecordStatus`)

| 상태 | 설명 |
|---|---|
| `DRAFT` | 작성중 (내부 중간 상태) |
| `TRANSMITTED` | 전송완료 |

**전이 규칙**

```
POST /api/dispatches/{dispatchNo}/record ──▶ DRAFT ──(rec.transmit())──▶ TRANSMITTED
```

- 하나의 요청 안에서 저장(DRAFT) → 사진 등록 → `rec.transmit()` → `TRANSMITTED`까지 원자적으로 처리됨.
- `TRANSMITTED`가 아니면 `badRequest`.
- 출동 건당 기록 1건만 허용 (중복 등록 시 `badRequest`).

---

## 5. ClaimRequest — 보험금 청구 (`ClaimRequestStatus`)

| 상태 | 설명 |
|---|---|
| `DRAFT` | 작성중 (enum 정의만 있음) |
| `RECEIVED` | 접수완료 |

**전이 규칙**

```
POST /api/claims ──▶ RECEIVED
```

- 생성 시 바로 `RECEIVED` 상태로 저장. DRAFT로 저장되는 흐름 없음.
- 조사 등록(`POST /api/investigations/{claimNo}`) 진입 조건: `claim.status == RECEIVED`.

---

## 6. DamageInvestigation — 손해 조사 (`InvestigationStatus`)

| 상태 | 설명 |
|---|---|
| `NEW_ASSIGNED` | 신규배정 (enum 정의만 있음) |
| `INVESTIGATING` | 조사중 (enum 정의만 있음) |
| `INVESTIGATED` | 조사완료 |
| `CLOSED` | 종결 |

**전이 규칙**

```
POST /api/investigations/{claimNo}
    result=APPROVED ──▶ INVESTIGATED   (inv.complete())
    result=REJECTED ──▶ CLOSED         (inv.closeAsRejected())
```

- 조사 결과 제출과 동시에 최종 상태로 전이됨. 중간 단계(`NEW_ASSIGNED`, `INVESTIGATING`)는 현재 서비스 코드에서 사용 안 함.
- 청구 건당 조사 1건만 허용 (중복 시 `badRequest`).
- `INVESTIGATED` 전이 후에만 산출(`ClaimCalculation`) 생성 가능.

---

## 7. ClaimCalculation — 보험금 산출 (`CalculationStatus`)

| 상태 | 설명 |
|---|---|
| `CALCULATED` | 산출완료 (초기값) |
| `APPROVAL_PENDING` | 결재대기 (enum 정의만 있음) |
| `APPROVED` | 승인완료 |
| `CLOSED` | 종결 |

**전이 규칙**

```
POST /api/calculations/{investigationNo}
    isExceededDeductible=false ──▶ CALCULATED   (자동 산출)
    isExceededDeductible=true  ──▶ CLOSED        (calc.closeAsExceeded())

CALCULATED ── POST /api/calculations/{calculationNo}/approve ──▶ APPROVED
```

- 진입 조건: `investigation.result == APPROVED`이어야 산출 등록 가능.
- `CLOSED`(자기부담금 초과): 지급 불가. 여기서 흐름이 종료됨.
- `APPROVED` 전이 조건: 현재 상태가 `CALCULATED`이어야 함. 그 외는 `badRequest`.
- `APPROVED` 후에만 지급(`ClaimPayment`) 생성 가능.

---

## 8. ClaimPayment — 보험금 지급 (`ClaimPaymentStatus`)

| 상태 | 설명 |
|---|---|
| `WAITING` | 대기 (즉시 지급 선택 시 초기값) |
| `SCHEDULED` | 예약 (예약 지급 선택 시 초기값) |
| `COMPLETED` | 완료 |
| `FAILED` | 실패 |
| `CLOSED` | 종결 (enum 정의만 있음) |

**전이 규칙**

```
POST /api/claim-payments/{calculationNo}
    paymentType=IMMEDIATE  ──▶ WAITING
    paymentType=SCHEDULED  ──▶ SCHEDULED

WAITING   ──┐
SCHEDULED ──┘── POST /api/claim-payments/{paymentNo}/execute
                  OTP 성공 ──▶ COMPLETED
                  OTP 실패 ──▶ FAILED     (예외 미발생, 클라이언트가 status로 판별)
```

- 진입 조건: `calculation.status == APPROVED`.
- `COMPLETED` 상태에서 재실행 시 `badRequest`.
- `FAILED`는 예외 없이 저장됨 — 롤백 방지를 위한 의도적 설계.

---

## 9. PaymentRecord — 납입 기록 (`PaymentRecordStatus`)

| 상태 | 설명 |
|---|---|
| `WAITING` | 대기 (초기값) |
| `COMPLETED` | 확정 |
| `REJECTED` | 반려 |

**전이 규칙**

```
(납입 기록 생성) ──────────────────────────────────────────────────▶ WAITING
WAITING ── POST /api/payment-records/{recordNo}/confirm ──▶ COMPLETED
WAITING ── POST /api/payment-records/{recordNo}/reject  ──▶ REJECTED
```

- `WAITING` 이외 상태에서 확정/반려 시 `badRequest`.
- 반려 시 `rejectCategory`(PAYMENT_ERROR/DUPLICATE_PAYMENT/CONTRACT_MISMATCH/OTHER) 필수.

---

## 10. RefundCalculation — 환급금 산출 (`RefundStatus`)

| 상태 | 설명 |
|---|---|
| `CALCULATION_PENDING` | 산출대기 (데이터 누락 시) |
| `CALCULATED` | 산출완료 |
| `PAID` | 지급완료 (enum 정의만 있음) |

**전이 규칙**

```
POST /api/refund-calculations/{cancellationNo}
    데이터 충분  ──▶ CALCULATED
    데이터 누락  ──▶ CALCULATION_PENDING → 즉시 badRequest (저장 안 됨)

CALCULATED ── POST /api/refund-calculations/{refundNo}/confirm ──▶ RefundPayment 생성(WAITING)
```

- 진입 조건: 해지(`Cancellation`) 건이 존재해야 함. 해지 건당 산출 1건만 허용.
- `confirm()`은 `RefundCalculation` 상태를 직접 변경하지 않고 `RefundPayment`를 생성함.

---

## 11. RefundPayment — 환급금 지급 (`RefundPaymentStatus`)

| 상태 | 설명 |
|---|---|
| `WAITING` | 대기 (초기값) |
| `COMPLETED` | 완료 |
| `FAILED` | 실패 |
| `LOCKED` | 잠금 (OTP 5회 실패) |

**전이 규칙**

```
(confirm으로 생성) ──────────────────────────────────────────────▶ WAITING
WAITING ── POST /api/refund-payments/{paymentNo}/execute
               OTP 성공  ──▶ COMPLETED
               OTP 1~4회 실패 ──▶ FAILED (재시도 가능)
               OTP 5회 실패   ──▶ LOCKED (관리자 해제 필요)
```

- `COMPLETED` 또는 `LOCKED` 상태에서 execute 시 `badRequest`.

---

## 12. Inquiry — 문의 (`InquiryStatus`)

| 상태 | 설명 |
|---|---|
| `PENDING` | 답변대기 |
| `ANSWERED` | 답변완료 |

**전이 규칙**

```
POST /api/inquiries ──────────────────────────▶ PENDING
PENDING ── POST /api/inquiries/{inquiryNo}/answer ──▶ ANSWERED
```

---

## 13. ChannelScreening — 채널 심사 (`ScreeningStatus`)

| 상태 | 설명 |
|---|---|
| `PENDING` | 심사대기 |
| `APPROVED` | 승인 |
| `REJECTED` | 거절 |

**전이 규칙**

```
POST /api/channel-screenings ──────────────────────────────────────▶ PENDING
PENDING ── POST /api/channel-screenings/{screeningNo}/approve ──▶ APPROVED
PENDING ── POST /api/channel-screenings/{screeningNo}/reject  ──▶ REJECTED
```

- `PENDING` 이외 상태에서 승인/거절 시 `badRequest`.

---

## 14. EducationPlan — 교육 계획 (`PlanStatus`)

| 상태 | 설명 |
|---|---|
| `TEMP_SAVE` | 임시저장 |
| `UNDER_REVIEW` | 승인요청 (검토중) |

**전이 규칙**

```
POST /api/education-plans
    action != "REQUEST_APPROVAL" ──▶ TEMP_SAVE
    action == "REQUEST_APPROVAL" ──▶ UNDER_REVIEW   (필수 항목 검증 통과 시)

UNDER_REVIEW ── POST /api/education-plans/{planNo}/approve ──▶ 승인
UNDER_REVIEW ── POST /api/education-plans/{planNo}/reject  ──▶ 반려 (reason 필수)
```

> **주의**: `approvePlan()`, `rejectPlan()` 내부 조건 검사는 `PlanStatus` enum이 아니라 한글 문자열 `"승인요청"`과 비교함. `plan.approve()`/`plan.reject()` 이후 실제로 저장되는 status 값은 `EducationPlan` 엔터티 내부에서 결정됨.

---

## 전이 금지 요약

각 도메인에서 서비스 계층이 명시적으로 막는 역방향/중복 전이:

| 도메인 | 금지 전이 | 응답 |
|---|---|---|
| ClaimCalculation | `CALCULATED` 아닌 상태에서 approve | 400 |
| ClaimPayment | `APPROVED` 아닌 산출에서 지급 생성 | 400 |
| ClaimPayment | `COMPLETED` 상태에서 execute | 400 |
| PaymentRecord | `WAITING` 아닌 상태에서 confirm/reject | 400 |
| RefundCalculation | `CALCULATED` 아닌 상태에서 confirm | 400 |
| RefundPayment | `COMPLETED`/`LOCKED` 상태에서 execute | 400 |
| ChannelScreening | `PENDING` 아닌 상태에서 approve/reject | 400 |
| DamageInvestigation | 동일 청구에 조사 중복 등록 | 400 |
| ClaimCalculation | 동일 조사에 산출 중복 등록 | 400 |