# 보험 시스템 클래스 다이어그램 (Mermaid)

> `Class_Diagram_Domain.md` 를 Mermaid `classDiagram` 으로 옮긴 버전.
> 클래스가 많아 한 장으로 그리면 렌더링이 무거워지므로 **도메인별 다이어그램 + 전체 관계 개요**로 분리한다.
> enum 값은 §0 표를 참조하고, 클래스 다이어그램에서는 속성의 타입명으로만 표기한다.
>
> **관계 표기 (Mermaid)**
> - 상속 `<|--` · 합성 `*--` · 집약 `o--` · 단방향 연관 `-->` · 의존 `..>`
> - 점선 의존 `..>` 은 **실제 객체 참조가 아니라** 메서드 호출 흐름이거나, 문자열 ID/이름(`contractNo`, `channelName` 등)으로만 가리키는 **느슨한 논리 연결**을 뜻한다.

---

## 0️⃣ 열거형 (enum) 참조

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

## 1️⃣ 행위자 (actor) 도메인

```mermaid
classDiagram
    direction TB

    class User {
        <<abstract>>
        #String userId
        #String name
        #String contact
        #String email
        +updateProfile(String contact, String email) void
    }
    class Customer {
        -String customerId
        -String phone
        -String residentNo
        -String address
        -LocalDate birthDate
        -List~BankAccount~ registeredAccounts
        -LocalDateTime registeredAt
        +enterAddress(String address) void
        +enterBirthDate(LocalDate date) void
        +registerAccount(BankAccount account) void
        +updateInfo() void
    }
    class Employee {
        <<abstract>>
        #String employeeId
        #String department
        #String position
        #LocalDate hireDate
    }
    class SalesChannel {
        -String channelId
        -String channelName
        -ChannelType channelType
        +acceptConsultation(ConsultationRequest request) void
        +createPolicyApplication() PolicyApplication
        +getActivityDetail() void
    }
    class Designer {
        -String licenseNumber
    }
    class Agency {
        -String agencyNumber
    }
    class SalesManager {
        -String managerId
        -String name
        -String department
        +approveEducationPlan(EducationPlan plan) void
        +rejectEducationPlan(EducationPlan plan, String reason) void
    }
    class EducationTrainer {
        +createEducationPlan() EducationPlan
        +registerEducationPreparation() EducationPreparation
        +conductEducation(EducationPreparation prep) EducationExecution
    }
    class InsuranceReviewer {
        +startUnderwriting(PolicyApplication app) Underwriting
        +deliverReviewResult(ReviewResult result) void
    }
    class ClaimsHandler {
        -long transferLimit
    }
    class DispatchAgent {
        -String region
        -String vehicleNo
    }
    class FinanceManager {
    }
    class ContractManager {
        -String managerId
        -String name
        -String department
    }
    class Applicant {
        -String applicantId
        -String name
        -ChannelType channelType
    }
    class BankAccount {
        -String bankName
        -String accountNo
        -String accountHolder
        -boolean verified
        +enter(String bank, String no, String holder) void
        +verify() boolean
    }

    User <|-- Customer
    User <|-- Employee
    Employee <|-- EducationTrainer
    Employee <|-- InsuranceReviewer
    Employee <|-- ClaimsHandler
    Employee <|-- DispatchAgent
    Employee <|-- FinanceManager
    SalesChannel <|-- Designer
    SalesChannel <|-- Agency

    Customer "1" o-- "*" BankAccount : 등록 계좌
    SalesChannel ..> ConsultationRequest : 수락
    SalesChannel ..> PolicyApplication : 생성
    SalesManager ..> EducationPlan : 승인/반려
    EducationTrainer ..> EducationPreparation : 생성
    InsuranceReviewer ..> Underwriting : 심사
    ContractManager ..> Contract : 계약 조회/편집(담당)
    ContractManager ..> ContractStatistics : 통계 관리(담당)
    ContractManager ..> ExpiringContractManagement : 만기 관리(담당)
    ChannelScreening ..> Applicant : 채용 심사 대상
```

> ※ `SalesManager`, `ContractManager`, `SalesChannel`, `Applicant` 는 `User`/`Employee` 를 상속하지 않는 독립 클래스이다.

