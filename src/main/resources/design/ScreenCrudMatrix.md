# 유스케이스 화면 CRUD 매트릭스

> 목적: `Usecase_scenario.md`의 유스케이스를 화면 구현 관점에서 분해하고, 각 화면에 필요한 CRUD/action을 정리한다.
> 원칙: 이 문서는 프론트 화면 기준이다. API 갭·개선 후보는 `FrontendIntegrationReview.md`에서 관리한다.
> 기준: 2026-06-04 `ApiSpec.md` 전 도메인 명세 완성 기준으로 최신화.

## 1. 읽는 방법

CRUD/action 구분:

- `List`: 목록 조회
- `Read`: 상세 조회
- `Create`: 신규 등록/신청/작성
- `Update`: 수정
- `Delete/Cancel`: 취소, 삭제, 해지 요청 취소 등
- `Transition`: 승인, 반려, 확정, 실행, 지급 같은 상태 전이
- `Upload`: 파일/이미지 업로드
- `Download`: PDF/파일 다운로드
- `Stats`: 통계/그래프/집계

우선순위:

- `P1`: 내부 직원용 MVP에 필요
- `P2`: 업무 workflow MVP에 필요
- `P3`: 고객 포털에 필요
- `P4`: 운영 완성도에 필요

API 상태 표기:

- ✅: ApiSpec.md 기준 구현 완료
- ⚠️: 부분 구현 또는 제약 있음 (비고 참조)
- ❌: 미구현 (API 없음)

---

## 2. 영업

| 유스케이스 | 화면 | Actor | 필요한 CRUD/action | 우선순위 | API 상태 | 비고 |
|---|---|---|---|---|---|---|
| 영업 활동을 관리한다 | 영업활동 관리 목록 | 영업 관리자 | List, Read | P1 | ✅ | `GET /api/sales-activity-managements?startDate=&endDate=&channelType=` |
| 영업 활동을 관리한다 | 영업활동 상세 패널 | 영업 관리자 | Read | P1 | ⚠️ | 단건 조회 API 없음. 목록 응답 데이터로 상세 패널 구성 가능 |
| 영업 활동을 관리한다 | 개선 지시 등록 폼 | 영업 관리자 | Create | P2 | ✅ | `POST /api/sales-activity-managements` (`improvementContent`, `revisedTarget` 포함) |
| 판매채널을 모집한다 | 채널 모집 목록 | 영업 관리자 | List | P2 | ✅ | `GET /api/channel-recruitments` |
| 판매채널을 모집한다 | 신규 모집 등록 | 영업 관리자 | Create | P2 | ✅ | `POST /api/channel-recruitments` |
| 판매채널 채용을 심사한다 | 채널 심사 목록 | 영업 관리자 | List, Read | P1 | ✅ | `GET /api/channel-screenings`. 필터는 클라이언트 처리 |
| 판매채널 채용을 심사한다 | 채널 심사 상세 | 영업 관리자 | Read, Transition | P1 | ✅ | `POST /api/channel-screenings/{screeningNo}/approve` · `/reject` |
| 영업조직을 평가한다 | 영업조직 평가 목록 | 영업 관리자 | List, Read | P1 | ✅ | `GET /api/sales-org-evaluations?startDate=&endDate=&channelType=` |
| 영업조직을 평가한다 | 평가 등록 폼 | 영업 관리자 | Create | P2 | ✅ | `POST /api/sales-org-evaluations`. 등급 S/A이면 성과급 요청 버튼 프론트에서 노출 |
| 성과급 지급을 요청한다 | 성과급 요청 폼 | 영업 관리자 | Create | P2 | ✅ | `POST /api/bonus-requests` (권한: `ADMIN`만). 목록: `GET /api/bonus-requests` |
| 고객 정보를 등록한다 | 영업 고객 등록 | 영업 관리자 | List, Create | P2 | ✅ | `GET /api/customer-registrations`, `POST /api/customer-registrations`. 공통 Customer master와 별개 |
| 활동 계획을 작성한다 | 활동 계획 목록/상세 | 판매채널 | List, Read, Create, Update | P2 | ⚠️ | `GET /api/activity-plans`, `GET /api/activity-plans/{planNo}`, `POST /api/activity-plans`. Update API 없음. 재등록으로 대체 |

