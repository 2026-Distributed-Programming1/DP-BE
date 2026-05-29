# 보험 시스템 클래스 다이어그램 (도메인 클래스 기준)

> 실제 구현 코드(`dp.*` 도메인 패키지)를 기준으로 재작성한 클래스 다이어그램.
> Runner / DAO / DBA 등 인프라·진행자 클래스는 제외하고, **도메인 클래스와 그 관계만** 정리한다.

---

## 📌 표기법 안내

### 가시성

- `+` public
- `-` private
- `#` protected

### 필드 / 메서드 표기

- 필드: `가시성 이름: 타입 [한글]`
- 메서드: `가시성 이름(매개변수: 타입): 반환타입 [한글]`
- `🔗` 마커: 다른 도메인 클래스 타입 필드 (관계선으로 대체 가능)
- `«enum»`: 열거형 타입 필드 (값은 §0 열거형 목록 참조)
- `{static}`: 정적 메서드
- getter/setter는 지면상 생략하며, 의미 있는 비즈니스 메서드만 표기한다.

### 관계

- **Association (단방향 연관)**: `────▶` (참조하는 쪽 → 참조되는 쪽)
- **Aggregation (집약, 약한 포함)**: `◇────▶`
- **Composition (합성, 강한 포함)**: `◆────▶`
- **Generalization (상속)**: `─────▷` (자식 → 부모)
- **Dependency (의존 - 메서드 인자/반환)**: `┄┄┄▶`
- **다중성**: `1`, `0..1`, `*`, `1..*`

---

# 0️⃣ 열거형 (enum) 목록

도메인 클래스의 상태/유형 필드는 대부분 enum 으로 정의되어 있다.

| enum | 값 |
|---|---|
| `ChannelType` | DESIGNER, AGENCY |
| `InsuranceType` | LIFE, HEALTH, AUTO, FIRE |
| `ContractStatus` | NORMAL, EXPIRED, CANCELLED, LAPSED |
| `ContractPaymentStatus` | NORMAL, OVERDUE, UNPAID |
| `CustomerResponse` | RENEWAL, TERMINATION, PENDING |
| `PlanStatus` | TEMP_SAVE, UNDER_REVIEW |
| `ScreeningStatus` | PENDING, APPROVED, REJECTED |
| `EvaluationGrade` | S, A, B, C, D |
| `ActivityType` | VISIT, CONSULTATION, CALL |
| `PaymentMethod` | IMMEDIATE_TRANSFER, VIRTUAL_ACCOUNT |
| `PaymentStatus` | DRAFT, COMPLETED, ERROR |
| `PaymentRecordStatus` | WAITING, COMPLETED, REJECTED |
| `RejectCategory` | PAYMENT_ERROR, DUPLICATE_PAYMENT, CONTRACT_MISMATCH, OTHER |
| `RefundStatus` | CALCULATION_PENDING, CALCULATED, PAID |
| `RefundPaymentStatus` | WAITING, COMPLETED, FAILED, LOCKED |
| `AccidentType` | OBJECT, PERSON |
| `AccidentSubType` | GENERAL, TRAFFIC |
| `AccidentReportStatus` | DRAFT, RECEIVED, CANCELED |
| `DispatchStatus` | REQUESTED, ASSIGNED, DEPARTED, ARRIVED, CANCELED, COMPLETED |
| `DispatchRecordStatus` | DRAFT, TRANSMITTED |
| `AuthMethod` | MOBILE, SIMPLE, CERTIFICATE |
| `ClaimType` | DISEASE, ACCIDENT |
| `ClaimRequestStatus` | DRAFT, RECEIVED |
| `ClaimStatus` | PAID, UNDER_REVIEW, REJECTED |
| `NoticeMethod` | KAKAO, SMS, EMAIL, POST, NONE |
| `InvestigationResult` | APPROVED, REJECTED |
| `InvestigationStatus` | NEW_ASSIGNED, INVESTIGATING, INVESTIGATED, CLOSED |
| `CalculationStatus` | CALCULATED, APPROVAL_PENDING, APPROVED, CLOSED |
| `PaymentType` | IMMEDIATE, SCHEDULED |
| `ClaimPaymentStatus` | WAITING, SCHEDULED, COMPLETED, FAILED, CLOSED |
| `InquiryType` | INSURANCE, CLAIM, CONTRACT_CHANGE, CANCELLATION, OTHER |
| `InquiryStatus` | PENDING, ANSWERED |
| `FaqCategory` | ALL, INSURANCE, CLAIM, CONTRACT_CHANGE, CANCELLATION, OTHER |

---

# 1️⃣ 행위자 (actor) 도메인 — `dp.actor`

## `User` [사용자] (추상)

```
# userId: String         [사용자 ID - USR##### 자동 부여]
# name: String           [이름]
# contact: String        [연락처]
# email: String          [이메일]
# password: String       [비밀번호]
# loggedIn: boolean      [로그인 상태]

+ login(id: String, pw: String): boolean
+ logout(): void
+ updateProfile(contact: String, email: String): void
```

## `Customer` [고객] - User 상속

```
- customerId: String                       [고객번호 - CUS##### 자동 부여]
- phone: String                            [연락처]
- residentNo: String                       [주민등록번호]
- address: String                          [주소]
- birthDate: LocalDate                     [생년월일]
🔗 registeredAccounts: List<BankAccount>    [등록 계좌 목록]
- registeredAt: LocalDateTime              [가입일시]

+ enterAddress(address: String): void
+ enterBirthDate(date: LocalDate): void
+ registerAccount(account: BankAccount): void
+ updateInfo(): void
```

## `Employee` [사원] (추상) - User 상속

```
# employeeId: String     [사원 ID - EMP##### 자동 부여]
# department: String     [부서]
# position: String       [직책]
# hireDate: LocalDate     [입사일]
```

## `SalesChannel` [판매채널] — Designer/Agency 의 부모

> ⚠️ 실제 코드에서 SalesChannel 은 Employee 를 상속하지 않는 독립 클래스이다.