---

## 2️⃣ 공통 (common) 도메인

```mermaid
classDiagram
    class Attachment {
        -String fileId
        -String fileName
        -long fileSize
        -String filePath
        -String mimeType
        -LocalDateTime uploadedAt
        +delete() void
        +download() File
    }
    class BankAccount {
        -String bankName
        -String accountNo
        -String accountHolder
        -boolean verified
        +enter(String bank, String no, String holder) void
        +verify() boolean
    }

    %% 이 부품을 객체로 보유(참조)하는 도메인 클래스 — 사용처 모아보기
    class Customer
    class Payment
    class ClaimRequest
    class ClaimPayment
    class RefundPayment
    class DispatchRecord

    Customer "1" o-- "*" BankAccount : 등록 계좌
    Payment "*" --> "0..1" BankAccount : 납입 계좌
    ClaimRequest "*" --> "1" BankAccount : 지급 계좌
    ClaimPayment "*" --> "1" BankAccount : 수령 계좌
    RefundPayment "*" --> "0..1" BankAccount : 수령 계좌
    DispatchRecord "1" o-- "*" Attachment : 사진
    ClaimRequest "1" o-- "*" Attachment : 첨부 서류
```

> `Attachment` / `BankAccount` 는 여러 도메인이 공유하는 부품 클래스다. 위 다이어그램은 **이 부품을 객체로 보유하는 사용처**를 모아 보여준 것이고, 동일한 관계가 각 도메인 다이어그램(§1·§7·§8)에도 표기된다.

---

## 3️⃣ 영업 (sales) 도메인

```mermaid
classDiagram
    direction TB

    class ActivityPlan {
        -String planId
        -String planName
        -LocalDate startDate
        -LocalDate endDate
        -String author
        -String memo
        -List~ScheduleItem~ schedules
        -Integer targetContractCount
        -Long targetContractAmount
        -Integer targetNewCustomer
        -String proposedCustomerId
        -InsuranceType proposedInsuranceType
        -String proposalReason
        -PlanStatus status
        +addSchedule(ScheduleItem item) void
        +deleteSchedule() void
        +validateRequired() Boolean
        +validateDateRange() Boolean
        +tempSave() void
        +submit() void
    }
    class ScheduleItem {
        -String customerId
        -ActivityType activityType
        -LocalDateTime activityDateTime
        -String location
        -String memo
    }
    class ChannelRecruitment {
        -String recruitmentNo
        -String managerName
        -ChannelType channelType
        -Integer recruitCount
        -LocalDate startDate
        -LocalDate endDate
        -String condition
        -LocalDateTime registeredAt
        +validateRequired() Boolean
        +save() void
    }
    class ChannelScreening {
        -String applicantName
        -ChannelType channelType
        -LocalDate applicationDate
        -String career
        -List~String~ certifications
        -ScreeningStatus screeningStatus
        -String approvalNo
        -LocalDateTime approvedAt
        -String rejectionReason
        +approve() void
        +reject() void
    }
    class SalesActivityManagement {
        -LocalDate startDate
        -LocalDate endDate
        -ChannelType channelType
        -String channelName
        -Integer visitCount
        -Integer contractCount
        -Double conversionRate
        -Double achievementRate
        -String improvementContent
        -Integer revisedTarget
        -String managerName
        -String managementNo
        -LocalDateTime registeredAt
        +saveImprovement() void
    }
    class SalesOrgEvaluation {
        -ChannelType channelType
        -String channelName
        -Long salesResult
        -Integer contractCount
        -Double achievementRate
        -EvaluationGrade evaluationGrade
        -String evaluationComment
        -String evaluationNo
        -LocalDateTime evaluatedAt
        +validateRequired() Boolean
        +saveEvaluation() void
    }
    class BonusRequest {
        -String evaluationNo
        -String channelName
        -ChannelType channelType
        -EvaluationGrade evaluationGrade
        -Double bonusRatio
        -Long baseSalary
        -Double bonusAmount
        -String requestReason
        -String requestNo
        -LocalDateTime requestedAt
        +calculateBonus() Double
        +submit() void
    }
    class CustomerRegistration {
        -String customerId
        -String name
        -String ssn
        -Boolean isSsnMasked
        -String phone
        -String address
        -String contractNo
        -InsuranceType insuranceType
        -LocalDate contractDate
        -LocalDate expiryDate
        -Long monthlyPremium
        -List~String~ specialClauses
        +validateRequired() Boolean
        +validateFormat() Boolean
        +validateDuplicate() Boolean
        +assignIds() void
    }

    %% 느슨한 연결 대상 (문자열 ID/이름 참조) — 점선 의존
    class Customer
    class Contract
    class SalesChannel
    class SalesManager
    class Applicant

    ActivityPlan "1" *-- "*" ScheduleItem : 일정 구성
    ActivityPlan ..> Customer : 제안 대상(proposedCustomerId)
    ScheduleItem ..> Customer : 대상 고객(customerId)
    ChannelRecruitment ..> SalesManager : 등록자(managerName)
    ChannelScreening ..> Applicant : 지원자(applicantName)
    SalesActivityManagement ..> SalesChannel : 대상 채널(channelName)
    SalesOrgEvaluation ..> SalesChannel : 평가 대상(channelName)
    BonusRequest ..> SalesOrgEvaluation : 평가 기반(evaluationNo)
    CustomerRegistration ..> Customer : 고객 생성(customerId)
    CustomerRegistration ..> Contract : 계약 생성(contractNo)
```

