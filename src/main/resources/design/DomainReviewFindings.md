# 도메인별 코드 리뷰 발견 사항

> 목적: Spring REST API 전환 완료 후 도메인별로 코드, 스키마, API 사용성을 재검토한다.
> 관점: 런타임 버그 가능성, 스키마/Repository 불일치, 예외 처리, format-on-read/id FK 일관성, 프론트 연동 사용성.

## 리뷰 기준

- Controller는 DTO 입출력만 담당하는가
- Service는 검증, 상태 전이, 트랜잭션 경계를 명확히 갖는가
- Repository SQL은 `schema.sql`과 일치하는가
- 업무번호는 저장하지 않고 `id`에서 파생하는가
- FK는 업무번호 문자열이 아니라 부모 `id(BIGINT)`를 쓰는가
- 잘못된 path/query/body 입력이 500이 아니라 4xx로 떨어지는가
- enum/string/null/default 복원이 의도대로 동작하는가
- 프론트가 자연스럽게 목록, 상세, 생성, 다음 단계 이동을 구현할 수 있는 응답을 받는가

---

## 1. 공통 기반

상태: 1차 검토 완료

### 발견 사항

- **COMMON-01 / 프론트 연동 차단 가능성 / CORS 설정 없음**
  - 확인 위치: `src/main/resources/application.yaml`, `src/main/java/org/dpbe`
  - 현재 `@CrossOrigin`, `CorsConfigurer`, Spring Security CORS 설정이 없다.
  - 프론트 개발 서버가 `localhost:3000`, `5173` 등 별도 origin에서 뜨면 브라우저 호출이 CORS로 막힐 수 있다.
  - 문서에도 선택 작업으로 남아 있으므로 프론트 도입 전 글로벌 CORS 설정을 추가하는 것이 좋다.

- **COMMON-02 / 프론트 에러 처리 사용성 / 에러 응답에 path·code가 없음**
  - 확인 위치: `global/exception/ErrorResponse.java`, `ApiExceptionHandler.java`
  - 현재 에러 응답은 `status`, `error`, `message`, `timestamp`만 제공한다.
  - 화면별 분기 처리는 `message` 문자열 의존이 될 가능성이 높다.
  - 프론트에서 안정적으로 처리하려면 선택적으로 `code`, `path`를 추가하거나 도메인별 에러 코드를 정하는 방안을 검토할 만하다.

- **COMMON-03 / 개발 데이터 안정성 / DataSeeder 멱등 기준이 고객 존재 여부 하나**
  - 확인 위치: `global/seed/DataSeeder.java`
  - `customerRepository.findAll()`이 비어 있지 않으면 계약과 보험상품 시드도 모두 건너뛴다.
  - 부분 데이터가 있는 개발 DB에서는 프론트가 상품 목록, 계약 목록을 기대했는데 비어 있는 상태가 될 수 있다.
  - 현재 Docker 볼륨 재생성 전제에서는 문제 가능성이 낮지만, 프론트 개발 편의성을 위해 테이블별 멱등 적재로 바꾸는 선택지가 있다.

- **COMMON-04 / 환경 이식성 / DB 접속 정보가 application.yaml에 고정**
  - 확인 위치: `application.yaml`
  - 로컬 Docker 기준으로는 단순해서 좋지만, 프론트/백엔드 협업 환경이나 배포 환경에서는 환경변수 오버라이드가 필요하다.
  - 즉시 버그는 아니며, 배포 전 설정 외부화 대상으로 분류한다.

### 프론트 연동 메모

- 현재 정상 응답은 도메인별 DTO를 그대로 반환하고, 에러는 공통 `ErrorResponse`로 내려간다.
- 응답 envelope는 아직 없다. 프론트 입장에서는 `response.data`가 바로 도메인 데이터라 사용은 단순하지만, 페이지네이션/공통 code/message 체계를 넣으려면 이후 일괄 변경 비용이 생긴다.
- 브라우저 기반 프론트와 바로 연결하려면 CORS가 첫 번째 선행 작업이다.

---

## 2. contract + payment + customer

상태: 1차 검토 완료

### 발견 사항