```
- channelId: String         [채널 ID]
- channelName: String       [채널명]
🔗 channelType: ChannelType  [채널 유형 «enum»]

+ acceptConsultation(request: ConsultationRequest): void   [상담 요청 수락]
+ createPolicyApplication(): PolicyApplication             [청약서 생성]
+ getActivityDetail(): void
```

## `Designer` [설계사] - SalesChannel 상속

```
- licenseNumber: String     [설계사 자격증 번호]
```

## `Agency` [대리점] - SalesChannel 상속

```
- agencyNumber: String      [대리점 번호]
```

## `SalesManager` [영업관리자]

> 독립 클래스 (Employee 미상속). managerId="MGR-"+name.

```
- managerId: String     [관리자 ID]
- name: String          [이름]
- department: String    [부서]

+ approveEducationPlan(plan: EducationPlan): void
+ rejectEducationPlan(plan: EducationPlan, reason: String): void
```

## `EducationTrainer` [영업교육담당자] - Employee 상속

```
+ createEducationPlan(): EducationPlan
+ registerEducationPreparation(): EducationPreparation
+ conductEducation(preparation: EducationPreparation): EducationExecution
```

## `InsuranceReviewer` [보험심사자] - Employee 상속

```
+ startUnderwriting(application: PolicyApplication): Underwriting
+ deliverReviewResult(result: ReviewResult): void
```

## `ClaimsHandler` [보상담당자] - Employee 상속

```
- transferLimit: long    [전결 한도]
```

## `DispatchAgent` [현장출동 직원] - Employee 상속

```
- region: String         [담당 지역]
- vehicleNo: String      [출동 차량 번호]
```

## `FinanceManager` [재무회계 담당자] - Employee 상속

```
(추가 필드 없음 - 비즈니스 로직은 payment 도메인 객체가 담당)
```

## `ContractManager` [계약관리 담당자]

> 독립 클래스 (Employee 미상속).

```
- managerId: String     [담당자 ID]
- name: String          [이름]
- department: String    [부서]
```

## `Applicant` [지원자]

```
- applicantId: String        [지원자 ID]
- name: String               [이름]
🔗 channelType: ChannelType   [지원 채널 유형 «enum»]
```

### 🔗 1️⃣ 관계

```
User ◁───── Customer
User ◁───── Employee
Employee ◁───── EducationTrainer
Employee ◁───── InsuranceReviewer
Employee ◁───── ClaimsHandler
Employee ◁───── DispatchAgent
Employee ◁───── FinanceManager
SalesChannel ◁───── Designer
SalesChannel ◁───── Agency

Customer "1" ◇────▶ "*" BankAccount                         : 등록 계좌
SalesChannel ┄┄┄▶ ConsultationRequest                       : 수락 (메서드)
SalesChannel ┄┄┄▶ PolicyApplication                         : 생성 (메서드)
SalesManager ┄┄┄▶ EducationPlan                             : 승인/반려 (메서드)
EducationTrainer ┄┄┄▶ EducationPlan, EducationPreparation, EducationExecution : 생성 (메서드)
InsuranceReviewer ┄┄┄▶ PolicyApplication, Underwriting, ReviewResult           : 심사 (메서드)
```

> ※ SalesManager, ContractManager, SalesChannel, Applicant 는 User/Employee 를 상속하지 않는 독립 도메인 클래스이다.

---

# 2️⃣ 공통 (common) 도메인 — `dp.common`

## `Attachment` [첨부파일]

```
- fileId: String             [파일 ID - ATT##### 자동 부여]
- fileName: String           [파일명]
- fileSize: long             [파일 크기]
- filePath: String           [저장 경로]
- mimeType: String           [파일 형식]
- uploadedAt: LocalDateTime  [업로드 일시]

+ delete(): void
+ download(): File
```

## `BankAccount` [계좌 정보]

```
- bankName: String       [은행명]
- accountNo: String      [계좌번호]
- accountHolder: String  [예금주명]
- verified: boolean      [인증 결과]

+ enter(bank: String, no: String, holder: String): void
+ verify(): boolean      [본인 명의 계좌 검증]
```

> Attachment / BankAccount 는 여러 도메인이 공유하는 부품 클래스이다. (관계는 각 도메인 절에 표기)

---

# 3️⃣ 영업 (sales) 도메인 — `dp.sales`

## `ActivityPlan` [활동 계획]

```
- planId: String                         [계획 ID]
- planName: String                       [계획명]
- startDate: LocalDate                   [시작일]
- endDate: LocalDate                     [종료일]
- author: String                         [작성자 - 자동 입력]
- memo: String                           [메모]
🔗 schedules: List<ScheduleItem>          [일정 목록]
- targetContractCount: Integer           [목표 계약 건수]
- targetContractAmount: Long             [목표 계약 금액]
- targetNewCustomer: Integer             [목표 신규 고객 수]
- proposedCustomerId: String             [제안 대상 고객번호]
🔗 proposedInsuranceType: InsuranceType   [제안 보험 종류 «enum»]
- proposalReason: String                 [제안 사유]
🔗 status: PlanStatus                     [상태 «enum»]

+ addSchedule(item: ScheduleItem): void
+ deleteSchedule(): void
+ sortSchedules(): void
+ validateRequired(): Boolean
+ validateDateRange(): Boolean
+ tempSave(): void
+ submit(): void
```

## `ScheduleItem` [일정 항목]

```
- customerId: String                [대상 고객번호]
🔗 activityType: ActivityType        [활동 유형 «enum»]
- activityDateTime: LocalDateTime   [활동 일시]
- location: String                  [활동 장소]
- memo: String                      [메모]
```

## `ChannelRecruitment` [판매채널 모집]