> 영업 도메인 클래스 대부분은 채널명·관리자명 등을 **문자열로 보관**하며 actor 클래스를 직접 참조하지 않는다. 점선(`..>`)은 그 문자열이 논리적으로 가리키는 대상을 표시한 것이다.

---

## 4️⃣ 교육 (education) 도메인

```mermaid
classDiagram
    direction TB

    class EducationPlan {
        -int planNumber
        -String trainerName
        -String educationName
        -LocalDate startDate
        -LocalDate endDate
        -String channelType
        -int targetCount
        -long budget
        -String educationGoal
        -String educationContent
        -String textbookList
        -String rejectReason
        -LocalDateTime approvedAt
        -String status
        +enterPlanInfo() void
        +enterContentInfo() void
        +validateRequiredFields() boolean
        +requestApproval() void
        +tempSave() void
        +approve() void
        +reject(String reason) void
    }
    class EducationPreparation {
        -int setupNumber
        -String planNo
        -LocalDateTime registeredAt
        -String location
        -String instructorName
        -String textbookStatus
        -boolean materialReady
        -String additionalNotice
        -List~Attendance~ attendanceList
        -String status
        +enterPreparationInfo() void
        +validateRequiredFields() boolean
        +save() void
        +addAttendee(String attendeeName) void
    }
    class EducationExecution {
        -int completionNumber
        -LocalDateTime completedAt
        -int attendanceCount
        -int totalCount
        -String memo
        -String status
        -EducationPreparation preparation
        +loadAttendanceList() List~Attendance~
        +markAttendance(String name, boolean attended) void
        +calculateAttendanceCount() int
        +complete() void
        +sendCompletionNotice() void
    }
    class Attendance {
        -String attendeeName
        -boolean isAttended
        +mark(boolean isAttended) void
    }

    EducationPreparation "1" *-- "*" Attendance : 출석 구성
    EducationExecution "*" --> "1" EducationPreparation : 진행 대상
    EducationExecution ..> Attendance : 출석 조회
    EducationPreparation ..> EducationPlan : 승인된 계획(planNo)
```

> `EducationPlan` → `EducationPreparation` 은 `planNo`(String) 로 느슨하게 연결된다(객체 참조 아님 → 점선 `..>`).

---

## 5️⃣ 상담/면담/청약/심사 (consultation) 도메인