- **PAY-01 / 동작 불일치 가능성 / 납입 preview와 submit이 중복 계약 항목을 다르게 처리**
  - 확인 위치: `payment/service/PaymentService.java`
  - `preview()`는 요청 `items`를 순회하며 같은 `contractNo`가 여러 번 있어도 모두 합산한다.
  - `submit()`은 계약 목록을 만든 뒤 `PaymentItem`별 count를 `stream().filter(...).findFirst()`로 찾는다. 같은 계약이 중복되면 뒤쪽 항목의 count가 무시될 수 있다.
  - 프론트에서 실수로 같은 계약을 두 번 담으면 미리보기 금액과 최종 저장 금액이 달라질 수 있다.
  - 권장: Service에서 `contractNo` 중복을 400으로 막거나, 같은 계약을 하나로 병합하는 정책을 명확히 한다.

- **PAY-02 / 잘못된 query 입력 시 500 가능성 / payment-records contractNo 필터 검증 부족**
  - 확인 위치: `PaymentRecordService.getAll()`, `PaymentRecordRepository.findByContractNo()`
  - `GET /api/payment-records?contractNo=abc`처럼 숫자를 파싱할 수 없는 값이 들어오면 Repository의 `Long.parseLong()`이 그대로 터질 수 있다.
  - 프론트가 검색창/필터 값을 그대로 보낼 때 400이 아니라 500이 될 수 있다.
  - 권장: Service에서 contractNo를 먼저 parse/검증하거나 Repository parse 예외를 `ApiException.badRequest`로 감싼다.

- **CONTRACT-01 / 잘못된 path 입력 시 500 가능성 / 일부 parseId null 처리 부족**
  - 확인 위치: `ContractService.parseId()`, `CancellationService.parseId()`, `ExpiringContractManagementService.parseId()`, `RefundService.parseId()`, 일부 Repository `parseId()`
  - `replaceAll()` 전에 null/blank 검증이 없는 메서드가 여럿 있다.
  - 일반적인 path variable은 null이 되기 어렵지만, DTO 필드나 query 필터에서 재사용되는 경우 500으로 새기 쉽다.
  - 권장: 도메인 공통 `parseBusinessId(label, value)` 유틸 또는 각 Service의 동일한 방어 패턴 적용을 검토한다.

- **CONTRACT-02 / 잘못된 body 입력 시 500 가능성 / 만기 안내 고객 응답 null 처리 부족**
  - 확인 위치: `ExpiringContractManagementService.recordResponse()`
  - `CustomerResponse.valueOf(req.customerResponse())`에서 `customerResponse`가 null이면 `NullPointerException`이 발생하고 현재 catch는 `IllegalArgumentException`만 잡는다.
  - 프론트에서 라디오/셀렉트 미선택 상태로 submit하면 400 대신 500이 될 수 있다.
  - 권장: null/blank 선검증 후 enum 파싱.

- **CONTRACT-03 / 프론트 UX 애매함 / 고객 계약 목록에서 고객 존재 여부를 구분하지 않음**
  - 확인 위치: `PaymentService.customerContracts()`
  - `GET /api/customers/{customerId}/contracts`는 고객 존재 여부를 확인하지 않고 계약 목록만 조회한다.
  - 없는 고객과 계약이 없는 고객이 모두 빈 배열로 보인다.
  - 권장: 화면에서 “고객 없음”과 “납입 가능한 계약 없음”을 구분해야 한다면 고객 조회를 먼저 수행해 404를 반환한다.

### 프론트 연동 메모

- 계약 목록/상세, 납입 preview/submit 구조는 프론트가 구현하기 자연스럽다. `GET 계약 목록 → POST preview → POST submit` 흐름이 명확하다.
- 다만 request DTO에 Bean Validation이 없어 프론트 실수 입력이 일부 500으로 새는 구간이 있다. 프론트 개발 편의성을 높이려면 400 응답을 더 일관되게 만드는 작업이 우선이다.
- 목록 API 중 일부는 단순 배열이고, 계약 목록은 `page/size/total/items` 형태다. 프론트에서 공통 리스트 컴포넌트를 만들려면 응답 형태 통일 여부를 결정할 필요가 있다.

---

## 3. claim

