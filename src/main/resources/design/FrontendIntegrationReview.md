# 프론트 연동 관점 검토

> 목적: 배포 및 REST API 전환 이후, 프론트 화면에서 자연스럽게 사용할 수 있는 API인지 도메인 단위로 확인한다.
> 원칙: 이 문서는 확인 결과와 후보 작업만 기록한다. 코드 수정은 별도 승인 후 진행한다.

## 세션 인수인계 메모

다른 세션에서 이어서 작업할 때는 아래 내용을 먼저 확인한다.

- 현재 배포 파이프라인은 GitHub Actions -> Docker Hub -> EC2 Docker Compose 흐름으로 구성되어 있다.
- EC2는 Linux이며, 현재 기준은 Amazon Linux 계열 사용자 홈 디렉토리 `/home/ec2-user`이다.
- 배포 확인은 완료되었다.
  - EC2 내부에서 `curl -i http://localhost:8080/api/contracts` 호출 시 200 응답 확인.
  - 외부 접속 문제는 EC2 public IPv4와 security group 8080 inbound 설정으로 해결했다.
- `.env` 파일 내용은 직접 열람하거나 문서에 복사하지 않는다.
  - GitHub Secret `ENV_FILE`에 `.env` 파일 내용이 들어간다.
  - secret 값, 실제 IP, 실제 계정명, private key 내용은 문서화하지 않는다.
- 코드 변경은 사용자 승인 후 진행한다.
  - 현재 단계는 “확인하고 문서화”가 우선이다.
  - 발견 사항이 있으면 먼저 문서에 남기고, 도메인 검토가 끝난 뒤 수정 우선순위를 정한다.
- 프론트 연동 검토는 도메인 단위로 진행한다.
  - 한 번에 전체를 수정하지 않는다.
  - 각 도메인마다 “현재 API로 화면 구성이 가능한가”, “프론트가 하드코딩해야 하는 값은 무엇인가”, “추가 endpoint가 필요한가”를 본다.
- 추가 기능 후보는 바로 구현하지 않는다.
  - 로그인/권한, 고객 CRUD, enum 값 명세, API 문서화, 목록 응답 통일은 백로그로 관리하되, 완료된 항목은 상태를 갱신한다.
- 기존 버그 가능성 중심 검토는 `DomainReviewFindings.md`에 있다.
  - 이 문서는 프론트 연동 관점만 다룬다.

### 현재 완료된 작업

- 배포 설정 추가 및 실제 GitHub Actions 배포 성공.
- Docker Hub access token 기반 로그인 설정.
- Amazon Linux 기준 `/home/ec2-user` 경로 통일.
- Docker 권한 문제 대응.
- datasource driver class 환경변수 전달 문제 대응.
- 배포 문서 업데이트: `DeploymentPlan.md`.
- 프론트 연동 검토 문서 신규 작성: `FrontendIntegrationReview.md`.
- 프론트 연동 관점 1차 검토 완료 도메인:
  - contract
  - payment
  - refund
  - customer entrypoint
  - claim
  - consultation
  - sales
  - education
  - inquiry
- 도메인별 role 기반 권한 보강 1차 적용 완료:
  - contract: 통계/만기계약 관리는 `CONTRACT_STAFF`, `ADMIN`으로 제한. 계약/해지는 고객 본인 소유권 검증 유지.
  - payment/refund: 납입 기록 관리는 `FINANCE_STAFF`, `ADMIN`, 환급 업무는 `FINANCE_STAFF`, `ADMIN`으로 제한. 고객 조회는 본인 계약 기준으로 제한.
  - claim: 손해조사/보험금 산출은 `CLAIM_STAFF`, `ADMIN`, 보험금 지급은 `FINANCE_STAFF`, `CLAIM_STAFF`, `ADMIN`, 출동 기록은 `DISPATCH_STAFF`, `CLAIM_STAFF`, `ADMIN`으로 제한.
  - consultation: 상담/제안/면담은 `SALES_STAFF`, `UNDERWRITING_STAFF`, `ADMIN`, 인수심사는 `UNDERWRITING_STAFF`, `ADMIN`으로 제한.
  - sales: 영업 운영 API는 `SALES_STAFF`, `ADMIN`, 성과급 요청 생성은 `ADMIN`으로 제한.
  - education: 교육 계획/제반/진행은 `EDUCATION_STAFF`, `ADMIN`으로 제한.
  - inquiry: 고객은 본인 문의만 조회하고, 답변은 직원/관리자만 수행하도록 제한.
  - customer: 검색은 직원/관리자 전용, 상세는 직원/관리자 또는 고객 본인 접근 허용.
- 주요 테이블 목록 응답 통일 및 DB pagination 1차 적용 완료:
  - `page/size/total/items` 응답 형태로 통일.
  - customer, contract, payment/refund, claim, consultation, sales, education, inquiry 주요 목록은 DB `COUNT` + `LIMIT/OFFSET` 기준으로 조회.
  - 고객 소유권이 필요한 목록은 SQL 조건에 고객 식별자를 포함해 `total`과 `items` 범위를 일치시킴.

### 다음에 이어서 할 일

1. 도메인별 상태 전이 규칙 문서화
2. `ApiSpec.md`에 계약/납입/청구 등 주요 도메인 API 명세 확장
3. 파일/S3 저장 정책 설계
4. 미사용 DTO/유틸 정리

## 검토 순서

1. contract + payment + refund + customer entrypoint
2. claim
3. consultation
4. sales
5. education + inquiry
6. common + auth + customer CRUD

## 전체 추가 기능 후보

인증/Flyway의 1차 구현은 완료됐고, 나머지는 할 일 목록으로 둔다.

- **AUTH-01 / 로그인·권한**
  - 1차 구현 완료:
    - DB 스키마 변경 관리는 Flyway로 도입했다.
    - HTTP 환경에서 세션 기반 로그인을 구현했다.
    - 브라우저 쿠키에는 세션 ID만 저장하고, 실제 로그인 상태는 서버/DB 세션 저장소에서 관리한다.
    - 세션 저장소는 Spring Session JDBC + MySQL을 사용한다.
    - Spring Session JDBC 테이블과 사용자/역할 테이블은 Flyway migration으로 추가했다.
    - 인증 API는 `POST /api/auth/signup/customer`, `POST /api/auth/login`, `POST /api/auth/logout`, `GET /api/auth/me`, `POST /api/auth/password`이다.
    - 고객 직접 회원가입은 `POST /api/auth/signup/customer`로 처리한다.
    - 고객 직접 회원가입 필수 입력값은 로그인 아이디, 비밀번호, 이름, 주민등록번호, 연락처, 주소, 생년월일이다.
    - 이메일은 선택값이다.
    - 회원가입 시 `customers`와 `auth_users`를 같은 트랜잭션에서 생성한다.
    - 고객번호는 서버가 `CUS00001` 형식으로 생성한다.
    - 직접 회원가입 계정은 `role=CUSTOMER`, `password_change_required=false`로 생성되며 자동 로그인은 하지 않는다.
    - 회원가입 요청 검증은 Bean Validation(`@Valid`)으로 처리하고, 검증 실패는 400 응답을 반환한다.
    - 관리자 계정 발급 API는 `POST /api/auth/customer-accounts`이다.
    - 관리자는 기존 고객번호와 로그인 아이디만 입력하고, 서버가 임시 비밀번호를 생성한다.
    - 관리자 발급 고객 계정은 `password_change_required=true`로 생성되며 최초 로그인 후 비밀번호 변경이 필요하다.
    - 직원 계정 발급 API는 `POST /api/auth/staff-accounts`이다.
    - 직원 계정은 관리자만 발급한다.
    - 직원 계정 발급 시 서버가 임시 비밀번호를 생성한다.
    - 직원 계정은 `password_change_required=true`, `linked_customer_id=null`로 생성한다.
    - `/api/auth/**`를 제외한 `/api/**`는 로그인 세션이 필요하다.
    - 프론트와 백엔드는 다른 origin으로 동작하므로 CORS credentials 설정이 필요하다.
    - 프론트 요청은 `credentials: "include"` 또는 `withCredentials: true`를 사용해야 한다.
    - HTTP 환경이므로 쿠키 `Secure=true`는 사용할 수 없다.
    - `HttpOnly=true`는 유지하고, SameSite 설정은 실제 브라우저 동작을 확인하면서 결정한다.
  - 발생 가능한 오류:
    - CORS에서 `Access-Control-Allow-Credentials`가 없으면 로그인 쿠키가 저장/전송되지 않을 수 있다.
    - `Access-Control-Allow-Origin: *`는 credentials 요청과 함께 사용할 수 없다.
    - 프론트가 credentials 옵션을 누락하면 이후 요청에 세션 쿠키가 포함되지 않는다.
    - HTTP + 다른 site 조합에서는 `SameSite=None`과 `Secure` 요구 때문에 브라우저가 쿠키를 차단할 수 있다.
    - 컨테이너 재시작 시 메모리 세션은 사라지므로 DB 세션 저장이 필요하다.
  - fallback:
    - HTTP + 다른 origin 환경에서 세션 쿠키가 안정적으로 동작하지 않으면 JWT 기반 로그인으로 전환한다.
    - JWT 전환 시 `Authorization: Bearer <token>` 헤더 기반으로 인증하고, CORS 쿠키 의존도를 줄인다.
  - 역할/소유권 적용 방침:
    - role은 도메인 엔터티가 아니라 `auth_users.role`에서 관리한다.
    - `Customer`는 보험 업무의 고객 master이고, `AuthUser`는 로그인 계정이므로 합치지 않는다.
    - 고객뿐 아니라 직원/관리자 계정도 필요하므로 인증 계정은 고객 테이블과 분리한다.
    - 고객 계정은 `auth_users.linked_customer_id`로 `customers.id`에 연결한다.
    - 현재 구현된 기본 역할은 `CUSTOMER`, `STAFF`, `ADMIN`이다.
    - 직원 세부 역할로 `CONTRACT_STAFF`, `CLAIM_STAFF`, `UNDERWRITING_STAFF`, `SALES_STAFF`, `EDUCATION_STAFF`, `FINANCE_STAFF`, `DISPATCH_STAFF`를 추가했다.
    - 공통 interceptor는 로그인 여부만 검사한다.
    - 고객 데이터 소유권은 서비스 계층에서 현재 로그인 사용자와 요청 데이터의 고객을 비교해 검증한다.
    - `CUSTOMER`는 `linked_customer_id`로 연결된 본인 고객 데이터만 접근한다.
    - `STAFF`, `ADMIN`은 업무 처리 목적상 전체 데이터 접근을 허용한다.
    - 1차 권한 보강은 role 기반으로 적용했다.
    - 고객 검색은 직원/관리자 전용이고, 고객 상세는 직원/관리자 또는 고객 본인 접근을 허용한다.
    - 내부 업무 API는 도메인별 담당 직원 role과 `ADMIN` 중심으로 제한한다.
  - 직원 역할 세분화 결정:
    - 직원 역할은 세분화했다.
    - 1차 역할은 `CONTRACT_STAFF`, `CLAIM_STAFF`, `UNDERWRITING_STAFF`, `SALES_STAFF`, `EDUCATION_STAFF`, `FINANCE_STAFF`, `DISPATCH_STAFF`이다.
    - 기존 `STAFF`는 과도기 호환용 또는 일반 직원 역할로 유지할 수 있다.
    - 직원 계정은 고객처럼 직접 회원가입하지 않고, 관리자가 발급한다.
    - 직원 계정 발급 시 서버가 임시 비밀번호를 생성하고 `password_change_required=true`로 둔다.
    - 1차 직원 계정은 `auth_users.role`만으로 API 접근 권한을 판단한다.
    - 직원 actor 테이블(`ClaimsHandler`, `FinanceManager`, `EducationTrainer`, `SalesManager`, `InsuranceReviewer`, `DispatchAgent` 등)과의 연결은 2차 확장으로 미룬다.
    - actor 연결이 필요한 시점은 “누가 처리했는지”, “누구에게 배정됐는지”, “담당자별 실적/한도/지역/교육 이력”을 DB에 남겨야 할 때다.
    - 후속 확장 시 `auth_users`에 `linked_actor_type`, `linked_actor_id` 같은 다형 참조를 추가하는 방식을 검토한다.
  - 직원 역할별 접근 정책 초안:
    - 계약 통계/만기계약 관리: `CONTRACT_STAFF`, `ADMIN`
    - 계약/해지 조회: 직원/관리자는 전체, 고객은 본인 계약만
    - 사고/청구/손해조사/보험금 산출: `CLAIM_STAFF`, `ADMIN`
    - 출동/출동 기록: `DISPATCH_STAFF`, `CLAIM_STAFF`, `ADMIN`
    - 보험금 지급: `FINANCE_STAFF`, `CLAIM_STAFF`, `ADMIN`
    - 납입 기록/환급: `FINANCE_STAFF`, `ADMIN`
    - 상담/청약/인수심사: `UNDERWRITING_STAFF`, `SALES_STAFF`, `ADMIN`
    - 영업/채널: `SALES_STAFF`, `ADMIN`
    - 성과급 요청 생성: `ADMIN`
    - 교육: `EDUCATION_STAFF`, `ADMIN`
    - 문의 답변: 직원 또는 관리자
  - 권한 검증 추상화 원칙:
    - `AuthAccessService.currentUser()`는 현재 세션 사용자를 읽는 저수준 조회 메서드로 둔다.
    - 도메인 서비스에서 `currentUser()`를 꺼내 role을 직접 비교하는 코드는 지양한다.
    - 서비스 코드는 가능하면 `requireRefundOperationAccess()`, `requireClaimInvestigationAccess()`처럼 업무 권한 메서드를 호출한다.
    - role 조합은 `AuthAccessService` 내부의 그룹 메서드와 업무 메서드에 숨긴다.
    - 그룹 메서드는 사용자 역할 묶음을 표현한다. 예: `requireFinanceStaff()`, `requireClaimStaff()`, `requireDispatchStaff()`.
    - 업무 메서드는 유스케이스 수행 권한을 표현한다. 예: `requirePaymentRecordManageAccess()`, `requireRefundOperationAccess()`, `requireClaimPaymentAccess()`.
    - 업무 메서드는 필요하면 그룹 메서드를 호출한다. 정책 변경 시 서비스 코드를 수정하지 않고 `AuthAccessService`만 수정하는 것을 목표로 한다.
    - 고객 소유권 검증은 `requireCustomerAccess()`, `requireCustomerNoAccess()`, `requireContractAccess()`, `canAccessContract()` 같은 데이터 접근 메서드를 우선 사용한다.
    - `currentUser()` 직접 사용은 인증 응답 구성, audit/logging, 또는 업무 메서드 내부처럼 현재 사용자 정보 자체가 필요한 경우로 제한한다.
  - 남은 작업:
    - 전체 API smoke test로 role별 403/200 동작을 확인한다.
    - 직원 actor 연결 schema와 연결 API는 담당자 배정/처리자 기록 기능을 시작할 때 설계한다.
    - 비밀번호 찾기/초기화 정책을 결정한다.
  - 검증 완료:
    - fresh MySQL volume 기준으로 Flyway `V1__init_schema.sql` 자동 실행을 확인했다.
    - `POST /api/auth/signup/customer` 고객 직접 회원가입 200 응답을 확인했다.
    - `POST /api/auth/login` 고객 로그인 및 `DPBE_SESSION` 발급을 확인했다.
    - `GET /api/auth/me` 고객 세션 조회 200 응답을 확인했다.
    - 관리자 로그인 후 `POST /api/auth/staff-accounts` 직원 계정 발급 200 응답을 확인했다.
    - 직원 계정 발급 시 `CUSTOMER` role 요청은 400으로 거부되는 것을 확인했다.
    - `./gradlew build -x test` 성공을 확인했다.
  - 수정된 운영 이슈:
    - Spring Boot 4에서는 Flyway auto-configuration을 위해 `spring-boot-starter-flyway`가 필요하다.
    - 신규 고객 저장 시 임시 `customer_id`는 `VARCHAR(20)`을 넘지 않도록 제한한다.