---

## 3. 교육

| 유스케이스 | 화면 | Actor | 필요한 CRUD/action | 우선순위 | API 상태 | 비고 |
|---|---|---|---|---|---|---|
| 교육 계획안을 작성한다 | 교육 계획 목록 | 영업교육 담당자/영업 관리자 | List, Read | P1 | ✅ | `GET /api/education-plans?status=`. `status` 필터 지원 |
| 교육 계획안을 작성한다 | 교육 계획 작성 폼 | 영업교육 담당자 | Create, Update | P2 | ⚠️ | `POST /api/education-plans`. `action: "REQUEST_APPROVAL"` → UNDER_REVIEW, 그 외 → TEMP_SAVE. Update API 없음 |
| 교육 계획안을 작성한다 | 교육 계획 승인/반려 | 영업 관리자 | Transition | P2 | ✅ | `POST /api/education-plans/{planNo}/approve` · `/reject` (반려 사유 포함) |
| 교육 제반을 등록한다 | 승인 교육 계획 선택 | 영업교육 담당자 | List, Read | P2 | ✅ | `GET /api/education-plans?status=APPROVED` |
| 교육 제반을 등록한다 | 교육 제반 등록 | 영업교육 담당자 | Create, Upload | P2 | ⚠️ | `POST /api/education-preparations`. Upload(대상자 명단 첨부) ❌ 미구현 |
| 교육을 진행한다 | 교육 실행 화면 | 영업교육 담당자 | Read, Create, Transition | P2 | ✅ | `GET /api/education-executions`, `POST /api/education-executions` (출석 체크 포함). 완료 처리는 등록으로 대체 |

---

## 4. 상담/청약/인수심사

| 유스케이스 | 화면 | Actor | 필요한 CRUD/action | 우선순위 | API 상태 | 비고 |
|---|---|---|---|---|---|---|
| 상담을 요청한다 | 고객 상담 신청 | 고객 | Create | P3 | ⚠️ | `POST /api/consultations`. 고객 master 연결 없음 (FE-CONSULT-11 참조) |
| 상담을 요청한다 | 상담 요청 목록/상세 | 판매채널 | List, Read, Transition | P2 | ✅ | `GET /api/consultations`, `GET /api/consultations/{consultNo}`, `POST /api/consultations/{consultNo}/accept` |
| 면담일정을 관리한다 | 면담 일정 목록 | 판매채널 | List, Read | P2 | ✅ | `GET /api/interview-schedules` |
| 면담일정을 관리한다 | 면담 등록/수정 | 판매채널 | Create, Update, Cancel | P2 | ✅ | `POST /api/interview-schedules`, `PUT /api/interview-schedules/{scheduleNo}`, `POST .../{scheduleNo}/cancel` |
| 면담기록을 관리한다 | 면담 기록 목록/상세 | 판매채널 | List, Read | P2 | ✅ | `GET /api/interview-records`, `GET /api/interview-records/{recordNo}` |
| 면담기록을 관리한다 | 면담 기록 등록/수정 | 판매채널 | Create, Update | P2 | ⚠️ | `POST /api/interview-records`, `PUT /api/interview-records/{recordNo}`. 면담 일정과 직접 연결 없음 (FE-CONSULT-07 참조) |
| 보험상품을 제안한다 | 상품 검색/제안 | 판매채널 | List, Read, Create | P2 | ⚠️ | `GET /api/insurance-products`, `POST /api/proposals`, `GET /api/proposals`. 상품 단건 상세 없음. 제안서 Upload ❌ 미구현 |
| 청약서를 작성한다 | 청약서 작성 | 판매채널 | Create, Upload | P2 | ⚠️ | `POST /api/policy-applications`. Upload(서명 파일) ❌ 미구현. 목록: `GET /api/policy-applications` |
| 보험상품을 조회한다 | 상품 목록/상세 | 고객 | List, Read | P3 | ⚠️ | `GET /api/insurance-products`. 상품 단건 상세 API 없음 |
| 보험을 신청한다 | 보험 신청 wizard | 고객 | Create, Transition | P3 | ✅ | `POST /api/insurance-applications`. 목록: `GET /api/insurance-applications`. 상태 전이는 인수심사 결과로 |
| 부활을 요청한다 | 부활 신청 wizard | 고객 | Create, Transition | P3 | ✅ | `POST /api/revivals`. 목록: `GET /api/revivals`. 미납보험료는 요청 body에 직접 입력 |
| 인수 심사를 한다 | 심사 대기 목록 | 보험 심사자 | List, Read | P2 | ✅ | `GET /api/underwriting/pending` (청약+보험신청 UNION) |
| 인수 심사를 한다 | 심사 결과 등록 | 보험 심사자 | Create, Transition, Upload | P2 | ⚠️ | `POST /api/underwriting`. Upload(첨부 서류) ❌ 미구현. `applicationType`: `POLICY`/`INSURANCE` |
| 심사 결과를 전달한다 | 심사 결과 확인 | 판매채널 | Read, Transition | P2 | ⚠️ | 전용 조회 API 없음. `GET /api/insurance-applications/{applicationNo}` · `GET /api/policy-applications/{applicationNo}`로 간접 확인 |