상태: 1차 검토 완료

### 발견 사항

- **CLAIM-01 / DTO-서비스 불일치 / 보험금 청구 accountHolder 입력값 미사용**
  - 확인 위치: `ClaimCreateRequest.java`, `ClaimRequestService.create()`, `ClaimRequest.registerNewAccount()`
  - 요청 DTO는 `accountHolder`를 받지만, Service는 `claim.registerNewAccount(request.bankName(), request.accountNo())`만 호출한다.
  - 엔터티는 예금주명을 항상 청구 고객 이름으로 설정한다.
  - 프론트는 예금주 입력란을 보여줘도 서버가 값을 무시하므로, 화면과 실제 저장 결과가 다르게 느껴질 수 있다.
  - 권장: 예금주가 고객명 고정이면 DTO에서 제거하고 프론트는 read-only로 표시한다. 예금주 입력을 허용하려면 엔터티 메서드 시그니처를 확장한다.

- **CLAIM-02 / 파일 업로드 안전성 / 원본 파일명을 그대로 경로에 사용**
  - 확인 위치: `DispatchRecordService.storePhotos()`
  - `MultipartFile.getOriginalFilename()`을 그대로 `dir.resolve(original)`에 사용한다.
  - 브라우저 일반 업로드에서는 보통 파일명만 오지만, 안전하게는 path traversal 문자와 중복 파일명을 정리해야 한다.
  - 권장: `StringUtils.cleanPath`, 허용 문자 치환, UUID prefix, 확장자 보존 정책을 적용한다.

- **CLAIM-03 / 트랜잭션-파일시스템 불일치 / DB 롤백과 업로드 파일 정리가 분리됨**
  - 확인 위치: `DispatchRecordService.create()`
  - DB 기록 저장 후 파일을 저장하고 사진 메타를 INSERT한다. 이후 예외가 나면 DB 트랜잭션은 롤백되지만 이미 저장된 파일은 남을 수 있다.
  - 프론트에서 재시도하면 orphan 파일이 누적될 가능성이 있다.
  - 권장: 실패 시 저장한 파일을 best-effort 삭제하거나, DB 커밋 후 파일 확정 방식/객체 스토리지 업로드 방식을 별도 설계한다.

- **CLAIM-04 / 잘못된 path 입력 시 500 가능성 / 조회 계열에서 Repository parseId 직접 호출**
  - 확인 위치: `DispatchRecordService.findByDispatchNo()`, `ClaimCalculationService.findByInvestigationNo()`, `ClaimPaymentService.findByCalculationNo()` 및 각 Repository `parseId()`
  - 생성/상태전이 경로는 Service에서 parse하는 경우가 많지만, 조회 경로 일부는 Repository의 `Long.parseLong()`에 바로 의존한다.
  - `GET /api/investigations/abc/calculation`, `GET /api/calculations/abc/payment` 같은 요청이 400/404 대신 500으로 떨어질 수 있다.
  - 권장: 조회 메서드도 Service에서 번호 검증을 선행한다. 이번에 `DamageInvestigationService.findByClaimNo()`에 적용한 패턴을 나머지 조회에도 확장하면 된다.

- **CLAIM-05 / 예약 지급 업무 규칙 모호 / SCHEDULED 지급도 즉시 execute 가능**
  - 확인 위치: `ClaimPaymentService.execute()`, `ClaimPayment.execute()`
  - 지급 생성 시 `paymentType=SCHEDULED`이면 상태가 `SCHEDULED`가 되지만, execute는 `COMPLETED`만 차단하고 예약 시각 도래 여부를 확인하지 않는다.
  - 프론트에서 예약 지급을 만든 직후 execute 버튼을 호출하면 예약을 무시하고 완료 처리될 수 있다.
  - 권장: `SCHEDULED` 상태는 `scheduledAt <= now`일 때만 execute 허용하거나, 별도 배치/스케줄러 실행 전용으로 분리한다.

### 프론트 연동 메모

