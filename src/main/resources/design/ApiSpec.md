# API 명세서

> 목적: 프론트 병렬 작업을 위한 수동 API 계약 문서.
> 기준: 2026-06-04 현재 구현된 전 도메인 API.
> 상태: 전 도메인 API 명세 작성 완료 (인증·고객·계약·납입·청구·교육·영업·문의·상담 — 전 컨트롤러 커버).

## 공통

- 로컬 Base URL: `http://localhost:8080`
- 배포 Base URL: `http://<EC2_PUBLIC_IP>:8080`
- 인증 방식: HTTP Session Cookie
- 쿠키 이름: `DPBE_SESSION`
- 로그인 이후 요청은 `Authorization` 헤더가 아니라 쿠키로 인증한다.
- 프론트와 백엔드 origin이 다르면 `fetch`는 `credentials: "include"`, axios는 `withCredentials: true`가 필요하다.
- 요청/응답 body는 JSON을 기본으로 한다.

## 목록 응답 정책

테이블 화면에 쓰는 목록 API는 아래 형태를 기본으로 한다.

```json
{
  "page": 1,
  "size": 20,
  "total": 100,
  "items": []
}
```

- `page`: 1부터 시작하는 현재 페이지 번호
- `size`: 요청한 페이지 크기. 기본값은 20, 최대값은 100
- `total`: 필터 적용 후 전체 건수
- `items`: 현재 페이지 데이터

페이지네이션이 필요 없는 참조용 소량 목록은 아래 형태를 허용한다.

```json
{
  "items": []
}
```

신규 목록 API는 배열을 직접 반환하지 않는다. 기존 배열 반환 API는 프론트 연결 전에 순차적으로 wrapper 또는 page 응답으로 전환한다.

2026-06-03 기준 주요 테이블 목록 API는 `page/size/total/items` 응답으로 통일했고, `total`과 `items`는 DB `COUNT` + `LIMIT/OFFSET` 기준으로 계산한다. 고객 본인 데이터만 보여야 하는 목록은 SQL 조건에 고객 식별자를 포함해 `total`도 본인 데이터 기준으로 계산한다.

## 권한 정책 요약

인증은 HTTP Session Cookie로 처리하고, 인가는 `auth_users.role` 기반으로 처리한다. `/api/auth/**`를 제외한 `/api/**`는 로그인 세션이 필요하다.

| 영역 | 접근 정책 |
|---|---|
| 고객 검색 | 직원 또는 관리자 |
| 고객 상세 | 직원 또는 관리자, 고객 본인 |
| 계약 목록/상세 | 직원 또는 관리자 전체, 고객은 본인 계약만 |
| 계약 통계/만기계약 관리 | `CONTRACT_STAFF`, `ADMIN` |
| 해지 | 직원 또는 관리자 전체, 고객은 본인 계약만 |
| 납입 기록 관리 | `FINANCE_STAFF`, `ADMIN` |
| 환급 계산/지급 | `FINANCE_STAFF`, `ADMIN` |
| 손해조사/보험금 산출 | `CLAIM_STAFF`, `ADMIN` |
| 보험금 지급 | `FINANCE_STAFF`, `CLAIM_STAFF`, `ADMIN` |
| 출동 기록 | `DISPATCH_STAFF`, `CLAIM_STAFF`, `ADMIN` |
| 상담/제안/면담 | `SALES_STAFF`, `UNDERWRITING_STAFF`, `ADMIN` |
| 인수심사 | `UNDERWRITING_STAFF`, `ADMIN` |
| 청약/계약체결/부활 신청 | 고객 본인 소유권 검증 |
| 영업/채널/활동계획 | `SALES_STAFF`, `ADMIN` |
| 성과급 요청 | `ADMIN` |
| 교육 계획/제반/실행 | `EDUCATION_STAFF`, `ADMIN` |
| 계약 통계/만기 계약 관리 | `CONTRACT_STAFF`, `ADMIN` |
| 문의 조회 | 직원/관리자 전체, 고객은 본인 문의만 |
| 문의 답변 | 직원 또는 관리자 |

## 인증 API 권한 요약

| Endpoint | 인증 필요 | 권한 |
|---|---:|---|
| `POST /api/auth/login` | 아니오 | 공개 |
| `POST /api/auth/signup/customer` | 아니오 | 공개 |
| `POST /api/auth/customer-accounts` | 예 | `ADMIN` |
| `POST /api/auth/staff-accounts` | 예 | `ADMIN` |
| `POST /api/auth/password` | 예 | 로그인 사용자 |
| `POST /api/auth/logout` | 예 | 로그인 사용자 |
| `GET /api/auth/me` | 예 | 로그인 사용자 |

비밀번호 변경이 필요한 계정은 `/api/auth/**`를 제외한 업무 API 호출이 403으로 차단된다.

## 에러 응답

공통 에러 응답:

```json
{
  "status": 400,
  "error": "Bad Request",
  "code": "BAD_REQUEST",
  "message": "오류 메시지",
  "path": "/api/example",
  "fieldErrors": [],
  "timestamp": "2026-06-02T10:00:00"
}
```

Validation 오류:

```json
{
  "status": 400,
  "error": "Bad Request",
  "code": "VALIDATION_ERROR",
  "message": "요청값 검증에 실패했습니다.",
  "path": "/api/auth/signup/customer",
  "fieldErrors": [
    {
      "field": "password",
      "message": "비밀번호는 8자 이상 100자 이하로 입력하세요."
    }
  ],
  "timestamp": "2026-06-02T10:00:00"
}
```

주요 error code:

- `VALIDATION_ERROR`: Bean Validation 실패
- `REQUEST_BODY_ERROR`: JSON 파싱, enum 값, 날짜 형식 등 request body 오류
- `BAD_REQUEST`: 일반 잘못된 요청
- `UNAUTHORIZED`: 로그인 필요 또는 로그인 실패
- `FORBIDDEN`: 권한 없음
- `NOT_FOUND`: 리소스 없음
- `INTERNAL_ERROR`: 서버 내부 오류

## 인증 API

### 고객 직접 회원가입

`POST /api/auth/signup/customer`

인증: 불필요

Request:

```json
{
  "username": "customer01",
  "password": "customer1234",
  "name": "테스트고객",
  "residentNo": "900101-1234567",
  "phone": "010-9999-0001",
  "email": "customer01@example.com",
  "address": "서울시 테스트구",
  "birthDate": "1990-01-01"
}
```

Response:

```json
{
  "customerId": "CUS00004",
  "username": "customer01",
  "message": "회원가입이 완료되었습니다."
}
```

### 로그인

`POST /api/auth/login`

인증: 불필요

Request:

```json
{
  "username": "admin",
  "password": "admin1234"
}
```

Response:

```json
{
  "user": {
    "id": 1,
    "username": "admin",
    "role": "ADMIN",
    "linkedCustomerId": null,
    "linkedCustomerNo": null,
    "displayName": "관리자",
    "passwordChangeRequired": false
  }
}
```

성공 시 `Set-Cookie: DPBE_SESSION=...`가 내려온다.

### 내 세션 조회

`GET /api/auth/me`

인증: 필요

Response:

```json
{
  "id": 1,
  "username": "admin",
  "role": "ADMIN",
  "linkedCustomerId": null,
  "linkedCustomerNo": null,
  "displayName": "관리자",
  "passwordChangeRequired": false
}
```

### 로그아웃

`POST /api/auth/logout`

인증: 필요

Response:

```json
{
  "message": "로그아웃되었습니다."
}
```

### 비밀번호 변경

`POST /api/auth/password`

인증: 필요

Request:

```json
{
  "currentPassword": "old-password",
  "newPassword": "new-password"
}
```

Response:

```json
{
  "message": "비밀번호가 변경되었습니다."
}
```

### 기존 고객 계정 발급

`POST /api/auth/customer-accounts`

권한: `ADMIN`

Request:

```json
{
  "customerId": "CUS00001",
  "username": "customer_account01"
}
```

Response:

```json
{
  "user": {
    "id": 2,
    "username": "customer_account01",
    "role": "CUSTOMER",
    "linkedCustomerId": 1,
    "linkedCustomerNo": "CUS00001",
    "displayName": "김고객",
    "passwordChangeRequired": true
  },
  "temporaryPassword": "임시비밀번호"
}
```

### 직원 계정 발급

`POST /api/auth/staff-accounts`

권한: `ADMIN`

Request:

```json
{
  "username": "claim_staff",
  "displayName": "보상직원",
  "role": "CLAIM_STAFF"
}
```

Response:

```json
{
  "user": {
    "id": 3,
    "username": "claim_staff",
    "role": "CLAIM_STAFF",
    "linkedCustomerId": null,
    "linkedCustomerNo": null,
    "displayName": "보상직원",
    "passwordChangeRequired": true
  },
  "temporaryPassword": "임시비밀번호"
}
```

직원 계정 발급에 사용할 수 있는 role:

- `STAFF`
- `CONTRACT_STAFF`
- `CLAIM_STAFF`
- `UNDERWRITING_STAFF`
- `SALES_STAFF`
- `EDUCATION_STAFF`
- `FINANCE_STAFF`
- `DISPATCH_STAFF`

`CUSTOMER`, `ADMIN`은 직원 계정 발급 role로 사용할 수 없다.

## 고객 API

### 고객 검색

`GET /api/customers?keyword=&page=1&size=20`

권한: 직원 또는 관리자

Query:

- `keyword`: 선택. 고객명, 고객번호, 전화번호 검색.
- `page`: 선택. 기본값 `1`.
- `size`: 선택. 기본값 `20`, 최대 `100`.

Response:

```json
{
  "page": 1,
  "size": 20,
  "total": 1,
  "items": [
    {
      "id": 4,
      "customerId": "CUS00004",
      "name": "테스트고객",
      "phone": "010-9999-0001",
      "email": "customer01@example.com"
    }
  ]
}
```

### 고객 상세

`GET /api/customers/{customerId}`

권한: 직원 또는 관리자, 고객 본인

Response:

```json
{
  "id": 4,
  "customerId": "CUS00004",
  "name": "테스트고객",
  "phone": "010-9999-0001",
  "email": "customer01@example.com",
  "address": "서울시 테스트구",
  "birthDate": "1990-01-01",
  "registeredAt": "2026-06-02T10:00:00"
}
```

## 주요 Enum 값

### UserRole

- `CUSTOMER`: 고객
- `STAFF`: 일반 직원
- `CONTRACT_STAFF`: 계약 담당 직원
- `CLAIM_STAFF`: 보상/청구 담당 직원
- `UNDERWRITING_STAFF`: 인수심사 담당 직원
- `SALES_STAFF`: 영업 담당 직원
- `EDUCATION_STAFF`: 교육 담당 직원
- `FINANCE_STAFF`: 재무/납입/환급 담당 직원
- `DISPATCH_STAFF`: 출동 담당 직원
- `ADMIN`: 관리자

### PaymentMethod

- `IMMEDIATE_TRANSFER`: 즉시이체
- `VIRTUAL_ACCOUNT`: 가상계좌

### ContractStatus

- `NORMAL`: 정상
- `EXPIRED`: 만기
- `CANCELLED`: 해지
- `LAPSED`: 실효

### ClaimType

- `DISEASE`: 질병
- `ACCIDENT`: 재해

### ChannelType

- `DESIGNER`: 설계사
- `AGENCY`: 대리점

### InquiryType

- `INSURANCE`: 보험료
- `CLAIM`: 보험금
- `CONTRACT_CHANGE`: 계약변경
- `CANCELLATION`: 해지
- `OTHER`: 기타

### InquiryStatus

- `PENDING`: 답변대기
- `ANSWERED`: 답변완료

### PlanStatus

- `TEMP_SAVE`: 임시저장
- `UNDER_REVIEW`: 승인요청 (검토중)
- `APPROVED`: 승인
- `REJECTED`: 반려

### AccidentType

- `PROPERTY`: 대물사고
- `PERSONAL`: 인명사고

### AuthMethod

- `KAKAO`: 카카오 인증
- `PASS`: PASS 인증
- `IPIN`: 아이핀

### NoticeMethod

- `SMS`: 문자
- `EMAIL`: 이메일
- `APP`: 앱 푸시

### PaymentType (보험금 지급)

- `IMMEDIATE`: 즉시 지급
- `SCHEDULED`: 예약 지급

### InvestigationResult

- `APPROVED`: 조사 완료 (지급 진행)
- `REJECTED`: 거절 (종결)

### RejectCategory (납입 기록 반려)

- `PAYMENT_ERROR`: 납입 오류
- `DUPLICATE_PAYMENT`: 중복 납입
- `CONTRACT_MISMATCH`: 계약 불일치
- `OTHER`: 기타

### ApplicationType (인수심사 대상 신청 유형)

- `POLICY`: 청약서 (`policy_applications`)
- `INSURANCE`: 청약신청 (`insurance_applications`)

---

## 계약 API

### 계약 목록

`GET /api/contracts?type=&page=1&size=20`

권한: `CONTRACT_STAFF`/`ADMIN`

Query:

- `type`: 선택. 보험 종류 필터.
- `page`: 선택. 기본값 `1`.
- `size`: 선택. 기본값 `20`, 최대 `100`.

Response:

```json
{
  "page": 1,
  "size": 20,
  "total": 3,
  "items": [
    {
      "contractNo": "CON00001",
      "customerName": "김고객",
      "insuranceType": "자동차보험",
      "contractDate": "2025-01-01",
      "expiryDate": "2026-01-01",
      "monthlyPremium": 150000,
      "status": "NORMAL"
    }
  ]
}
```

### 계약 상세

`GET /api/contracts/{contractNo}`

권한: `CONTRACT_STAFF`/`ADMIN`

Response:

```json
{
  "contractNo": "CON00001",
  "policyNo": "POL00001",
  "customerName": "김고객",
  "insuranceType": "자동차보험",
  "contractDate": "2025-01-01",
  "expiryDate": "2026-01-01",
  "monthlyPremium": 150000,
  "status": "NORMAL",
  "isExpiringSoon": false,
  "isOverdue": false,
  "overdueCount": 0
}
```

### 가입 보험 목록

`GET /api/subscribed-insurances?type=&page=1&size=20`

권한: `CUSTOMER`. 기존 `GET /api/contracts`는 계약관리 담당자의 계약 정보 조회 유스케이스로 유지하고, 고객의 가입 보험 조회 유스케이스는 1:1 Controller/Service 대응을 위해 이 URL로 분리한다. 응답 DTO는 계약 목록과 동일하게 `ContractSummaryResponse`를 사용한다.

Response: `PageResponse<ContractSummaryResponse>`

### 가입 보험 상세

`GET /api/subscribed-insurances/{contractNo}`

권한: `CUSTOMER`. 본인 계약만 조회 가능. 응답 DTO는 계약 상세와 동일하게 `ContractDetailResponse`를 사용한다.

### 계약 해지 신청

`POST /api/contracts/{contractNo}/cancellation`

권한: 직원/관리자, 고객 본인

Request:

```json
{
  "reason": "기타",
  "detailReason": "개인 사정으로 해지합니다.",
  "noticeAgreed": true
}
```

Response:

```json
{
  "cancellationNo": "CAN00001",
  "contractNo": "CON00001",
  "cancellationDate": "2026-06-03",
  "reason": "기타",
  "detailReason": "개인 사정으로 해지합니다.",
  "status": "완료"
}
```

### 해지 목록

`GET /api/cancellations?page=1&size=20`

권한: 직원/관리자 전체, 고객은 본인 계약 해지만

Response: `PageResponse<CancellationResponse>` (해지 상세와 동일한 구조)

### 해지 단건 조회

`GET /api/cancellations/{cancellationNo}`

권한: 직원/관리자 전체, 고객은 본인 계약 해지만

---

### 계약 통계 현황 조회

`GET /api/contract-statistics`

권한: `CONTRACT_STAFF`, `ADMIN`

Response:

```json
{
  "statsNo": "STA00001",
  "totalCount": 150,
  "activeCount": 120,
  "expiredCount": 20,
  "cancelledCount": 10,
  "createdAt": "2026-06-04T09:00:00"
}
```

- 가장 최근 snapshot을 반환한다. snapshot이 없으면 실시간 집계를 반환한다.

### 계약 통계 snapshot 생성

`POST /api/contract-statistics`

권한: `CONTRACT_STAFF`, `ADMIN`

- 현재 시점 계약 현황을 집계해 저장한다.

Response: 계약 통계 단건 (신규 생성)

### 계약 통계 이력 목록

`GET /api/contract-statistics/history?page=1&size=20`

권한: `CONTRACT_STAFF`, `ADMIN`

Response:

```json
{
  "page": 1,
  "size": 20,
  "total": 5,
  "items": [
    {
      "statsNo": "STA00001",
      "totalCount": 150,
      "activeCount": 120,
      "expiredCount": 20,
      "cancelledCount": 10,
      "createdAt": "2026-06-04T09:00:00"
    }
  ]
}
```

---

### 만기 임박 계약 목록

`GET /api/expiring-contracts?page=1&size=20`

권한: `CONTRACT_STAFF`, `ADMIN`

- 만기까지 D-30 이내인 계약을 반환한다.

Response:

```json
{
  "page": 1,
  "size": 20,
  "total": 8,
  "items": [
    {
      "contractNo": "CON00001",
      "contractorName": "김고객",
      "insuranceType": "자동차보험",
      "expiryDate": "2026-07-01",
      "remainingDays": 27,
      "monthlyPremium": 150000,
      "status": "NORMAL"
    }
  ]
}
```

### 만기 안내 기록 등록

`POST /api/expiring-contracts/{contractNo}/notice`

권한: `CONTRACT_STAFF`, `ADMIN`

Request:

```json
{
  "phone": "010-1234-5678",
  "email": "customer@example.com",
  "isRenewable": true,
  "expectedPremium": 160000,
  "noticeMemo": "갱신 의향 확인 요망"
}
```

Response:

```json
{
  "noticeNo": "NOT00001",
  "contractNo": "CON00001",
  "contractorName": "김고객",
  "expiryDate": "2026-07-01",
  "phone": "010-1234-5678",
  "email": "customer@example.com",
  "isRenewable": true,
  "expectedPremium": 160000,
  "noticeDate": "2026-06-04T10:00:00",
  "noticeMemo": "갱신 의향 확인 요망",
  "customerResponse": null,
  "renewalPremium": null,
  "premiumDiff": null
}
```

### 만기 안내 기록 목록

`GET /api/expiring-notices?contractNo=&page=1&size=20`

권한: `CONTRACT_STAFF`, `ADMIN`

Query:

- `contractNo`: 선택. 계약번호 필터.

Response: `PageResponse<NoticeResponse>`

### 고객 응답 기록

`POST /api/expiring-notices/{noticeNo}/response`

권한: `CONTRACT_STAFF`, `ADMIN`

Request:

```json
{
  "customerResponse": "갱신 의향 있음",
  "renewalPremium": 162000
}
```

Response: 안내 기록 단건 (`customerResponse`, `renewalPremium`, `premiumDiff` 포함)

---

## 납입 API

### 고객 계약 목록 (납입용)

`GET /api/customers/{customerId}/contracts`

권한: 고객 본인, 직원/관리자

Response:

```json
{
  "items": [
    {
      "contractNo": "CON00001",
      "insuranceType": "자동차보험",
      "monthlyPremium": 150000,
      "unpaidCount": 2
    }
  ]
}
```

### 납입 미리보기

`POST /api/payments/preview`

권한: 로그인 사용자

Request:

```json
{
  "items": [
    { "contractNo": "CON00001", "count": 2 }
  ]
}
```

Response:

```json
{
  "items": [
    {
      "contractNo": "CON00001",
      "insuranceType": "자동차보험",
      "count": 2,
      "amount": 300000
    }
  ],
  "totalAmount": 300000
}
```

### 납입 제출

`POST /api/payments`

권한: 고객 본인 (고객 소유권 검증)

Request:

```json
{
  "customerId": "CUS00001",
  "items": [
    { "contractNo": "CON00001", "count": 2 }
  ],
  "paymentMethod": "IMMEDIATE_TRANSFER",
  "bankName": "국민은행",
  "accountNo": "123-456-789",
  "accountHolder": "김고객"
}
```

Response:

```json
{
  "paymentNo": "PMT00001",
  "totalAmount": 300000,
  "paymentMethod": "IMMEDIATE_TRANSFER",
  "paidAt": "2026-06-03T10:00:00",
  "items": [
    { "contractNo": "CON00001", "count": 2, "amount": 300000 }
  ]
}
```

### 납입 기록 목록

`GET /api/payment-records?contractNo=&status=&page=1&size=20`

권한: `FINANCE_STAFF`, `ADMIN`

Query:

- `contractNo`: 선택. 계약번호 필터.
- `status`: 선택. `WAITING` / `COMPLETED` / `REJECTED`.

Response:

```json
{
  "page": 1,
  "size": 20,
  "total": 5,
  "items": [
    {
      "recordNo": "REC00001",
      "contractNo": "CON00001",
      "customerName": "김고객",
      "dueDate": "2026-06-01",
      "amount": 150000,
      "status": "WAITING"
    }
  ]
}
```

### 납입 기록 확정

`POST /api/payment-records/{recordNo}/confirm`

권한: `FINANCE_STAFF`, `ADMIN`

- 진입 조건: `status == WAITING`

Response: 납입 기록 단건 (status: `COMPLETED`)

### 납입 기록 반려

`POST /api/payment-records/{recordNo}/reject`

권한: `FINANCE_STAFF`, `ADMIN`

- 진입 조건: `status == WAITING`

Request:

```json
{
  "rejectCategory": "DUPLICATE_PAYMENT",
  "rejectReason": "동일 건 중복 납입 확인"
}
```

Response: 납입 기록 단건 (status: `REJECTED`)

### 환급금 산출

`POST /api/cancellations/{cancellationNo}/refund-calculation`

권한: `FINANCE_STAFF`, `ADMIN`

- 해지 건당 1건만 허용

Response:

```json
{
  "refundNo": "RFC00001",
  "cancellationNo": "CAN00001",
  "totalPaidPremium": 3600000,
  "paymentPeriod": "24개월",
  "reserveAmount": 2520000,
  "appliedRate": 0.025,
  "baseRefund": 2583000,
  "unpaidPremium": 0,
  "finalRefund": 2583000,
  "status": "CALCULATED",
  "calculatedAt": "2026-06-03T10:00:00"
}
```

### 환급금 산출 단건 조회

`GET /api/refund-calculations/{refundNo}`

권한: `FINANCE_STAFF`, `ADMIN`, 고객 본인

### 환급금 산출 목록

`GET /api/refund-calculations?page=1&size=20`

권한: `FINANCE_STAFF`, `ADMIN`, 고객 본인

### 환급금 확정 (지급 이관)