```
- recruitmentNo: String          [모집번호]
- managerName: String            [등록 영업관리자명]
🔗 channelType: ChannelType       [채널 유형 «enum»]
- recruitCount: Integer          [모집 인원]
- startDate: LocalDate           [모집 시작일]
- endDate: LocalDate             [모집 종료일]
- condition: String              [모집 조건]
- registeredAt: LocalDateTime    [등록 일시]

+ validateRequired(): Boolean
+ save(): void
```

## `ChannelScreening` [판매채널 채용 심사]

```
- applicantName: String              [지원자명]
🔗 channelType: ChannelType           [채널 유형 «enum»]
- applicationDate: LocalDate         [지원일]
- career: String                     [경력 사항]
- certifications: List<String>       [자격증 목록]
🔗 screeningStatus: ScreeningStatus   [심사 상태 «enum»]
- approvalNo: String                 [승인번호]
- approvedAt: LocalDateTime          [승인 일시]
- rejectionReason: String            [거절 사유]
- filterStartDate / filterEndDate: LocalDate   [조회 기간]

+ approve(): void
+ reject(): void
```

## `SalesActivityManagement` [영업 활동 관리]

```
- startDate / endDate: LocalDate     [관리 기간]
🔗 channelType: ChannelType           [채널 유형 «enum»]
- channelName: String                [채널명]
- visitCount: Integer                [방문건수]
- contractCount: Integer             [계약건수]
- conversionRate: Double             [전환율]
- achievementRate: Double            [목표달성률]
- improvementContent: String         [개선 지시 내용]
- revisedTarget: Integer             [수정 목표]
- managerName: String                [담당 영업관리자명]
- managementNo: String               [관리번호]
- registeredAt: LocalDateTime        [등록 일시]

+ saveImprovement(): void
```

## `SalesOrgEvaluation` [영업 조직 평가]

```
- filterStartDate / filterEndDate: LocalDate    [평가 기간]
🔗 channelType: ChannelType           [채널 유형 «enum»]
- channelName: String                [채널명]
- salesResult: Long                  [매출실적]
- contractCount: Integer             [계약건수]
- achievementRate: Double            [목표달성률]
🔗 evaluationGrade: EvaluationGrade   [평가 등급 «enum»]
- evaluationComment: String          [평가 의견]
- evaluationNo: String               [평가번호]
- evaluatedAt: LocalDateTime         [등록 일시]

+ validateRequired(): Boolean
+ saveEvaluation(): void
```

## `BonusRequest` [성과급 지급 요청]

```
- evaluationNo: String               [평가번호]
- channelName: String                [채널명]
🔗 channelType: ChannelType           [채널 유형 «enum»]
🔗 evaluationGrade: EvaluationGrade   [평가 등급 «enum»]
- bonusRatio: Double                 [지급 비율 - S:150%, A:120%]
- baseSalary: Long                   [기본급]
- bonusAmount: Double                [산출 성과급]
- requestReason: String              [요청 사유]
- requestNo: String                  [요청 번호]
- requestedAt: LocalDateTime         [요청일시]

+ calculateBonus(): Double
+ submit(): void
```

## `CustomerRegistration` [고객 정보 등록]

```
- customerId: String                 [고객번호 - 자동 부여]
- name: String                       [이름]
- ssn: String                        [주민등록번호]
- isSsnMasked: Boolean               [뒷자리 마스킹 여부]
- phone: String                      [연락처]
- address: String                    [주소]
- contractNo: String                 [계약번호 - 자동 부여]
🔗 insuranceType: InsuranceType       [보험종류 «enum»]
- contractDate / expiryDate: LocalDate   [계약일 / 만료일]
- monthlyPremium: Long               [월 보험료]
- specialClauses: List<String>       [특약 정보]

+ validateRequired(): Boolean
+ validateFormat(): Boolean
+ validateDuplicate(): Boolean
+ assignIds(): void   [고객번호·계약번호 자동 부여]
```

### 🔗 3️⃣ 관계

```
ActivityPlan "1" ◆────▶ "*" ScheduleItem                    : 일정 구성
ActivityPlan ────▶ «enum» InsuranceType, PlanStatus
ChannelRecruitment / ChannelScreening / BonusRequest / SalesOrgEvaluation ────▶ «enum» ChannelType
SalesOrgEvaluation, BonusRequest ────▶ «enum» EvaluationGrade
```

> 영업 도메인의 클래스 대부분은 채널명·관리자명 등을 **문자열로 보관**하며 actor 클래스를 직접 참조하지 않는다(화면 입력 데이터 보관 위주).

---

# 4️⃣ 교육 (education) 도메인 — `dp.education`

## `EducationPlan` [교육계획안]

```
- planNumber: int            [계획번호 - 자동 부여]
- trainerName: String        [작성 교육담당자명]
- educationName: String      [교육명]
- startDate / endDate: LocalDate    [교육 기간]
- channelType: String        [채널유형]
- targetCount: int           [교육 대상자 수]
- budget: long               [교육 예산]
- educationGoal: String      [교육 목표]
- educationContent: String   [교육 내용]
- textbookList: String       [교재 목록]
- rejectReason: String       [반려 사유]
- approvedAt: LocalDateTime  [승인일시]
- status: String             [상태 - 작성중/임시저장/승인요청/승인/반려]

+ enterPlanInfo(...): void
+ enterContentInfo(...): void
+ validateRequiredFields(): boolean
+ requestApproval(): void
+ tempSave(): void
+ approve(): void
+ reject(reason: String): void
```

## `EducationPreparation` [교육제반]

```
- setupNumber: int                   [등록번호 - 자동 부여]
- planNo: String                     [교육 계획 번호 참조]
- registeredAt: LocalDateTime        [등록일시]
- location: String                   [교육 장소]
- instructorName: String             [강사명]
- textbookStatus: String             [교재 준비 현황]
- materialReady: boolean             [교재 준비 여부]
- additionalNotice: String           [기타 준비 사항]
🔗 attendanceList: List<Attendance>   [출석 목록]
- status: String                     [상태 - 작성중/등록완료]

+ enterPreparationInfo(...): void
+ validateRequiredFields(): boolean
+ save(): void
+ addAttendee(attendeeName: String): void
```