- 청구 흐름은 `사고접수 → 출동기록 → 청구 → 조사 → 산출 → 지급생성 → 지급실행`으로 API 단계가 비교적 명확하다.
- multipart 출동 기록 API는 프론트에서 `FormData`로 구현하면 자연스럽다. 다만 파일명/실패 재시도 정책이 서버에 더 명확해야 운영 중 혼선이 적다.
- claim 쪽도 일부 조회 API가 단순 배열/단건 직접 반환이다. 프론트에서 단계별 워크플로우를 만들려면 각 응답에 다음 단계 가능 여부(`canCreateCalculation`, `canCreatePayment` 등)를 별도로 계산해야 한다.

---

## 4. consultation

상태: 1차 검토 완료

### 발견 사항

- **CONSULT-01 / 데이터 정합성 / 인수심사 대상 신청번호가 없어도 심사 결과가 저장될 수 있음**
  - 확인 위치: `UnderwritingService.complete()`
  - `applicationType`이 `청약` 또는 `보험신청`이면 `findPending()` 목록에서 `appNo`를 찾아 상태를 갱신한다.
  - 하지만 대상 신청번호가 pending 목록에 없으면 `ifPresent()`가 아무 일도 하지 않고, 인수심사 이력(`underwritings`)은 이미 저장된다.
  - 프론트에서는 심사 완료 응답을 받았는데 원본 신청 상태가 바뀌지 않는 불일치가 생길 수 있다.
  - 권장: 원본 신청 건을 먼저 찾고 없으면 404/400을 반환한 뒤, 찾은 경우에만 심사 이력 저장 + 상태 갱신을 같은 트랜잭션에서 수행한다.

- **CONSULT-02 / DTO-서비스 불일치 / UnderwritingRequest.riskGrade 입력값 미사용**
  - 확인 위치: `UnderwritingRequest.java`, `UnderwritingService.complete()`, `Underwriting.java`
  - 요청 DTO는 `riskGrade`를 받지만 Service는 이를 사용하지 않는다.
  - 자동 심사면 엔터티가 `"일반"`으로 고정하고, 수동 심사면 riskGrade가 설정되지 않는다.
  - 프론트에서 위험등급 필드를 입력해도 응답/저장 결과에 반영되지 않을 수 있다.
  - 권장: riskGrade를 서버 산출값으로 둘지, 심사자가 입력하는 값으로 둘지 결정하고 DTO/서비스를 맞춘다.

- **CONSULT-03 / 잘못된 path 입력 시 500 가능성 / parseId null 처리 패턴 반복**
  - 확인 위치: `ConsultationService`, `InterviewScheduleService`, `InterviewRecordService`, `RevivalService`
  - `parseId()` 계열이 `replaceAll()` 전에 null/blank 검증을 하지 않는 패턴이 반복된다.
  - path variable은 일반적으로 null이 아니지만, 잘못된 값에 대한 상태 코드가 404와 400 사이에서 섞여 있고 공통 정책이 없다.
  - 권장: 업무번호 포맷 오류는 400, 존재하지 않는 번호는 404로 통일한다.

### 프론트 연동 메모

- 상담/면담 CRUD는 프론트에서 사용하기 단순하다. 목록, 상세, 생성, 수정, 취소 API가 명확하다.
- 인수심사 pending 목록은 `applicationType`이 한글 문자열(`청약`, `보험신청`)이다. UI 표시에는 편하지만 API 분기값으로도 쓰이므로 프론트 타입 안정성은 낮다.
- 청약/보험신청 생성 후 상세 조회 API가 거의 없고 pending 목록 중심이다. 신청 완료 화면이나 신청 상세 페이지가 필요하면 단건 조회 endpoint가 추가로 필요할 수 있다.

---

## 5. sales

상태: 1차 검토 완료

### 발견 사항

- **SALES-01 / 입력 검증 누락 / 영업활동관리 channelType이 잘못돼도 null 저장 가능**
  - 확인 위치: `SalesActivityManagementService.create()`, `SalesActivityManagement.setActivityType()`
  - Service는 `channelType` null 여부만 확인하고 enum 유효성은 검사하지 않는다.
  - 엔터티의 `setActivityType()`은 `ChannelType.valueOf()` 실패를 무시한다.
  - 프론트가 잘못된 channelType을 보내도 400이 아니라 `activity_type = null`로 저장될 수 있다.
  - 권장: 다른 sales 서비스들과 동일하게 Service에서 `ChannelType.valueOf()`를 직접 검증하고 실패 시 400을 반환한다.