- **CUSTOMER-01 / 고객 검색·상세 API**
  - 상태: 1차 완료
  - 완료:
    - `GET /api/customers?keyword=&page=&size=`
    - `GET /api/customers/{customerId}`
    - 직원/관리자 권한 제한
    - 고객 본인 상세 조회 허용
    - DB pagination
    - 전화번호 keyword 검색
    - `size` 최대 100 제한
  - 남은 작업:
    - 고객 생성/수정 API는 별도 정책 결정 후 진행

- **COMMON-API-01 / enum 값 명세**
  - 상태: 1차 완료
  - 프론트는 메뉴/화면 구성을 직접 관리한다.
  - `PaymentMethod`, `ContractStatus`, `ClaimType`, `ChannelType` 같은 enum 입력값은 `ApiSpec.md`에 명시한다.
  - 현재 단계에서는 별도 option API를 두지 않고, 프론트가 `ApiSpec.md`의 enum 값을 기준으로 select/radio/filter 값을 구성한다.
  - `src/main/resources/design/ApiSpec.md`에 주요 enum 값을 정리했다.
  - 코드 점검 결과 별도 option/options API 컨트롤러는 발견되지 않았다.
  - enum이 많아지고 여러 화면에서 반복되거나 서버 기준 동기화가 필요해지면 option API를 다시 검토한다.

- **COMMON-API-02 / 에러 응답 확장**
  - 상태: 1차 완료
  - 기존 에러 응답은 `status`, `error`, `message`, `timestamp` 중심이었다.
  - 프론트에서 화면별 분기를 안정적으로 할 수 있도록 아래 구조를 추가했다.
  - 1차 구현:
    - `code`: 프론트 분기용 안정 코드
    - `path`: 요청 path
    - `fieldErrors`: 필드 단위 검증 오류
  - `MethodArgumentNotValidException`은 `VALIDATION_ERROR`와 `fieldErrors[]`로 처리한다.
  - `HttpMessageNotReadableException`은 `REQUEST_BODY_ERROR`로 처리한다.
  - `AuthInterceptor`의 미인증/비밀번호 변경 필요 응답도 공통 JSON 에러 포맷으로 맞췄다.

- **COMMON-API-03 / API 문서화**
  - 프론트 연동 전 OpenAPI/Swagger 또는 별도 API 명세 문서가 있으면 화면 작업 속도가 빨라진다.
  - 최소한 endpoint, request, response, enum 값을 도메인별로 정리할 필요가 있다.
  - 1차 구현 후보:
    - Springdoc OpenAPI 도입
    - 또는 도메인별 markdown 명세 작성
  - 프론트와 동시에 작업하려면 request/response 예시가 필수에 가깝다.

- **COMMON-API-04 / 목록 응답 형태 통일**
  - 상태: 1차 완료
  - 주요 테이블 목록은 `page/size/total/items`로 통일했다.
  - 주요 도메인 목록은 DB `COUNT` + `LIMIT/OFFSET` 기반 pagination으로 전환했다.
  - 고객 본인 데이터만 조회해야 하는 목록은 SQL 조건에 고객 식별자를 포함해 `total`과 `items`가 같은 범위를 보도록 했다.
  - 프론트 공통 테이블/페이지네이션 컴포넌트를 생각하면 목록 응답 정책을 정해야 한다.
  - `List<ResponseDto>`를 Controller에서 직접 반환하는 API는 확장성이 낮다.
    - 응답에 `total`, `page`, `size`, `hasNext`, 집계값 같은 메타데이터를 추가하기 어렵다.
    - 나중에 페이지네이션이나 정렬을 붙이면 응답 형태가 깨진다.
    - 프론트가 목록 API마다 배열 응답과 wrapper 응답을 따로 처리해야 한다.
  - 확정 정책:
    - 테이블 화면에 쓰는 목록은 `page`, `size`, `total`, `items`로 통일한다.
    - `page`는 1부터 시작한다.
    - `size`는 요청 크기이며 기본값은 20, 최대값은 100을 기본 정책으로 둔다.
    - `total`은 필터 적용 후 전체 건수다.
    - `items`는 현재 페이지 데이터다.
    - 페이지네이션이 필요 없는 참조용 소량 목록은 `{ "items": [] }` wrapper를 사용한다.
    - 배열 직접 반환은 신규 API에서 사용하지 않는다.
    - select option용 API는 현재 두지 않기로 했으므로 배열 반환 예외도 만들지 않는다.
  - 예외 기준:
    - 단건 조회, 생성/수정/상태변경 응답은 wrapper를 강제하지 않는다.
    - 프론트가 화면 테이블로 쓰지 않는 “현재 요청의 하위 리소스 소량 목록”은 `{ "items": [] }`를 허용한다.
    - 예: 특정 고객의 납입 가능 계약 목록은 테이블 페이지보다 선택용 목록에 가까우므로 `{ "items": [] }` 후보.
  - 적용 완료 범위:
    - customer 검색
    - contract: 계약, 해지, 만기계약, 만기 안내, 통계 이력
    - payment/refund: 납입 기록, 환급 산출, 환급 지급
    - claim: 사고, 청구, 출동
    - consultation: 상담, 면담 일정, 면담 기록, 제안, 인수심사 대기
    - sales: 활동계획, 채널 모집, 채널 심사, 고객 등록, 영업활동, 조직평가
    - education: 교육 계획, 교육 제반, 교육 실행
    - inquiry: 문의 목록
  - 정리 완료:
    - `global/util/PageResponses.java` 삭제 — DB pagination 전환 후 참조 없음
    - `domain/customer/dto/CustomerListResponse.java` 삭제 — `PageResponse<CustomerSummary>` 전환 후 참조 없음
    - `domain/contract/dto/ContractListResponse.java` 삭제 — `PageResponse<ContractSummaryResponse>` 전환 후 참조 없음
    - 도메인별 상세 API 명세에 page query와 응답 예시 반영