```mermaid
classDiagram
    direction TB

    class ConsultationRequest {
        -int consultationNumber
        -String type
        -LocalDateTime scheduledAt
        -String location
        -String contact
        -String content
        -String status
        -LocalDateTime receivedAt
        -LocalDateTime acceptedAt
        +selectType(String type) void
        +enterConsultationInfo() void
        +validateRequiredFields() boolean
        +submit() void
        +accept() void
    }
    class InterviewSchedule {
        -int interviewNumber
        -String customerName
        -String designerName
        -String type
        -LocalDateTime scheduledAt
        -String location
        -String preparation
        -String status
        -LocalDateTime registeredAt
        -List~InterviewRecord~ interviewRecordList
        +register() void
        +modify() void
        +cancel() void
        +sendNotice() void
    }
    class InterviewRecord {
        -int recordNumber
        -String customerName
        -LocalDateTime interviewedAt
        -LocalDateTime recordedAt
        -String content
        -String customerReaction
        -String followUpAction
        +save(String content, String reaction, String followUp) void
        +modify(String content, String reaction, String followUp) void
        +navigateToProposal() Proposal
    }
    class Proposal {
        -int proposalId
        -LocalDateTime sentAt
        -String customerName
        -InsuranceProduct insuranceProduct
        +selectProduct(InsuranceProduct product) void
        +send() void
    }
    class InsuranceProduct {
        -String productName
        -String type
        -long monthlyPremium
        -String coverage
        -String specialTerms
    }
    class InsuranceApplication {
        -int applicationNumber
        -Customer customer
        -InsuranceProduct product
        -LocalDateTime appliedAt
        -String paymentMethod
        -List~String~ selectedSpecialTerms
        -String status
        +enterPersonalInfo() void
        +selectSpecialTerms(List~String~ list) void
        +selectPaymentMethod(String method) void
        +authenticate() boolean
        +apply() void
    }
    class PolicyApplication {
        -int applicationNumber
        -LocalDateTime submittedAt
        -LocalDateTime uploadedAt
        -Customer customer
        -String customerName
        -String productName
        -int period
        -String paymentMethod
        -String status
        +enterCustomerInfo() void
        +selectProduct(String name, int period, String method) void
        +attachSignature(String file) void
        +requestElectronicSignature() void
        +submit() void
    }
    class Underwriting {
        -int reviewNumber
        -String appNo
        -String customerName
        -LocalDateTime reviewedAt
        -String riskGrade
        -String reviewType
        -String reviewOpinion
        -ReviewResult reviewResult
        +startReview() void
        +autoReview() ReviewResult
        +manualReview(String type, String opinion) ReviewResult
        +attachDocument(String file) void
        +complete(String result, String condition, String reason) void
    }
    class ReviewResult {
        -String result
        -String condition
        -String rejectionReason
        -LocalDateTime confirmedAt
        -String processingNo
        +deliver() void
        +confirm() void
    }
    class Revival {
        -int revivalNumber
        -Customer customer
        -String contractNo
        -LocalDateTime appliedAt
        -long unpaidAmount
        -String paymentMethod
        -String contact
        +checkEligibility() boolean
        +calculateUnpaidAmount() long
        +pay(String method) boolean
        +authenticate() boolean
        +submit() void
    }
    class Customer
    class Contract

    InterviewSchedule "1" o-- "*" InterviewRecord : 면담 기록 보유
    InterviewRecord ..> Proposal : 제안서로 이동
    Proposal "*" --> "1" InsuranceProduct : 제안 상품
    InsuranceApplication "*" --> "1" Customer : 신청 고객
    InsuranceApplication "*" --> "1" InsuranceProduct : 대상 상품
    PolicyApplication "*" --> "1" Customer : 청약 고객
    Underwriting "1" *-- "1" ReviewResult : 심사 결과
    Underwriting ..> PolicyApplication : 청약 참조(appNo)
    ConsultationRequest ..> InterviewSchedule : 상담 후 면담(흐름)
    Revival "*" --> "1" Customer : 신청 고객
    Revival ..> Contract : 실효 계약(contractNo)
```

> `Underwriting`→`PolicyApplication`, `Revival`→`Contract` 등은 `appNo`·`contractNo`(String) 로 느슨하게 연결된다.

---

## 6️⃣ 계약 (contract) 도메인

