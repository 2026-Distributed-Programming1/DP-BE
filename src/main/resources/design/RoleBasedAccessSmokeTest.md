# Role 기반 접근 제어 Smoke Test

> 목적: HTTP 세션 로그인과 `auth_users.role` 기반 인가가 프론트 연결 전에 의도대로 동작하는지 빠르게 확인한다.
> 기준: 2026-06-03 현재 구현된 role 기반 접근 정책.

## 전제

- 앱이 `http://localhost:8080`에서 실행 중이어야 한다.
- MySQL schema는 Flyway로 생성되어 있어야 한다.
- 관리자 계정은 bootstrap 또는 별도 생성 절차로 준비되어 있어야 한다.
- 실제 비밀번호, EC2 IP, secret 값은 문서에 기록하지 않는다.
- 직원 계정 발급 API는 임시 비밀번호를 반환한다.
- 임시 비밀번호로 로그인한 직원 계정은 `passwordChangeRequired=true`일 수 있다.
  - 이 상태에서는 `/api/auth/**` 외 업무 API가 403으로 차단된다.
  - role별 업무 API 테스트 전 `POST /api/auth/password`로 비밀번호를 변경한다.

## 공통 curl 패턴

Base URL:

```bash
BASE_URL=http://localhost:8080
```

관리자 로그인:

```bash
curl -i -c admin.cookie \
  -H "Content-Type: application/json" \
  -d '{"username":"<ADMIN_USERNAME>","password":"<ADMIN_PASSWORD>"}' \
  "$BASE_URL/api/auth/login"
```

현재 로그인 사용자 확인:

```bash
curl -i -b admin.cookie "$BASE_URL/api/auth/me"
```

직원 계정 발급:

```bash
curl -i -b admin.cookie \
  -H "Content-Type: application/json" \
  -d '{"username":"finance01","displayName":"Finance Smoke","role":"FINANCE_STAFF"}' \
  "$BASE_URL/api/auth/staff-accounts"
```

직원 로그인:

```bash
curl -i -c finance.cookie \
  -H "Content-Type: application/json" \
  -d '{"username":"finance01","password":"<TEMP_PASSWORD>"}' \
  "$BASE_URL/api/auth/login"
```

직원 비밀번호 변경:

```bash
curl -i -b finance.cookie \
  -H "Content-Type: application/json" \
  -d '{"currentPassword":"<TEMP_PASSWORD>","newPassword":"<NEW_PASSWORD>"}' \
  "$BASE_URL/api/auth/password"
```

## 인증 API 확인

| Scenario | Request | Expected |
|---|---|---:|
| 로그인 없이 `me` 호출 | `GET /api/auth/me` | 401 |
| 로그인 성공 | `POST /api/auth/login` | 200 |
| 고객 직접 회원가입 | `POST /api/auth/signup/customer` | 200 |
| 일반 사용자로 고객 계정 발급 | `POST /api/auth/customer-accounts` | 403 |
| 관리자 고객 계정 발급 | `POST /api/auth/customer-accounts` | 200 |
| 일반 사용자로 직원 계정 발급 | `POST /api/auth/staff-accounts` | 403 |
| 관리자 직원 계정 발급 | `POST /api/auth/staff-accounts` | 200 |
| 로그아웃 | `POST /api/auth/logout` | 200 |

## Role별 대표 API Matrix

아래 matrix는 대표 endpoint만 확인한다. 데이터가 없거나 path variable이 존재하지 않으면 허용 role에서도 404가 나올 수 있다. 이 경우 권한 검증 관점에서는 403이 아닌지만 먼저 본다.