- **AUTH-02 / 인증 API 권한 최종 점검**
  - 상태: 1차 점검 완료
  - 확인 위치:
    - `AuthController`
    - `AuthService`
    - `AuthInterceptor`
    - `CorsConfig`
  - 현재 동작:
    - `/api/auth/**`는 interceptor에서 제외되어 로그인 전에도 controller까지 도달한다.
    - `POST /api/auth/login`은 공개 API이며 성공 시 세션에 `AUTHENTICATED_USER`를 저장한다.
    - `POST /api/auth/signup/customer`는 공개 API이며 고객 직접 회원가입만 수행한다.
    - `POST /api/auth/customer-accounts`는 controller는 공개 경로지만 service에서 `requireAdmin()`으로 관리자만 허용한다.
    - `POST /api/auth/staff-accounts`도 service에서 `requireAdmin()`으로 관리자만 허용한다.
    - `POST /api/auth/password`, `POST /api/auth/logout`, `GET /api/auth/me`는 service에서 세션 사용자 조회를 수행하므로 로그인하지 않으면 401이다.
    - 비밀번호 변경 필요 계정은 `/api/auth/**` 외 API 호출 시 interceptor에서 403으로 차단된다.
  - 권한 관점 결론:
    - 회원가입, 로그인은 공개 API로 의도와 일치한다.
    - 고객 계정 발급과 직원 계정 발급은 관리자 전용으로 의도와 일치한다.
    - 로그아웃, me, 비밀번호 변경은 로그인 사용자 전용으로 의도와 일치한다.
  - 개선 후보:
    - `LoginRequest`, `PasswordChangeRequest`, `CustomerAccountCreateRequest`에도 Bean Validation을 추가하고 controller에 `@Valid`를 붙이면 검증 응답이 더 일관된다.
    - 현재도 service 내부에서 기본 검증은 수행하므로 당장 권한 우회 문제는 아니다.
    - `/api/auth/customer-accounts`, `/api/auth/staff-accounts`는 interceptor 제외 경로지만 service에서 관리자 검증을 수행한다. 향후 유지보수자가 놓치지 않도록 Auth 계정 발급 API는 반드시 service 권한 검증을 유지해야 한다.
    - `GET /api/dispatches`
    - `GET /api/claims`
  - 권장 응답 형태:
    - 페이지네이션 목록: `{ "page": 1, "size": 20, "total": 0, "items": [] }`
    - 단순 목록: `{ "items": [] }`

- **COMMON-API-05 / 상태 전이 규칙 문서화**
  - 여러 도메인에서 `status`만 내려주고, 프론트가 버튼 가능 여부를 직접 해석해야 한다.
  - 현재 결정:
    - 알림, 화면 이동, 버튼 노출, 업무 단계 표시는 프론트 책임으로 둔다.
    - 백엔드는 `canApprove`, `nextAction` 같은 화면용 파생 필드를 필수로 추가하지 않는다.
  - 1차 보완 후보:
    - 상태값별 가능한 전이 표 문서화
    - 잘못된 상태 전이 요청은 400 또는 409로 명확히 응답
    - enum 값 또는 API 명세로 허용 상태값 제공

- **COMMON-API-06 / 첨부파일·이미지 업로드 정책**
  - claim 출동 사진과 inquiry 첨부 파일에서 파일 접근 정책이 필요하다.
  - S3 전환 전이라도 프론트는 업로드 후 미리보기/다운로드 URL이 필요하다.
  - 1차 정책 후보:
    - 파일 메타데이터는 DB에 저장
    - 응답에는 `fileName`, `fileUrl` 또는 `objectKey` 제공
    - S3 적용 시 signed URL 사용 여부 결정
  - S3는 배포 설정이 아니라 기능 작업으로 별도 진행한다.

- **COMMON-API-07 / 업무번호 파싱·검증 공통화**
  - `CON00001`, `CLM00001`, `APL00001` 같은 업무번호 파싱 로직이 서비스/리포지토리에 반복되어 있다.
  - 잘못된 path/query 입력이 일부 500 또는 404로 섞일 수 있다.
  - 1차 구현 후보:
    - 업무번호 format 오류는 400
    - 존재하지 않는 번호는 404
    - 공통 유틸 또는 도메인별 helper로 정책 통일

---

## 1. contract + payment + refund + customer entrypoint

상태: 프론트 연동 관점 1차 검토 완료

### 확인한 엔드포인트

계약:

- `GET /api/contracts?type=&page=&size=`
- `GET /api/contracts/{contractNo}`
- `POST /api/contracts/{contractNo}/cancellation`
- `GET /api/cancellations`
- `GET /api/cancellations/{cancellationNo}`
- `GET /api/expiring-contracts`
- `POST /api/expiring-contracts/{contractNo}/notice`
- `GET /api/expiring-notices?contractNo=`
- `POST /api/expiring-notices/{noticeNo}/response`
- `GET /api/contract-statistics`
- `POST /api/contract-statistics`
- `GET /api/contract-statistics/history`

납입:

- `GET /api/customers/{customerId}/contracts`
- `POST /api/payments/preview`
- `POST /api/payments`
- `GET /api/payment-records?contractNo=&status=`
- `POST /api/payment-records/{recordNo}/confirm`
- `POST /api/payment-records/{recordNo}/reject`

환급:

- `POST /api/cancellations/{cancellationNo}/refund-calculation`
- `GET /api/refund-calculations`
- `GET /api/refund-calculations/{refundNo}`
- `POST /api/refund-calculations/{refundNo}/confirm`
- `GET /api/refund-payments`
- `GET /api/refund-payments/{paymentNo}`
- `POST /api/refund-payments/{paymentNo}/execute`

### 프론트 화면 흐름 적합성

- 계약 조회 화면은 바로 구현 가능하다.
  - 목록 응답이 `page`, `size`, `total`, `items` 구조라 테이블 페이지네이션에 맞다.
  - 상세 응답에 고객명, 연락처, 만기 D-day, 연체 여부, 특약 목록이 포함되어 상세 패널을 구성하기 쉽다.

- 보험료 납입 흐름은 `고객 계약 조회 -> 미리보기 -> 제출 -> 수납 확정/반려`로 구성할 수 있다.
  - `POST /api/payments/preview`가 저장 없이 총액과 할인액을 계산하므로 결제 확인 화면을 만들기 좋다.
  - `POST /api/payments` 응답에 생성된 `recordNo` 목록이 포함되어 다음 단계로 이동하기 쉽다.

- 해지/환급 흐름은 `해지 신청 -> 환급 산출 -> 환급 확정 -> 환급 지급 실행`으로 구성할 수 있다.
  - 각 단계별 endpoint가 분리되어 있어 wizard 형태 화면에 맞다.

### 발견 사항

- **FE-CONTRACT-01 / 고객 진입점 부족**
  - 확인 위치: `PaymentController.customerContracts()`, `CustomerRepository`
  - 프론트에서 납입 화면을 시작하려면 먼저 고객을 검색하거나 선택해야 한다.
  - 현재 API는 `GET /api/customers/{customerId}/contracts`만 있고, 고객 목록/검색 API가 없다.
  - 화면 구현 시 임시로 customerId를 직접 입력해야 하므로 UX가 어색하다.
  - 후보 작업: `GET /api/customers?keyword=`, `GET /api/customers/{customerId}` 추가.

- **FE-CONTRACT-02 / 없는 고객과 계약 없는 고객 구분 어려움**
  - 확인 위치: `PaymentService.customerContracts()`
  - 없는 고객 ID를 넣어도 계약 목록 조회 결과가 빈 배열이면, 프론트는 “고객 없음”과 “납입 가능한 계약 없음”을 구분하기 어렵다.
  - 후보 작업: 고객 존재 확인 후 없는 고객은 404, 계약이 없는 고객은 빈 배열로 구분.

- **FE-CONTRACT-03 / 목록 응답 형태 불일치** ✅ 완료
  - 확인 위치: `ContractController.list()`, `CancellationController.list()`, `PaymentRecordController.list()`, `RefundController`
  - 계약/해지/납입내역/환급 목록은 `page/size/total/items` 응답으로 통일했다.
  - 주요 목록은 DB `COUNT` + `LIMIT/OFFSET` 기반으로 전환했다.

- **FE-CONTRACT-04 / enum 값 출처 없음**
  - 확인 위치: `PaymentSubmitRequest.paymentMethod`, `PaymentRecordRejectRequest.rejectCategory`, `NoticeResponseRequest.customerResponse`
  - 프론트가 select/radio 옵션을 만들려면 enum 값을 알아야 한다.
  - 현재는 서버가 허용하는 값 목록을 제공하지 않으므로 프론트가 Java enum 이름을 하드코딩해야 한다.
  - 후보 작업: API 명세 문서에 허용 enum 값 추가.

- **FE-CONTRACT-05 / 납입 preview와 submit의 중복 항목 정책 필요**
  - 확인 위치: `PaymentService.preview()`, `PaymentService.submit()`
  - 같은 계약이 요청 `items`에 중복될 때 preview와 submit의 처리 방식이 다를 수 있다.
  - 프론트에서 중복 선택을 막더라도 서버 정책이 명확해야 한다.
  - 후보 작업: 중복 `contractNo`는 400으로 막거나 서버에서 병합.

- **FE-CONTRACT-06 / 해지 후 환급 산출 가능 여부를 화면에서 계산해야 함**
  - 확인 위치: `CancellationResponse`, `RefundCalculationResponse`
  - 해지 목록 응답에는 이미 환급 산출이 완료됐는지 여부가 없다.
  - 프론트는 환급 산출 버튼 노출 여부를 알기 위해 환급 목록을 별도로 조회해 매칭해야 한다.
  - 후보 작업: 프론트가 매칭할 수 있도록 해지/환급 조회 API와 상태 전이 규칙을 문서화.

- **FE-CONTRACT-07 / 수납 내역 상태 옵션과 액션 가능 여부가 응답에 없음**
  - 확인 위치: `PaymentRecordDetail`
  - `status`는 내려오지만, 현재 상태에서 confirm/reject 버튼을 보여도 되는지 프론트가 직접 판단해야 한다.
  - 후보 작업: 화면 버튼 판단은 프론트 책임으로 두고, 상태 전이 표를 문서화.

- **FE-CONTRACT-08 / OTP 환급 실행은 데모용 흐름으로 보임**
  - 확인 위치: `RefundService.execute()`
  - 환급 지급 실행은 `otpInput`만 받고, 계좌 정보는 서버 stub 계좌로 처리한다.
  - 프론트 연결은 가능하지만 운영 기능처럼 보이게 만들려면 계좌 선택/검증/OTP 발송 흐름이 더 필요하다.
  - 후보 작업: 환급 계좌 정보 응답/입력, OTP 발송 API, OTP 재시도 정책 노출.

- **FE-PAYMENT-01 / 납입 가능 계약 목록이 배열을 직접 반환함**
  - 확인 위치: `PaymentController.customerContracts()`
  - `GET /api/customers/{customerId}/contracts`는 `List<PaymentContractResponse>`를 직접 반환한다.
  - 현재는 계약 선택용 작은 목록이라 동작은 가능하지만, 프론트에서 선택 가능 계약 수, 고객 정보, 납입 가능 여부 메시지를 함께 표시하려면 응답 wrapper가 필요해진다.
  - 후보 작업: `PaymentContractListResponse` 또는 공통 `{ "items": [] }` wrapper 적용 검토.