```mermaid
classDiagram
    direction TB

    class Contract {
        -String contractNo
        -String policyNo
        -Customer customer
        -LocalDate contractDate
        -LocalDate expiryDate
        -long monthlyPremium
        -String insuranceType
        -ContractStatus status
        -Boolean isExpiringSoon
        -Integer totalPayCount
        -Integer paidCount
        -LocalDate lastPaymentDate
        -Boolean isOverdue
        -Integer overdueCount
        -List~String~ specialClauses
        -List~Long~ clausePremiums
        +isMaturityNear() boolean
        +updateStatus(String newStatus) void
        +changePaymentMethod(String method, BankAccount account) void
        +verifyAccount(int amount, String code) boolean
    }
    class Cancellation {
        -String cancellationNo
        -Contract contract
        -String reason
        -String detailReason
        -boolean noticeAgreed
        -boolean authResult
        -long expectedRefund
        -LocalDateTime canceledAt
        -String status
        +selectReason(String reason) void
        +enterDetailReason(String detail) void
        +validateReasonInput() boolean
        +agreeToNotice() void
        +authenticate() boolean
        +calculateExpectedRefund() long
        +submit() void
        +confirm() void
        +cancel() void
    }
    class ContractStatistics {
        -String contractNo
        -String contractorName
        -Integer paySequence
        -LocalDate paymentDate
        -Long paymentAmount
        -ContractPaymentStatus paymentStatus
        -LocalDateTime claimDate
        -Long claimAmount
        -Long paidAmount
        -ClaimStatus claimStatus
        -YearMonth filterStartMonth
        -YearMonth filterEndMonth
        -String fileName
        +validateDateRange() Boolean
        +exportToExcel() File
    }
    class ContractFilter {
        -String customerId
        -String name
        -String ssn
        -String phone
        -String contractNo
        -String insuranceType
        -LocalDate contractDate
        +apply() void
        +reset() void
    }
    class ExpiringContractManagement {
        -String contractNo
        -String contractorName
        -String insuranceType
        -LocalDate expiryDate
        -Integer remainingDays
        -String phone
        -String email
        -Boolean isRenewable
        -Long expectedPremium
        -LocalDateTime noticeDate
        -String noticeMemo
        -CustomerResponse customerResponse
        -Long renewalPremium
        -Long premiumDiff
        +sendNoticeSms() void
        +confirmRenewal() void
        +switchToTermination() void
        +sendPendingAlert() void
    }
    class Customer

    Contract "*" --> "1" Customer : 계약자
    Cancellation "*" --> "1" Contract : 대상 계약
    ContractStatistics ..> Contract : 통계 대상(contractNo)
    ExpiringContractManagement ..> Contract : 만기 대상(contractNo)
    ContractFilter ..> Contract : 검색 조건(contractNo)
```

> `ContractStatistics` / `ContractFilter` / `ExpiringContractManagement` 는 `contractNo`(String) 와 화면 데이터로 계약을 보관하며 `Contract` 객체를 직접 참조하지 않는다(점선 `..>` 으로 논리적 대상만 표시).

---

## 7️⃣ 사고/현장출동/보험금 (claim) 도메인