`POST /api/refund-calculations/{refundNo}/confirm`

권한: `FINANCE_STAFF`, `ADMIN`

- 진입 조건: `status == CALCULATED`
- 실행 후 `RefundCalculation.status`가 `PAID`로 전환되고 `RefundPayment`(WAITING)가 생성됨

Response: `RefundPaymentResponse` (아래 지급 응답과 동일)

### 환급금 지급 실행

`POST /api/refund-payments/{paymentNo}/execute`

권한: `FINANCE_STAFF`, `ADMIN`

Request:

```json
{
  "otpInput": "123456"
}
```

Response:

```json
{
  "paymentNo": "RFP00001",
  "refundNo": "RFC00001",
  "status": "COMPLETED",
  "otpFailCount": 0,
  "executedAt": "2026-06-03T10:30:00"
}
```

- OTP 1~4회 실패: `status: "FAILED"`, 예외 없음 (클라이언트가 status로 판별)
- OTP 5회 실패: `status: "LOCKED"`, 이후 `badRequest` 반환

### 환급금 지급 단건 조회

`GET /api/refund-payments/{paymentNo}`

권한: `FINANCE_STAFF`, `ADMIN`, 고객 본인

### 환급금 지급 목록

`GET /api/refund-payments?page=1&size=20`

권한: `FINANCE_STAFF`, `ADMIN`, 고객 본인

---

## 청구 API

> **주의**: `POST /api/payments/{paymentNo}/execute` (보험금 지급 실행) 경로가 납입 API의 `/api/payments` 네임스페이스와 겹칩니다. 프론트 연동 전 경로 분리 검토 필요.

### 사고 접수

`POST /api/accidents`

권한: 고객 본인 (고객 소유권 검증)

Request:

```json
{
  "customerId": "CUS00001",
  "vehicleNo": "12가1234",
  "ownerName": "김고객",
  "phoneNo": "010-1234-5678",
  "accidentType": "PROPERTY",
  "damageType": "추돌",
  "location": "서울시 강남구 테헤란로",
  "needsDispatch": true,
  "agreedTerms": true,
  "casualtyCount": 0,
  "injurySeverity": null,
  "emergencyReported": false
}
```

Response:

```json
{
  "reportNo": "ACC00001",
  "customerId": "CUS00001",
  "vehicleNo": "12가1234",
  "accidentType": "PROPERTY",
  "location": "서울시 강남구 테헤란로",
  "needsDispatch": true,
  "status": "RECEIVED",
  "reportedAt": "2026-06-03T10:00:00",
  "dispatchNo": "DIS00001"
}
```

- `needsDispatch: true`이면 같은 트랜잭션에서 Dispatch(REQUESTED)가 자동 생성되고 `dispatchNo`가 함께 반환됨

### 사고 접수 목록

`GET /api/accidents?page=1&size=20`

권한: 고객 본인, 직원/관리자

### 사고 접수 단건 조회

`GET /api/accidents/{accidentNo}`

권한: 고객 본인, 직원/관리자

### 출동 목록

`GET /api/dispatches?page=1&size=20`

권한: `DISPATCH_STAFF`, `CLAIM_STAFF`, `ADMIN`

### 출동 기록 등록

`POST /api/dispatches/{dispatchNo}/record`

권한: `DISPATCH_STAFF`, `CLAIM_STAFF`, `ADMIN`

- Content-Type: `multipart/form-data`
- 출동 건당 기록 1건만 허용

Request (form-data):

| 필드 | 타입 | 필수 |
|---|---|---|
| `agentName` | String | 선택 |
| `policeRequired` | boolean | 선택 (기본 false) |
| `towingRequired` | boolean | 선택 (기본 false) |
| `notes` | String | 선택 |
| `photos` | MultipartFile[] | 선택 |

Response:

```json
{
  "recordNo": "DCR00001",
  "dispatchNo": "DIS00001",
  "agentName": "홍길동",
  "policeRequired": false,
  "towingRequired": true,
  "notes": "차량 견인 완료",
  "photoCount": 2,
  "status": "TRANSMITTED"
}
```

### 출동 기록 단건 조회

`GET /api/dispatches/{dispatchNo}/record`

권한: `DISPATCH_STAFF`, `CLAIM_STAFF`, `ADMIN`

### 보험금 청구 접수

`POST /api/claims`

권한: 고객 본인 (고객 소유권 검증)

Request:

```json
{
  "customerId": "CUS00001",
  "contractNo": "CON00001",
  "claimType": "ACCIDENT",
  "claimReasons": ["차량 파손"],
  "diagnosis": null,
  "bankName": "국민은행",
  "accountNo": "123-456-789",
  "accountHolder": "김고객",
  "personalInfoAgreed": true,
  "authMethod": "KAKAO"
}
```

Response:

```json
{
  "claimNo": "CLM00001",
  "contractNo": "CON00001",
  "customerName": "김고객",
  "claimType": "ACCIDENT",
  "status": "RECEIVED",
  "requestedAt": "2026-06-03T10:00:00"
}
```

### 보험금 청구 목록

`GET /api/claims?page=1&size=20`

권한: 고객 본인, 직원/관리자

### 보험금 청구 단건 조회

`GET /api/claims/{claimNo}`

권한: 고객 본인, 직원/관리자

### 손해 조사 등록

`POST /api/claims/{claimNo}/investigation`

권한: `CLAIM_STAFF`, `ADMIN`

- 진입 조건: `claim.status == RECEIVED`
- 청구 건당 1건만 허용

Request:

```json
{
  "handlerName": "조사담당자",
  "recognizedDamage": 5000000,
  "ourFaultRatio": 80.0,
  "counterFaultRatio": 20.0,
  "opinion": "과실 80% 인정",
  "result": "APPROVED",
  "rejectReason": null
}
```

- `result`: `APPROVED` → 조사완료(INVESTIGATED), `REJECTED` → 종결(CLOSED)

Response:

```json
{
  "investigationNo": "INV00001",
  "claimNo": "CLM00001",
  "handlerName": "조사담당자",
  "recognizedDamage": 5000000,
  "ourFaultRatio": 80.0,
  "result": "APPROVED",
  "status": "INVESTIGATED"
}
```

### 손해 조사 단건 조회

`GET /api/claims/{claimNo}/investigation`

권한: `CLAIM_STAFF`, `ADMIN`

### 보험금 산출

`POST /api/investigations/{investigationNo}/calculation`

권한: `CLAIM_STAFF`, `ADMIN`

- 진입 조건: `investigation.result == APPROVED`
- 조사 건당 1건만 허용

Response:

```json
{
  "calculationNo": "CAL00001",
  "investigationNo": "INV00001",
  "recognizedDamage": 5000000,
  "faultRatio": 80.0,
  "finalAmount": 3900000,
  "exceededDeductible": false,
  "status": "CALCULATED",
  "calculatedAt": "2026-06-03T10:00:00"
}
```

- `exceededDeductible: true`이면 `status: "CLOSED"` (지급 불가, 흐름 종료)

### 보험금 산출 단건 조회

`GET /api/investigations/{investigationNo}/calculation`

권한: `CLAIM_STAFF`, `ADMIN`

### 보험금 산출 승인

`POST /api/calculations/{calculationNo}/approve`

권한: `CLAIM_STAFF`, `ADMIN`

- 진입 조건: `status == CALCULATED`

Response: 보험금 산출 단건 (status: `APPROVED`)

### 보험금 지급 생성

`POST /api/calculations/{calculationNo}/payment`

권한: `FINANCE_STAFF`, `CLAIM_STAFF`, `ADMIN`

- 진입 조건: `calculation.status == APPROVED`

Request:

```json
{
  "paymentType": "IMMEDIATE",
  "scheduledAt": null
}
```