- **FE-PAYMENT-02 / 납입 내역 목록 권한 검증 보강 완료**
  - 확인 위치: `PaymentRecordController.list()`, `PaymentRecordService.getAll()`
  - 고객 세션은 본인 계약의 납입 내역만 조회하도록 제한했다.
  - 직원/관리자 계정은 업무 목적상 전체 조회를 허용한다.
  - 남은 작업: 목록 필터와 페이지네이션 정책 결정.

- **FE-PAYMENT-03 / 수납 확정·반려 API role 검증 완료**
  - 확인 위치: `PaymentRecordController.confirm()`, `PaymentRecordController.reject()`, `PaymentRecordService`
  - `POST /api/payment-records/{recordNo}/confirm`, `POST /api/payment-records/{recordNo}/reject`는 수납 담당 업무로 보인다.
  - 현재는 `FINANCE_STAFF`, `ADMIN`만 수행하도록 제한했다.
  - 남은 작업: 상태 전이 표 문서화.

- **FE-PAYMENT-04 / 납입 submit에서 customerId와 계약 목록의 관계 검증이 명확하지 않음**
  - 확인 위치: `PaymentService.submit()`, `Payment.selectContracts()`
  - 고객 계정은 `customerId` 접근과 각 계약 접근이 모두 검증되어 큰 문제는 줄어든다.
  - 다만 직원/관리자 호출에서는 요청의 `customerId`와 `items[].contractNo`가 같은 고객의 계약인지 명시적으로 검증하는 코드가 보이지 않는다.
  - 프론트가 잘못된 조합을 보내면 납입 신청의 고객과 납입 대상 계약 고객이 달라질 가능성을 확인해야 한다.
  - 후보 작업: submit 시 모든 계약의 customer id가 요청 `customerId`와 같은지 서버에서 400으로 검증.

- **FE-PAYMENT-05 / preview와 submit의 중복 계약 처리 정책이 명확하지 않음**
  - 확인 위치: `PaymentService.preview()`, `PaymentService.submit()`
  - `preview`는 요청 항목 순서대로 금액을 합산한다.
  - `submit`은 `PaymentItem`별로 첫 번째 matching request count를 찾기 때문에 같은 `contractNo`가 중복되면 preview와 submit 결과가 어긋날 수 있다.
  - 후보 작업: 동일 요청 내 중복 `contractNo`는 400으로 거부하거나, preview/submit 양쪽에서 같은 방식으로 병합.

- **FE-REFUND-01 / 환급 목록·상세·실행 권한 검증 완료**
  - 확인 위치: `RefundController`, `RefundService`
  - 고객은 본인 계약/해지와 연결된 환급 건만 조회하도록 제한했다.
  - 환급 산출/확정/지급 실행은 `FINANCE_STAFF`, `ADMIN`으로 제한했다.
  - 남은 작업: 환급 목록 필터와 페이지네이션 정책 결정.

- **FE-REFUND-02 / 환급 목록 API 페이지네이션** ✅ 1차 완료
  - 확인 위치: `RefundController.getAllCalculations()`, `RefundController.getAllPayments()`
  - 환급 산출 목록과 지급 목록은 `page/size/total/items` 응답으로 통일했다.
  - 고객 조회 시 SQL 조건에 고객 식별자를 포함해 본인 환급 데이터만 count/list 한다.
  - 남은 작업: `status`, `customerId`, `contractNo`, 기간 조건 같은 추가 필터 지원 검토.

- **FE-REFUND-03 / 환급 지급 실행의 OTP는 검증용 stub임**
  - 확인 위치: `RefundPayment.verifyOTP()`, `RefundService.execute()`
  - OTP는 6자리 입력이면 성공으로 간주되고, 실제 발송/검증 API가 없다.
  - 프론트에서 운영 기능처럼 구현하면 사용자가 실제 OTP 발송을 기대할 수 있다.
  - 후보 작업: 1차 화면에서는 데모 인증으로 명시하거나, OTP 발송/재발송/검증 실패 정책을 별도 API로 설계.

- **FE-REFUND-04 / 환급금 확정 시 산출 엔터티의 확정 상태가 저장되지 않을 수 있음**
  - 확인 위치: `RefundService.confirm()`, `RefundCalculation.confirm()`
  - `RefundService.confirm()`은 `new RefundPayment(refund)`를 생성해 저장하지만, `RefundCalculation.confirm()`을 호출하거나 산출 건의 `confirmedAt`을 저장하는 흐름은 보이지 않는다.
  - 프론트에서 환급 산출 상세를 다시 조회했을 때 “확정 완료” 이력이 명확히 보이지 않을 수 있다.
  - 후보 작업: 환급 확정 시 산출 건의 상태/확정일시를 함께 갱신하고, 응답에 확정 여부를 노출.

### 우선순위 제안

1. 목록 응답 페이지네이션 정책 결정
2. 납입 submit의 고객-계약 관계 검증
3. 납입 중복 항목 정책 확정
4. 해지/환급 상태 전이 규칙 문서화

---

## 2. claim

상태: 프론트 연동 관점 1차 검토 완료

### 확인한 엔드포인트

사고 접수:

- `POST /api/accidents`
- `GET /api/accidents`
- `GET /api/accidents/{accidentNo}`

출동:

- `GET /api/dispatches`
- `POST /api/dispatches/{dispatchNo}/record`
- `GET /api/dispatches/{dispatchNo}/record`

청구:

- `POST /api/claims`
- `GET /api/claims`
- `GET /api/claims/{claimNo}`

조사:

- `POST /api/claims/{claimNo}/investigation`
- `GET /api/claims/{claimNo}/investigation`

산출:

- `POST /api/investigations/{investigationNo}/calculation`
- `GET /api/investigations/{investigationNo}/calculation`
- `POST /api/calculations/{calculationNo}/approve`

보험금 지급:

- `POST /api/calculations/{calculationNo}/payment`
- `GET /api/calculations/{calculationNo}/payment`
- `POST /api/payments/{paymentNo}/execute`

### 프론트 화면 흐름 적합성

- 사고 접수 화면은 바로 구현 가능하다.
  - `needsDispatch=true`이면 사고 접수 응답에 `dispatchNo`가 내려와 다음 출동 기록 화면으로 이동하기 쉽다.
  - 사고 목록/상세도 제공된다.

- 출동 기록 화면은 `multipart/form-data`로 구현 가능하다.
  - `agentName`, `policeRequired`, `towingRequired`, `notes`, `photos`를 form data로 전송하면 된다.
  - 사진 1장 이상 필수라는 정책은 서버에서 검증한다.

- 청구부터 지급까지는 wizard 형태로 구현 가능하다.
  - `청구 등록 -> 조사 등록 -> 산출 등록 -> 산출 승인 -> 지급 생성 -> 지급 실행` 순서가 endpoint에 반영되어 있다.
  - 각 단계별 생성과 조회 API가 분리되어 있어 새로고침 후에도 현재 단계 데이터를 다시 조회할 수 있다.

### 발견 사항

- **FE-CLAIM-01 / 단계형 화면의 현재 진행 상태 계산이 프론트에 집중됨**
  - 확인 위치: `ClaimResponse`, `InvestigationResponse`, `CalculationResponse`, `PaymentResponse`
  - 청구 목록 응답에는 해당 청구의 조사번호, 산출번호, 지급번호가 포함되지 않는다.
  - 프론트가 청구 한 건의 현재 단계를 보여주려면 청구 목록 조회 후 각 청구마다 조사/산출/지급 조회를 추가 호출해야 한다.
  - 후보 작업: 프론트가 단계 표시를 계산할 수 있도록 청구별 조사/산출/지급 조회 정책과 상태 전이 규칙을 문서화.

- **FE-CLAIM-02 / 다음 단계 버튼 노출 조건이 응답에 없음**
  - 확인 위치: `InvestigationResponse.status/result`, `CalculationResponse.status`, `PaymentResponse.status`
  - 산출 생성 가능 여부, 산출 승인 가능 여부, 지급 생성 가능 여부, 지급 실행 가능 여부를 프론트가 상태값으로 직접 해석해야 한다.
  - 후보 작업: 화면 버튼 판단은 프론트 책임으로 두고, 상태 전이 표를 문서화.

- **FE-CLAIM-03 / claim 목록 API 페이지네이션** ✅ 1차 완료
  - 확인 위치: `ClaimController.list()`, `AccidentController.list()`, `DispatchController.list()`
  - 사고/청구/출동 목록은 `page/size/total/items` 응답으로 통일했다.
  - 고객 조회가 필요한 사고/청구 목록은 DB pagination에서 고객 조건을 적용한다.
  - 남은 작업: `status`, `customerId`, `contractNo` 같은 추가 필터 query 지원 검토.

- **FE-CLAIM-04 / 사고 접수와 보험금 청구의 연결 관계가 약함**
  - 확인 위치: `AccidentCreateRequest`, `ClaimCreateRequest`
  - 사고 접수는 `accidentNo`를 만들지만, 청구 등록 요청에는 `accidentNo`가 없다.
  - 프론트에서 “사고 접수 후 해당 사고로 보험금 청구” 흐름을 만들면 서버에는 두 업무를 연결할 수 있는 필드가 없다.
  - 현재 구조에서는 청구가 계약 기준으로만 생성된다.
  - 후보 작업: 실제 업무 흐름에서 사고와 청구를 연결해야 한다면 `ClaimCreateRequest.accidentNo` 또는 별도 매핑을 검토.

- **FE-CLAIM-05 / 출동 사진 응답이 파일명만 제공됨**
  - 확인 위치: `DispatchRecordResponse.photoNames`, `DispatchRecordService.storePhotos()`
  - 출동 기록 응답은 `photoNames`만 내려준다.
  - 프론트가 업로드된 사진을 미리보기/상세에서 표시하려면 다운로드 URL 또는 정적 파일 제공 경로가 필요하다.
  - 현재 로컬 파일 저장소 기준으로도 `/api/.../photos/{fileName}` 같은 조회 endpoint가 없다.
  - S3 전환 시에는 S3 object key 또는 signed URL 정책이 필요하다.

- **FE-CLAIM-06 / 파일 업로드 재시도와 중복 파일명 정책이 불명확함**
  - 확인 위치: `DispatchRecordService.storePhotos()`
  - 원본 파일명을 그대로 저장하고, 같은 record 디렉터리에 같은 이름이 있으면 덮어쓸 수 있다.
  - 프론트가 같은 파일명으로 재업로드하거나 네트워크 실패 후 재시도할 때 결과가 예측하기 어렵다.
  - 후보 작업: UUID 기반 저장명, 원본 파일명 별도 보관, 실패 시 정리 정책 추가.