- **SALES-02 / 입력 검증 누락 / 영업활동관리 날짜 범위 검증 없음**
  - 확인 위치: `SalesActivityManagementService.create()`
  - `startDate`, `endDate` 필수 검증은 있지만 `endDate`가 `startDate` 이후인지 확인하지 않는다.
  - 다른 도메인의 모집/활동계획은 날짜 범위를 검증하므로 정책이 불일치한다.
  - 권장: `endDate.isBefore(startDate)` 또는 동일일 허용 여부를 결정해 400 처리한다.

- **SALES-03 / FK 오류가 500으로 노출 가능 / 성과급 요청 evaluationNo 존재 검증 없음**
  - 확인 위치: `BonusRequestService.create()`, `BonusRequestRepository.save()`
  - `evaluationNo`는 선택값처럼 동작하지만, 값이 있고 숫자 파싱에 성공하면 그대로 `evaluation_id` FK로 저장한다.
  - 해당 `sales_org_evaluations.id`가 없으면 DB FK 예외가 500으로 반환될 수 있다.
  - 권장: evaluationNo를 필수로 할지 선택으로 둘지 정하고, 값이 있으면 `SalesOrgEvaluationRepository.findById()` 같은 조회로 선검증한다.

- **SALES-04 / 목록 응답 사용성 / 활동계획 목록에는 schedule이 비어 있음**
  - 확인 위치: `ActivityPlanRepository.findAll()`, `ActivityPlanResponse.from()`
  - 단건 조회는 `loadSchedules()`를 호출하지만, 목록 조회는 계획 본문만 읽는다.
  - 응답 DTO에는 `schedules`가 항상 포함되므로 목록 API에서 빈 배열로 보인다.
  - 프론트에서 목록 행 확장이나 캘린더 미리보기를 기대하면 단건 상세를 추가 호출해야 한다.
  - 권장: 현재 목록은 요약 응답으로 분리하거나, 문서에 “목록의 schedules는 비어 있음, 상세 조회 사용”을 명시한다.

### 프론트 연동 메모

- 영업 도메인은 생성 API와 목록 API 중심이라 폼 화면은 붙이기 쉽다.
- enum 입력값(`ChannelType`, `InsuranceType`, `PlanStatus`, `ActivityType`, `EvaluationGrade`)이 많다. 프론트가 하드코딩하지 않도록 enum 옵션 API나 문서화된 상수 표가 있으면 좋다.
- 성과급 요청은 “평가 목록에서 특정 평가를 선택 → 성과급 요청” 흐름이 자연스러운데, 현재 요청은 `evaluationNo` 없이도 가능하다. 화면 플로우와 서버 정책을 맞출 필요가 있다.

---

## 6. education + inquiry

상태: 1차 검토 완료

### 발견 사항

- **EDU-01 / 업무 규칙 불일치 / 교육 제반 등록 시 계획안 승인 상태를 확인하지 않음**
  - 확인 위치: `EducationPreparationService.createPreparation()`
  - 에러 메시지는 “승인된 교육 계획안”을 찾는다고 되어 있지만, 실제 검증은 `planRepository.findById()` 존재 여부뿐이다.
  - 임시저장/승인요청/반려 상태의 계획안에도 교육 제반이 등록될 수 있다.
  - 프론트에서 승인된 계획안만 선택하게 해도 직접 API 호출이나 오래된 화면 상태에서 규칙이 깨질 수 있다.
  - 권장: 조회한 계획안의 `status == "승인"`을 확인한 뒤 제반 등록을 허용한다.

- **EDU-02 / 액션 오타 흡수 / 교육 계획안 action 값이 잘못되면 임시저장으로 처리**
  - 확인 위치: `EducationPlanService.createPlan()`
  - `action`이 정확히 `"REQUEST_APPROVAL"`일 때만 승인요청이고, 그 외 모든 값은 임시저장이다.
  - 프론트에서 `REQUEST_APROVAL` 같은 오타를 보내면 400이 아니라 임시저장으로 성공한다.
  - 권장: 허용값을 `TEMP_SAVE`, `REQUEST_APPROVAL`로 명시 검증하고 알 수 없는 action은 400 처리한다.