---

## 5. 사고/청구/보상

| 유스케이스 | 화면 | Actor | 필요한 CRUD/action | 우선순위 | API 상태 | 비고 |
|---|---|---|---|---|---|---|
| 사고를 접수한다 | 사고 접수 시작 | 고객 | Create | P3 | ✅ | `POST /api/accidents`. `needsDispatch: true`이면 출동 자동 생성 |
| 사고를 접수한다 | 자동차 사고 접수 | 고객 | Create, Upload, Cancel | P3 | ⚠️ | `POST /api/accidents`. Upload(사진) ❌ 미구현. Cancel ❌ 미구현 |
| 현장 출동 정보를 기록한다 | 출동 배정 목록 | 현장출동 직원 | List, Read | P2 | ✅ | `GET /api/dispatches` (권한: `DISPATCH_STAFF`, `CLAIM_STAFF`, `ADMIN`) |
| 현장 출동 정보를 기록한다 | 현장 기록 입력 | 현장출동 직원 | Create, Upload, Transition | P2 | ✅ | `POST /api/dispatches/{dispatchNo}/record` (multipart, 사진 업로드 포함). 사진 다운로드 URL은 ❌ 미구현 |
| 보험금을 요청한다 | 보험금 청구 wizard | 고객 | Create, Upload | P3 | ⚠️ | `POST /api/claims`. Upload(서류 첨부) ❌ 미구현. 목록: `GET /api/claims` |
| 손해를 조사한다 | 손해조사 목록/상세 | 보상담당자 | List, Read | P2 | ✅ | `GET /api/claims` (청구 목록), `GET /api/claims/{claimNo}/investigation` |
| 손해를 조사한다 | 조사 결과 등록 | 보상담당자 | Create, Transition | P2 | ✅ | `POST /api/claims/{claimNo}/investigation`. `result`: `APPROVED`→INVESTIGATED, `REJECTED`→CLOSED |
| 손해를 조사한다 | 보완 서류/추가 조사 | 보상담당자 | Create, Transition | P4 | ❌ | 미구현 |
| 보험금을 산출한다 | 보험금 산출 상세 | 보상담당자 | Read, Create, Transition | P2 | ✅ | `POST /api/investigations/{investigationNo}/calculation`, `POST /api/calculations/{calculationNo}/approve` |
| 보험금을 지급한다 | 보험금 지급 실행 | 보상담당자 | Read, Create, Transition | P2 | ✅ | `POST /api/calculations/{calculationNo}/payment`, `POST /api/payments/{paymentNo}/execute` (OTP 검증). 예약 지급도 지원 |

---

## 6. 계약/납입/환급