## `EducationExecution` [교육진행]

```
- completionNumber: int                  [완료번호 - 자동 부여]
- completedAt: LocalDateTime             [완료일시]
- attendanceCount: int                   [출석 인원]
- totalCount: int                        [전체 인원]
- memo: String                           [교육 진행 메모]
- status: String                         [상태 - 진행중/완료]
🔗 preparation: EducationPreparation      [교육 제반 (final)]

+ loadAttendanceList(): List<Attendance>
+ markAttendance(attendeeName: String, isAttended: boolean): void
+ calculateAttendanceCount(): int
+ complete(): void
+ sendCompletionNotice(): void
```

## `Attendance` [출석]

```
- attendeeName: String   [교육 대상자명]
- isAttended: boolean    [출석 여부]

+ mark(isAttended: boolean): void
```

### 🔗 4️⃣ 관계

```
EducationPreparation "1" ◆────▶ "*" Attendance              : 출석 구성 (Composition)
EducationExecution "*" ────▶ "1" EducationPreparation       : 진행 대상
EducationExecution ┄┄┄▶ Attendance                          : 출석 목록 위임 조회
```

> EducationPlan → EducationPreparation 은 `planNo`(String) 으로 느슨하게 연결된다(객체 참조 아님).

---

# 5️⃣ 상담/면담/청약/심사 (consultation) 도메인 — `dp.consultation`

## `ConsultationRequest` [상담신청]

```
- consultationNumber: int        [접수번호 - 자동 부여]
- type: String                   [상담 유형]
- scheduledAt: LocalDateTime     [희망 일시]
- location: String               [방문 장소]
- contact: String                [연락처]
- content: String                [상담 내용]
- status: String                 [상태 - 접수/수락]
- receivedAt / acceptedAt: LocalDateTime

+ selectType(type: String): void
+ enterConsultationInfo(...): void
+ validateRequiredFields(): boolean
+ submit(): void
+ accept(): void
```

## `InterviewSchedule` [면담일정]

```
- interviewNumber: int                       [면담번호 - 자동 부여]
- customerName: String                       [고객명]
- designerName: String                       [담당 설계사명]
- type: String                               [면담 유형]
- scheduledAt: LocalDateTime                 [면담일시]
- location: String                           [면담장소]
- preparation: String                        [면담 준비사항]
- status: String                             [상태 - 예정/취소]
- registeredAt / modifiedAt / cancelledAt: LocalDateTime
🔗 interviewRecordList: List<InterviewRecord> [면담 기록 목록]

+ register(...): void
+ modify(...): void
+ cancel(): void
+ sendNotice(): void
+ validateRequiredFields(): boolean
```

## `InterviewRecord` [면담기록]

```
- recordNumber: int              [기록번호 - 자동 부여]
- customerName: String           [고객명]
- interviewedAt: LocalDateTime   [면담 일시]
- recordedAt / modifiedAt: LocalDateTime
- content: String                [면담 내용]
- customerReaction: String       [고객 반응]
- followUpAction: String         [후속 조치]

+ save(content, reaction, followUp): void
+ modify(content, reaction, followUp): void
+ validateRequiredFields(): boolean
+ navigateToProposal(): Proposal
```

## `Proposal` [제안서]

```
- proposalId: int                       [제안번호 - 자동 부여]
- sentAt: LocalDateTime                 [발송일시]
- customerName: String                  [수신 고객명]
🔗 insuranceProduct: InsuranceProduct    [제안 상품]

+ selectProduct(product: InsuranceProduct): void
+ send(): void
```

## `InsuranceProduct` [보험상품]

```
- productName: String     [상품명]
- type: String            [보험유형]
- monthlyPremium: long    [월 보험료]
- coverage: String        [보장내용]
- specialTerms: String    [특약]
```

## `InsuranceApplication` [보험가입신청서 - 고객 직접]

```
- applicationNumber: int                 [신청번호 - 자동 부여]
🔗 customer: Customer                     [고객]
🔗 product: InsuranceProduct              [상품]
- appliedAt: LocalDateTime               [신청일시]
- paymentMethod: String                  [납입방법]
- selectedSpecialTerms: List<String>     [선택 특약]
- status: String                         [상태]

+ enterPersonalInfo(...): void
+ selectSpecialTerms(list): void
+ selectPaymentMethod(method): void
+ authenticate(): boolean
+ apply(): void
```

## `PolicyApplication` [청약서 - 설계사 작성]

```
- applicationNumber: int          [청약번호 - 자동 부여]
- submittedAt / uploadedAt: LocalDateTime
🔗 customer: Customer              [고객]
- customerName: String            [고객명]
- productName: String             [상품명]
- period: int                     [보험기간]
- paymentMethod: String           [납입방법]
- status: String                  [상태]

+ enterCustomerInfo(...): void
+ selectProduct(name, period, method): void
+ attachSignature(file: String): void
+ requestElectronicSignature(): void
+ submit(): void
```

## `Underwriting` [인수심사]

```
- reviewNumber: int              [심사번호 - 자동 부여]
- appNo: String                  [청약번호 참조]
- customerName: String           [고객명]
- reviewedAt: LocalDateTime      [심사일시]
- riskGrade: String              [위험등급]
- reviewType: String             [수동 심사 유형]
- reviewOpinion: String          [심사 의견]
🔗 reviewResult: ReviewResult     [심사 결과]

+ startReview(): void
+ autoReview(): ReviewResult
+ manualReview(type, opinion): ReviewResult
+ attachDocument(file: String): void
+ complete(result, condition, rejectionReason): void
```

## `ReviewResult` [심사결과]

```
- result: String             [결과 - 승인/거절/조건부]
- condition: String          [조건부 승인 조건]
- rejectionReason: String    [거절 사유]
- confirmedAt: LocalDateTime [확인일시]
- processingNo: String       [처리번호]

+ deliver(): void
+ confirm(): void
```