| Role | 200 또는 404, 403 아님 | 반드시 403 |
|---|---|---|
| `CUSTOMER` | `GET /api/contracts`, `GET /api/customers/{본인고객번호}`, `GET /api/inquiries` | `GET /api/payment-records` 전체 업무 접근, `GET /api/education-plans`, `GET /api/contract-statistics`, `GET /api/dispatches` |
| `ADMIN` | 모든 대표 업무 API | 없음 |
| `CONTRACT_STAFF` | `GET /api/contract-statistics`, `GET /api/expiring-contracts`, `GET /api/expiring-notices` | `GET /api/education-plans`, `GET /api/dispatches`, `GET /api/activity-plans` |
| `FINANCE_STAFF` | `GET /api/payment-records`, `GET /api/refund-calculations`, `GET /api/refund-payments` | `GET /api/education-plans`, `GET /api/activity-plans`, `GET /api/contract-statistics` |
| `CLAIM_STAFF` | `GET /api/dispatches`, `GET /api/claims/{claimNo}/investigation`, `GET /api/investigations/{investigationNo}/calculation` | `GET /api/education-plans`, `GET /api/activity-plans`, `GET /api/contract-statistics` |
| `DISPATCH_STAFF` | `GET /api/dispatches` | `GET /api/education-plans`, `GET /api/activity-plans`, `GET /api/contract-statistics` |
| `SALES_STAFF` | `GET /api/consultations`, `GET /api/proposals`, `GET /api/interview-schedules`, `GET /api/activity-plans` | `GET /api/education-plans`, `GET /api/payment-records`, `GET /api/contract-statistics` |
| `UNDERWRITING_STAFF` | `GET /api/consultations`, `GET /api/proposals`, `GET /api/interview-schedules`, `GET /api/underwriting/pending` | `GET /api/education-plans`, `GET /api/payment-records`, `GET /api/activity-plans` |
| `EDUCATION_STAFF` | `GET /api/education-plans`, `GET /api/education-preparations`, `GET /api/education-executions` | `GET /api/payment-records`, `GET /api/dispatches`, `GET /api/activity-plans` |

## 고객 소유권 확인

고객 계정은 `linked_customer_id` 기준으로 본인 데이터만 접근해야 한다.

| Scenario | Request | Expected |
|---|---|---:|
| 본인 고객 상세 | `GET /api/customers/{본인고객번호}` | 200 |
| 타인 고객 상세 | `GET /api/customers/{타인고객번호}` | 403 |
| 본인 계약 목록 | `GET /api/contracts` | 200, 본인 계약만 |
| 타인 계약 상세 | `GET /api/contracts/{타인계약번호}` | 403 |
| 본인 문의 목록 | `GET /api/inquiries` | 200, 본인 문의만 |
| 타인 문의 상세 | `GET /api/inquiries/{타인문의번호}` | 403 |

## 대표 curl 예시

고객이 교육 API를 호출하면 403:

```bash
curl -i -b customer.cookie "$BASE_URL/api/education-plans"
```

교육 담당 직원은 교육 API가 200:

```bash
curl -i -b education.cookie "$BASE_URL/api/education-plans"
```

재무 담당 직원이 영업 API를 호출하면 403:

```bash
curl -i -b finance.cookie "$BASE_URL/api/activity-plans"
```

관리자는 직원 계정 발급이 200:

```bash
curl -i -b admin.cookie \
  -H "Content-Type: application/json" \
  -d '{"username":"sales01","displayName":"Sales Smoke","role":"SALES_STAFF"}' \
  "$BASE_URL/api/auth/staff-accounts"
```

## 기록 방식

테스트할 때는 아래 형식으로 결과를 남긴다.

```text
[PASS] CUSTOMER /api/education-plans -> 403
[PASS] EDUCATION_STAFF /api/education-plans -> 200
[PASS] FINANCE_STAFF /api/activity-plans -> 403
[PASS] ADMIN /api/auth/staff-accounts -> 200
```

## 2026-06-03 실행 결과

### 1차 실행

인증/고객/role별 대표 API 대부분은 통과했다.

통과:

```text
[PASS] anonymous GET /api/auth/me -> 401
[PASS] ADMIN login -> 200
[PASS] ADMIN GET /api/auth/me -> 200
[PASS] CUSTOMER signup -> 200
[PASS] CUSTOMER login -> 200
[PASS] CUSTOMER own detail -> 200
[PASS] CUSTOMER search customers forbidden -> 403
[PASS] CUSTOMER education forbidden -> 403
[PASS] CUSTOMER own inquiries list -> 200
[PASS] 직원 계정 발급/로그인/비밀번호 변경 -> 200
[PASS] FINANCE_STAFF payment records -> 200
[PASS] CLAIM_STAFF dispatches -> 200
[PASS] SALES_STAFF consultations/activity plans -> 200
[PASS] EDUCATION_STAFF education plans -> 200
[PASS] CONTRACT_STAFF contract statistics/expiring contracts -> 200
[PASS] UNDERWRITING_STAFF underwriting pending -> 200
[PASS] DISPATCH_STAFF dispatches -> 200
```

발견된 실패:

```text
[FAIL] SALES_STAFF /api/payment-records -> expected 403, got 200
[FAIL] EDUCATION_STAFF /api/payment-records -> expected 403, got 200
[FAIL] UNDERWRITING_STAFF /api/payment-records -> expected 403, got 200
```

추가 확인 결과 아래 환급 목록 API도 비재무 직원에게 200을 반환했다.

```text
SALES_STAFF /api/refund-calculations -> 200
EDUCATION_STAFF /api/refund-calculations -> 200
UNDERWRITING_STAFF /api/refund-calculations -> 200
SALES_STAFF /api/refund-payments -> 200
EDUCATION_STAFF /api/refund-payments -> 200
UNDERWRITING_STAFF /api/refund-payments -> 200
```

원인:

- `CUSTOMER` 소유권 검증은 있었지만, 비고객 사용자의 업무 role 제한이 목록/조회 API에 빠져 있었다.
- `canAccessContract()`는 `CUSTOMER`가 아니면 true를 반환하므로, 직원 role별 업무 제한에는 직접 사용할 수 없다.

수정:

- `PaymentRecordService.getAll()`에서 비고객 사용자는 `FINANCE_STAFF`, `ADMIN`만 접근하도록 제한했다.
- `RefundService.getAllCalculations()`, `getAllPayments()`, 단건 조회에서 비고객 사용자는 `FINANCE_STAFF`, `ADMIN`만 접근하도록 제한했다.

검증:

- 수정 후 `./gradlew compileJava` 통과.

### 수정 후 재실행

앱 재시작으로 이전 쿠키가 401이 되어 직원 계정을 다시 로그인한 뒤 실패 케이스를 재검증했다.

```text
[PASS] SALES_STAFF login -> 200
[PASS] EDUCATION_STAFF login -> 200
[PASS] UNDERWRITING_STAFF login -> 200
[PASS] FINANCE_STAFF login -> 200
[PASS] SALES_STAFF /api/payment-records -> 403
[PASS] EDUCATION_STAFF /api/payment-records -> 403
[PASS] UNDERWRITING_STAFF /api/payment-records -> 403
[PASS] SALES_STAFF /api/refund-calculations -> 403
[PASS] EDUCATION_STAFF /api/refund-calculations -> 403
[PASS] UNDERWRITING_STAFF /api/refund-calculations -> 403
[PASS] SALES_STAFF /api/refund-payments -> 403
[PASS] EDUCATION_STAFF /api/refund-payments -> 403
[PASS] UNDERWRITING_STAFF /api/refund-payments -> 403
[PASS] FINANCE_STAFF /api/payment-records -> 200
[PASS] FINANCE_STAFF /api/refund-calculations -> 200
[PASS] FINANCE_STAFF /api/refund-payments -> 200
```

결론:

- role 기반 접근 제어의 대표 smoke test는 수정 후 통과했다.
- 다음 단계에서는 Postman collection 또는 shell script로 자동화할 수 있다.

## 후속 작업

- 위 시나리오를 Postman collection 또는 shell script로 자동화한다.
- path variable이 필요한 상세 API는 seed data 기준 업무번호를 문서화한다.
- 목록 응답 통일 이후 matrix의 기대 응답 body까지 검증한다.