- `paymentType: "SCHEDULED"`이면 `scheduledAt` 필수

Response:

```json
{
  "paymentNo": "CPM00001",
  "calculationNo": "CAL00001",
  "paymentType": "IMMEDIATE",
  "status": "WAITING",
  "scheduledAt": null
}
```

### 보험금 지급 단건 조회

`GET /api/calculations/{calculationNo}/payment`

권한: `FINANCE_STAFF`, `CLAIM_STAFF`, `ADMIN`

### 보험금 지급 실행

`POST /api/payments/{paymentNo}/execute`

권한: `FINANCE_STAFF`, `CLAIM_STAFF`, `ADMIN`

- 진입 조건: `status != COMPLETED`

Request:

```json
{
  "otp": "123456"
}
```

Response:

```json
{
  "paymentNo": "CPM00001",
  "status": "COMPLETED"
}
```

- OTP 실패 시 `status: "FAILED"` (예외 없음, 클라이언트가 status로 판별)

---

## 교육 API

### 교육 계획 목록

`GET /api/education-plans?status=&page=1&size=20`

권한: `EDUCATION_STAFF`, `ADMIN`

Query:

- `status`: 선택. `TEMP_SAVE` / `UNDER_REVIEW` / `APPROVED` / `REJECTED`.

Response:

```json
{
  "page": 1,
  "size": 20,
  "total": 5,
  "items": [
    {
      "id": 1,
      "planNo": "PLN00001",
      "trainerName": "강사명",
      "educationName": "보험 기초 교육",
      "channelType": "DESIGNER",
      "startDate": "2026-07-01",
      "endDate": "2026-07-03",
      "targetCount": 30,
      "status": "UNDER_REVIEW"
    }
  ]
}
```

### 교육 계획 단건 조회

`GET /api/education-plans/{planNo}`

권한: `EDUCATION_STAFF`, `ADMIN`

### 교육 계획 저장/제출

`POST /api/education-plans`

권한: `EDUCATION_STAFF`, `ADMIN`

Request:

```json
{
  "trainerName": "강사명",
  "educationName": "보험 기초 교육",
  "channelType": "DESIGNER",
  "startDate": "2026-07-01",
  "endDate": "2026-07-03",
  "targetCount": 30,
  "budget": 500000,
  "educationGoal": "기초 보험 지식 습득",
  "educationContent": "보험 상품 개요, 청약 절차 등",
  "textbookList": "교재1",
  "action": "REQUEST_APPROVAL"
}
```

- `action: "REQUEST_APPROVAL"` → `UNDER_REVIEW` (필수 항목 검증 통과 시)
- 그 외 → `TEMP_SAVE`

### 교육 계획 승인

`POST /api/education-plans/{planNo}/approve`

권한: `EDUCATION_STAFF`, `ADMIN`

- 진입 조건: `status == UNDER_REVIEW`

Response: 교육 계획 단건 (status: `APPROVED`)

### 교육 계획 반려

`POST /api/education-plans/{planNo}/reject`

권한: `EDUCATION_STAFF`, `ADMIN`

- 진입 조건: `status == UNDER_REVIEW`

Request:

```json
{
  "reason": "예산 초과로 반려합니다."
}
```

Response: 교육 계획 단건 (status: `REJECTED`)

---

### 교육 제반 목록

`GET /api/education-preparations?planNo=&page=1&size=20`

권한: `EDUCATION_STAFF`, `ADMIN`

Query:

- `planNo`: 선택. 교육 계획번호 필터.

Response:

```json
{
  "page": 1,
  "size": 20,
  "total": 2,
  "items": [
    {
      "id": 1,
      "prepNo": "PRP00001",
      "planNo": "PLN00001",
      "instructorName": "강사명",
      "venue": "2층 교육장",
      "materialReady": false,
      "textbookStatus": "인쇄 완료",
      "attendees": ["홍설계사", "김설계사"],
      "additionalNotice": "노트북 지참",
      "status": "준비중",
      "registeredAt": "2026-06-10T09:00:00"
    }
  ]
}
```

### 교육 제반 단건 조회

`GET /api/education-preparations/{prepNo}`

권한: `EDUCATION_STAFF`, `ADMIN`

### 교육 제반 등록

`POST /api/education-preparations`

권한: `EDUCATION_STAFF`, `ADMIN`

- 진입 조건: 연결된 교육 계획이 `APPROVED` 상태여야 한다.

Request:

```json
{
  "planNo": "PLN00001",
  "instructorName": "강사명",
  "venue": "2층 교육장",
  "textbookStatus": "인쇄 완료",
  "attendees": ["홍설계사", "김설계사"],
  "additionalNotice": "노트북 지참"
}
```

Response: 교육 제반 단건

---

### 교육 실행 목록

`GET /api/education-executions?prepNo=&page=1&size=20`

권한: `EDUCATION_STAFF`, `ADMIN`

Query:

- `prepNo`: 선택. 교육 제반번호 필터.

Response:

```json
{
  "page": 1,
  "size": 20,
  "total": 1,
  "items": [
    {
      "id": 1,
      "executionNo": "EXE00001",
      "prepNo": "PRP00001",
      "trainerName": "강사명",
      "executedAt": "2026-07-01T10:00:00",
      "attendeeCount": 18,
      "totalCount": 20,
      "memo": "일부 결석",
      "status": "완료",
      "attendances": [
        { "attendeeName": "홍설계사", "attended": true },
        { "attendeeName": "김설계사", "attended": false }
      ]
    }
  ]
}
```

### 교육 실행 단건 조회

`GET /api/education-executions/{executionNo}`

권한: `EDUCATION_STAFF`, `ADMIN`

### 교육 실행 등록

`POST /api/education-executions`

권한: `EDUCATION_STAFF`, `ADMIN`

Request:

```json
{
  "prepNo": "PRP00001",
  "trainerName": "강사명",
  "attendances": [
    { "attendeeName": "홍설계사", "attended": true },
    { "attendeeName": "김설계사", "attended": false }
  ],
  "memo": "일부 결석"
}
```

Response: 교육 실행 단건

---

## 영업 API

### 채널 심사 목록

`GET /api/channel-screenings?page=1&size=20`

권한: `SALES_STAFF`, `ADMIN`

Response:

```json
{
  "page": 1,
  "size": 20,
  "total": 3,
  "items": [
    {
      "screeningNo": "SCR00001",
      "applicantName": "홍길동",
      "channelType": "DESIGNER",
      "applicationDate": "2026-06-01",
      "status": "PENDING"
    }
  ]
}
```

### 채널 심사 접수

`POST /api/channel-screenings`

권한: `SALES_STAFF`, `ADMIN`

Request:

```json
{
  "applicantName": "홍길동",
  "channelType": "DESIGNER",
  "applicationDate": "2026-06-01",
  "career": "3년",
  "certifications": ["생명보험", "손해보험"]
}
```

### 채널 심사 승인

`POST /api/channel-screenings/{screeningNo}/approve`

권한: `SALES_STAFF`, `ADMIN`

- 진입 조건: `status == PENDING`

Response: 채널 심사 단건 (status: `APPROVED`)

### 채널 심사 반려

`POST /api/channel-screenings/{screeningNo}/reject`

권한: `SALES_STAFF`, `ADMIN`

- 진입 조건: `status == PENDING`

Request:

```json
{
  "rejectionReason": "자격 요건 미충족"
}
```

Response: 채널 심사 단건 (status: `REJECTED`)

---

### 채널 모집 목록

`GET /api/channel-recruitments?page=1&size=20`

권한: `SALES_STAFF`, `ADMIN`

Response:

```json
{
  "page": 1,
  "size": 20,
  "total": 2,
  "items": [
    {
      "id": 1,
      "recruitmentNo": "RCT00001",
      "managerName": "홍영업",
      "channelType": "DESIGNER",
      "recruitCount": 5,
      "startDate": "2026-07-01",
      "endDate": "2026-07-31",
      "condition": "생명보험 자격 보유",
      "status": "진행중",
      "registeredAt": "2026-06-04T09:00:00"
    }
  ]
}
```

### 채널 모집 등록

`POST /api/channel-recruitments`

권한: `SALES_STAFF`, `ADMIN`

Request:

```json
{
  "managerName": "홍영업",
  "channelType": "DESIGNER",
  "recruitCount": 5,
  "startDate": "2026-07-01",
  "endDate": "2026-07-31",
  "condition": "생명보험 자격 보유"
}
```

Response: 채널 모집 단건

---

### 활동 계획 목록

`GET /api/activity-plans?page=1&size=20`

권한: `SALES_STAFF`, `ADMIN`

Response:

```json
{
  "page": 1,
  "size": 20,
  "total": 3,
  "items": [
    {
      "id": 1,
      "planNo": "ACT00001",
      "planName": "2026년 7월 영업 계획",
      "startDate": "2026-07-01",
      "endDate": "2026-07-31",
      "author": "홍영업",
      "memo": null,
      "targetContractCount": 10,
      "targetContractAmount": 15000000,
      "targetNewCustomer": 5,
      "proposedCustomerId": null,
      "proposedInsuranceType": "CAR",
      "proposalReason": "자동차보험 갱신 시즌",
      "status": "TEMP_SAVE",
      "schedules": []
    }
  ]
}
```

### 활동 계획 단건 조회

`GET /api/activity-plans/{planNo}`

권한: `SALES_STAFF`, `ADMIN`

### 활동 계획 등록

`POST /api/activity-plans`

권한: `SALES_STAFF`, `ADMIN`

Request:

```json
{
  "planName": "2026년 7월 영업 계획",
  "startDate": "2026-07-01",
  "endDate": "2026-07-31",
  "author": "홍영업",
  "memo": null,
  "targetContractCount": 10,
  "targetContractAmount": 15000000,
  "targetNewCustomer": 5,
  "proposedCustomerId": null,
  "proposedInsuranceType": "CAR",
  "proposalReason": "자동차보험 갱신 시즌",
  "status": "TEMP_SAVE",
  "schedules": [
    {
      "customerId": "CUS00001",
      "activityType": "VISIT",
      "activityDateTime": "2026-07-05T10:00:00",
      "location": "서울 강남지점",
      "memo": "계약 갱신 상담"
    }
  ]
}
```

Response: 활동 계획 단건

- `status`: `TEMP_SAVE`로 전송하거나 생략 시 임시저장. `UNDER_REVIEW`로 전송 시 승인 요청.
- `schedules`: 일정 항목 목록. 생략 가능.
- `activityType`: `VISIT` (방문) / `CONSULTATION` (상담) / `CALL` (전화)

---

### 영업 활동 관리 목록

`GET /api/sales-activity-managements?startDate=&endDate=&channelType=&page=1&size=20`

권한: `SALES_STAFF`, `ADMIN`

Query:

- `startDate`: 선택. 조회 시작일 (`YYYY-MM-DD`).
- `endDate`: 선택. 조회 종료일 (`YYYY-MM-DD`).
- `channelType`: 선택. `DESIGNER` / `AGENCY`.

Response:

```json
{
  "page": 1,
  "size": 20,
  "total": 5,
  "items": [
    {
      "id": 1,
      "activityNo": "SAM00001",
      "managerName": "홍영업",
      "channelName": "강남지점",
      "channelType": "DESIGNER",
      "startDate": "2026-06-01",
      "endDate": "2026-06-30",
      "visitCount": 20,
      "contractCount": 8,
      "conversionRate": 40.0,
      "achievementRate": 80.0,
      "improvementContent": "주 2회 방문 강화",
      "revisedTarget": 12,
      "registeredAt": "2026-07-01T09:00:00"
    }
  ]
}
```

### 영업 활동 관리 등록

`POST /api/sales-activity-managements`

권한: `SALES_STAFF`, `ADMIN`

Request:

```json
{
  "managerName": "홍영업",
  "channelName": "강남지점",
  "channelType": "DESIGNER",
  "startDate": "2026-06-01",
  "endDate": "2026-06-30",
  "visitCount": 20,
  "contractCount": 8,
  "achievementRate": 80.0,
  "improvementContent": "주 2회 방문 강화",
  "revisedTarget": 12
}
```

Response: 영업 활동 관리 단건

---

### 영업 조직 평가 목록

`GET /api/sales-org-evaluations?startDate=&endDate=&channelType=&page=1&size=20`

권한: `SALES_STAFF`, `ADMIN`

Query:

- `startDate`: 선택. 평가 기간 시작일 (`YYYY-MM-DD`).
- `endDate`: 선택. 평가 기간 종료일 (`YYYY-MM-DD`).
- `channelType`: 선택. `DESIGNER` / `AGENCY`.

Response:

```json
{
  "page": 1,
  "size": 20,
  "total": 3,
  "items": [
    {
      "id": 1,
      "evaluationNo": "EVL00001",
      "channelName": "강남지점",
      "channelType": "DESIGNER",
      "salesResult": 120000000,
      "contractCount": 80,
      "achievementRate": 96.0,
      "evaluationGrade": "A",
      "evaluationComment": "목표 초과 달성",
      "evaluatedAt": "2026-07-01T09:00:00"
    }
  ]
}
```

### 영업 조직 평가 등록

`POST /api/sales-org-evaluations`

권한: `SALES_STAFF`, `ADMIN`

Request:

```json
{
  "channelName": "강남지점",
  "channelType": "DESIGNER",
  "salesResult": 120000000,
  "contractCount": 80,
  "achievementRate": 96.0,
  "evaluationGrade": "A",
  "evaluationComment": "목표 초과 달성"
}
```

Response: 영업 조직 평가 단건

- `evaluationGrade`: `S` / `A` / `B` / `C` / `D`

---

### 성과급 요청 목록

`GET /api/bonus-requests?page=1&size=20`

권한: `SALES_STAFF`, `ADMIN`

Response:

```json
{
  "page": 1,
  "size": 20,
  "total": 3,
  "items": [
    {
      "id": 1,
      "requestNo": "BNS00001",
      "evaluationNo": "EVL00001",
      "channelName": "강남지점",
      "channelType": "DESIGNER",
      "evaluationGrade": "A",
      "bonusRatio": 1.2,
      "bonusAmount": 360000.0,
      "requestReason": "2분기 목표 초과 달성",
      "requestedAt": "2026-07-01T09:00:00"
    }
  ]
}
```

### 성과급 요청 단건 조회

`GET /api/bonus-requests/{requestNo}`

권한: `SALES_STAFF`, `ADMIN`

---

### 성과급 요청

`POST /api/bonus-requests`

권한: `ADMIN`

Request:

```json
{
  "evaluationNo": "EVL00001",
  "channelName": "강남지점",
  "channelType": "DESIGNER",
  "evaluationGrade": "A",
  "baseSalary": 3000000,
  "requestReason": "2분기 목표 초과 달성에 따른 성과급 요청"
}
```

Response:

```json
{
  "id": 1,
  "requestNo": "BNS00001",
  "evaluationNo": "EVL00001",
  "channelName": "강남지점",
  "channelType": "DESIGNER",
  "evaluationGrade": "A",
  "bonusRatio": 0.1,
  "bonusAmount": 300000.0,
  "requestReason": "2분기 목표 초과 달성에 따른 성과급 요청",
  "requestedAt": "2026-07-01T09:00:00"
}
```

---

### 고객 등록 목록