## `Revival` [부활신청]

```
- revivalNumber: int          [신청번호 - 자동 부여]
🔗 customer: Customer          [고객]
- contractNo: String          [실효 계약번호 참조]
- appliedAt: LocalDateTime    [신청일시]
- unpaidAmount: long          [미납 보험료]
- paymentMethod: String       [납입방법]
- contact: String             [연락처]

+ checkEligibility(): boolean
+ calculateUnpaidAmount(): long
+ pay(method: String): boolean
+ authenticate(): boolean
+ submit(): void
```

### 🔗 5️⃣ 관계

```
InterviewSchedule "1" ◇────▶ "*" InterviewRecord            : 면담 기록 보유
InterviewRecord ┄┄┄▶ Proposal                               : 제안서로 이동 (메서드 반환)
Proposal "*" ────▶ "1" InsuranceProduct                     : 제안 상품
InsuranceApplication "*" ────▶ "1" Customer                 : 신청 고객
InsuranceApplication "*" ────▶ "1" InsuranceProduct         : 대상 상품
PolicyApplication "*" ────▶ "1" Customer                    : 청약 고객
Underwriting "1" ◆────▶ "1" ReviewResult                    : 심사 결과 (Composition)
Revival "*" ────▶ "1" Customer                              : 신청 고객
```

> Underwriting→PolicyApplication, Revival→Contract 등은 `appNo`·`contractNo`(String)로 느슨하게 연결된다.

---

# 6️⃣ 계약 (contract) 도메인 — `dp.contract`

## `Contract` [보험계약]

> 7·8 도메인(자동 부여 생성자)과 6 도메인(수동 입력) 을 통합한 클래스.

```
- contractNo: String              [계약번호 - CON##### 자동 부여]
- policyNo: String                [증권번호 - POL##### 자동 부여]
🔗 customer: Customer              [계약자]
- contractDate: LocalDate         [계약일 (alias startDate)]
- expiryDate: LocalDate           [만료일 (alias endDate)]
- monthlyPremium: long            [월 보험료]
- insuranceType: String           [보험종류]
🔗 status: ContractStatus          [계약 상태 «enum»]
- isExpiringSoon: Boolean          [만기 임박 여부]
- totalPayCount / paidCount: Integer
- lastPaymentDate: LocalDate
- isOverdue: Boolean / overdueCount: Integer
- specialClauses: List<String>    [특약 목록]
- clausePremiums: List<Long>      [특약 보험료]

+ isMaturityNear(): boolean       [만기 30일 이내]
+ updateStatus(newStatus: String): void
+ changePaymentMethod(method, account: BankAccount): void
+ verifyAccount(amount, code): boolean
```

## `Cancellation` [해지]

```
- cancellationNo: String          [해지번호 - CAN##### 자동 부여]
🔗 contract: Contract              [대상 계약]
- reason: String                  [해지 사유]
- detailReason: String            [상세 사유]
- noticeAgreed: boolean           [유의사항 동의]
- authResult: boolean             [본인인증 결과]
- expectedRefund: long            [예상 환급금]
- canceledAt: LocalDateTime       [해지일시]
- status: String                  [상태 - 작성중/완료/실패/취소]

+ selectReason(reason): void
+ enterDetailReason(detail): void
+ validateReasonInput(): boolean
+ agreeToNotice(): void
+ authenticate(): boolean
+ calculateExpectedRefund(): long
+ submit() / confirm() / cancel(): void
```

## `ContractStatistics` [계약 통계]

```
- contractNo: String                 [계약번호]
- contractorName: String             [계약자명]
- paySequence: Integer               [납부 회차]
- paymentDate: LocalDate / paymentAmount: Long
🔗 paymentStatus: PaymentStatus       [납부 상태 «enum: ContractPaymentStatus 류»]
- claimDate: LocalDateTime / claimAmount: Long / paidAmount: Long
🔗 claimStatus: ClaimStatus           [청구 처리 상태 «enum»]
- monthlyRetentionData: List<Object> [월별 유지 데이터]
- filterStartMonth / filterEndMonth: YearMonth
- fileName: String                   [엑셀 파일명]
- globalInsuranceType / globalContractStatus: String

+ validateDateRange(): Boolean
+ exportToExcel(): File
```

## `ContractFilter` [계약 검색 필터]

```
- customerId / name / ssn / phone / contractNo / insuranceType: String
- contractDate: LocalDate

+ apply(): void
+ reset(): void
```

## `ExpiringContractManagement` [만기 계약 관리]

```
- contractNo / contractorName / insuranceType: String
- expiryDate: LocalDate / remainingDays: Integer
- phone / email: String
- isRenewable: Boolean
- expectedPremium: Long           [갱신 예상 보험료]
- noticeDate: LocalDateTime / noticeMemo: String
🔗 customerResponse: CustomerResponse  [고객 응답 «enum»]
- renewalPremium: Long / premiumDiff: Long

+ sendNoticeSms() / saveNoticeRecord() / confirmRenewal()
+ switchToTermination() / sendPendingAlert(): void
```

### 🔗 6️⃣ 관계

```
Contract "*" ────▶ "1" Customer                             : 계약자
Contract ────▶ «enum» ContractStatus
Cancellation "*" ────▶ "1" Contract                         : 대상 계약
ContractStatistics ────▶ «enum» (ContractPaymentStatus, ClaimStatus)
ExpiringContractManagement ────▶ «enum» CustomerResponse
```

> ContractStatistics / ExpiringContractManagement 는 계약 정보를 `contractNo`(String) 와 화면 데이터로 보관하며 Contract 객체를 직접 참조하지 않는다.

---

# 7️⃣ 사고/현장출동/보험금 (claim) 도메인 — `dp.claim`

## `AccidentReport` [사고 접수]