- **FE-CLAIM-07 / 청구 계좌 예금주 입력값이 서버에서 그대로 쓰이지 않음**
  - 확인 위치: `ClaimCreateRequest.accountHolder`, `ClaimRequestService.create()`
  - 요청 DTO에는 `accountHolder`가 있지만, 서비스는 `bankName`, `accountNo`만 계좌 등록에 넘긴다.
  - 프론트에서 예금주 입력란을 보여주면 사용자가 입력한 값과 저장/응답 값이 다를 수 있다.
  - 후보 작업: 예금주가 고객명 고정이면 프론트에서 read-only로 보여주거나 요청 DTO에서 제외. 입력값을 허용한다면 서비스/엔터티 반영.

- **FE-CLAIM-08 / 지급 endpoint 경로가 납입 payment와 의미상 충돌 가능**
  - 확인 위치: `ClaimPaymentController.execute()`, `PaymentController`, `RefundController`
  - 보험료 납입은 `/api/payments`, 보험금 지급 실행은 `/api/payments/{paymentNo}/execute`를 사용한다.
  - 프론트 라우팅/클라이언트 모듈에서 premium payment와 claim payment가 같은 `payments` namespace로 보여 혼동될 수 있다.
  - 후보 작업: 장기적으로 `/api/claim-payments/{paymentNo}/execute` 같은 명확한 경로 검토.

- **FE-CLAIM-09 / 예약 지급 실행 정책이 화면과 맞지 않을 수 있음**
  - 확인 위치: `ClaimPaymentService.execute()`
  - 지급 생성 시 `paymentType=SCHEDULED`이면 예약 상태가 되지만, execute API는 예약 시각 도래 여부를 확인하지 않는다.
  - 프론트에서 예약 지급을 만들고 바로 실행 버튼을 누르면 예약 의미가 약해진다.
  - 후보 작업: 예약 지급은 scheduledAt 이후에만 실행 허용하거나, 수동 실행 버튼을 숨기고 배치/관리자 전용으로 분리.

- **FE-CLAIM-10 / enum 값 출처 없음**
  - 확인 위치: `AccidentCreateRequest.accidentType`, `ClaimCreateRequest.claimType/authMethod`, `InvestigationCreateRequest.result`, `PaymentCreateRequest.paymentType`
  - 프론트가 select/radio 옵션을 만들려면 enum 값을 하드코딩해야 한다.
  - 후보 작업: 도메인별 API 명세에 허용값 정리.

- **FE-CLAIM-11 / 출동 목록·기록 API 직원 role 검증 완료**
  - 확인 위치: `DispatchRecordService.listDispatches()`, `DispatchRecordService.create()`, `DispatchRecordService.findByDispatchNo()`
  - 출동 목록/기록 등록/상세는 `DISPATCH_STAFF`, `CLAIM_STAFF`, `ADMIN`으로 제한했다.
  - 남은 작업: 고객에게 출동 처리 현황을 보여줄 별도 요약 API 필요 여부 검토.

- **FE-CLAIM-12 / 조사·산출·지급 API 업무 role 검증 완료**
  - 확인 위치: `DamageInvestigationService`, `ClaimCalculationService`, `ClaimPaymentService`
  - 손해조사/보험금 산출은 `CLAIM_STAFF`, `ADMIN`으로 제한했다.
  - 보험금 지급 생성/실행은 `FINANCE_STAFF`, `CLAIM_STAFF`, `ADMIN`으로 제한했다.
  - 남은 작업: 고객용 청구 진행 상태 조회 API 필요 여부 검토.

- **FE-CLAIM-13 / 조사 담당자 입력이 로그인 사용자와 연결되지 않음**
  - 확인 위치: `InvestigationCreateRequest.handlerEmpId`, `InvestigationCreateRequest.handlerName`, `DamageInvestigationService.create()`
  - 조사 담당자는 request body의 `handlerEmpId`, `handlerName`으로 만들어진 `ClaimsHandler` 셸 객체에 의존한다.
  - 프론트가 담당자 값을 임의로 보내면 실제 로그인 직원과 처리자가 달라질 수 있다.
  - 후보 작업: 1차는 로그인 사용자 displayName을 처리자로 사용하거나, 직원 actor 연결이 생긴 뒤 서버에서 담당자를 결정.

- **FE-CLAIM-14 / claim 지급 실행 경로가 보험료 납입 경로와 실제로 충돌함**
  - 확인 위치: `ClaimPaymentController.execute()`, `PaymentController`
  - 보험금 지급 실행은 `POST /api/payments/{paymentNo}/execute`이고, 보험료 납입 생성은 `POST /api/payments`이다.
  - 현재는 메서드/경로가 달라 라우팅 충돌은 없지만, 프론트 API 모듈에서는 premium payment와 claim payment가 같은 namespace로 묶여 혼동될 수 있다.
  - 후보 작업: API 명세에서 두 payment 개념을 분리해 적고, 장기적으로 `/api/claim-payments/{paymentNo}/execute`로 변경 검토.

- **FE-CLAIM-15 / 출동 사진 저장 실패 시 DB와 파일의 원자성이 보장되지 않음**
  - 확인 위치: `DispatchRecordService.create()`, `DispatchRecordService.storePhotos()`
  - 출동 기록을 먼저 DB에 저장한 뒤 파일을 로컬 디스크에 저장하고, 이후 사진 메타를 저장한다.
  - 파일 저장 중 예외가 나면 DB 트랜잭션은 롤백될 수 있지만 이미 저장된 파일 정리는 별도로 수행되지 않는다.
  - S3로 전환해도 DB와 object storage는 같은 트랜잭션으로 묶이지 않으므로 실패 보상 정책이 필요하다.
  - 후보 작업: 저장 파일명 UUID화, 실패 시 업로드된 파일 삭제, S3 전환 시 object key/상태값 기반 보상 로직 설계.

### 우선순위 제안

1. 출동/조사/산출/지급 API role 검증 보강
2. 청구 상태 전이 규칙과 단계 계산 방식 문서화
3. 출동 사진 조회 URL 또는 S3 전환 후 파일 접근 정책 결정
4. claim 목록 필터/페이지네이션 추가 여부 결정
5. 사고 접수와 청구를 연결할지 업무 정책 결정
6. claim payment endpoint namespace 정리 여부 결정

---

## 다음 검토 예정

## 3. consultation

상태: 프론트 연동 관점 1차 검토 완료

### 확인한 엔드포인트

상담:

- `GET /api/consultations`
- `GET /api/consultations/{consultNo}`
- `POST /api/consultations`
- `POST /api/consultations/{consultNo}/accept`

보험상품/제안:

- `GET /api/insurance-products`
- `GET /api/proposals`
- `POST /api/proposals`

면담 일정/기록:

- `GET /api/interview-schedules`
- `GET /api/interview-schedules/{scheduleNo}`
- `POST /api/interview-schedules`
- `PUT /api/interview-schedules/{scheduleNo}`
- `POST /api/interview-schedules/{scheduleNo}/cancel`
- `GET /api/interview-records`
- `GET /api/interview-records/{recordNo}`
- `POST /api/interview-records`
- `PUT /api/interview-records/{recordNo}`

청약/보험신청/부활:

- `POST /api/policy-applications`
- `POST /api/insurance-applications`
- `POST /api/revivals`

인수심사:

- `GET /api/underwriting/pending`
- `POST /api/underwriting`

### 프론트 화면 흐름 적합성

- 상담 접수와 수락 화면은 구현 가능하다.
  - 상담 목록/상세/생성/수락 API가 있다.
  - 수락은 별도 액션 API로 분리되어 버튼 처리에 적합하다.

- 보험상품 목록과 제안서 발송 화면은 구현 가능하다.
  - 상품 목록을 불러오고 상품명을 선택해 제안서를 만들 수 있다.
  - 제안 목록도 제공된다.

- 면담 일정과 면담 기록은 일반 CRUD 화면으로 구현하기 쉽다.
  - 일정은 생성/수정/취소가 있고, 기록은 생성/수정이 있다.
  - 응답에 등록/수정/취소 시각이 포함되어 이력 표시가 가능하다.

- 인수심사는 pending 목록을 기반으로 구현할 수 있다.
  - `GET /api/underwriting/pending`이 청약과 보험신청을 합쳐 내려준다.
  - 프론트는 pending 목록에서 한 건을 선택한 뒤 `POST /api/underwriting`으로 심사 결과를 제출하는 흐름을 만들 수 있다.

### 발견 사항

- **FE-CONSULT-01 / 청약·보험신청·부활은 생성 API만 있음**
  - 확인 위치: `PolicyApplicationController`, `InsuranceApplicationController`, `RevivalController`
  - 청약서, 보험신청, 부활신청은 `POST`만 있고 목록/상세 조회 API가 없다.
  - 생성 직후 완료 화면은 응답으로 처리 가능하지만, 새로고침 후 상세 확인이나 신청 이력 화면을 만들기 어렵다.
  - 후보 작업: `GET /api/policy-applications`, `GET /api/policy-applications/{applicationNo}`, `GET /api/insurance-applications`, `GET /api/revivals` 추가 검토.

- **FE-CONSULT-02 / pending 목록의 applicationType이 한글 문자열임**
  - 확인 위치: `PendingApplicationResponse.applicationType`, `UnderwritingService.findPending()`
  - pending 목록은 `applicationType`을 `"청약"`, `"보험신청"`으로 내려준다.
  - 화면 표시에는 좋지만, 프론트 분기값으로 쓰기에는 타입 안정성이 낮다.
  - 후보 작업: `applicationTypeCode`는 `POLICY_APPLICATION`/`INSURANCE_APPLICATION`처럼 고정 코드로 주고, 표시용 label을 별도 제공.

- **FE-CONSULT-03 / 인수심사 결과 저장과 원본 신청 상태 갱신의 성공 여부가 분리됨**
  - 확인 위치: `UnderwritingService.complete()`
  - 인수심사 이력은 먼저 저장되고, 원본 신청 상태 갱신은 pending 목록에서 찾아 `ifPresent()`로 수행된다.
  - pending 목록에 없는 `appNo`가 들어오면 심사 결과는 저장되지만 원본 신청 상태는 바뀌지 않을 수 있다.
  - 프론트에서는 심사 완료 응답을 받았는데 pending 목록에 계속 남거나 원본 상태가 변하지 않는 혼선이 생길 수 있다.
  - 후보 작업: 원본 신청 건을 먼저 조회/검증하고, 없으면 404/400으로 막은 뒤 같은 트랜잭션에서 심사 저장과 상태 갱신 수행.

- **FE-CONSULT-04 / UnderwritingRequest.riskGrade가 응답에 기대한 대로 반영되지 않을 수 있음**
  - 확인 위치: `UnderwritingRequest.riskGrade`, `UnderwritingService.complete()`
  - 요청 DTO는 `riskGrade`를 받지만 서비스는 해당 값을 직접 사용하지 않는다.
  - 프론트에서 위험등급 입력 필드를 만들면 사용자가 입력한 값이 저장/응답에 반영되지 않는 것처럼 보일 수 있다.
  - 후보 작업: riskGrade가 자동 산출값인지 수동 입력값인지 정책 결정 후 DTO/서비스/화면 정리.