| 유스케이스 | 화면 | Actor | 필요한 CRUD/action | 우선순위 | API 상태 | 비고 |
|---|---|---|---|---|---|---|
| 가입 보험을 조회한다 | 내 계약 목록 | 고객 | List | P3 | ✅ | `GET /api/contracts` (고객 본인 계약만) |
| 가입 보험을 조회한다 | 계약 상세 탭 | 고객 | Read, Download | P3 | ⚠️ | `GET /api/contracts/{contractNo}`. Download(PDF/가입증명서) ❌ 미구현 |
| 보험을 해지한다 | 해지 신청 wizard | 고객 | Create, Transition | P3 | ⚠️ | `POST /api/contracts/{contractNo}/cancellation`. 예상 환급금 사전 산출 API ❌ 없음 |
| 보험료를 납입한다 | 납입 대상 선택 | 고객 | List | P3 | ✅ | `GET /api/customers/{customerId}/contracts` |
| 보험료를 납입한다 | 납입 preview/신청 | 고객 | Create, Transition | P3 | ✅ | `POST /api/payments/preview`, `POST /api/payments` |
| 보험료 납부 내역을 관리한다 | 납부 내역 목록/상세 | 계약 담당자 | List, Read, Transition | P2 | ✅ | `GET /api/payment-records?contractNo=&status=`, `POST .../confirm`, `POST .../reject` |
| 해약 환급 내역을 조회한다 | 환급 내역 목록/상세 | 계약 담당자 | List, Read | P2 | ✅ | `GET /api/refund-calculations`, `GET /api/refund-calculations/{refundNo}` |
| 해약 환급금을 산출한다 | 환급 산출 | 계약 담당자 | Create, Read, Transition | P2 | ✅ | `POST /api/cancellations/{cancellationNo}/refund-calculation`, `POST .../confirm` |
| 환급금을 지급한다 | 환급 지급 | 계약 담당자 | List, Read, Transition | P2 | ✅ | `GET /api/refund-payments`, `POST /api/refund-payments/{paymentNo}/execute` (OTP 검증) |
| 계약 정보를 조회한다 | 계약 목록/상세 | 계약 담당자 | List, Read | P1 | ✅ | `GET /api/contracts`, `GET /api/contracts/{contractNo}` |
| 계약 통계 정보를 관리한다 | 계약 통계 대시보드 | 계약 담당자 | Stats, Download | P4 | ⚠️ | `GET /api/contract-statistics`, `POST /api/contract-statistics`, `GET /api/contract-statistics/history`. 그래프/엑셀 Download ❌ 미구현. 숫자 데이터는 사용 가능 |
| 만기 계약을 관리한다 | 만기 계약 목록/안내 | 계약 담당자 | List, Create, Transition | P2 | ✅ | `GET /api/expiring-contracts`, `POST /api/expiring-contracts/{contractNo}/notice`, `POST /api/expiring-notices/{noticeNo}/response` |

---

## 7. 고객센터

| 유스케이스 | 화면 | Actor | 필요한 CRUD/action | 우선순위 | API 상태 | 비고 |
|---|---|---|---|---|---|---|
| 문의한다 | 문의 등록 | 고객 | Create, Upload | P3 | ⚠️ | `POST /api/inquiries`. Upload(첨부 파일) ❌ 미구현. `attachmentFileName`/`attachmentFileSize` 필드는 있으나 실제 업로드 없음 |
| 문의한다 | 내 문의 내역 | 고객 | List, Read | P3 | ✅ | `GET /api/inquiries` (고객은 본인 문의만). `GET /api/inquiries/{inquiryNo}` |
| 문의한다 | 문의 목록/상세 | 고객센터 담당자 | List, Read | P1 | ✅ | `GET /api/inquiries?customerName=&status=`, `GET /api/inquiries/{inquiryNo}` |
| 문의한다 | 문의 답변 | 고객센터 담당자 | Create, Transition | P2 | ✅ | `POST /api/inquiries/{inquiryNo}/answer` (진입 조건: `status == PENDING`) |
| 문의한다 | FAQ | 고객 | List, Read | P4 | ❌ | 미구현. API 없음 |

---

## 8. 공통 CRUD 요구

### 8.1 고객 선택

다음 화면은 고객 검색/상세가 필요하다.

- 계약 조회, 납입/환급, 사고/청구, 상담/면담, 고객센터, 영업 고객 등록

| 기능 | API 상태 | 비고 |
|---|---|---|
| 고객명/전화번호/고객번호 검색 | ✅ | `GET /api/customers?keyword=&page=&size=` (직원/관리자 전용) |
| 고객 상세 | ✅ | `GET /api/customers/{customerId}` (직원/관리자 또는 고객 본인) |
| 고객별 계약 목록 | ✅ | `GET /api/customers/{customerId}/contracts` (납입용 계약 목록) |
| 고객 회원가입 | ✅ | `POST /api/auth/signup/customer` (직접 회원가입) |
| 고객 계정 발급 (관리자) | ✅ | `POST /api/auth/customer-accounts` |
| 고객 정보 수정 | ❌ | `PUT /api/customers/{customerId}` 없음 |