```
- reportNo: String                   [접수번호 - 자동 부여]
🔗 customer: Customer                 [사고자]
- vehicleNo / ownerName / phoneNo: String
🔗 accidentType: AccidentType         [사고 유형 «enum»]
- damageType: String / location: String
- needsDispatch: boolean / agreedTerms: boolean
- reportedAt: LocalDateTime
🔗 status: AccidentReportStatus        [상태 «enum»]
- casualtyCount: int / injurySeverity: String / emergencyReported: boolean  [인명사고 A1]

+ enterVehicleInfo(...) / selectAccidentType(...) / enterLocation(...): void
+ setDispatchOption(needs): void / agreeTerms(): void
+ validateRequiredFields() / verifyContract(): boolean
+ receive(): void
+ requestDispatch(): Dispatch
+ enterCasualtyInfo(...): void
```

## `Dispatch` [현장 출동]

```
- dispatchNo: String              [출동번호 - 자동 부여]
🔗 accident: AccidentReport        [사고 접수]
🔗 agent: DispatchAgent            [출동 직원]
- estimatedArrival / arrivalTime: LocalDateTime
🔗 status: DispatchStatus          [상태 «enum»]
- cancelReason: String

+ assignAgent(agent): void / setEstimatedArrival(time): void
+ depart() / arrive() / complete(): void
+ updateLocation(newLocation): void / cancel(reason): void
```

## `DispatchRecord` [현장 출동 기록]

```
- recordId: String                [기록 ID - 자동 부여]
🔗 dispatch: Dispatch              [출동 건]
🔗 photos: List<Attachment>        [사진 목록]
- policeRequired / towingRequired: boolean
- notes: String
- transmittedAt: LocalDateTime
🔗 status: DispatchRecordStatus    [상태 «enum»]

+ uploadPhoto(category, photo: Attachment): void
+ removePhoto(photo): void
+ validateRequired(): boolean
+ transmit(): void
```

## `ClaimRequest` [보험금 청구]

```
- claimNo: String                 [청구번호 - 자동 부여]
🔗 customer: Customer              [청구 고객]
🔗 contract: Contract              [대상 계약]
🔗 insured: Customer               [피보험자]
🔗 authMethod: AuthMethod          [본인인증 방법 «enum»]
- authenticated / personalInfoAgreed: boolean
🔗 claimType: ClaimType            [청구 유형 «enum»]
- claimReasons: List<String> / diagnosis: String
🔗 accidentDetail: AccidentDetail  [사고 상세 - 재해 시]
🔗 recipientInfo: RecipientInfo    [수령인 정보]
🔗 bankAccount: BankAccount        [지급 계좌]
🔗 noticeMethod: NoticeMethod      [안내 방법 «enum»]
- progressNoticeAgreed / fpNoticeAgreed: boolean
🔗 attachments: List<Attachment>   [첨부 서류]
- requestedAt: LocalDateTime
🔗 status: ClaimRequestStatus      [상태 «enum»]

+ authenticate() / verifyAccount() / validateBeforeSubmit(): boolean
+ selectInsured / selectClaimType / selectClaimReasons / enterAccidentDetail ...
+ submit() / cancel(): void
```

## `AccidentDetail` [사고 상세 - 재해 청구]

```
🔗 accidentSubType: AccidentSubType  [사고 유형 «enum»]
- content: String / date: LocalDate / location: String

+ enter(subType, content, date, location): void
```

## `RecipientInfo` [수령인 정보]

```
- name / residentNo / contact: String

+ changeContact(contact): void
```

## `SupplementRequest` [보완 서류 요청]

```
- requestedItems: List<String>
- message: String
- sentAt: LocalDateTime

+ send(): void
```

## `AdditionalInvestigation` [추가 조사 지시]

```
- visitLocation: String
- schedule: LocalDateTime
- reason: String
- registeredAt: LocalDateTime
```

## `DamageInvestigation` [손해 조사]

```
- investigationNo: String                       [조사번호 - 자동 부여]
🔗 claim: ClaimRequest                           [대상 청구]
🔗 handler: ClaimsHandler                        [보상담당자]
- ourFaultRatio / counterFaultRatio: double
- recognizedDamage: long / opinion: String
🔗 result: InvestigationResult                   [처리 결과 «enum»]
- rejectReason: String
🔗 supplementRequest: SupplementRequest          [보완 서류 요청 A1]
🔗 additionalInvestigation: AdditionalInvestigation [추가 조사 A2]
- investigatedAt: LocalDateTime
🔗 status: InvestigationStatus                   [상태 «enum»]

+ assignHandler(handler): void
+ enterFaultRatio(our, counter): void / validateFaultRatio(): boolean
+ requestSupplement(items, msg): SupplementRequest
+ requestAdditionalInvestigation(loc, schedule, reason): AdditionalInvestigation
+ complete(): ClaimCalculation
+ closeAsRejected(): void
```

## `ClaimCalculation` [보험금 산출]

```
- calculationNo: String              [산출번호 - 자동 부여]
🔗 investigation: DamageInvestigation [손해 조사]
- recognizedDamage: long / faultRatio: double
- deductible: long / coverageLimit: long / finalAmount: long
- adjusted / exceededDeductible / approvalRequired: boolean
🔗 approver: Employee                 [결재권자 A1]
- calculatedAt: LocalDateTime
🔗 status: CalculationStatus          [상태 «enum»]

+ loadCalculationData(): void / calculate(): long
+ applyCoverageLimit(): void / checkDeductibleExceeded(): boolean
+ selectApprover(approver): void / submitForApproval(): void
+ approve(): ClaimPayment
+ closeAsExceeded(): void
```

## `ClaimPayment` [보험금 지급]

```
- paymentNo: String                  [지급번호 - 자동 부여]
🔗 calculation: ClaimCalculation      [산출 건]
🔗 recipient: RecipientInfo           [수령인]
🔗 account: BankAccount               [수령 계좌]
- finalAmount: long
🔗 paymentType: PaymentType           [지급 유형 «enum»]
- scheduledAt / paidAt: LocalDateTime
- otpInput: String / otpVerified: boolean
🔗 noticeOption: List<NoticeMethod>   [안내 옵션 «enum»]
- noticeSent / transferFailed: boolean / failureReason: String
🔗 status: ClaimPaymentStatus         [상태 «enum»]

+ selectPaymentType(type): void / setScheduledDateTime(dt): void
+ enterOTP(otp): void / verifyOTP(): boolean
+ execute() / schedule(): void
+ handleTransferFailure(reason): void
+ sendCompletionNotice() / close(): void
```