```mermaid
classDiagram
    direction TB

    class AccidentReport {
        -String reportNo
        -Customer customer
        -String vehicleNo
        -String ownerName
        -String phoneNo
        -AccidentType accidentType
        -String damageType
        -String location
        -boolean needsDispatch
        -boolean agreedTerms
        -LocalDateTime reportedAt
        -AccidentReportStatus status
        +selectAccidentType(AccidentType type, String damage) void
        +validateRequiredFields() boolean
        +verifyContract() boolean
        +receive() void
        +requestDispatch() Dispatch
    }
    class Dispatch {
        -String dispatchNo
        -AccidentReport accident
        -DispatchAgent agent
        -LocalDateTime estimatedArrival
        -LocalDateTime arrivalTime
        -DispatchStatus status
        -String cancelReason
        +assignAgent(DispatchAgent agent) void
        +depart() void
        +arrive() void
        +complete() void
        +cancel(String reason) void
    }
    class DispatchRecord {
        -String recordId
        -Dispatch dispatch
        -List~Attachment~ photos
        -boolean policeRequired
        -boolean towingRequired
        -String notes
        -LocalDateTime transmittedAt
        -DispatchRecordStatus status
        +uploadPhoto(String category, Attachment photo) void
        +validateRequired() boolean
        +transmit() void
    }
    class ClaimRequest {
        -String claimNo
        -Customer customer
        -Contract contract
        -Customer insured
        -AuthMethod authMethod
        -boolean authenticated
        -ClaimType claimType
        -List~String~ claimReasons
        -String diagnosis
        -AccidentDetail accidentDetail
        -RecipientInfo recipientInfo
        -BankAccount bankAccount
        -NoticeMethod noticeMethod
        -List~Attachment~ attachments
        -LocalDateTime requestedAt
        -ClaimRequestStatus status
        +authenticate() boolean
        +verifyAccount() boolean
        +validateBeforeSubmit() boolean
        +submit() void
        +cancel() void
    }
    class AccidentDetail {
        -AccidentSubType accidentSubType
        -String content
        -LocalDate date
        -String location
        +enter(AccidentSubType subType, String content, LocalDate date, String loc) void
    }
    class RecipientInfo {
        -String name
        -String residentNo
        -String contact
        +changeContact(String contact) void
    }
    class SupplementRequest {
        -List~String~ requestedItems
        -String message
        -LocalDateTime sentAt
        +send() void
    }
    class AdditionalInvestigation {
        -String visitLocation
        -LocalDateTime schedule
        -String reason
        -LocalDateTime registeredAt
    }
    class DamageInvestigation {
        -String investigationNo
        -ClaimRequest claim
        -ClaimsHandler handler
        -double ourFaultRatio
        -double counterFaultRatio
        -long recognizedDamage
        -String opinion
        -InvestigationResult result
        -String rejectReason
        -SupplementRequest supplementRequest
        -AdditionalInvestigation additionalInvestigation
        -LocalDateTime investigatedAt
        -InvestigationStatus status
        +assignHandler(ClaimsHandler handler) void
        +enterFaultRatio(double our, double counter) void
        +validateFaultRatio() boolean
        +requestSupplement(List~String~ items, String msg) SupplementRequest
        +requestAdditionalInvestigation(String loc, LocalDateTime sch, String reason) AdditionalInvestigation
        +complete() ClaimCalculation
        +closeAsRejected() void
    }
    class ClaimCalculation {
        -String calculationNo
        -DamageInvestigation investigation
        -long recognizedDamage
        -double faultRatio
        -long deductible
        -long coverageLimit
        -long finalAmount
        -boolean adjusted
        -boolean exceededDeductible
        -Employee approver
        -boolean approvalRequired
        -LocalDateTime calculatedAt
        -CalculationStatus status
        +loadCalculationData() void
        +calculate() long
        +applyCoverageLimit() void
        +checkDeductibleExceeded() boolean
        +selectApprover(Employee approver) void
        +submitForApproval() void
        +approve() ClaimPayment
        +closeAsExceeded() void
    }
    class ClaimPayment {
        -String paymentNo
        -ClaimCalculation calculation
        -RecipientInfo recipient
        -BankAccount account
        -long finalAmount
        -PaymentType paymentType
        -LocalDateTime scheduledAt
        -LocalDateTime paidAt
        -String otpInput
        -boolean otpVerified
        -List~NoticeMethod~ noticeOption
        -boolean noticeSent
        -boolean transferFailed
        -ClaimPaymentStatus status
        +selectPaymentType(PaymentType type) void
        +enterOTP(String otp) void
        +verifyOTP() boolean
        +execute() void
        +schedule() void
        +handleTransferFailure(String reason) void
        +sendCompletionNotice() void
        +close() void
    }
    class Customer
    class Contract
    class DispatchAgent
    class ClaimsHandler
    class Employee
    class Attachment
    class BankAccount

    AccidentReport "*" --> "1" Customer : 사고자
    AccidentReport ..> Dispatch : requestDispatch()
    Dispatch "*" --> "1" AccidentReport : 대상 사고
    Dispatch "*" --> "0..1" DispatchAgent : 배정 직원
    DispatchRecord "*" --> "1" Dispatch : 대상 출동
    DispatchRecord "1" o-- "*" Attachment : 사진 보유

    ClaimRequest "*" --> "1" Customer : 청구 고객
    ClaimRequest "*" --> "1" Customer : 피보험자
    ClaimRequest "*" --> "1" Contract : 대상 계약
    ClaimRequest "1" *-- "0..1" AccidentDetail : 사고 상세
    ClaimRequest "1" *-- "1" RecipientInfo : 수령인 정보
    ClaimRequest "*" --> "1" BankAccount : 지급 계좌
    ClaimRequest "1" o-- "*" Attachment : 첨부 서류

    DamageInvestigation "*" --> "1" ClaimRequest : 대상 청구
    DamageInvestigation "*" --> "0..1" ClaimsHandler : 담당자
    DamageInvestigation "1" *-- "0..1" SupplementRequest : 보완 서류 요청
    DamageInvestigation "1" *-- "0..1" AdditionalInvestigation : 추가 조사 지시
    DamageInvestigation ..> ClaimCalculation : complete()

    ClaimCalculation "*" --> "1" DamageInvestigation : 대상 조사
    ClaimCalculation "*" --> "0..1" Employee : 결재권자
    ClaimCalculation ..> ClaimPayment : approve()

    ClaimPayment "*" --> "1" ClaimCalculation : 대상 산출
    ClaimPayment "*" --> "1" RecipientInfo : 수령인
    ClaimPayment "*" --> "1" BankAccount : 수령 계좌
```

