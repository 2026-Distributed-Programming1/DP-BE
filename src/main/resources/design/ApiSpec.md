# API 명세서

> 목적: 프론트 병렬 작업을 위한 수동 API 계약 문서.
> 기준: 2026-06-03 현재 구현된 전 도메인 API.
> 상태: 인증·고객·계약·납입·청구·교육·영업·문의·상담 API 명세 작성 완료.

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
| 영업/채널 | `SALES_STAFF`, `ADMIN` |
| 성과급 요청 생성 | `ADMIN` |
| 교육 계획/제반/진행 | `EDUCATION_STAFF`, `ADMIN` |
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

---

## 계약 API

### 계약 목록

`GET /api/contracts?type=&page=1&size=20`

권한: 직원/관리자 전체, 고객은 본인 계약만

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

권한: 직원/관리자 전체, 고객은 본인 계약만

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
