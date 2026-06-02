# API 명세서

> 목적: 프론트 병렬 작업을 위한 수동 API 계약 문서.
> 기준: 2026-06-02 현재 구현된 주요 인증/고객 API와 프론트 입력에 필요한 enum 값.
> 상태: 인증 API, 고객 검색/상세 API, 주요 enum 값 1차 작성 완료. 계약/납입/청구 등 도메인 API는 후속 확장 예정.

## 공통

- 로컬 Base URL: `http://localhost:8080`
- 배포 Base URL: `http://<EC2_PUBLIC_IP>:8080`
- 인증 방식: HTTP Session Cookie
- 쿠키 이름: `DPBE_SESSION`
- 로그인 이후 요청은 `Authorization` 헤더가 아니라 쿠키로 인증한다.
- 프론트와 백엔드 origin이 다르면 `fetch`는 `credentials: "include"`, axios는 `withCredentials: true`가 필요하다.
- 요청/응답 body는 JSON을 기본으로 한다.

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

권한: 직원 또는 관리자

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
- `UNDER_REVIEW`: 검토중