---

## 8️⃣ 납입/환급 (payment) 도메인

```mermaid
classDiagram
    direction TB

    class Payment {
        -String paymentNo
        -Customer customer
        -List~PaymentItem~ items
        -PaymentMethod paymentMethod
        -BankAccount account
        -long totalAmount
        -long discountedAmount
        -long earlyDiscount
        -LocalDateTime requestedAt
        -PaymentStatus status
        +selectContracts(List~Contract~ contracts) void
        +enterPaymentCount(PaymentItem item, int count) void
        +selectPaymentMethod(PaymentMethod method) void
        +registerNewAccount(String bank, String no, String holder) void
        +verifyAccount() boolean
        +calculateTotal() long
        +submit() void
    }
    class PaymentItem {
        -Payment payment
        -Contract contract
        -long premiumPerCount
        -int count
        -long subtotal
        +setCount(int count) void
        +calculateSubtotal() long
    }
    class PaymentRecord {
        -String recordNo
        -Contract contract
        -LocalDate paymentDate
        -long amount
        -String method
        -PaymentRecordStatus status
        -int installmentNo
        -long lateFee
        -String approvalNo
        -RejectCategory rejectCategory
        -String rejectReason
        -LocalDateTime confirmedAt
        -LocalDateTime rejectedAt
        +load() void
        +confirm() void
        +enterRejectInfo(RejectCategory category, String reason) void
        +reject() void
    }
    class OverdueNoticeSetting {
        -boolean enabled
        -int daysAfterDue
        -String messageTemplate
        -LocalDateTime savedAt
        +toggleEnabled(boolean enabled) void
        +setDaysAfterDue(int days) void
        +previewMessage() String
        +save() void
    }
    class RefundCalculation {
        -String refundNo
        -Cancellation cancellation
        -long totalPaidPremium
        -long reserveAmount
        -String paymentPeriod
        -double appliedRate
        -long baseRefund
        -long unpaidPremium
        -long loanPrincipal
        -long loanInterest
        -long finalRefund
        -List~DeductionAdjustment~ adjustments
        -RefundStatus status
        -LocalDateTime calculatedAt
        -LocalDateTime confirmedAt
        +loadContractData() void
        +validateRequiredData() boolean
        +calculateFinalRefund() long
        +adjustDeduction(String item, long amount, String note) DeductionAdjustment
        +recalculate() void
        +exportPDF() File
        +confirm() RefundPayment
    }
    class DeductionAdjustment {
        -String itemName
        -long originalAmount
        -long adjustedAmount
        -LocalDateTime adjustedAt
        -FinanceManager adjustedBy
        -String note
        +apply() void
    }
    class RefundPayment {
        -String paymentNo
        -RefundCalculation refund
        -BankAccount account
        -long finalAmount
        -String otpInput
        -boolean otpVerified
        -int otpFailCount
        -boolean locked
        -LocalDateTime transferredAt
        -boolean noticeSent
        -RefundPaymentStatus status
        +loadAccountInfo() void
        +enterOTP(String otp) void
        +verifyOTP() boolean
        +lockOnFailure() void
        +execute() void
        +handleTransferFailure() void
        +sendNotice() void
    }
    class Customer
    class Contract
    class Cancellation
    class FinanceManager
    class BankAccount

    Payment "*" --> "1" Customer : 신청 고객
    Payment "1" *-- "1..*" PaymentItem : 납입 항목
    Payment "*" --> "0..1" BankAccount : 납입 계좌
    PaymentItem "*" --> "1" Payment : 소속 납입
    PaymentItem "*" --> "1" Contract : 대상 계약
    PaymentRecord "*" --> "1" Contract : 대상 계약
    RefundCalculation "*" --> "1" Cancellation : 대상 해지
    RefundCalculation "1" o-- "*" DeductionAdjustment : 공제 조정
    RefundCalculation ..> RefundPayment : confirm()
    DeductionAdjustment "*" --> "0..1" FinanceManager : 조정자
    RefundPayment "*" --> "1" RefundCalculation : 대상 산출
    RefundPayment "*" --> "0..1" BankAccount : 수령 계좌
    FinanceManager ..> OverdueNoticeSetting : 미납 알림 설정(담당)
```