### 🔗 7️⃣ 관계

```
AccidentReport "*" ────▶ "1" Customer                       : 사고자
AccidentReport ┄┄┄▶ Dispatch                                : requestDispatch() 생성

Dispatch "*" ────▶ "1" AccidentReport                       : 대상 사고
Dispatch "*" ────▶ "0..1" DispatchAgent                     : 배정 직원

DispatchRecord "*" ────▶ "1" Dispatch                       : 대상 출동
DispatchRecord "1" ◇────▶ "*" Attachment                    : 사진 보유

ClaimRequest "*" ────▶ "1" Customer                         : 청구 고객
ClaimRequest "*" ────▶ "1" Customer                         : 피보험자(insured)
ClaimRequest "*" ────▶ "1" Contract                         : 대상 계약
ClaimRequest "1" ◆────▶ "0..1" AccidentDetail               : 사고 상세 (Composition)
ClaimRequest "1" ◆────▶ "1" RecipientInfo                   : 수령인 정보
ClaimRequest "*" ────▶ "1" BankAccount                      : 지급 계좌
ClaimRequest "1" ◇────▶ "*" Attachment                      : 첨부 서류

DamageInvestigation "*" ────▶ "1" ClaimRequest              : 대상 청구
DamageInvestigation "*" ────▶ "0..1" ClaimsHandler          : 담당자
DamageInvestigation "1" ◆────▶ "0..1" SupplementRequest     : 보완 서류 요청
DamageInvestigation "1" ◆────▶ "0..1" AdditionalInvestigation : 추가 조사 지시
DamageInvestigation ┄┄┄▶ ClaimCalculation                  : complete() 생성

ClaimCalculation "*" ────▶ "1" DamageInvestigation          : 대상 조사
ClaimCalculation "*" ────▶ "0..1" Employee                  : 결재권자
ClaimCalculation ┄┄┄▶ ClaimPayment                          : approve() 생성

ClaimPayment "*" ────▶ "1" ClaimCalculation                 : 대상 산출
ClaimPayment "*" ────▶ "1" RecipientInfo                    : 수령인
ClaimPayment "*" ────▶ "1" BankAccount                      : 수령 계좌
```

---

# 8️⃣ 납입/환급 (payment) 도메인 — `dp.payment`

## `Payment` [보험료 납입]

```
- paymentNo: String               [납입 신청번호 - PAY##### 자동 부여]
🔗 customer: Customer              [고객]
🔗 items: List<PaymentItem>        [납입 항목 목록]
🔗 paymentMethod: PaymentMethod    [납입 방법 «enum»]
🔗 account: BankAccount            [납입 계좌]
- totalAmount / discountedAmount / earlyDiscount: long
- requestedAt: LocalDateTime
🔗 status: PaymentStatus           [상태 «enum»]

+ selectContracts(contracts: List<Contract>): void  [PaymentItem 생성]
+ enterPaymentCount(item, count): void
+ selectPaymentMethod(method) / selectExistingAccount(account): void
+ registerNewAccount(bank, no, holder): void / verifyAccount(): boolean
+ calculateTotal(): long
+ submit(): void
```

## `PaymentItem` [납입 항목] *(N:M 매핑)*

```
🔗 payment: Payment        [납입 건]
🔗 contract: Contract      [대상 계약]
- premiumPerCount: long   [회당 보험료 - contract에서 로드]
- count: int              [납입 횟수]
- subtotal: long          [소계]

+ setCount(count): void
+ calculateSubtotal(): long
```

## `PaymentRecord` [납부 내역]

```
- recordNo: String                [결제번호 - PRC##### 자동 부여]
🔗 contract: Contract              [대상 계약]
- paymentDate: LocalDate / amount: long / method: String
🔗 status: PaymentRecordStatus     [수납 상태 «enum»]
- installmentNo: int / lateFee: long / approvalNo: String
🔗 rejectCategory: RejectCategory  [반려 분류 «enum»]
- rejectReason: String
- confirmedAt / rejectedAt: LocalDateTime

+ load(): void
+ confirm(): void / recordOnLedger() / updateContractStatus(): void
+ enterRejectInfo(category, reason): void / reject(): void
```

## `OverdueNoticeSetting` [미납 알림 자동 발송 설정]

```
- enabled: boolean            [활성화 여부 - 기본 false]
- daysAfterDue: int           [발송 기준일]
- messageTemplate: String     [메시지 템플릿]
- savedAt: LocalDateTime

+ toggleEnabled(enabled): void / setDaysAfterDue(days): void
+ previewMessage(): String / save(): void
```

## `RefundCalculation` [해약 환급금 산출]

```
- refundNo: String                          [환급 접수번호 - RFC##### 자동 부여]
🔗 cancellation: Cancellation                [해지 건]
- totalPaidPremium / reserveAmount: long
- paymentPeriod: String / appliedRate: double
- baseRefund / unpaidPremium / loanPrincipal / loanInterest / finalRefund: long
🔗 adjustments: List<DeductionAdjustment>    [수기 조정 내역]
🔗 status: RefundStatus                      [상태 «enum»]
- calculatedAt / confirmedAt: LocalDateTime

+ loadContractData(): void / validateRequiredData(): boolean
+ calculateBaseRefund() / calculateDeductions() / calculateFinalRefund(): long
+ adjustDeduction(item, amount, note): DeductionAdjustment
+ recalculate(): void / exportPDF(): File
+ confirm(): RefundPayment
```

## `DeductionAdjustment` [공제 수기 조정 내역]