- **FE-CONSULT-05 / 고객·상품 선택이 이름 기반으로 섞여 있음**
  - 확인 위치: `ProposalCreateRequest`, `PolicyApplicationRequest`, `InsuranceApplicationRequest`
  - 일부 API는 `customerId`를 받지만, 제안서는 `customerName`과 `productName`만 받는다.
  - 상품도 `productName`으로 조회한다.
  - 프론트에서 select box를 만들 수는 있지만, 이름 중복이나 이름 변경에 취약하다.
  - 후보 작업: 상품에는 `productNo` 같은 안정 식별자를 제공하고, 제안/청약/보험신청은 가능한 ID 기반으로 연결.

- **FE-CONSULT-06 / 상담·면담 목록 페이지네이션** ✅ 1차 완료
  - 확인 위치: `ConsultationController.findAll()`, `InterviewScheduleController.findAll()`, `InterviewRecordController.findAll()`
  - 상담, 면담 일정, 면담 기록, 제안, 인수심사 대기 목록은 `page/size/total/items` 응답으로 통일했다.
  - 인수심사 대기 목록은 `policy_applications`와 `insurance_applications`를 `UNION ALL`로 합쳐 DB pagination 한다.
  - 남은 작업: `status`, `from`, `to`, `customerName`, `designerName` 같은 추가 필터 query 지원 검토.

- **FE-CONSULT-07 / 면담 일정과 면담 기록이 직접 연결되지 않음**
  - 확인 위치: `InterviewScheduleCreateRequest`, `InterviewRecordCreateRequest`
  - 면담 기록 생성 요청에 `scheduleNo`가 없다.
  - 프론트에서 일정 상세에서 바로 면담 기록을 작성해도 서버 데이터상 어느 일정에서 파생된 기록인지 연결되지 않는다.
  - 후보 작업: `InterviewRecordCreateRequest.scheduleNo` 추가 또는 일정-기록 매핑 정책 결정.

- **FE-CONSULT-08 / 상태값과 옵션값 출처 없음**
  - 확인 위치: 상담 type/status, interviewType/status, paymentMethod, underwriting result/reviewType
  - 프론트가 상담 유형, 면담 유형, 납입방법, 심사 결과 옵션을 하드코딩해야 한다.
  - 특히 심사 결과는 `"조건부승인"`, `"거절"` 같은 한글 문자열로 검증된다.
  - 후보 작업: enum/옵션 API 또는 도메인별 상수 문서화. 장기적으로는 code/label 분리.

- **FE-CONSULT-09 / 신청 완료 후 다음 단계 안내 정보가 부족함**
  - 확인 위치: `PolicyApplicationResponse`, `InsuranceApplicationResponse`
  - 청약/보험신청 생성 응답에는 신청번호와 상태는 있지만, 인수심사 pending으로 넘어간다는 다음 액션 정보는 없다.
  - 프론트는 신청 완료 후 “심사 대기 목록으로 이동” 같은 UX를 별도 지식으로 처리해야 한다.
  - 후보 작업: 화면 이동 안내는 프론트 책임으로 두고, 명세 문서에 workflow 표 추가.

- **FE-CONSULT-10 / 상담·제안·면담 API 권한 검증 완료**
  - 확인 위치: `ConsultationService`, `ProposalService`, `InterviewScheduleService`, `InterviewRecordService`
  - 상담 목록/상세/수락은 `SALES_STAFF`, `UNDERWRITING_STAFF`, `ADMIN`으로 제한했다.
  - 제안/면담 관리는 `SALES_STAFF`, `UNDERWRITING_STAFF`, `ADMIN`으로 제한했다.
  - 상담 생성은 로그인 사용자가 요청할 수 있는 흐름으로 유지했다.
  - 남은 작업: 상담 신청과 고객 master 연결 여부 결정.

- **FE-CONSULT-11 / 상담 신청이 고객 master와 연결되지 않음**
  - 확인 위치: `ConsultationCreateRequest`, `ConsultationService.create()`, `ConsultationRequest`
  - 상담 신청은 연락처와 내용 중심으로 저장되고, 로그인 고객 또는 `customers.id`와 연결되는 흐름이 보이지 않는다.
  - 프론트에서 “내 상담 내역” 화면을 만들려면 고객 소유권 기준으로 필터링할 수 있는 식별자가 필요하다.
  - 후보 작업: 상담 요청에 `customer_id`를 추가하거나, 로그인 고객이 상담 신청 시 서버가 현재 세션의 linked customer를 저장.

- **FE-CONSULT-12 / 제안서가 customerName/productName 기반이라 안정 식별자가 부족함**
  - 확인 위치: `ProposalCreateRequest`, `ProposalService.create()`
  - 제안서는 고객명과 상품명으로 생성된다.
  - 고객명/상품명이 중복되거나 변경되면 프론트가 정확한 대상을 선택하기 어렵다.
  - 후보 작업: 제안서 생성은 `customerId`, `productNo` 또는 상품 `id` 기반으로 받고, 응답에는 표시용 이름을 함께 제공.

- **FE-CONSULT-13 / 인수심사 API 심사 담당 role 검증 완료**
  - 확인 위치: `UnderwritingService.findPending()`, `UnderwritingService.complete()`
  - pending 목록 조회와 심사 완료는 `UNDERWRITING_STAFF`, `ADMIN`으로 제한했다.
  - 남은 작업: `SALES_STAFF`에게 pending 조회만 허용할지 정책 검토.

- **FE-CONSULT-14 / 심사 요청 값이 한글 문자열 중심이라 프론트 분기 안정성이 낮음**
  - 확인 위치: `UnderwritingRequest.applicationType`, `UnderwritingRequest.result`, `UnderwritingRequest.reviewType`
  - 인수심사 완료 요청은 `"청약"`, `"보험신청"`, `"조건부승인"`, `"거절"` 같은 한글 문자열에 의존한다.
  - 화면 표시에는 자연스럽지만, 프론트 request 값과 서버 분기값으로는 오타와 다국어 변경에 취약하다.
  - 후보 작업: request code는 `POLICY_APPLICATION`, `INSURANCE_APPLICATION`, `APPROVED`, `CONDITIONAL_APPROVED`, `REJECTED`처럼 고정하고 label은 프론트 표시값으로 분리.

- **FE-CONSULT-15 / 청약·보험신청·부활 생성에는 소유권 검증이 있지만 조회 API가 없음**
  - 확인 위치: `PolicyApplicationService`, `InsuranceApplicationService`, `RevivalService`
  - 생성 단계에는 `AuthAccessService.requireCustomerAccess()` 또는 계약 접근 검증이 들어가 있다.
  - 그러나 생성 이후 고객이 본인 신청 내역을 다시 조회할 목록/상세 API가 없다.
  - 후보 작업: 고객 본인 신청 목록/상세와 직원 전체 신청 목록/상세를 role/소유권 기준으로 분리해 추가.

### 우선순위 제안

1. 상담·제안·면담·인수심사 API role/소유권 검증 보강
2. 상담 요청과 고객 master 연결 여부 결정
3. 청약·보험신청·부활 목록/상세 조회 API 추가 여부 결정
4. 인수심사 pending의 `applicationType` code/label 분리
5. 인수심사 완료 시 원본 신청 존재 검증 및 상태 갱신 보장
6. 상담/면담 목록 필터와 페이지네이션 추가 여부 결정
7. 면담 일정과 면담 기록 연결 정책 결정

## 4. sales

상태: 프론트 연동 관점 1차 검토 완료

### 확인한 엔드포인트

활동계획:

- `GET /api/activity-plans`
- `GET /api/activity-plans/{planNo}`
- `POST /api/activity-plans`

영업활동 관리:

- `GET /api/sales-activity-managements?startDate=&endDate=&channelType=&page=&size=`
- `POST /api/sales-activity-managements`

채널 모집/심사:

- `GET /api/channel-recruitments`
- `POST /api/channel-recruitments`
- `GET /api/channel-screenings`
- `POST /api/channel-screenings`
- `POST /api/channel-screenings/{screeningNo}/approve`
- `POST /api/channel-screenings/{screeningNo}/reject`

고객 등록:

- `GET /api/customer-registrations`
- `POST /api/customer-registrations`

영업조직 평가/성과급:

- `GET /api/sales-org-evaluations?startDate=&endDate=&channelType=&page=&size=`
- `POST /api/sales-org-evaluations`
- `POST /api/bonus-requests`

### 프론트 화면 흐름 적합성

- 영업활동 관리와 영업조직 평가는 테이블 화면 구현에 비교적 적합하다.
  - 두 목록은 `page`, `size`, `total`, `items` 구조를 반환한다.
  - 기간과 채널 유형 필터도 이미 존재한다.

- 활동계획 작성 화면은 구현 가능하다.
  - 계획 기본 정보와 일정 목록을 한 번에 제출할 수 있다.
  - 상세 조회에서는 schedules가 포함되어 계획 상세/일정 확인 화면을 만들 수 있다.

- 채널 심사는 등록 후 승인/거절 액션이 분리되어 있다.
  - 심사 목록에서 상태에 따라 승인/거절 버튼을 보여주는 화면을 만들 수 있다.

- 성과급 요청은 평가 결과를 보고 신청하는 흐름으로 만들 수 있다.
  - 다만 현재 성과급 요청은 생성 API만 있고 조회 API가 없다.

### 발견 사항

- **FE-SALES-01 / 성과급 요청 조회 API가 없음**
  - 확인 위치: `BonusRequestController`
  - 성과급 요청은 `POST /api/bonus-requests`만 제공된다.
  - 프론트에서 성과급 요청 이력, 상세, 제출 완료 후 새로고침 화면을 만들 수 없다.
  - 후보 작업: `GET /api/bonus-requests`, `GET /api/bonus-requests/{requestNo}` 추가 검토.

- **FE-SALES-02 / 평가와 성과급 요청 연결 검증이 약함**
  - 확인 위치: `BonusRequestService.create()`, `BonusRequestRepository.save()`
  - 요청에는 `evaluationNo`가 있지만 서비스에서 실제 평가 존재 여부를 확인하지 않는다.
  - 프론트는 평가 목록에서 선택해 요청하더라도, 오래된 화면 상태나 직접 호출로 잘못된 평가번호가 들어갈 수 있다.
  - 후보 작업: `evaluationNo`가 있으면 `SalesOrgEvaluationRepository`로 선조회하고, 없는 평가는 404/400 반환.

- **FE-SALES-03 / 성과급 요청 화면에서 평가 정보를 중복 입력해야 함**
  - 확인 위치: `BonusRequestRequest`
  - 성과급 요청은 `evaluationNo`와 함께 `channelName`, `channelType`, `evaluationGrade`, `baseSalary`를 다시 받는다.
  - 프론트가 평가 목록에서 선택한 값을 그대로 복사해 보내야 하며, evaluationNo와 나머지 값이 서로 불일치할 수 있다.
  - 후보 작업: `evaluationNo`, `baseSalary`, `requestReason`만 받고 평가 정보는 서버에서 조회해 채우는 방식 검토.