- **EDU-03 / 잘못된 query 입력 시 500 가능성 / planNo·prepNo 필터가 Repository parseId에 직접 의존**
  - 확인 위치: `EducationPreparationService.getPreparations()`, `EducationExecutionService.getExecutions()`, 각 Repository `findByPlanNo/findByPrepNo`
  - `GET /api/education-preparations?planNo=abc` 같은 요청에서 Repository의 `Long.parseLong()`이 그대로 터질 수 있다.
  - 권장: Service에서 필터 번호를 먼저 검증해 400을 반환한다.

- **INQ-01 / 잘못된 enum JSON 처리 / InquiryRequest.inquiryType 역직렬화 실패가 500 가능**
  - 확인 위치: `InquiryRequest.java`, `ApiExceptionHandler.java`
  - `InquiryRequest.inquiryType`은 enum 타입이라 잘못된 문자열이 오면 Jackson 역직렬화 예외가 발생한다.
  - 현재 전역 핸들러는 일반 `Exception`을 모두 500으로 응답한다.
  - 권장: `HttpMessageNotReadableException`을 400으로 처리하거나, 다른 도메인처럼 문자열 DTO + Service enum 파싱 패턴으로 맞춘다.

- **INQ-02 / 필터 사용성 / status 필터 유효성 검증 없음**
  - 확인 위치: `InquiryService.getInquiries()`, `InquiryRepository.findByStatus()`
  - `status=foo`는 400이 아니라 빈 목록을 반환한다.
  - 프론트 검색 필터 오타나 상태 상수 불일치를 조기에 찾기 어렵다.
  - 권장: `InquiryStatus` enum으로 선검증하거나 문서상 “알 수 없는 status는 빈 결과” 정책을 명시한다.

### 프론트 연동 메모

- 교육 계획안은 `POST /education-plans`에 action을 싣는 구조라 폼의 “임시저장/승인요청” 버튼 구현이 쉽다.
- 다만 action이 자유 문자열이라 프론트와 백엔드 상수 불일치가 조용히 성공할 수 있다. enum 옵션 문서화 또는 서버 검증이 필요하다.
- 문의 등록은 enum 타입 DTO를 쓰므로 TypeScript 프론트와는 잘 맞지만, 잘못된 값에 대한 400 응답 처리가 보강되어야 개발 중 디버깅이 쉽다.

---

## 7. actor + customer + common

상태: 1차 검토 완료

### 발견 사항

- **MASTER-01 / 프론트 기본 데이터 사용성 / 고객 검색·상세 API 부족**
  - 확인 위치: `CustomerRepository.java`
  - 백엔드 내부에서는 `CustomerRepository.findById()`와 `findAll()`이 있지만, 고객 전용 Controller는 없다.
  - 여러 프론트 화면이 `customerId`를 직접 입력해야 하는 구조가 되기 쉽다.
  - 권장: 프론트가 고객 선택 UI를 만들 수 있도록 `GET /api/customers`, `GET /api/customers/{customerId}` 또는 검색 API를 추가하는 방안을 검토한다.

- **MASTER-02 / 외부 연동 stub 명시 필요 / BankAccount.verify()는 모든 필드 존재 시 성공**
  - 확인 위치: `common/entity/BankAccount.java`
  - 현재 계좌 검증은 더미 구현이다. 보험료 납입, 보험금 청구, 보험금 지급, 환급금 지급이 모두 이 동작에 의존한다.
  - 프론트 개발 중에는 유용하지만, 실제 실패 시나리오 테스트를 하려면 별도 테스트 hook 또는 명시적인 실패 입력 규칙이 필요하다.

### 프론트 연동 메모

- 고객번호를 이미 아는 관리자형 화면이면 현재 API만으로도 주요 흐름을 진행할 수 있다.
- 일반적인 프론트 UX에서는 고객/계약/상품 선택 컴포넌트가 먼저 필요하다. 보험상품 목록 API는 있지만 고객 목록 API는 노출되어 있지 않다.