> `OverdueNoticeSetting` 은 다른 도메인 객체를 필드로 참조하지 않는 **시스템 단위 설정값**이라, 재무회계 담당자(`FinanceManager`)가 설정한다는 점선(담당)으로만 연결된다.

---

## 9️⃣ 문의 (inquiry) 도메인

```mermaid
classDiagram
    class Inquiry {
        -String inquiryNo
        -String customerName
        -InquiryType inquiryType
        -String title
        -String content
        -Integer currentLength
        -String attachmentFileName
        -Long attachmentFileSize
        -LocalDateTime receivedAt
        -InquiryStatus status
        -String answerContent
        -LocalDateTime answeredAt
        -FaqCategory faqCategory
        -String faqQuestion
        -String faqAnswer
        +validateRequired() Boolean
        +validateFileSize() Boolean
        +attachFile() void
        +removeFile() void
        +submit() void
    }
    class CustomerCenterPage {
        -String activeTab
        +switchTab() void
    }
    class Customer

    Inquiry ..> Customer : 문의 고객(customerName)
    CustomerCenterPage ..> Inquiry : 1대1 문의 탭(흐름)
```

> `Inquiry` 는 답변·FAQ 를 자체 필드로 보관하며 별도 Answer/FAQ 클래스를 두지 않는다.

---

## 🎯 전체 관계 개요 (도메인 간 핵심 흐름)

> 속성/메서드는 생략하고 도메인 간 주요 연관·생성 흐름만 표현한다.

```mermaid
classDiagram
    direction LR

    Customer --> InsuranceApplication : 신청
    InsuranceApplication --> InsuranceProduct
    SalesChannel --> PolicyApplication : 작성
    PolicyApplication ..> Underwriting : 심사
    Underwriting *-- ReviewResult

    ConsultationRequest ..> InterviewSchedule
    InterviewSchedule o-- InterviewRecord
    InterviewRecord ..> Proposal
    Proposal --> InsuranceProduct

    Customer --> Contract : 계약자
    Cancellation --> Contract
    Cancellation --> RefundCalculation
    RefundCalculation o-- DeductionAdjustment
    RefundCalculation ..> RefundPayment

    Customer --> Payment
    Payment *-- PaymentItem
    PaymentItem --> Contract
    PaymentRecord --> Contract

    Customer --> AccidentReport
    AccidentReport ..> Dispatch
    Dispatch --> DispatchAgent
    Dispatch ..> DispatchRecord

    Customer --> ClaimRequest
    ClaimRequest --> Contract
    ClaimRequest ..> DamageInvestigation
    DamageInvestigation --> ClaimsHandler
    DamageInvestigation ..> ClaimCalculation
    ClaimCalculation ..> ClaimPayment

    Customer --> Revival
```