- **FE-SALES-04 / 활동계획 목록의 schedules가 비어 보일 수 있음**
  - 확인 위치: `ActivityPlanRepository.findAll()`, `ActivityPlanRepository.findById()`
  - 단건 조회는 `loadSchedules()`를 호출하지만 목록 조회는 schedule을 로드하지 않는다.
  - `ActivityPlanResponse`에는 `schedules` 필드가 있으므로 목록 화면에서 빈 배열로 보일 수 있다.
  - 후보 작업: 목록 응답을 요약 DTO로 분리하거나, 목록에도 schedule 요약을 포함할지 정책 결정.

- **FE-SALES-05 / 활동계획 목록 페이지네이션** ✅ 1차 완료
  - 확인 위치: `ActivityPlanController.findAll()`
  - 활동계획 목록은 `page/size/total/items` 응답으로 통일했고 DB pagination을 적용했다.
  - 남은 작업: `status`, `author`, `from`, `to` 같은 추가 필터 query 지원 검토.

- **FE-SALES-06 / 채널모집·채널심사·고객등록 목록 응답** ✅ 완료
  - 확인 위치: `ChannelRecruitmentController.findAll()`, `ChannelScreeningController.findAll()`, `CustomerRegistrationController.findAll()`
  - 활동계획, 채널모집, 채널심사, 고객등록, 영업활동, 조직평가 목록은 모두 `page/size/total/items` 응답으로 통일했다.
  - DB pagination을 적용해 sales 테이블 컴포넌트 공통화가 쉬워졌다.

- **FE-SALES-07 / 채널심사 버튼 가능 여부가 응답에 없음**
  - 확인 위치: `ChannelScreeningResponse.status`, `ChannelScreeningService.approve/reject()`
  - 서버는 `PENDING` 상태에서만 승인/거절을 허용한다.
  - 프론트는 `status`를 직접 해석해서 버튼을 숨겨야 한다.
  - 후보 작업: 화면 버튼 판단은 프론트 책임으로 두고, 상태 전이 표를 문서화.

- **FE-SALES-08 / SalesActivityManagement channelType 검증이 엔터티에 숨어 있음**
  - 확인 위치: `SalesActivityManagementService.create()`, `SalesActivityManagement.setActivityType()`
  - 서비스는 channelType의 enum 유효성을 직접 검증하지 않고 엔터티 메서드에 문자열을 넘긴다.
  - 잘못된 channelType이 들어오면 명확한 400 응답 대신 null 저장 가능성이 있다.
  - 프론트 개발 중 옵션값 오타를 빠르게 발견하기 어렵다.
  - 후보 작업: 다른 sales 서비스처럼 Service에서 `ChannelType.valueOf()`로 명시 검증.

- **FE-SALES-09 / SalesActivityManagement 날짜 범위 검증이 없음**
  - 확인 위치: `SalesActivityManagementService.create()`
  - `startDate`, `endDate` 필수 검증은 있지만 `endDate`가 `startDate` 이후인지 확인하지 않는다.
  - 프론트가 잘못된 날짜를 보내도 저장될 수 있다.
  - 후보 작업: 동일일 허용 여부를 정하고 날짜 범위 검증 추가.

- **FE-SALES-10 / 고객등록은 실제 공통 고객과의 관계가 애매함**
  - 확인 위치: `CustomerRegistrationController`, `CustomerRegistrationService`
  - sales의 고객등록 API는 `customer_registrations` 성격의 별도 등록으로 보이며, 공통 `CustomerRepository`의 고객 CRUD와 연결이 명확하지 않다.
  - 프론트 입장에서는 “고객 등록”이 실제 고객 master 생성인지, 영업 등록 이력인지 구분이 필요하다.
  - 후보 작업: 화면명을 “영업 고객 등록 이력”처럼 분리하거나, 공통 고객 CRUD와 연결 정책 정의.

- **FE-SALES-11 / enum 값 출처 없음**
  - 확인 위치: `ChannelType`, `InsuranceType`, `PlanStatus`, `ActivityType`, `EvaluationGrade`, `ScreeningStatus`
  - sales 도메인은 선택 옵션이 많다.
  - 프론트가 하드코딩하면 오타나 상태값 변경에 취약하다.
  - 후보 작업: API 명세에 sales 관련 허용값 정리.

- **FE-SALES-12 / sales 도메인 전반 업무 role 검증 완료**
  - 확인 위치: `domain/sales/service/*`, `domain/sales/controller/*`
  - 활동계획, 영업활동, 채널 모집/심사, 고객등록, 조직평가는 `SALES_STAFF`, `ADMIN`으로 제한했다.
  - 성과급 요청 생성은 `ADMIN`으로 제한했다.
  - 남은 작업: 성과급 요청 목록/상세 조회 API 추가 여부 결정.

- **FE-SALES-13 / 생성 API와 목록 응답 정책** ✅ 목록 정책 완료
  - 확인 위치: `SalesActivityManagementController`, `SalesOrgEvaluationController`, 나머지 sales controller
  - sales 목록 응답은 `page/size/total/items`로 통일했다.
  - 생성 API 201 응답 사용 여부는 목록 정책과 별개로 유지한다.

### 우선순위 제안

1. 성과급 요청 목록/상세 조회 API 추가 여부 결정
2. 성과급 요청을 evaluationNo 기반으로 서버에서 평가 정보 조회하도록 정리
3. 활동계획 목록 DTO와 schedule 포함 정책 결정
4. sales 목록 응답 페이지네이션 통일 여부 결정
5. 채널심사 버튼 가능 여부와 enum 값 제공 방식 결정

## 5. education + inquiry

상태: 프론트 연동 관점 1차 검토 완료

### 확인한 엔드포인트

교육 계획:

- `GET /api/education-plans?status=`
- `GET /api/education-plans/{planNo}`
- `POST /api/education-plans`
- `POST /api/education-plans/{planNo}/approve`
- `POST /api/education-plans/{planNo}/reject`

교육 제반:

- `GET /api/education-preparations?planNo=`
- `GET /api/education-preparations/{prepNo}`
- `POST /api/education-preparations`

교육 실행:

- `GET /api/education-executions?prepNo=`
- `GET /api/education-executions/{executionNo}`
- `POST /api/education-executions`

문의:

- `GET /api/inquiries?customerName=&status=`
- `GET /api/inquiries/{inquiryNo}`
- `POST /api/inquiries`
- `POST /api/inquiries/{inquiryNo}/answer`

### 프론트 화면 흐름 적합성

- 교육 계획은 `임시저장/승인요청 -> 승인/반려` 흐름으로 구현 가능하다.
  - `action` 값으로 임시저장과 승인요청을 구분한다.
  - 승인/반려가 별도 action endpoint로 분리되어 있어 버튼 처리에 맞다.

- 교육 제반과 교육 실행은 `계획 -> 제반 -> 실행` 단계 화면으로 구성할 수 있다.
  - 제반은 `planNo`로 조회 가능하다.
  - 실행은 `prepNo`로 조회 가능하고, 출석 목록을 포함해 등록할 수 있다.

- 문의는 고객 문의 등록과 담당자 답변 화면으로 구현 가능하다.
  - 문의 목록은 고객명/status 필터를 제공한다.
  - 답변 완료 상태에서는 서버가 중복 답변을 막는다.

### 발견 사항

- **FE-EDU-01 / 교육 제반 생성 시 계획안 승인 상태 검증이 부족함**
  - 확인 위치: `EducationPreparationService.createPreparation()`
  - 에러 메시지는 “승인된 교육 계획안”을 말하지만, 실제로는 계획안 존재 여부만 확인한다.
  - 프론트에서 승인된 계획만 선택하게 해도 직접 API 호출이나 오래된 화면 상태에서는 임시저장/승인요청/반려 계획에 제반이 등록될 수 있다.
  - 후보 작업: `plan.status == "승인"`인 경우에만 제반 등록 허용.

- **FE-EDU-02 / 교육 실행 생성 시 제반 상태와 중복 실행 검증이 부족함**
  - 확인 위치: `EducationExecutionService.createExecution()`
  - 실행 생성은 `prepNo` 존재 여부와 출석 목록만 확인한다.
  - 이미 실행된 제반인지, 제반이 등록완료 상태인지 같은 업무 조건은 확인하지 않는다.
  - 프론트에서 실행 버튼을 숨겨도 직접 호출로 중복 실행이 가능할 수 있다.
  - 후보 작업: `prepNo`별 실행 중복 제한 여부와 실행 가능 상태를 정책화.

- **FE-EDU-03 / 교육 계획 action 오타가 임시저장으로 처리됨**
  - 확인 위치: `EducationPlanService.createPlan()`
  - `action`이 정확히 `"REQUEST_APPROVAL"`일 때만 승인요청이고, 나머지는 모두 임시저장 처리된다.
  - 프론트에서 action 오타가 나도 성공 응답이 내려와 디버깅이 어렵다.
  - 후보 작업: 허용값을 `TEMP_SAVE`, `REQUEST_APPROVAL`로 명시 검증하고 알 수 없는 action은 400 반환.

- **FE-EDU-04 / 교육 목록 페이지네이션** ✅ 1차 완료
  - 확인 위치: `EducationPlanController`, `EducationPreparationController`, `EducationExecutionController`
  - 교육 계획/제반/실행 목록은 `page/size/total/items` 응답으로 통일했고 DB pagination을 적용했다.
  - 계획 status, 제반 planNo, 실행 prepNo 조건은 SQL에서 처리한다.
  - 남은 작업: 기간, 강사, 채널 유형 같은 추가 필터 query 검토.

- **FE-EDU-05 / 다음 단계 버튼 가능 여부가 응답에 없음**
  - 확인 위치: `EducationPlanResponse`, `EducationPreparationResponse`, `EducationExecutionResponse`
  - 계획 승인/반려, 제반 등록, 실행 등록 버튼 노출 조건을 프론트가 상태 문자열로 직접 판단해야 한다.
  - 후보 작업: 화면 버튼 판단은 프론트 책임으로 두고, 상태 전이 표를 문서화.

- **FE-EDU-06 / 교육 제반과 실행 응답에 원본 계획 정보가 부족함**
  - 확인 위치: `EducationPreparationResponse`, `EducationExecutionResponse`
  - 제반 응답은 `planNo`만 있고 교육명, 교육 기간, 채널 유형 같은 원본 계획 요약이 없다.
  - 실행 응답도 `prepNo` 중심이며 계획 정보는 별도 조회가 필요하다.
  - 프론트 상세 화면에서는 계획/제반/실행 정보를 합쳐 보여줘야 하므로 추가 호출이 많아진다.
  - 후보 작업: 제반/실행 응답에 원본 계획 요약 필드 추가 또는 상세 합성 endpoint 검토.

- **FE-INQ-01 / InquiryRequest가 enum 타입을 직접 받아 잘못된 JSON 값 처리에 취약함**
  - 확인 위치: `InquiryRequest.inquiryType`, `ApiExceptionHandler`
  - `inquiryType`이 Java enum 타입이므로 잘못된 문자열이 오면 Jackson 역직렬화 단계에서 실패한다.
  - 전역 예외 처리에서 `HttpMessageNotReadableException`을 400으로 다루지 않으면 프론트에는 500처럼 보일 수 있다.
  - 후보 작업: enum 역직렬화 오류를 400으로 처리하거나 DTO를 문자열로 받고 서비스에서 enum 검증.