### 8.2 파일

| 기능 | API 상태 | 비고 |
|---|---|---|
| multipart 업로드 | ⚠️ | 출동 기록(`POST /api/dispatches/{dispatchNo}/record`)만 구현됨. 나머지 업로드 ❌ 미구현 |
| 파일 메타데이터 저장 | ⚠️ | 출동 사진 파일명만 DB 저장. URL/key 없음 |
| 미리보기 URL 또는 다운로드 URL | ❌ | 미구현. S3/정적 파일 제공 경로 없음 |
| 권한 검증된 파일 접근 | ❌ | 미구현 |

파일이 필요한 화면 (모두 ❌ 미구현):

- 청약서 서명 파일, 사고 접수 사진, 보험금 청구 서류, 교육 대상자 명단, 문의 첨부, 약관/PDF/가입증명서 다운로드

출동 현장 사진은 업로드만 구현됨 (`photoCount`만 반환, 다운로드 URL 없음).

### 8.3 통계/다운로드

| 기능 | API 상태 | 비고 |
|---|---|---|
| 계약 통계 수치 조회 | ✅ | `GET /api/contract-statistics` (snapshot + 실시간 집계) |
| 통계 이력 목록 | ✅ | `GET /api/contract-statistics/history` |
| 그래프/차트 데이터 | ❌ | 시계열/비율 분석 API 없음. 프론트에서 이력 데이터로 직접 차트 구성 가능 |
| 엑셀/CSV 다운로드 | ❌ | 미구현 |

---

## 9. 인증 관련 화면

| 화면 | Actor | API 상태 | 비고 |
|---|---|---|---|
| 로그인 | 전체 | ✅ | `POST /api/auth/login`. 세션 쿠키(`DPBE_SESSION`) 발급 |
| 로그아웃 | 전체 | ✅ | `POST /api/auth/logout` |
| 고객 직접 회원가입 | 고객 | ✅ | `POST /api/auth/signup/customer` |
| 내 세션 조회 | 전체 | ✅ | `GET /api/auth/me` (`role`, `linkedCustomerNo` 포함) |
| 비밀번호 변경 | 전체 | ✅ | `POST /api/auth/password`. 최초 로그인 시 필수 (`passwordChangeRequired`) |
| 고객 계정 발급 | 관리자 | ✅ | `POST /api/auth/customer-accounts` |
| 직원 계정 발급 | 관리자 | ✅ | `POST /api/auth/staff-accounts` |
| 비밀번호 찾기/초기화 | 전체 | ❌ | 미구현 |

---

## 10. 미구현 API 요약 (❌ 항목)

| 화면/기능 | 필요한 action | 참고 |
|---|---|---|
| 활동 계획 수정 | `PUT /api/activity-plans/{planNo}` | |
| 교육 계획 수정 | `PUT /api/education-plans/{planNo}` | |
| 사고 접수 취소 | `POST /api/accidents/{accidentNo}/cancel` | |
| 보완 서류/추가 조사 | 신규 API 필요 | P4 |
| 예상 환급금 사전 산출 | `GET /api/cancellations/preview` 등 | |
| 파일 업로드 (청약, 청구, 교육, 문의 등) | multipart 전반 확장 | S3 정책 설계 선행 필요 |
| 파일 다운로드 URL | `GET /api/.../files/{fileId}` 등 | |
| 고객 정보 수정 | `PUT /api/customers/{customerId}` | |
| 보험상품 단건 상세 | `GET /api/insurance-products/{productNo}` | |
| 상담과 고객 master 연결 | `consultation_requests.customer_id` 추가 | FE-CONSULT-11 |
| 면담 일정-기록 연결 | `interview_records.schedule_no` 추가 | FE-CONSULT-07 |
| 제안서/청약 ID 기반 연결 | `customerId`, `productNo` 기반 전환 | FE-CONSULT-05, 12 |
| 통계 그래프/엑셀 다운로드 | read model query API + 파일 생성 | P4 |
| FAQ | `GET /api/faqs` | P4 |
| 비밀번호 찾기/초기화 | 정책 결정 필요 | |