```
- itemName: String
- originalAmount / adjustedAmount: long
- adjustedAt: LocalDateTime
🔗 adjustedBy: FinanceManager   [조정자]
- note: String

+ apply(): void
```

## `RefundPayment` [환급금 지급]

```
- paymentNo: String               [지급번호 - RPY##### 자동 부여]
🔗 refund: RefundCalculation       [산출 건]
🔗 account: BankAccount            [수령 계좌 - 자동 로드]
- finalAmount: long
- otpInput: String / otpVerified: boolean
- otpFailCount: int / locked: boolean   [5회 실패 시 잠금]
- transferredAt: LocalDateTime
- noticeSent: boolean / noticeFailureMessage: String
🔗 status: RefundPaymentStatus     [상태 «enum»]

+ loadAccountInfo(): void
+ enterOTP(otp): void / verifyOTP(): boolean / lockOnFailure(): void
+ execute(): void / handleTransferFailure(): void
+ sendNotice(): void
```

### 🔗 8️⃣ 관계

```
Payment "*" ────▶ "1" Customer                              : 신청 고객
Payment "1" ◆────▶ "1..*" PaymentItem                       : 납입 항목 (Composition)
Payment "*" ────▶ "0..1" BankAccount                        : 납입 계좌

PaymentItem "*" ────▶ "1" Payment                           : 소속 납입
PaymentItem "*" ────▶ "1" Contract                          : 대상 계약

PaymentRecord "*" ────▶ "1" Contract                        : 대상 계약

RefundCalculation "*" ────▶ "1" Cancellation                : 대상 해지
RefundCalculation "1" ◇────▶ "*" DeductionAdjustment        : 공제 조정 보유
RefundCalculation ┄┄┄▶ RefundPayment                        : confirm() 생성

DeductionAdjustment "*" ────▶ "0..1" FinanceManager         : 조정자

RefundPayment "*" ────▶ "1" RefundCalculation               : 대상 산출
RefundPayment "*" ────▶ "0..1" BankAccount                  : 수령 계좌
```

---

# 9️⃣ 문의 (inquiry) 도메인 — `dp.inquiry`

## `Inquiry` [문의]

```
- inquiryNo: String              [문의 번호 - 자동 부여]
- customerName: String           [문의 고객명]
🔗 inquiryType: InquiryType       [문의 유형 «enum»]
- title: String (≤50자) / content: String (≤1000자) / currentLength: Integer
- attachmentFileName: String / attachmentFileSize: Long
- receivedAt: LocalDateTime
🔗 status: InquiryStatus          [처리 상태 «enum»]
- answerContent: String / answeredAt: LocalDateTime
🔗 faqCategory: FaqCategory       [FAQ 카테고리 «enum»]
- faqQuestion / faqAnswer: String

+ validateRequired() / validateFileSize(): Boolean
+ attachFile() / removeFile(): void
+ submit(): void
```

## `CustomerCenterPage` [고객센터 페이지]

```
- activeTab: String   [활성 탭 - FAQ/1:1문의/문의내역]

+ switchTab(): void
```

### 🔗 9️⃣ 관계

```
Inquiry ────▶ «enum» InquiryType, InquiryStatus, FaqCategory
```

> Inquiry 는 답변(answerContent)·FAQ 를 자체 필드로 보관하며, 별도 Answer/FAQ 클래스를 두지 않는다.
> CustomerCenterPage 는 탭 상태만 관리하는 화면 보조 클래스이다.

---

# 🎯 전체 핵심 관계 요약

## [상속 / Generalization]

```
User ◁── Customer
User ◁── Employee ◁── EducationTrainer / InsuranceReviewer / ClaimsHandler / DispatchAgent / FinanceManager
SalesChannel ◁── Designer / Agency
(독립 클래스) SalesManager, ContractManager, Applicant
```

## [합성 / Composition - 강한 포함]

```
EducationPreparation ◆── Attendance
ActivityPlan ◆── ScheduleItem
Underwriting ◆── ReviewResult
Payment ◆── PaymentItem
ClaimRequest ◆── AccidentDetail, RecipientInfo
DamageInvestigation ◆── SupplementRequest, AdditionalInvestigation
```

## [집약 / Aggregation - 약한 포함]

```
Customer ◇── BankAccount
InterviewSchedule ◇── InterviewRecord
DispatchRecord ◇── Attachment
ClaimRequest ◇── Attachment
RefundCalculation ◇── DeductionAdjustment
```

## [연관 / Association - 주요 흐름]

```
[보험 가입 흐름]
Customer ──▶ InsuranceApplication ──▶ InsuranceProduct
SalesChannel ──▶ PolicyApplication ──▶ Underwriting ──▶ ReviewResult

[상담/면담/제안 흐름]
ConsultationRequest → InterviewSchedule ──▶ InterviewRecord ──▶ Proposal ──▶ InsuranceProduct

[계약/해지/환급 흐름]
Customer ──▶ Contract ◀── Cancellation ──▶ RefundCalculation ──▶ RefundPayment
                                            RefundCalculation ◇── DeductionAdjustment ──▶ FinanceManager

[보험료 납입 흐름]
Customer ──▶ Payment ◆── PaymentItem ──▶ Contract
PaymentRecord ──▶ Contract

[사고/보험금 흐름]
Customer ──▶ AccidentReport ──▶ Dispatch ──▶ DispatchRecord
Customer ──▶ ClaimRequest ──▶ DamageInvestigation ──▶ ClaimCalculation ──▶ ClaimPayment
Dispatch ──▶ DispatchAgent      DamageInvestigation ──▶ ClaimsHandler
```

> **느슨한 연결 주의**: 일부 도메인 클래스는 다른 객체를 직접 참조하지 않고 번호 문자열(`planNo`, `contractNo`, `appNo`, `evaluationNo` 등)이나 이름 문자열(`customerName`, `channelName` 등)로 연결한다. 다이어그램에서는 점선 의존(`┄┄┄▶`) 또는 별도 표기 없이 생략한다.