`GET /api/customer-registrations?page=1&size=20`

권한: `SALES_STAFF`, `ADMIN`

Response:

```json
{
  "page": 1,
  "size": 20,
  "total": 5,
  "items": [
    {
      "id": 1,
      "customerId": "CUS00001",
      "name": "김고객",
      "maskedSsn": "900101-*******",
      "phone": "010-1234-5678",
      "address": "서울시 강남구",
      "insuranceType": "CAR",
      "contractDate": "2026-06-01",
      "expiryDate": "2027-06-01",
      "monthlyPremium": 150000
    }
  ]
}
```

### 고객 등록

`POST /api/customer-registrations`

권한: `SALES_STAFF`, `ADMIN`

Request:

```json
{
  "name": "김고객",
  "ssn": "900101-1234567",
  "phone": "010-1234-5678",
  "address": "서울시 강남구",
  "insuranceType": "CAR",
  "contractDate": "2026-06-01",
  "expiryDate": "2027-06-01",
  "monthlyPremium": 150000
}
```

Response: 고객 등록 단건 (`maskedSsn` 필드로 반환, `ssn` 원본 미노출)

---

## 문의 API

### 문의 목록

`GET /api/inquiries?customerName=&status=&page=1&size=20`

권한: 직원/관리자 전체, 고객은 본인 문의만

Query:

- `customerName`: 선택. 고객명 필터.
- `status`: 선택. `PENDING` / `ANSWERED`.

Response:

```json
{
  "page": 1,
  "size": 20,
  "total": 2,
  "items": [
    {
      "inquiryNo": "INQ00001",
      "customerName": "김고객",
      "inquiryType": "CLAIM",
      "title": "보험금 청구 문의",
      "status": "PENDING",
      "createdAt": "2026-06-01T10:00:00"
    }
  ]
}
```

### 문의 단건 조회

`GET /api/inquiries/{inquiryNo}`

권한: 직원/관리자 전체, 고객은 본인 문의만

### 문의 접수

`POST /api/inquiries`

권한: 로그인 사용자

Request:

```json
{
  "customerName": "김고객",
  "inquiryType": "CLAIM",
  "title": "보험금 청구 문의",
  "content": "보험금 청구 절차를 알고 싶습니다.",
  "attachmentFileName": null,
  "attachmentFileSize": null
}
```

Response:

```json
{
  "inquiryNo": "INQ00001",
  "customerName": "김고객",
  "inquiryType": "CLAIM",
  "title": "보험금 청구 문의",
  "status": "PENDING",
  "createdAt": "2026-06-03T10:00:00"
}
```

### 문의 답변

`POST /api/inquiries/{inquiryNo}/answer`

권한: 직원 또는 관리자

- 진입 조건: `status == PENDING`

Request:

```json
{
  "answerContent": "청구 절차는 다음과 같습니다..."
}
```

Response: 문의 단건 (status: `ANSWERED`, answerContent 포함)

---

## 상담 API

### 상담 목록

`GET /api/consultations?page=1&size=20`

권한: `SALES_STAFF`, `UNDERWRITING_STAFF`, `ADMIN`

Response:

```json
{
  "page": 1,
  "size": 20,
  "total": 4,
  "items": [
    {
      "consultNo": "CST00001",
      "type": "신규",
      "contact": "010-1234-5678",
      "scheduledAt": "2026-06-10T14:00:00",
      "status": "접수"
    }
  ]
}
```

### 상담 단건 조회

`GET /api/consultations/{consultNo}`

권한: `SALES_STAFF`, `UNDERWRITING_STAFF`, `ADMIN`

### 상담 요청 접수

`POST /api/consultations`

권한: 로그인 사용자

Request:

```json
{
  "type": "신규",
  "location": "서울 강남지점",
  "contact": "010-1234-5678",
  "content": "자동차보험 상담 요청",
  "scheduledAt": "2026-06-10T14:00:00"
}
```

### 상담 수락

`POST /api/consultations/{consultNo}/accept`

권한: `SALES_STAFF`, `UNDERWRITING_STAFF`, `ADMIN`

- 진입 조건: `status == "접수"` (현재 String 비교)

Response: 상담 단건 (status: `"수락"`)

---

### 면담 일정 목록

`GET /api/interview-schedules?page=1&size=20`

권한: `SALES_STAFF`, `UNDERWRITING_STAFF`, `ADMIN`

Response:

```json
{
  "page": 1,
  "size": 20,
  "total": 3,
  "items": [
    {
      "scheduleNo": "ISC00001",
      "customerName": "김고객",
      "designerName": "홍설계사",
      "interviewType": "초회면담",
      "scheduledAt": "2026-06-15T10:00:00",
      "location": "서울 강남지점",
      "preparation": "신분증 지참",
      "status": "예정",
      "registeredAt": "2026-06-04T09:00:00",
      "modifiedAt": null,
      "cancelledAt": null
    }
  ]
}
```

### 면담 일정 단건 조회

`GET /api/interview-schedules/{scheduleNo}`

권한: `SALES_STAFF`, `UNDERWRITING_STAFF`, `ADMIN`

### 면담 일정 등록

`POST /api/interview-schedules`

권한: `SALES_STAFF`, `UNDERWRITING_STAFF`, `ADMIN`

Request:

```json
{
  "customerName": "김고객",
  "designerName": "홍설계사",
  "interviewType": "초회면담",
  "scheduledAt": "2026-06-15T10:00:00",
  "location": "서울 강남지점",
  "preparation": "신분증 지참"
}
```

Response: 면담 일정 단건

### 면담 일정 수정

`PUT /api/interview-schedules/{scheduleNo}`

권한: `SALES_STAFF`, `UNDERWRITING_STAFF`, `ADMIN`

Request:

```json
{
  "interviewType": "초회면담",
  "scheduledAt": "2026-06-16T14:00:00",
  "location": "서울 강남지점",
  "preparation": "신분증, 인감도장 지참"
}
```

Response: 면담 일정 단건 (수정 후)

### 면담 일정 취소

`POST /api/interview-schedules/{scheduleNo}/cancel`

권한: `SALES_STAFF`, `UNDERWRITING_STAFF`, `ADMIN`

Response: 면담 일정 단건 (status: `"취소"`, `cancelledAt` 포함)

---

### 면담 기록 목록

`GET /api/interview-records?page=1&size=20`

권한: `SALES_STAFF`, `UNDERWRITING_STAFF`, `ADMIN`

Response:

```json
{
  "page": 1,
  "size": 20,
  "total": 2,
  "items": [
    {
      "recordNo": "IRD00001",
      "customerName": "김고객",
      "content": "보장 내용 안내 완료",
      "customerReaction": "긍정적",
      "followUpAction": "제안서 발송",
      "interviewedAt": "2026-06-15T10:00:00",
      "recordedAt": "2026-06-15T11:00:00",
      "modifiedAt": null
    }
  ]
}
```

### 면담 기록 단건 조회

`GET /api/interview-records/{recordNo}`

권한: `SALES_STAFF`, `UNDERWRITING_STAFF`, `ADMIN`

### 면담 기록 등록

`POST /api/interview-records`

권한: `SALES_STAFF`, `UNDERWRITING_STAFF`, `ADMIN`

Request:

```json
{
  "customerName": "김고객",
  "interviewedAt": "2026-06-15T10:00:00",
  "content": "보장 내용 안내 완료",
  "customerReaction": "긍정적",
  "followUpAction": "제안서 발송"
}
```

Response: 면담 기록 단건

### 면담 기록 수정

`PUT /api/interview-records/{recordNo}`

권한: `SALES_STAFF`, `UNDERWRITING_STAFF`, `ADMIN`

Request:

```json
{
  "content": "보장 내용 및 보험료 안내 완료",
  "customerReaction": "긍정적, 추가 검토 요청",
  "followUpAction": "제안서 재발송"
}
```

Response: 면담 기록 단건 (수정 후)

---

### 제안서 목록

`GET /api/proposals?page=1&size=20`

권한: `SALES_STAFF`, `UNDERWRITING_STAFF`, `ADMIN`

Response:

```json
{
  "page": 1,
  "size": 20,
  "total": 3,
  "items": [
    {
      "proposalNo": "PRP00001",
      "customerName": "김고객",
      "productName": "자동차보험 기본형",
      "monthlyPremium": 150000,
      "sentAt": "2026-06-10T09:00:00"
    }
  ]
}
```

### 제안서 발송

`POST /api/proposals`

권한: `SALES_STAFF`, `UNDERWRITING_STAFF`, `ADMIN`

Request:

```json
{
  "customerName": "김고객",
  "productName": "자동차보험 기본형"
}
```

Response: 제안서 단건

- `productName`은 보험 상품 목록(`GET /api/insurance-products`)에서 조회한 값을 사용한다.

---

### 인수심사 대기 목록

`GET /api/underwriting/pending?page=1&size=20`

권한: `UNDERWRITING_STAFF`, `ADMIN`

Response:

```json
{
  "page": 1,
  "size": 20,
  "total": 2,
  "items": [
    {
      "applicationType": "POLICY",
      "applicationNo": "APP00001",
      "customerName": "김고객",
      "productName": "자동차보험 기본형",
      "paymentMethod": "IMMEDIATE_TRANSFER",
      "status": "PENDING"
    }
  ]
}
```

### 인수심사 완료

`POST /api/underwriting`

권한: `UNDERWRITING_STAFF`, `ADMIN`

Request:

```json
{
  "applicationType": "POLICY",
  "appNo": "APP00001",
  "customerName": "김고객",
  "reviewType": "표준심사",
  "reviewOpinion": "건강 상태 양호, 표준 인수 적합",
  "riskGrade": "표준",
  "result": "APPROVED",
  "resultCondition": null,
  "rejectionReason": null
}
```

Response:

```json
{
  "underwritingNo": "UWR00001",
  "appNo": "APP00001",
  "customerName": "김고객",
  "reviewType": "표준심사",
  "riskGrade": "표준",
  "reviewOpinion": "건강 상태 양호, 표준 인수 적합",
  "result": "APPROVED",
  "resultCondition": null,
  "rejectionReason": null,
  "reviewedAt": "2026-06-04T10:30:00"
}
```

- `applicationType`: `POLICY`(청약) 또는 `INSURANCE`(보험신청). 지정한 유형의 신청 건 상태가 심사 결과(`result`)로 갱신된다.
- `result`: `APPROVED` 또는 `REJECTED`.

---

### 청약 신청 목록

`GET /api/insurance-applications?page=1&size=20`

권한: `SALES_STAFF`, `UNDERWRITING_STAFF`, `ADMIN`

Response:

```json
{
  "page": 1,
  "size": 20,
  "total": 3,
  "items": [
    {
      "applicationNo": "APP00001",
      "customerName": "김고객",
      "productName": "자동차보험 기본형",
      "paymentMethod": "IMMEDIATE_TRANSFER",
      "status": "신청",
      "appliedAt": "2026-06-04T09:00:00"
    }
  ]
}
```

### 청약 신청 단건 조회

`GET /api/insurance-applications/{applicationNo}`

권한: `SALES_STAFF`, `UNDERWRITING_STAFF`, `ADMIN`

---

### 청약 접수

`POST /api/insurance-applications`

권한: 고객 본인 (고객 소유권 검증)

Request:

```json
{
  "customerId": "CUS00001",
  "customerName": "김고객",
  "productName": "자동차보험 기본형",
  "paymentMethod": "IMMEDIATE_TRANSFER"
}
```

Response:

```json
{
  "applicationNo": "APP00001",
  "customerName": "김고객",
  "productName": "자동차보험 기본형",
  "paymentMethod": "IMMEDIATE_TRANSFER",
  "status": "PENDING",
  "appliedAt": "2026-06-04T09:00:00"
}
```

### 청약서 목록

`GET /api/policy-applications?page=1&size=20`

권한: `SALES_STAFF`, `UNDERWRITING_STAFF`, `ADMIN`

Response:

```json
{
  "page": 1,
  "size": 20,
  "total": 2,
  "items": [
    {
      "applicationNo": "POL00001",
      "customerName": "김고객",
      "productName": "자동차보험 기본형",
      "period": 12,
      "paymentMethod": "IMMEDIATE_TRANSFER",
      "status": "신청",
      "submittedAt": "2026-06-04T09:00:00",
      "uploadedAt": "2026-06-04T09:00:00"
    }
  ]
}
```

### 청약서 단건 조회

`GET /api/policy-applications/{applicationNo}`

권한: `SALES_STAFF`, `UNDERWRITING_STAFF`, `ADMIN`

---

### 계약 체결

`POST /api/policy-applications`

권한: 고객 본인 (고객 소유권 검증)

Request:

```json
{
  "customerId": "CUS00001",
  "customerName": "김고객",
  "productName": "자동차보험 기본형",
  "period": 12,
  "paymentMethod": "IMMEDIATE_TRANSFER",
  "uploadedAt": "2026-06-04T09:00:00"
}
```

Response:

```json
{
  "applicationNo": "POL00001",
  "customerName": "김고객",
  "productName": "자동차보험 기본형",
  "period": 12,
  "paymentMethod": "IMMEDIATE_TRANSFER",
  "status": "SUBMITTED",
  "submittedAt": "2026-06-04T09:00:00",
  "uploadedAt": "2026-06-04T09:00:00"
}
```

---

### 부활 신청 목록

`GET /api/revivals?page=1&size=20`

권한: `SALES_STAFF`, `UNDERWRITING_STAFF`, `ADMIN`

Response:

```json
{
  "page": 1,
  "size": 20,
  "total": 1,
  "items": [
    {
      "revivalNo": "REV00001",
      "contractNo": "CON00001",
      "customerName": "김고객",
      "contact": "010-1234-5678",
      "unpaidAmount": 300000,
      "paymentMethod": "IMMEDIATE_TRANSFER",
      "appliedAt": "2026-06-04T09:00:00"
    }
  ]
}
```

### 부활 신청 단건 조회

`GET /api/revivals/{revivalNo}`

권한: `SALES_STAFF`, `UNDERWRITING_STAFF`, `ADMIN`

---

### 부활 신청

`POST /api/revivals`

권한: 고객 본인 (고객·계약 소유권 검증)

Request:

```json
{
  "customerId": "CUS00001",
  "customerName": "김고객",
  "contractNo": "CON00001",
  "contact": "010-1234-5678",
  "unpaidAmount": 300000,
  "paymentMethod": "IMMEDIATE_TRANSFER"
}
```

Response:

```json
{
  "revivalNo": "RVV00001",
  "contractNo": "CON00001",
  "customerName": "김고객",
  "contact": "010-1234-5678",
  "unpaidAmount": 300000,
  "paymentMethod": "IMMEDIATE_TRANSFER",
  "appliedAt": "2026-06-04T09:00:00"
}
```

---

### 보험 상품 목록

`GET /api/insurance-products`

권한: 로그인 사용자

- 페이지네이션 없음. 상품 목록 전체를 `items` 배열로 반환한다.

Response:

```json
{
  "items": [
    {
      "productName": "자동차보험 기본형",
      "category": "자동차",
      "monthlyPremium": 150000,
      "coverageSummary": "대인배상I, 대인배상II, 대물배상",
      "exclusionSummary": "음주운전, 무면허 운전"
    }
  ]
}
```