- **FE-INQ-02 / 문의 status 필터 유효성 검증이 없음**
  - 확인 위치: `InquiryService.getInquiries()`, `InquiryRepository.findByStatus()`
  - `status` query는 문자열 그대로 DB 조회에 사용된다.
  - 잘못된 status 값은 에러가 아니라 빈 목록으로 보일 수 있어 프론트 옵션 불일치를 찾기 어렵다.
  - 후보 작업: `InquiryStatus`로 선검증하거나 알 수 없는 status는 400 반환.

- **FE-INQ-03 / 문의 목록 페이지네이션** ✅ 1차 완료
  - 확인 위치: `InquiryController.list()`
  - 문의 목록은 `page/size/total/items` 응답으로 통일했고 DB pagination을 적용했다.
  - 고객 role은 `customer_id`, 직원/관리자는 `customerName`, `status` 조건으로 SQL filtering 한다.
  - 남은 작업: `inquiryType`, `from`, `to` query 추가 검토.

- **FE-INQ-04 / 답변 버튼 가능 여부가 응답에 없음**
  - 확인 위치: `InquiryResponse.status`, `InquiryService.answer()`
  - 서버는 이미 답변된 문의의 중복 답변을 막는다.
  - 프론트는 `status == ANSWERED`를 직접 해석해 버튼을 숨겨야 한다.
  - 후보 작업: 응답에 `canAnswer` 추가 또는 상태 전이 표 문서화.

- **FE-INQ-05 / 첨부 파일은 메타데이터만 있고 실제 업로드/다운로드 흐름이 없음**
  - 확인 위치: `InquiryRequest.attachmentFileName`, `attachmentFileSize`
  - 문의 등록은 파일명과 크기만 받는다.
  - 프론트가 실제 파일 업로드 UI를 만들려면 multipart 업로드 또는 S3 업로드 정책이 별도로 필요하다.
  - 후보 작업: 문의 첨부 파일 업로드 endpoint, S3 key 저장, 다운로드 URL 정책 검토.

- **FE-EDU-INQ-01 / 상태값과 옵션값 출처 없음**
  - 확인 위치: 교육 status/action/channelType, 문의 InquiryType/InquiryStatus
  - 프론트가 상태/유형 옵션을 하드코딩해야 한다.
  - 교육 상태는 한글 문자열, 문의 상태/유형은 enum name이라 형식도 섞여 있다.
  - 후보 작업: code/label 기반 옵션 API 또는 도메인별 상수 문서화.

- **FE-EDU-INQ-02 / 교육·문의 도메인 role/소유권 검증 완료**
  - 확인 위치: `domain/education/service/*`, `InquiryService`
  - 교육 계획/제반/실행은 `EDUCATION_STAFF`, `ADMIN`으로 제한했다.
  - 문의는 `customer_id`를 연결했고, 고객은 본인 문의만 조회하도록 제한했다.
  - 문의 답변은 직원 또는 관리자만 수행하도록 제한했다.
  - 남은 작업: 문의 목록 페이지네이션과 첨부 파일 정책 결정.

- **FE-INQ-06 / 문의 customer_id 연결 완료**
  - 확인 위치: `InquiryRequest`, `Inquiry`, `InquiryRepository`
  - `inquiries.customer_id` migration을 추가했다.
  - 로그인 고객 문의 생성 시 서버가 현재 세션의 linked customer를 저장한다.
  - 프론트는 고객 세션에서 별도 customerName 필터 없이 내 문의 목록을 구성할 수 있다.

### 우선순위 제안

1. 교육 제반 생성 시 승인 상태 검증 추가 여부 결정
2. 교육 action 허용값 검증과 상태 전이 표 정리
3. 교육/문의 목록 페이지네이션과 필터 확장 여부 결정
4. 문의 첨부 파일 업로드/S3 정책 결정

---

## 다음 검토 예정

다음은 common + auth + customer CRUD 백로그를 정리한다.

중점 확인:

- 지금까지 모든 도메인에서 반복되는 공통 개선 항목을 묶는다.
- 로그인/권한, 고객 CRUD, enum 값 명세, 에러 응답 확장, API 문서화 우선순위를 정한다.
- 프론트가 먼저 붙을 화면 기준으로 1차 수정 범위를 선정한다.

---

## 6. common + auth + customer CRUD 백로그

상태: 1차 정리 완료

### 반복 발견 패턴

- 고객 선택이 필요한 화면은 많은데 공통 고객 검색/상세 API가 부족하다.
- enum/status/action 값을 프론트가 하드코딩해야 하는 구간이 많다.
- 목록 응답은 주요 테이블 API 기준 `page/size/total/items`로 통일됐다.
- 업무 단계 버튼 가능 여부를 프론트가 상태 문자열로 직접 해석해야 한다.
- 일부 잘못된 입력은 400이 아니라 500 또는 빈 목록으로 보일 수 있다.
- 생성 API만 있고 목록/상세 조회가 없어 새로고침 이후 화면 복원이 어려운 도메인이 있다.
- 파일 업로드는 배포와 별개로 S3/URL/메타데이터 정책이 필요하다.

### 1차 수정 우선순위 제안

프론트 연결을 빠르게 시작하기 위한 최소 수정 순서다.

1. **고객 검색/상세 API 추가** ✅ 완료
   - 영향 도메인: contract/payment/claim/consultation/sales
   - 이유: 프론트 대부분의 업무 시작점이 고객 선택이다.
   - 후보 endpoint:
     - `GET /api/customers?keyword=&page=&size=`
     - `GET /api/customers/{customerId}`

2. **enum 값 명세 추가** ✅ 완료
   - 영향 도메인: 전체
   - 이유: 프론트가 API에 보낼 enum code와 화면 label을 확인해야 한다.
   - 결과:
     - `src/main/resources/design/ApiSpec.md`에 주요 enum 입력값을 정리한다.

3. **공통 에러 응답 보강**
   - 영향 도메인: 전체
   - 이유: 프론트 개발 중 입력 오류와 서버 오류를 구분해야 한다.
   - 우선 처리:
     - `HttpMessageNotReadableException` 400 처리
     - `code`, `path`, `fieldErrors` 도입 검토

4. **프론트 첫 화면 기준 목록 페이지네이션 정리** ✅ 완료
   - 영향 도메인: claim, consultation, sales 일부, education, inquiry
   - 이유: 테이블 화면이 배열 전체 응답에 의존하지 않게 한다.
   - 결과:
     - 테이블 목록: `page/size/total/items`
     - 주요 테이블 목록은 DB pagination으로 전환
     - 참조성 소량 목록은 `{ "items": [] }` wrapper 유지

5. **상태 전이 규칙 문서화**
   - 영향 도메인: claim, contract/payment/refund, education, inquiry, sales screening
   - 이유: 화면 흐름은 프론트가 담당하되, 상태별 허용 action은 명확해야 한다.
   - 후보:
     - 상태값별 허용 action 표
     - 잘못된 상태 전이의 공통 에러 응답
     - 프론트에서 참조할 enum 값 또는 API 명세

6. **claim 파일 접근 정책 정리**
   - 영향 도메인: claim, inquiry
   - 이유: 프론트에서 업로드 후 사진/첨부 파일을 볼 수 있어야 한다.
   - 후보:
     - 임시 로컬 URL 제공
     - S3 object key 저장
     - signed URL 제공 여부 결정

7. **도메인별 업무 규칙 보강**
   - 영향 도메인: 전체
   - 이유: 프론트가 막아도 직접 API 호출이나 오래된 화면 상태로 규칙이 깨질 수 있다.
   - 예:
     - 교육 제반은 승인된 계획안만 허용
     - 납입 items 중복 정책 확정
     - 예약 지급 실행 조건 검증
     - 인수심사 원본 신청 존재 검증
     - sales 성과급 요청 evaluationNo 존재 검증

8. **생성 전용 API에 목록/상세 추가**
   - 영향 도메인: consultation, sales bonus
   - 이유: 새로고침 후 완료/상세/이력 화면 복원이 필요하다.
   - 예:
     - `GET /api/policy-applications`
     - `GET /api/insurance-applications`
     - `GET /api/revivals`
     - `GET /api/bonus-requests`

9. **로그인/권한 확장**
   - 영향 도메인: 전체
   - 현재 상태:
     - Flyway, HTTP 세션 로그인, Spring Session JDBC, 세부 직원 role, 주요 고객 데이터 소유권 검증은 구현 완료.
     - contract, payment/refund, claim, consultation, sales, education, inquiry, customer에 role 기반 접근 제한 1차 적용 완료.
   - 남은 후보:
     - role별 API smoke test 시나리오 작성
     - 직원 actor 연결
     - 쿠키가 브라우저 정책 때문에 안정적으로 동작하지 않을 경우 JWT 전환

10. **OpenAPI/Swagger 또는 API 명세 정리**
    - 영향 도메인: 전체
    - 이유: 프론트와 병렬 작업하려면 endpoint와 DTO 예시가 필요하다.
    - enum 값은 option API가 아니라 문서 명세로 먼저 제공한다.

### 1차 구현 묶음 제안

한 번에 모든 문제를 고치지 않고 아래 묶음으로 나누는 것이 좋다.

- **Batch A / 프론트 시작 기반**
  - 고객 검색/상세 API ✅ 완료
  - enum 값 명세 ✅ 완료
  - 공통 에러 응답 400 처리 ✅ 완료

- **Batch B / 첫 화면 테이블 안정화** ✅ 완료
  - claim 목록 DB 페이지네이션
  - inquiry 목록 DB 페이지네이션
  - education 목록 DB 페이지네이션
  - contract/payment/refund/consultation/sales 주요 목록 DB 페이지네이션
  - 배열 목록과 페이지 목록 정책 확정

- **Batch C / 업무 workflow 안정화**
  - claim 상태 전이 규칙 문서화
  - contract/payment/refund 상태 전이 규칙 문서화
  - education 제반/실행 상태 검증
  - sales 성과급 요청 평가 연결 검증

- **Batch D / 파일 업로드**
  - claim 출동 사진 URL 정책
  - inquiry 첨부 업로드 정책
  - S3 저장 구조 설계

- **Batch E / 운영 보강**
  - role별 API smoke test 시나리오 문서화 ✅ 완료
  - role별 API smoke test 수행
  - 직원 actor 연결
  - 세션 쿠키 불안정 시 JWT 전환
  - API 문서화 확장
  - audit/logging 필요 여부 검토

### 다음 작업 제안

다음 작업은 상태 전이 문서화 또는 상세 API 명세 확장 중 하나를 선택한다.

우선 검토할 파일:

- `domain/*/controller`
- `domain/*/dto`
- `src/main/resources/design/ApiSpec.md`
- 상태 전이를 수행하는 `domain/*/service`

진행 원칙:

- 코드 변경은 사용자 승인 후 적용한다.
- 먼저 변경 예정 diff 또는 구체 설계를 보여준다.
- 프론트가 가장 먼저 붙을 화면을 기준으로 범위를 줄인다.
