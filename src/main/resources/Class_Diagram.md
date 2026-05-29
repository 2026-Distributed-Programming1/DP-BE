# 클래스다이어그램_통합정리

# 보험 시스템 클래스 다이어그램 (데이터 중심 설계)

> 분산 프로그래밍 시나리오 기반 클래스 다이어그램 통합 정리
> 

---

## 📌 표기법 안내

### 가시성

- `+` public
- `-` private
- `#` protected

### 필드 / 메서드 표기

- 필드: `가시성 이름: 타입 [한글]`
- 메서드: `가시성 이름(매개변수: 타입): 반환타입 [한글]`
- `🔗` 마커: 다른 클래스 타입 필드 (다이어그램 그릴 때 관계선으로 대체 가능, **추후 지우셔도 됨**)
- `{static}`: 클래스 단위(인스턴스 없이) 호출되는 정적 메서드
- 한글 [대괄호] 표시는 이해를 위한 주석이며, **추후 지우셔도 됨**

### 관계

- **Association (단방향 연관)**: `────▶` (참조하는 쪽 → 참조되는 쪽)
- **Association (양방향 연관)**: `─────` (화살표 없음)
- **Aggregation (집약, 약한 포함)**: `◇────▶`
- **Composition (합성, 강한 포함)**: `◆────▶`
- **Generalization (상속)**: `─────▷` (자식 → 부모)
- **다중성**: `1`, `0..1`, , `1..*`

### 화살표 방향 원칙

- 클래스 안에 다른 클래스 타입의 필드(🔗)가 있으면 → 그 클래스를 향해 화살표
- 라벨은 행위형(“관리한다”)보다 역할형(“관리자”)로 표기

---

# 1️⃣ 행위자 (Actor) 도메인

## `User` [사용자] (추상 클래스)

```
- userId: String                  [사용자 ID - 가입 시 자동 부여]
- name: String                    [이름]
- contact: String                 [연락처]
- email: String                   [이메일]
- password: String                [비밀번호]
- loggedIn: boolean               [로그인 상태]

+ User(name: String, contact: String, email: String): User    [생성자 - userId 자동 부여]
+ login(id: String, pw: String): boolean                       [로그인]
+ logout(): void                                               [로그아웃]
+ updateProfile(contact: String, email: String): void          [정보 수정]
+ authenticate(method: String): boolean                        [본인인증 - 휴대폰/간편/공동인증서]
```

## `Customer` [고객]

```
- customerNo: String              [고객번호 - 자동 부여]
- residentNo: String              [주민등록번호]
- address: String                 [주소]
- birthDate: Date                 [생년월일]
- registeredAccounts: List<BankAccount>     [등록된 계좌 목록]
- registeredAt: Date              [가입일시 - 생성 시 자동 설정]

+ Customer(name: String, residentNo: String, contact: String, email: String): Customer
                                          [생성자 - 고객번호 자동 부여]
+ enterAddress(address: String): void     [주소 입력]
+ enterBirthDate(date: Date): void        [생년월일 입력]
+ registerAccount(account: BankAccount): void              [계좌 등록]
+ agreeToTerms(termsType: String): void                    [약관 동의 - 개인정보/위치기반 등]

# 상담/문의
+ requestConsultation(): Consultation                      [상담 요청]
+ inquire(type: String, title: String, content: String): Inquiry [문의하기]
+ viewMyInquiries(): List<Inquiry>                         [문의 내역 조회]
+ viewFAQ(category: String): List<FAQ>                     [FAQ 조회]

# 보험 가입·조회·관리
+ searchProducts(criteria: SearchCriteria): List<InsuranceProduct>   [보험상품 조회]
+ applyInsurance(product: InsuranceProduct): InsuranceApplication    [보험 신청]
+ requestReinstatement(contract: InsuranceContract): Reinstatement   [부활 요청]
+ viewMyContracts(): List<InsuranceContract>              [가입 보험 조회]
+ viewContractDetail(contract: InsuranceContract): void   [계약 상세 조회]
+ requestCancellation(contract: InsuranceContract): Cancellation     [보험 해지 신청]
+ viewCertificate(contract: InsuranceContract): File      [가입 증명서 조회]
+ viewPolicy(contract: InsuranceContract): File           [약관 조회]

# 보험료 납입
+ requestPayment(contracts: List<InsuranceContract>, count: int, account: BankAccount): Payment
                                          [보험료 납입 신청]
+ changePaymentMethod(contract: InsuranceContract, account: BankAccount): void
                                          [납입 방법 변경]
+ viewPaymentHistory(contract: InsuranceContract, period: String): List<PaymentRecord>
                                          [납입 내역 조회]

# 사고/보험금
+ reportAccident(): AccidentReport        [사고 접수]
+ submitClaim(contract: InsuranceContract): ClaimRequest   [보험금 청구]
+ updateAccidentLocation(dispatch: Dispatch, location: String): void   [사고 위치 수정]
+ cancelDispatch(dispatch: Dispatch, reason: String): void [출동 요청 취소]

# 만기/갱신 응답
+ respondToMaturity(contract: InsuranceContract, choice: String): void
                                          [만기 응답 - 갱신/해지/추후결정]
```

## `Employee` [사원] (추상 클래스)

```
- employeeId: String              [사원 ID]
- department: String              [부서]
- position: String                [직책]
- hireDate: Date                  [입사일]

+ Employee(name: String, dept: String, position: String): Employee   [생성자]
```

## `SalesChannel` [판매채널] (추상 클래스 - Employee 상속)

```
- channelId: String               [채널 ID]
- channelName: String             [채널명]
- channelType: String             [채널 유형 - 설계사/대리점]
- visitCount: int                 [누적 방문건수]
- contractCount: int              [누적 계약건수]
- targetGoal: long                [목표]
- achievement: long               [실적]

+ SalesChannel(name: String, type: String): SalesChannel    [생성자]

# 고객 관리
+ registerCustomerInfo(): CustomerInfo                     [고객 정보 등록]

# 활동 계획
+ writeActivityPlan(): ActivityPlan                        [활동 계획 작성]
+ saveActivityPlanDraft(plan: ActivityPlan): void          [활동 계획 임시저장]

# 상담·면담
+ acceptConsultation(consultation: Consultation): void     [상담 수락]
+ viewInterviewSchedule(criteria: SearchCriteria): List<Interview> [면담 일정 조회]
+ registerInterview(customer: Customer): Interview         [면담 등록]
+ modifyInterview(interview: Interview): void              [면담 수정]
+ cancelInterview(interview: Interview): void              [면담 취소]
+ sendInterviewNotice(interview: Interview): void          [면담 알림 발송]

# 면담 기록
+ viewInterviewRecords(criteria: SearchCriteria): List<InterviewRecord>   [면담 기록 조회]
+ writeInterviewRecord(interview: Interview): InterviewRecord  [면담 기록 작성]
+ modifyInterviewRecord(record: InterviewRecord): void     [면담 기록 수정]

# 보험상품 제안
+ proposeProduct(record: InterviewRecord): ProductProposal [상품 제안 시작]
+ searchProductForProposal(keyword: String): List<InsuranceProduct>  [제안용 상품 검색]
+ sendProposal(proposal: ProductProposal): void            [제안서 발송]

# 청약/심사
+ writeApplication(customer: Customer, product: InsuranceProduct): Application  [청약서 작성]
+ requestEsign(application: Application): void             [전자서명 요청]
+ confirmUnderwritingResult(result: UnderwritingResult): void   [심사 결과 확인]
+ guideCustomer(result: UnderwritingResult): void          [조건부 승인 시 고객 안내]

# 활동 요약
+ getActivitySummary(): SalesActivity                      [활동 요약 조회]
```

## `Planner` [설계사] - SalesChannel 상속

```
- licenseNo: String               [설계사 자격증 번호]
- affiliatedAgency: String        [소속 지점]

+ Planner(name: String, licenseNo: String, agency: String): Planner   [생성자]
```

## `Agency` [대리점] - SalesChannel 상속

```
- agencyCode: String              [대리점 코드]
- representativeName: String      [대표자명]
- businessNo: String              [사업자등록번호]

+ Agency(name: String, code: String, rep: String, bizNo: String): Agency   [생성자]
```

## `SalesManager` [영업 관리자] - Employee 상속

```
+ SalesManager(name: String, dept: String, position: String): SalesManager   [생성자]

# 영업 활동 관리
+ viewSalesActivities(period: DateRange, channelType: String): List<SalesActivity>
                                          [영업활동 현황 조회]
+ selectChannel(channel: SalesChannel): SalesActivity   [채널 선택 → 상세 조회]
+ registerImprovementOrder(activity: SalesActivity, content: String, goal: int): ImprovementOrder
                                          [개선 지시 등록]

# 채널 모집
+ openRecruitment(): Recruitment          [신규 모집 등록]
+ viewRecruitmentList(): List<Recruitment> [기존 모집 공고 목록 조회]

# 채용 심사
+ viewApplicants(period: DateRange, type: String, status: String): List<Applicant>
                                          [지원자 목록 조회]
+ approveApplicant(applicant: Applicant): ReviewResult       [심사 승인]
+ rejectApplicant(applicant: Applicant, reason: String): ReviewResult [심사 거절]

# 채널 평가
+ viewChannelPerformance(period: DateRange, type: String): List<ChannelEvaluation>
                                          [채널별 성과 조회]
+ registerEvaluation(channel: SalesChannel, grade: String, opinion: String): ChannelEvaluation
                                          [평가 등록]

# 교육 계획 승인
+ approveTrainingPlan(plan: TrainingPlan): void              [교육 계획 승인]
+ rejectTrainingPlan(plan: TrainingPlan, reason: String): void [교육 계획 반려]

# 활동 계획 검토
+ reviewActivityPlan(plan: ActivityPlan): void               [활동 계획 검토]
```

## `TrainingManager` [영업교육 담당자] - Employee 상속

```
+ TrainingManager(name: String, dept: String, position: String): TrainingManager   [생성자]

# 교육 계획
+ createTrainingPlan(): TrainingPlan      [교육 계획 작성]
+ requestApproval(plan: TrainingPlan): void [승인 요청]

# 교육 제반 등록
+ viewApprovedPlans(keyword: String, period: DateRange): List<TrainingPlan>
                                          [승인된 교육 계획안 조회]
+ registerPreparation(plan: TrainingPlan): TrainingPreparation [제반 등록]

# 교육 진행
+ proceedTraining(prep: TrainingPreparation): TrainingExecution [교육 진행 시작]
+ markAttendance(execution: TrainingExecution, trainee: SalesChannel, attended: boolean): void
                                          [출석 체크]
+ completeTraining(execution: TrainingExecution): void       [교육 완료 처리]
```

## `Underwriter` [보험 심사자] - Employee 상속

```
- specialty: String               [전문 심사 분야 - 진단/특인/일반/이미지]

+ Underwriter(name: String, dept: String, position: String, specialty: String): Underwriter   [생성자]

+ viewPendingApplications(): List<Application>     [심사 대기 목록 조회]
+ selectApplication(app: Application): Underwriting [심사할 청약 건 선택]
+ startReview(uw: Underwriting): void              [심사 시작]
+ runAutoReview(uw: Underwriting): void            [자동 심사 실행]
+ performManualReview(uw: Underwriting, type: String, docs: List<Attachment>, opinion: String): void
                                          [수동 심사 수행 - A1]
+ submitFinalResult(uw: Underwriting, result: String, conditions: String, reason: String): UnderwritingResult
                                          [최종 심사 결과 제출]
```

## `ClaimsHandler` [보상담당자] - Employee 상속

```
- transferLimit: long             [전결 한도 - 결재 상신 기준]

+ ClaimsHandler(name: String, dept: String, position: String, limit: long): ClaimsHandler   [생성자]

# 손해 조사
+ viewNewAssignments(): List<DamageInvestigation>   [신규 배정 목록 조회]
+ selectInvestigation(claim: ClaimRequest): DamageInvestigation [조사 건 선택]
+ enterFaultRatio(inv: DamageInvestigation, our: double, counter: double): void
                                          [과실 비율 입력]
+ requestSupplement(inv: DamageInvestigation, items: List<String>, msg: String): SupplementRequest
                                          [보완 서류 요청 - A1]
+ requestAdditionalInvestigation(inv: DamageInvestigation, loc: String, schedule: Date, reason: String): AdditionalInvestigation
                                          [추가 조사 지시 - A2]
+ completeInvestigation(inv: DamageInvestigation): ClaimCalculation [조사 완료 및 산출 이관]
+ closeAsRejected(inv: DamageInvestigation, reason: String): void [면책 종결 처리 - A3]

# 보험금 산출
+ reviewCalculation(calc: ClaimCalculation): void   [산출 내역 검토]
+ submitForApproval(calc: ClaimCalculation, approver: Employee): void
                                          [결재 상신 - A1, 전결 한도 초과 시]
+ approveCalculation(calc: ClaimCalculation): ClaimPayment
                                          [지급 승인 및 이관]

# 보험금 지급
+ executePayment(payment: ClaimPayment, otp: String): void   [이체 실행]
+ schedulePayment(payment: ClaimPayment, dt: Date): void     [예약 지급 - A1]
+ handleTransferFailure(payment: ClaimPayment): void         [이체 실패 처리 - E2]
```

## `DispatchAgent` [현장출동 직원] - Employee 상속

```
- region: String                  [담당 지역]
- vehicleNo: String               [출동 차량 번호]

+ DispatchAgent(name: String, dept: String, position: String, region: String, vehicleNo: String): DispatchAgent
                                          [생성자]

+ viewAssignedDispatches(): List<Dispatch>          [배정 목록 조회]
+ selectDispatch(dispatch: Dispatch): void          [출동 건 선택]
+ callCustomer(dispatch: Dispatch): void            [고객에게 전화 - A2]
+ markArrival(dispatch: Dispatch): DispatchRecord   [현장 도착 처리]
+ uploadPhotos(record: DispatchRecord, category: String, photos: List<Attachment>): void
                                          [필수 사진 업로드 - 전경/파손/번호판/블랙박스]
+ enterFieldInfo(record: DispatchRecord, police: boolean, towing: boolean, notes: String): void
                                          [경찰·견인·특이사항 입력]
+ transmitRecord(record: DispatchRecord): void      [기록 전송]
```

## `FinanceManager` [재무회계 담당자] - Employee 상속

```
+ FinanceManager(name: String, dept: String, position: String): FinanceManager   [생성자]

# 납부 내역 관리
+ viewPaymentRecords(period: DateRange, status: String): List<PaymentRecord>
                                          [납부 내역 조회]
+ selectPaymentRecord(record: PaymentRecord): void  [상세 확인]
+ confirmPayment(record: PaymentRecord): void       [수납 확정 및 장부 반영]
+ rejectPayment(record: PaymentRecord, category: String, reason: String): void
                                          [수납 반려 - A3]
+ configureOverdueNotice(setting: OverdueNoticeSetting): void [미납 알림 자동 발송 설정]

# 해약 환급금
+ viewRefundList(status: String, name: String, policyNo: String): List<RefundCalculation>
                                          [환급금 대상 조회]
+ selectRefund(refund: RefundCalculation): void     [상세 확인]
+ calculateRefund(cancellation: Cancellation): RefundCalculation [환급금 산출]
+ adjustDeduction(refund: RefundCalculation, item: String, amount: long, note: String): DeductionAdjustment
                                          [공제 수기 조정 - A1]
+ exportRefundPDF(refund: RefundCalculation): File  [산출 내역서 PDF - A2]
+ confirmRefund(refund: RefundCalculation): RefundPayment    [환급금 확정 및 이관]

# 환급금 지급
+ executeRefundPayment(payment: RefundPayment, otp: String): void [이체 실행]
+ handleRefundTransferFailure(payment: RefundPayment): void  [이체 실패 처리 - E2]
```

## `ContractManager` [계약관리 담당자] - Employee 상속

```
+ ContractManager(name: String, dept: String, position: String): ContractManager   [생성자]

# 계약 정보 조회
+ searchContracts(criteria: SearchCriteria): List<InsuranceContract>
                                          [계약 검색 필터 조회]
+ selectContract(contract: InsuranceContract): void [계약 상세 조회]
+ editContract(contract: InsuranceContract): void   [계약 편집]

# 만기 계약 관리
+ initiateMaturityManagement(contract: InsuranceContract): MaturityManagement
                                          [만기 관리 시작]
+ sendMaturityNotice(mgmt: MaturityManagement): void [만기 안내 문자 발송]
+ recordNotice(mgmt: MaturityManagement, memo: String): NoticeRecord [안내 기록]
+ processRenewal(mgmt: MaturityManagement, end: Date, premium: long): Renewal
                                          [갱신 처리 - A1]
+ switchToCancellation(mgmt: MaturityManagement): Cancellation [해지 처리 전환 - A2]

# 계약 통계
+ viewContractStatistics(contract: InsuranceContract): ContractStatistics [개별 계약 통계]
+ viewOverallStatistics(criteria: SearchCriteria): StatsSummary [전체 계약 통계 - A1]
+ exportStatisticsExcel(stats: ContractStatistics): File [엑셀 다운로드]
```

## `HRManager` [인사 담당자] - Employee 상속

```
+ HRManager(name: String, dept: String, position: String): HRManager   [생성자]

+ receiveChannelRegistrationRequest(applicant: Applicant): void
                                          [채널 등록 요청 수신]
+ registerAsChannel(applicant: Applicant): SalesChannel      [승인된 지원자를 채널로 등록]
+ handleRegistrationFailure(applicant: Applicant): void      [등록 처리 실패 - E2]
+ receiveBonusRequest(request: BonusRequest): void  [성과급 요청 수신]
+ processBonusRequest(request: BonusRequest): void  [성과급 요청 처리]
```

### 🔗 1️⃣ 도메인 관계 (상속)

```
User [사용자] ◁───── Customer [고객]
User [사용자] ◁───── Employee [사원]
Employee [사원] ◁───── SalesManager [영업 관리자]
Employee [사원] ◁───── TrainingManager [영업교육 담당자]
Employee [사원] ◁───── Underwriter [보험 심사자]
Employee [사원] ◁───── ClaimsHandler [보상담당자]
Employee [사원] ◁───── DispatchAgent [현장출동 직원]
Employee [사원] ◁───── FinanceManager [재무회계 담당자]
Employee [사원] ◁───── ContractManager [계약관리 담당자]
Employee [사원] ◁───── HRManager [인사 담당자]
Employee [사원] ◁───── SalesChannel [판매채널]
SalesChannel [판매채널] ◁───── Planner [설계사]
SalesChannel [판매채널] ◁───── Agency [대리점]
```

---

# 2️⃣ 영업 관리 도메인

## `SalesActivity` [영업활동]

```
- activityId: String              [활동 ID - 자동 부여]
🔗 channel: SalesChannel           [대상 채널 - 생성자에서 주입]
- visitCount: int                 [방문건수]
- contractCount: int              [계약건수]
- conversionRate: double          [전환율]
- achievementRate: double         [목표달성률]
- periodStart: Date               [관리 기간 시작일]
- periodEnd: Date                 [관리 기간 종료일]

+ SalesActivity(channel: SalesChannel, start: Date, end: Date): SalesActivity
                                          [생성자 - 활동 ID 자동 부여, 자동 집계]
+ aggregateStats(): void                  [방문건수·계약건수 집계]
+ calculateConversionRate(): double       [전환율 계산]
+ calculateAchievementRate(): double      [목표달성률 계산]
+ isUnderTarget(): boolean                [목표 미달 여부 - 70% 미만]
+ registerImprovementOrder(content: String, modifiedGoal: int): ImprovementOrder
                                          [개선 지시 등록]
```

## `ImprovementOrder` [개선지시]

```
- orderId: String                 [관리번호 - 자동 부여]
🔗 activity: SalesActivity         [대상 활동 - 생성자에서 주입]
🔗 registrar: SalesManager         [등록자 - 생성자에서 주입]
- content: String                 [지시 내용]
- modifiedGoal: int               [수정 목표]
- registeredAt: Date              [등록일시 - 생성 시 자동 설정]

+ ImprovementOrder(activity: SalesActivity, registrar: SalesManager, content: String, goal: int): ImprovementOrder
                                          [생성자 - 관리번호 자동 부여, 등록일시=now()]
+ save(): void                            [저장]
+ handleSaveFailure(): void               [저장 실패 - E1]
```

## `Recruitment` [모집공고]

```
- recruitmentNo: String           [모집번호 - 자동 부여]
🔗 registrar: SalesManager         [등록자 - 생성자에서 주입]
- channelType: String             [채널유형 - 설계사/대리점]
- recruitCount: int               [모집인원]
- periodStart: Date               [모집기간 시작일]
- periodEnd: Date                 [모집기간 종료일]
- conditions: String              [모집 조건 - 경력/자격증]
- registeredAt: Date              [등록일시 - register() 시 설정]
- status: String                  [상태 - 작성중/모집중/마감]

+ Recruitment(registrar: SalesManager): Recruitment
                                          [생성자 - 모집번호 자동 부여]
+ enterChannelType(type: String): void    [채널 유형 선택]
+ enterRecruitCount(count: int): void     [모집 인원 입력]
+ enterPeriod(start: Date, end: Date): void [모집 기간 입력]
+ enterConditions(conditions: String): void [모집 조건 입력]
+ validateRequiredFields(): boolean       [필수 항목 검증 - E1]
+ register(): void                        [등록 - registeredAt=now()]
+ close(): void                           [마감]
```

## `Applicant` [지원자]

```
- applicantId: String             [지원자 ID - 자동 부여]
🔗 recruitment: Recruitment        [모집 공고 - 생성자에서 주입]
- name: String                    [지원자명]
- channelType: String             [채널유형]
- appliedDate: Date               [지원일 - 생성 시 자동 설정]
- career: String                  [경력]
- certificates: List<String>      [자격증 목록]
- reviewStatus: String            [심사상태 - 대기/승인/거절]

+ Applicant(recruitment: Recruitment, name: String, type: String, career: String, certs: List<String>): Applicant
                                          [생성자 - 지원자 ID 자동 부여, status="대기"]
+ approve(): ReviewResult                 [심사 승인]
+ reject(reason: String): ReviewResult    [심사 거절 - A1]
+ requestChannelRegistration(): void      [인사 담당자에게 채널 등록 요청 - E1, E2]
```

## `ReviewResult` [심사 결과]

```
- reviewNo: String                [승인번호 - 자동 부여]
🔗 applicant: Applicant            [심사 대상 - 생성자에서 주입]
🔗 reviewer: SalesManager          [심사자]
- result: String                  [결과 - 승인/거절]
- rejectReason: String            [거절 사유 - 거절 시만]
- reviewedAt: Date                [심사일시 - 생성 시 자동 설정]

+ ReviewResult(applicant: Applicant, reviewer: SalesManager, result: String, rejectReason: String): ReviewResult
                                          [생성자 - 승인번호 자동 부여, reviewedAt=now()]
```

## `ChannelEvaluation` [채널 평가]

```
- evaluationNo: String            [평가번호 - 자동 부여]
🔗 channel: SalesChannel           [평가 대상 채널 - 생성자에서 주입]
🔗 evaluator: SalesManager         [평가자 - 생성자에서 주입]
- salesAmount: long               [매출실적 - 자동 로드]
- contractCount: int              [계약건수 - 자동 로드]
- achievementRate: double         [목표달성률 - 자동 로드]
- grade: String                   [평가등급 - S/A/B/C/D]
- opinion: String                 [평가의견]
- registeredAt: Date              [등록일시 - register() 시 설정]
- periodStart: Date               [평가 기간 시작일]
- periodEnd: Date                 [평가 기간 종료일]

+ ChannelEvaluation(channel: SalesChannel, evaluator: SalesManager, start: Date, end: Date): ChannelEvaluation
                                          [생성자 - 평가번호 자동 부여, 데이터 자동 로드]
+ loadPerformanceData(): void             [성과 데이터 자동 로드]
+ selectGrade(grade: String): void        [평가 등급 선택]
+ enterOpinion(opinion: String): void     [평가 의견 입력]
+ validateRequiredFields(): boolean       [필수 항목 검증 - E1]
+ register(): void                        [등록 - registeredAt=now()]
+ requiresBonus(): boolean                [성과급 대상 여부 - S/A 등급]
+ triggerBonusRequest(): BonusRequest     [성과급 요청 발생 - A3]
```

## `BonusRequest` [성과급 요청]

```
- requestNo: String               [요청번호 - 자동 부여]
🔗 evaluation: ChannelEvaluation   [관련 평가 - 생성자에서 주입]
- baseRate: double                [지급 비율 - S:150%, A:120%]
- amount: long                    [요청 금액 - 자동 산출]
- reason: String                  [요청 사유]
- requestedAt: Date               [요청일시 - submit() 시 설정]
- status: String                  [상태 - 작성중/요청완료/실패]

+ BonusRequest(evaluation: ChannelEvaluation): BonusRequest
                                          [생성자 - 요청번호 자동 부여, baseRate·amount 자동 산출]
+ calculateAmount(baseSalary: long): long [금액 산출 = 기본급 × baseRate]
+ enterReason(reason: String): void       [요청 사유 입력]
+ submit(): void                          [요청 제출 - requestedAt=now()]
+ handleSubmitError(): void               [요청 처리 실패 - E1]
+ retry(): void                           [재시도 - E1]
+ cancel(): void                          [취소 - A1]
```

### 🔗 2️⃣ 도메인 관계

```
SalesActivity [영업활동] "*" ────▶ "1" SalesChannel [판매채널]                  : 대상 채널
SalesActivity [영업활동] "1" ◇────▶ "*" ImprovementOrder [개선지시]            : 가진다
ImprovementOrder [개선지시] "*" ────▶ "1" SalesManager [영업 관리자]           : 등록자

Recruitment [모집공고] "*" ────▶ "1" SalesManager [영업 관리자]                : 등록자
Recruitment [모집공고] "1" ◇────▶ "*" Applicant [지원자]                      : 모은다
Applicant [지원자] "1" ────▶ "0..1" ReviewResult [심사 결과]                   : 결과
ReviewResult [심사 결과] "*" ────▶ "1" SalesManager [영업 관리자]              : 심사자

ChannelEvaluation [채널 평가] "*" ────▶ "1" SalesChannel [판매채널]            : 평가 대상
ChannelEvaluation [채널 평가] "*" ────▶ "1" SalesManager [영업 관리자]         : 평가자
ChannelEvaluation [채널 평가] "1" ────▶ "0..1" BonusRequest [성과급 요청]      : 유발
```

---

# 3️⃣ 교육 도메인

## `TrainingPlan` [교육 계획]

```
- planNo: String                  [계획번호 - 자동 부여]
🔗 author: TrainingManager         [작성자 - 생성자에서 주입]
🔗 approver: SalesManager          [승인자 - 승인 시 채워짐]
- title: String                   [교육명]
- periodStart: Date               [교육기간 시작일]
- periodEnd: Date                 [교육기간 종료일]
- channelType: String             [채널유형]
- targetCount: int                [교육 대상자 수]
- budget: long                    [교육 예산]
- objective: String               [교육 목표]
- content: String                 [교육 내용]
- materials: String               [교재 목록]
- approvedAt: Date                [승인일시 - approve() 시 설정]
- rejectReason: String            [반려 사유]
- approvalStatus: String          [승인 상태 - 임시저장/승인요청/승인/반려]

+ TrainingPlan(author: TrainingManager): TrainingPlan
                                          [생성자 - 계획번호 자동 부여]
+ enterBasicInfo(title: String, start: Date, end: Date, type: String, count: int, budget: long): void
                                          [기본 정보 입력]
+ enterContent(objective: String, content: String, materials: String): void
                                          [교육 내용 입력]
+ validateRequiredFields(): boolean       [필수 항목 검증 - E1]
+ saveDraft(): void                       [임시저장 - A1]
+ requestApproval(): void                 [승인 요청 - status="승인요청"]
+ handleNotificationFailure(): void       [승인 알림 발송 실패 - E2]
+ approve(approver: SalesManager): void   [승인 - approvedAt=now()]
+ reject(reason: String): void            [반려 - A3]
+ cancel(): void                          [취소 - A2]
```

## `TrainingPreparation` [교육 제반]

```
- prepNo: String                  [등록번호 - 자동 부여]
🔗 plan: TrainingPlan              [교육 계획 - 생성자에서 주입]
🔗 registrar: TrainingManager      [등록자 - 생성자에서 주입]
- location: String                [교육 장소]
- instructor: String              [강사명]
- materialStatus: String          [교재 준비 현황]
🔗 traineeListFile: Attachment     [교육 대상자 명단 파일]
- otherPreparations: String       [기타 준비 사항]
- registeredAt: Date              [등록일시 - register() 시 설정]
- status: String                  [상태 - 작성중/등록완료]

+ TrainingPreparation(plan: TrainingPlan, registrar: TrainingManager): TrainingPreparation
                                          [생성자 - 등록번호 자동 부여, 교육 기본 정보 자동 로드]
+ enterLocation(location: String): void   [교육 장소 입력]
+ enterInstructor(name: String): void     [강사명 입력]
+ enterMaterialStatus(status: String): void [교재 준비 현황 입력]
+ attachTraineeList(file: Attachment): void [교육 대상자 명단 첨부]
+ enterOtherPreparations(notes: String): void [기타 준비 사항 입력]
+ validateRequiredFields(): boolean       [필수 항목 검증 - E1]
+ register(): void                        [저장 - registeredAt=now()]
+ proceed(): TrainingExecution            [교육 진행 - TrainingExecution 생성]
+ cancel(): void                          [취소 - A1]
```

## `TrainingExecution` [교육 진행]

```
- executionNo: String             [완료번호 - 자동 부여]
🔗 preparation: TrainingPreparation [교육 제반 - 생성자에서 주입]
🔗 attendances: List<Attendance>   [출석 목록]
- memo: String                    [교육 진행 메모]
- attendanceCount: int            [출석 인원]
- totalCount: int                 [전체 인원]
- completedAt: Date               [완료일시 - complete() 시 설정]
- status: String                  [상태 - 진행중/완료]

+ TrainingExecution(preparation: TrainingPreparation): TrainingExecution
                                          [생성자 - 완료번호 자동 부여, 명단 자동 로드 → Attendance 생성]
+ markAttendance(trainee: SalesChannel, attended: boolean): void
                                          [출석 체크]
+ updateAttendanceStats(): void           [출석 현황 실시간 갱신]
+ enterMemo(memo: String): void           [진행 메모 입력]
+ complete(): void                        [진행 완료 - completedAt=now() - E1]
+ sendCompletionNotice(): void            [수료 알림 발송 - E2]
+ cancel(): void                          [취소 - A1]
```

## `Attendance` [출석]

```
🔗 execution: TrainingExecution    [교육 진행 - 생성자에서 주입]
🔗 trainee: SalesChannel           [교육 대상자 - 생성자에서 주입]
- attended: boolean               [출석 여부 - 출석/결석]

+ Attendance(execution: TrainingExecution, trainee: SalesChannel): Attendance
                                          [생성자 - 기본값 attended=false]
+ mark(attended: boolean): void           [출석/결석 체크]
```

### 🔗 3️⃣ 도메인 관계

```
TrainingPlan [교육 계획] "*" ────▶ "1" TrainingManager [영업교육 담당자]       : 작성자
TrainingPlan [교육 계획] "*" ────▶ "0..1" SalesManager [영업 관리자]           : 승인자
TrainingPlan [교육 계획] "1" ────▶ "0..1" TrainingPreparation [교육 제반]      : 이어진다

TrainingPreparation [교육 제반] "*" ────▶ "1" TrainingManager [영업교육 담당자] : 등록자
TrainingPreparation [교육 제반] "1" ────▶ "1" Attachment [명단 파일]           : 첨부
TrainingPreparation [교육 제반] "1" ────▶ "0..1" TrainingExecution [교육 진행] : 이어진다

TrainingExecution [교육 진행] "1" ◆────▶ "*" Attendance [출석]                 : 구성된다
Attendance [출석] "*" ────▶ "1" SalesChannel [판매채널]                        : 교육 대상자
```

---

# 4️⃣ 상담/면담 도메인

## `Consultation` [상담]

```
- consultationNo: String          [접수번호 - 자동 부여]
🔗 customer: Customer              [고객 - 생성자에서 주입]
🔗 channel: SalesChannel           [수락 채널 - accept() 시 채워짐]
- type: String                    [상담 유형 - 방문/전화/온라인]
- desiredDateTime: Date           [희망 일시]
- location: String                [방문 장소 - 방문 상담]
- detailAddress: String           [상세 주소]
- onlineChannel: String           [상담 채널 - 채팅/화상 - 온라인 상담]
- availableTimeRange: String      [연락 가능 시간대 - 전화 상담]
- contact: String                 [연락처]
- content: String                 [상담 내용]
- status: String                  [상태 - 신청/수락/완료/취소]
- requestedAt: Date               [접수일시 - request() 시 설정]
- acceptedAt: Date                [수락일시 - accept() 시 설정]

+ Consultation(customer: Customer): Consultation
                                          [생성자 - 접수번호 자동 부여]
+ selectType(type: String): void          [상담 유형 선택 - A1, A2]
+ enterVisitInfo(dateTime: Date, location: String, detail: String): void
                                          [방문 상담 정보 입력]
+ enterPhoneInfo(timeRange: String): void [전화 상담 정보 입력 - A1]
+ enterOnlineInfo(channel: String): void  [온라인 상담 정보 입력 - A2]
+ enterContact(contact: String): void     [연락처 입력]
+ enterContent(content: String): void     [상담 내용 입력]
+ validateRequiredFields(): boolean       [필수 항목 검증 - E1]
+ request(): void                         [상담 신청 - requestedAt=now()]
+ accept(channel: SalesChannel): void     [상담 수락 - acceptedAt=now()]
```

## `Interview` [면담]

```
- interviewNo: String             [면담번호 - 자동 부여]
🔗 customer: Customer              [고객 - 생성자에서 주입]
🔗 channel: SalesChannel           [담당 판매채널 - 생성자에서 주입]
- type: String                    [면담유형 - 방문/전화/온라인]
- dateTime: Date                  [면담일시]
- location: String                [면담장소 - 방문]
- onlineChannel: String           [면담 채널 - 채팅/화상 - 온라인]
- availableTimeRange: String      [고객 연락 가능 시간대 - 전화]
- preparation: String             [면담 준비사항]
- status: String                  [면담상태 - 예정/완료/취소]
- registeredAt: Date              [등록일시]
- modifiedAt: Date                [수정일시 - modify() 시 설정]
- canceledAt: Date                [취소일시 - cancel() 시 설정]

+ Interview(customer: Customer, channel: SalesChannel): Interview
                                          [생성자 - 면담번호 자동 부여]
+ selectType(type: String): void          [면담 유형 선택 - A2, A3]
+ enterVisitInfo(dateTime: Date, location: String): void
                                          [방문 면담 정보 입력]
+ enterPhoneInfo(timeRange: String): void [전화 면담 정보 입력 - A2]
+ enterOnlineInfo(channel: String, dateTime: Date): void
                                          [온라인 면담 정보 입력 - A3]
+ enterPreparation(preparation: String): void [면담 준비사항 입력]
+ validateRequiredFields(): boolean       [필수 항목 검증 - E1, E2]
+ register(): void                        [등록 - registeredAt=now()]
+ modify(): void                          [수정 - A4, modifiedAt=now()]
+ cancel(): void                          [취소 - A5, canceledAt=now()]
+ sendNotice(): void                      [고객 알림 발송]
+ complete(): void                        [면담 완료]
```

## `InterviewRecord` [면담 기록]

```
- recordNo: String                [기록번호 - 자동 부여]
🔗 interview: Interview            [면담 - 생성자에서 주입]
- content: String                 [면담 내용]
- customerReaction: String        [고객 반응]
- followUp: String                [후속 조치]
- savedAt: Date                   [저장일시 - save() 시 설정]
- modifiedAt: Date                [수정일시 - modify() 시 설정]

+ InterviewRecord(interview: Interview): InterviewRecord
                                          [생성자 - 기록번호 자동 부여, 면담 정보 자동 로드]
+ enterContent(content: String): void     [면담 내용 입력]
+ enterCustomerReaction(reaction: String): void [고객 반응 입력]
+ enterFollowUp(followUp: String): void   [후속 조치 입력]
+ validateRequiredFields(): boolean       [필수 항목 검증 - E1, E2]
+ save(): void                            [저장 - savedAt=now()]
+ modify(): void                          [수정 - A3, modifiedAt=now()]
+ proposeProduct(): ProductProposal       [보험상품 제안으로 이동]
```

## `ProductProposal` [보험상품 제안]

```
- proposalNo: String              [제안번호 - 자동 부여]
🔗 record: InterviewRecord         [면담 기록 - 생성자에서 주입]
🔗 customer: Customer              [수신 고객 - 자동 로드]
🔗 channel: SalesChannel           [담당 판매채널 - 자동 로드]
🔗 product: InsuranceProduct       [제안 상품 - selectProduct() 시 채워짐]
- sentAt: Date                    [발송일시 - send() 시 설정]
- status: String                  [상태 - 작성중/발송완료/실패]

+ ProductProposal(record: InterviewRecord): ProductProposal
                                          [생성자 - 제안번호 자동 부여, 고객·채널 자동 로드]
+ searchProduct(keyword: String): List<InsuranceProduct> [상품 검색 - A1]
+ selectProduct(product: InsuranceProduct): void [상품 선택]
+ send(): void                            [제안서 발송 - sentAt=now()]
+ handleSendFailure(): void               [발송 실패 - E1]
+ close(): void                           [닫기 - A2]
```

### 🔗 4️⃣ 도메인 관계

```
Consultation [상담] "*" ────▶ "1" Customer [고객]                              : 신청자
Consultation [상담] "*" ────▶ "0..1" SalesChannel [판매채널]                   : 수락 채널

Interview [면담] "*" ────▶ "1" Customer [고객]                                 : 면담 대상
Interview [면담] "*" ────▶ "1" SalesChannel [판매채널]                         : 담당 채널
Interview [면담] "1" ────▶ "0..1" InterviewRecord [면담 기록]                  : 기록

InterviewRecord [면담 기록] "1" ────▶ "0..*" ProductProposal [상품 제안]       : 제안 발생
ProductProposal [상품 제안] "*" ────▶ "1" Customer [고객]                      : 수신자
ProductProposal [상품 제안] "*" ────▶ "1" SalesChannel [판매채널]              : 담당 채널
ProductProposal [상품 제안] "*" ────▶ "1" InsuranceProduct [보험상품]          : 제안 상품
```

---

# 5️⃣ 보험상품 / 청약 / 인수심사 도메인

## `InsuranceProduct` [보험상품]

```
- productId: String               [상품 ID - 자동 부여]
- productName: String             [상품명]
- type: String                    [보험유형 - 생명/건강/자동차/화재]
- monthlyPremium: long            [월 보험료]
- coverage: String                [보장내용]
- insurancePeriodOptions: List<String>    [가입 가능 보험기간 옵션]
- joinConditions: String          [가입조건]

+ InsuranceProduct(name: String, type: String, premium: long, coverage: String): InsuranceProduct
                                          [생성자 - 상품 ID 자동 부여]
+ {static} search(criteria: SearchCriteria): List<InsuranceProduct>     [조회]
+ getDetail(): InsuranceProduct           [상세 조회]
+ checkEligibility(customer: Customer): boolean   [가입 가능 여부 확인 - E2]
```

## `Rider` [특약]

```
- riderId: String                 [특약 ID - 자동 부여]
🔗 product: InsuranceProduct       [모상품 - 생성자에서 주입]
- name: String                    [특약명]
- premium: long                   [특약 보험료]
- description: String             [보장 내용]

+ Rider(product: InsuranceProduct, name: String, premium: long, desc: String): Rider
                                          [생성자 - 특약 ID 자동 부여]
```

## `Application` [청약서]

```
- applicationNo: String           [청약번호 - 제출 시 자동 부여]
🔗 customer: Customer              [고객 - 생성자에서 주입]
🔗 channel: SalesChannel           [담당 판매채널 - 생성자에서 주입]
🔗 product: InsuranceProduct       [상품 - selectProduct() 시 채워짐]
🔗 selectedRiders: List<Rider>     [선택 특약]
🔗 signatureFile: Attachment       [서면 서명본 첨부]
- birthDate: Date                 [고객 생년월일]
- contact: String                 [고객 연락처]
- address: String                 [고객 주소]
- insurancePeriod: String         [보험기간]
- paymentMethod: String           [납입방법 - 월납/연납]
- paymentPeriod: String           [납입기간]
- beneficiary: String             [수익자]
- esignRequested: boolean         [전자서명 요청 여부 - A1]
- submittedAt: Date               [제출일시 - submit() 시 설정]
🔗 reviewer: Underwriter           [담당 심사자]
- status: String                  [상태 - 작성중/제출완료/심사중/완료]

+ Application(customer: Customer, channel: SalesChannel): Application
                                          [생성자]
+ enterCustomerInfo(birth: Date, contact: String, address: String): void
                                          [고객 기본정보 입력]
+ loadEligibleProducts(): List<InsuranceProduct> [가입 가능 상품 목록 조회]
+ selectProduct(product: InsuranceProduct): void [상품 선택]
+ enterContractInfo(period: String, method: String, payPeriod: String, beneficiary: String): void
                                          [계약 정보 입력]
+ selectRiders(riders: List<Rider>): void [특약 선택]
+ generateDraft(): void                   [청약서 초안 생성]
+ attachSignatureFile(file: Attachment): void [서명본 첨부]
+ handleUploadFailure(): void             [업로드 실패 - E1]
+ requestEsign(): void                    [전자서명 요청 - A1]
+ submit(): void                          [최종 제출 - applicationNo 부여, submittedAt=now()]
```

## `InsuranceApplication` [보험 신청 - 고객 직접]

```
- applicationNo: String           [신청번호 - 자동 부여]
🔗 customer: Customer              [고객 - 생성자에서 주입]
🔗 product: InsuranceProduct       [상품 - 생성자에서 주입]
🔗 selectedRiders: List<Rider>     [선택 특약]
- birthDate: Date                 [생년월일]
- contact: String                 [연락처]
- address: String                 [주소]
- basicPremium: long              [기본 보험료]
- riderPremium: long              [특약 보험료]
- totalPremium: long              [총 보험료]
- paymentMethod: String           [납입방법 - 월납/연납]
- authMethod: String              [인증방법 - 휴대폰/공동인증서]
- authenticated: boolean          [인증 결과]
- appliedAt: Date                 [신청일시 - apply() 시 설정]
- status: String                  [상태 - 작성중/완료/실패]

+ InsuranceApplication(customer: Customer, product: InsuranceProduct): InsuranceApplication
                                          [생성자 - 신청번호 자동 부여]
+ enterPersonalInfo(birth: Date, contact: String, address: String): void
                                          [개인정보 입력]
+ calculatePremium(): void                [보험료 산출]
+ selectRiders(riders: List<Rider>): void [특약 선택 - A1]
+ recalculatePremium(): void              [특약 변경 시 재산출]
+ selectPaymentMethod(method: String): void [납입 방법 선택]
+ selectAuthMethod(method: String): void  [인증 방법 선택]
+ authenticate(): boolean                 [본인인증 - E1]
+ checkEligibility(): boolean             [가입 가능 여부 - E2]
+ apply(): void                           [신청 - appliedAt=now()]
+ deliverToUnderwriter(): void            [심사자에게 자동 전달]
```

## `Reinstatement` [부활 신청]

```
- reinstatementNo: String         [신청번호 - 자동 부여]
🔗 customer: Customer              [고객 - 생성자에서 주입]
🔗 contract: InsuranceContract     [실효 계약 - 생성자에서 주입]
- lapseReason: String             [실효 사유]
- lapsedDate: Date                [실효일자]
- unpaidPremium: long             [미납 보험료 - 자동 산출]
- unpaidCount: int                [미납 횟수]
- interest: long                  [이자 금액 - 자동 산출]
- totalAmount: long               [총 납입금액]
- contact: String                 [연락처]
- paymentMethod: String           [납입방법 - 카드/계좌이체]
- paymentReceiptNo: String        [영수증번호]
- paidAt: Date                    [납입일시]
- authMethod: String              [인증방법]
- authenticated: boolean          [본인인증 결과]
- appliedAt: Date                 [신청일시 - apply() 시 설정]
- status: String                  [상태 - 작성중/납입완료/신청완료/불가]

+ Reinstatement(customer: Customer, contract: InsuranceContract): Reinstatement
                                          [생성자 - 신청번호 자동 부여, 실효 정보 자동 로드]
+ checkReinstatable(): boolean            [부활 가능 여부 - E1, A1]
+ calculateUnpaidAmount(): void           [미납액·이자 산출]
+ enterContact(contact: String): void     [연락처 입력]
+ selectPaymentMethod(method: String): void [납입 방법 선택]
+ pay(): void                             [납입 처리 - paidAt=now() - E2]
+ handlePaymentFailure(): void            [납입 처리 실패 - E2]
+ authenticate(): boolean                 [본인인증]
+ apply(): void                           [부활 신청 - appliedAt=now()]
+ deliverToUnderwriter(): void            [심사자에게 자동 전달]
```

## `Underwriting` [인수 심사]

```
- underwritingNo: String          [심사번호 - 자동 부여]
🔗 application: Application        [청약 건 - 생성자에서 주입]
🔗 underwriter: Underwriter        [심사자 - 생성자에서 주입]
🔗 attachedDocs: List<Attachment>  [첨부 서류 - 수동 심사 시]
- autoResult: String              [자동 심사 결과]
- autoProcessable: boolean        [자동 심사 가능 여부]
- riskLevel: String               [위험등급]
- specialNotes: String            [특이사항]
- reviewType: String              [수동 심사 유형 - 진단/특인/일반/이미지]
- reviewerOpinion: String         [심사 의견]
- finalResult: String             [최종 결과 - 승인/거절/조건부승인]
- conditions: String              [조건부 승인 조건]
- rejectReason: String            [거절 사유]
- reviewedAt: Date                [심사일시 - submitFinalResult() 시 설정]
- status: String                  [상태 - 대기/자동심사완료/수동심사중/완료]

+ Underwriting(application: Application, underwriter: Underwriter): Underwriting
                                          [생성자 - 심사번호 자동 부여]
+ startReview(): void                     [심사 시작]
+ runAutoReview(): void                   [자동 심사 실행]
+ selectManualReviewType(type: String): void [수동 심사 유형 선택 - A1]
+ attachDocument(doc: Attachment): void   [필요 서류 첨부]
+ enterReviewerOpinion(opinion: String): void [심사 의견 입력]
+ selectFinalResult(result: String): void [최종 결과 선택]
+ enterConditions(conditions: String): void [조건부 승인 조건 입력]
+ enterRejectReason(reason: String): void [거절 사유 입력]
+ submitFinalResult(): void               [심사 완료 - reviewedAt=now() - E2]
+ deliverResult(): UnderwritingResult     [결과 전달 - UnderwritingResult 생성]
```

## `UnderwritingResult` [심사 결과]

```
- resultNo: String                [처리번호 - 자동 부여]
🔗 underwriting: Underwriting      [심사 - 생성자에서 주입]
🔗 channel: SalesChannel           [수신 판매채널 - 자동 로드]
- notifiedAt: Date                [전달일시 - deliver() 시 설정]
- confirmedAt: Date               [확인일시 - confirm() 시 설정]
- guidedToCustomer: boolean       [고객 안내 여부]

+ UnderwritingResult(underwriting: Underwriting): UnderwritingResult
                                          [생성자 - 처리번호 자동 부여]
+ deliver(): void                         [결과 전달 - notifiedAt=now()]
+ handleNotificationFailure(): void       [알림 발송 실패 - E1]
+ confirm(): void                         [확인 완료 - confirmedAt=now()]
+ guideCustomer(): void                   [고객 안내 - A1 조건부 승인]
+ close(): void                           [닫기]
```

### 🔗 5️⃣ 도메인 관계

```
InsuranceProduct [보험상품] "1" ◇────▶ "*" Rider [특약]                       : 가진다

Application [청약서] "*" ────▶ "1" Customer [고객]                            : 청약자
Application [청약서] "*" ────▶ "1" SalesChannel [판매채널]                    : 작성자
Application [청약서] "*" ────▶ "1" InsuranceProduct [보험상품]                : 대상 상품
Application [청약서] "*" ────▶ "*" Rider [특약]                               : 선택 특약
Application [청약서] "1" ────▶ "0..*" Attachment [첨부파일]                   : 첨부

InsuranceApplication [보험 신청] "*" ────▶ "1" Customer [고객]                : 신청자
InsuranceApplication [보험 신청] "*" ────▶ "1" InsuranceProduct [보험상품]    : 대상 상품
InsuranceApplication [보험 신청] "*" ────▶ "*" Rider [특약]                   : 선택 특약

Reinstatement [부활 신청] "*" ────▶ "1" Customer [고객]                       : 신청자
Reinstatement [부활 신청] "*" ────▶ "1" InsuranceContract [보험계약]          : 대상 계약

Underwriting [인수 심사] "*" ────▶ "1" Application [청약서]                   : 심사 대상
Underwriting [인수 심사] "*" ────▶ "1" Underwriter [보험 심사자]              : 심사자
Underwriting [인수 심사] "1" ────▶ "0..*" Attachment [첨부서류]               : 첨부
Underwriting [인수 심사] "1" ────▶ "0..1" UnderwritingResult [심사 결과]      : 산출

UnderwritingResult [심사 결과] "*" ────▶ "1" SalesChannel [판매채널]          : 수신 채널
```

---

# 6️⃣ 계약 관리 도메인

## `InsuranceContract` [보험계약]

```
- contractNo: String              [계약번호 - 자동 부여]
🔗 customer: Customer              [계약자 - 생성자에서 주입]
🔗 product: InsuranceProduct       [상품 - 생성자에서 주입]
🔗 channel: SalesChannel           [담당 판매채널 - 생성자에서 주입]
🔗 paymentStatus: PaymentStatus    [납입 현황]
🔗 riders: List<Rider>             [가입 특약]
- contractDate: Date              [계약일자]
- expiryDate: Date                [만료일]
- monthlyPremium: long            [월 보험료]
- contractStatus: String          [계약 상태 - 정상/만기임박/실효/해지]
- policyNo: String                [증권번호]

+ InsuranceContract(customer: Customer, product: InsuranceProduct, channel: SalesChannel, contractDate: Date, expiryDate: Date, premium: long): InsuranceContract
                                          [생성자 - 계약번호·증권번호 자동 부여, status="정상"]
+ getDetail(): InsuranceContract          [상세 조회]
+ isMaturityNear(): boolean               [만기 임박 여부 - 30일 이내]
+ isExpired(): boolean                    [만료 여부]
+ updateStatus(status: String): void      [상태 갱신 - 정상/만기임박/실효/해지]
+ changePaymentMethod(method: String, account: BankAccount): void
                                          [납입방법 변경 - A4]
+ verifyAccount(amount: int, code: String): boolean [1원 송금 인증]
+ getCertificatePDF(): File               [가입 증명서 PDF - A5]
+ getPolicyPDF(): File                    [약관 PDF - A6]
+ initiateMaturityProcess(): MaturityManagement   [만기 관리 시작]
+ initiateCancellation(): Cancellation    [해지 시작]
```

## `PaymentStatus` [납입 현황]

```
🔗 contract: InsuranceContract     [대상 계약 - 생성자에서 주입]
- totalCount: int                 [전체 납입 횟수]
- paidCount: int                  [정상 납입 횟수]
- lastPaidDate: Date              [최근 납입일]
- overdue: boolean                [연체 여부]
- overdueCount: int               [연체 횟수]

+ PaymentStatus(contract: InsuranceContract): PaymentStatus
                                          [생성자 - 초기값 0]
+ recordPayment(date: Date): void         [납부 기록 반영]
+ checkOverdue(): boolean                 [연체 여부 검사]
```

## `ContractStatistics` [계약 통계]

```
🔗 contract: InsuranceContract     [대상 계약 - 생성자에서 주입]
- paymentHistory: List<PaymentRecord>     [납부 이력]
- claimHistory: List<ClaimRequest>        [청구 이력]
- retentionRate: double[]         [월별 유지율]
- filterStart: Date               [필터 시작]
- filterEnd: Date                 [필터 종료]

+ ContractStatistics(contract: InsuranceContract): ContractStatistics
                                          [생성자 - 데이터 자동 로드]
+ filterByPeriod(start: Date, end: Date): void [기간 필터 - E1]
+ validatePeriod(): boolean               [기간 유효성 검증 - E1]
+ exportExcel(): File                     [엑셀 다운로드]
+ {static} getOverallStats(criteria: SearchCriteria): StatsSummary
                                          [전체 계약 통계 조회 - A1]
```

## `MaturityManagement` [만기 계약 관리]

```
🔗 contract: InsuranceContract     [대상 계약 - 생성자에서 주입]
🔗 noticeRecords: List<NoticeRecord>       [안내 기록 목록]
- customerResponse: String        [고객 응답 - 갱신/해지/추후결정]
- responseAt: Date                [응답 수신일시]
- expired: boolean                [만료 여부 - E1]
- activeTab: String               [현재 탭 - 만기안내/갱신처리/처리이력]

+ MaturityManagement(contract: InsuranceContract): MaturityManagement
                                          [생성자 - 계약 데이터 자동 로드]
+ checkExpiredStatus(): boolean           [만료일 경과 여부 - E1]
+ sendNoticeMessage(): void               [안내 문자 발송 - E2]
+ handleSendFailure(): void               [발송 실패 - E2]
+ recordNotice(memo: String, agent: Employee): NoticeRecord
                                          [안내 기록]
+ receiveCustomerResponse(response: String): void [고객 응답 수신]
+ processRenewal(): Renewal               [갱신 처리 - A1]
+ switchToCancellation(): Cancellation    [해지 처리로 전환 - A2]
+ markPostponed(): void                   [추후 결정 기록 - A3]
+ sendReminderD7(): void                  [D-7 알림 자동 발송 - A3]
```

## `NoticeRecord` [안내 기록]

```
🔗 maturityMgmt: MaturityManagement   [만기 관리 - 생성자에서 주입]
🔗 noticeAgent: Employee           [안내 담당자]
- noticeDateTime: Date            [안내 일시 - 자동 입력]
- memo: String                    [메모]

+ NoticeRecord(mgmt: MaturityManagement, agent: Employee, memo: String): NoticeRecord
                                          [생성자 - noticeDateTime=now()]
+ save(): void                            [저장]
```

## `Renewal` [갱신]

```
- renewalNo: String               [갱신번호 - 자동 부여]
🔗 contract: InsuranceContract     [대상 계약 - 생성자에서 주입]
🔗 maturityMgmt: MaturityManagement   [만기 관리]
- newPeriodStart: Date            [갱신 시작일 - 자동 입력]
- newPeriodEnd: Date              [갱신 종료일]
- newPremium: long                [갱신 후 월 보험료]
- premiumDifference: long         [변동액 = newPremium - 기존 보험료]
- renewedAt: Date                 [갱신일시 - confirm() 시 설정]
- status: String                  [상태 - 작성중/확정]

+ Renewal(contract: InsuranceContract): Renewal
                                          [생성자 - 갱신번호 자동 부여, 시작일 자동]
+ enterRenewalInfo(end: Date, premium: long): void
                                          [갱신 정보 입력]
+ calculateDifference(): long             [변동액 산출]
+ confirm(): void                         [갱신 확정 - renewedAt=now()]
```

## `Cancellation` [해지]

```
- cancellationNo: String          [해지번호 - 자동 부여]
🔗 contract: InsuranceContract     [대상 계약 - 생성자에서 주입]
- reason: String                  [해지 사유 - 경제적 사정/타사가입/.../기타]
- detailReason: String            [상세 사유 - 기타 시 필수]
- noticeAgreed: boolean           [유의사항 동의 여부]
- authResult: boolean             [본인인증 결과]
- expectedRefund: long            [예상 환급금]
- canceledAt: Date                [해지일시 - confirm() 시 설정]
- status: String                  [상태 - 작성중/완료/실패]

+ Cancellation(contract: InsuranceContract): Cancellation
                                          [생성자 - 해지번호 자동 부여]
+ selectReason(reason: String): void      [해지 사유 선택 - A1]
+ enterDetailReason(detail: String): void [상세 사유 입력 - 기타 시 필수]
+ validateReasonInput(): boolean          [기타 시 1자 이상 검증]
+ agreeToNotice(): void                   [유의사항 동의]
+ authenticate(): boolean                 [본인인증 - E1]
+ calculateExpectedRefund(): long         [예상 환급금 산출]
+ submit(): void                          [해약 신청]
+ handleSubmitError(): void               [신청 처리 오류 - E2]
+ confirm(): void                         [해약 확정 - canceledAt=now()]
+ cancel(): void                          [중간 취소 - A2]
```

### 🔗 6️⃣ 도메인 관계

```
InsuranceContract [보험계약] "*" ────▶ "1" Customer [고객]                    : 계약자
InsuranceContract [보험계약] "*" ────▶ "1" InsuranceProduct [보험상품]        : 기반 상품
InsuranceContract [보험계약] "*" ────▶ "1" SalesChannel [판매채널]            : 담당 채널
InsuranceContract [보험계약] "1" ◆────▶ "1" PaymentStatus [납입 현황]         : 구성된다
InsuranceContract [보험계약] "*" ────▶ "*" Rider [가입 특약]                  : 가입한다
InsuranceContract [보험계약] "1" ────▶ "0..1" ContractStatistics [계약 통계]  : 가진다
InsuranceContract [보험계약] "1" ────▶ "0..1" MaturityManagement [만기 관리]   : 만기 시 가진다
InsuranceContract [보험계약] "1" ────▶ "0..1" Cancellation [해지]              : 종결된다
InsuranceContract [보험계약] "1" ────▶ "0..*" Renewal [갱신]                   : 갱신된다

ContractStatistics [계약 통계] "*" ────▶ "1" InsuranceContract [보험계약]      : 대상 계약

MaturityManagement [만기 관리] "*" ────▶ "1" InsuranceContract [보험계약]      : 대상 계약
MaturityManagement [만기 관리] "1" ◇────▶ "*" NoticeRecord [안내 기록]        : 가진다
MaturityManagement [만기 관리] "1" ────▶ "0..1" Renewal [갱신]                 : 산출
MaturityManagement [만기 관리] "1" ────▶ "0..1" Cancellation [해지]            : 전환

NoticeRecord [안내 기록] "*" ────▶ "1" Employee [사원]                        : 안내 담당자
Renewal [갱신] "*" ────▶ "1" InsuranceContract [보험계약]                     : 대상 계약
Cancellation [해지] "*" ────▶ "1" InsuranceContract [보험계약]                : 대상 계약
```

---

# 7️⃣ 사고/현장출동/보험금 도메인

## `AccidentReport` [사고 접수]

```
- reportNo: String                [접수번호 - 생성 시 자동 부여]
🔗 customer: Customer              [사고자]
- vehicleNo: String               [차량번호 - 사용자 입력]
- ownerName: String               [자동차 소유자명/피보험자명 - 사용자 입력]
- contact: String                 [휴대폰 번호 - 사용자 입력]
- accidentType: String            [사고 유형 - 사물/사람]
- damageType: String              [피해 유형]
- location: String                [사고 위치 - GPS 또는 수정값]
- needsDispatch: boolean          [현장출동 필요 여부]
- agreedTerms: boolean            [위치기반 서비스 약관 동의]
- reportedAt: Date                [접수일시 - 생성 시 자동 설정]
- status: String                  [상태 - 작성중/접수완료/취소]

+ AccidentReport(customer: Customer): AccidentReport
                                          [생성자 - 접수번호 자동 부여, 접수일시 = now()]
+ enterVehicleInfo(vehicleNo: String, ownerName: String, contact: String): void
                                          [차량 정보 입력]
+ selectAccidentType(type: String, damage: String): void
                                          [사고 유형 선택]
+ enterLocation(location: String): void   [사고 위치 입력 - GPS 자동 또는 수동]
+ setDispatchOption(needs: boolean): void [현장출동 옵션 선택]
+ agreeTerms(): void                      [약관 동의]
+ validateRequiredFields(): boolean       [필수 항목 검증]
+ verifyContract(): boolean               [당사 가입 내역 대조 - E1]
+ receive(): void                         [접수 처리 - status 갱신]
+ requestDispatch(): Dispatch             [현장출동 신청 - Dispatch 객체 생성]
+ cancel(): void                          [접수만 진행/취소 - A2]
```

## `Dispatch` [현장 출동]

```
- dispatchNo: String              [출동번호 - 생성 시 자동 부여]
🔗 accident: AccidentReport        [사고 접수 - 생성자에서 주입]
🔗 agent: DispatchAgent            [출동 직원 - assignAgent()로 채워짐]
- estimatedArrival: Date          [도착 예정 시간]
- arrivalTime: Date               [실제 도착 시간 - arrive()에서 자동 설정]
- status: String                  [상태 - 신청/배정/출발/도착/취소/완료]
- cancelReason: String            [취소 사유]

+ Dispatch(accident: AccidentReport): Dispatch
                                          [생성자 - 출동번호 자동 부여, status="신청"]
+ assignAgent(agent: DispatchAgent): void [직원 배정 - status="배정"]
+ setEstimatedArrival(time: Date): void   [도착 예정 시간 설정]
+ depart(): void                          [현장 출발 - status="출발"]
+ arrive(): void                          [현장 도착 - arrivalTime=now(), status="도착"]
+ updateLocation(newLocation: String): void [위치 정보 갱신 - A3]
+ cancel(reason: String): void            [출동 취소 - A4]
+ complete(): void                        [출동 완료 - status="완료"]
```

## `DispatchRecord` [현장 출동 기록]

```
- recordId: String                [기록 ID - 생성 시 자동 부여]
🔗 dispatch: Dispatch              [출동 건 - 생성자에서 주입]
🔗 photos: List<Attachment>        [사진 목록 - 전경/파손/번호판/블랙박스]
- policeRequired: boolean         [경찰 출동 여부]
- towingRequired: boolean         [견인 필요 여부]
- notes: String                   [현장 특이사항 및 요원 소견]
- transmittedAt: Date             [전송일시 - transmit()에서 자동 설정]
- status: String                  [상태 - 작성중/전송완료]

+ DispatchRecord(dispatch: Dispatch): DispatchRecord
                                          [생성자 - 기록 ID 자동 부여]
+ uploadPhoto(category: String, photo: Attachment): void
                                          [사진 카테고리별 업로드]
+ removePhoto(photo: Attachment): void    [사진 삭제]
+ setPoliceRequired(required: boolean): void [경찰 출동 여부 선택]
+ setTowingRequired(required: boolean): void [견인 필요 여부 선택]
+ enterNotes(notes: String): void         [특이사항 입력]
+ validateRequired(): boolean             [필수 사진/항목 검증 - E1]
+ transmit(): void                        [기록 전송 - transmittedAt=now()]
```

## `ClaimRequest` [보험금 청구]

```
- claimNo: String                 [청구번호 - 생성 시 자동 부여]
🔗 customer: Customer              [청구 고객]
🔗 contract: InsuranceContract     [대상 계약]
🔗 insured: Customer               [피보험자]
- authMethod: String              [본인인증 방법 - 휴대폰/간편]
- authenticated: boolean          [본인인증 성공 여부]
- personalInfoAgreed: boolean     [개인정보 수집·이용 동의]
- claimType: String               [청구 유형 - 질병/재해]
- claimReasons: List<String>      [청구 사유 - 입원/수술/통원/실손...]
- diagnosis: String               [병명/진단명]
🔗 accidentDetail: AccidentDetail  [사고 상세 - 재해인 경우만]
🔗 recipientInfo: RecipientInfo    [수령인 정보]
🔗 bankAccount: BankAccount        [지급 계좌]
- noticeMethod: String            [안내 방법 - 알림톡/문자/이메일/우편/신청안함]
- progressNoticeAgreed: boolean   [진행과정 안내 받기 동의]
- fpNoticeAgreed: boolean         [담당 FP 통지 동의]
🔗 attachments: List<Attachment>   [첨부 서류]
- requestedAt: Date               [청구일시 - submit()에서 설정]
- status: String                  [상태 - 작성중/접수완료]

+ ClaimRequest(customer: Customer, contract: InsuranceContract): ClaimRequest
                                          [생성자 - 청구번호 자동 부여]
+ agreePersonalInfoTerms(): void          [개인정보 동의]
+ selectAuthMethod(method: String): void  [인증 방법 선택]
+ authenticate(): boolean                 [본인인증]
+ confirmRecipientInfo(): void            [수령인 정보 확인]
+ changeRecipientContact(contact: String): void [수령인 연락처 변경]
+ selectInsured(insured: Customer): void  [피보험자 선택]
+ selectClaimType(type: String): void     [청구 유형 선택 - A2]
+ selectClaimReasons(reasons: List<String>): void [청구 사유 선택 - 다중]
+ enterDiagnosis(diagnosis: String): void [진단명 입력]
+ enterAccidentDetail(detail: AccidentDetail): void [재해 사고 상세 입력 - A2]
+ confirmInsuranceBenefits(): void        [실손 의료비 청구 확인 - A3]
+ selectExistingAccount(account: BankAccount): void [등록된 계좌 선택 - A4]
+ registerNewAccount(bank: String, no: String): void [새 계좌 등록]
+ verifyAccount(): boolean                [계좌 인증 - E1]
+ selectNoticeMethod(method: String): void [안내 방법 선택]
+ setProgressNoticeAgreed(agreed: boolean): void [진행과정 안내 동의]
+ setFpNoticeAgreed(agreed: boolean): void [FP 통지 동의]
+ attachDocument(doc: Attachment): void   [서류 첨부]
+ removeAttachment(doc: Attachment): void [첨부 삭제]
+ validateBeforeSubmit(): boolean         [최종 확인 검증]
+ submit(): void                          [청구 제출 - requestedAt=now()]
+ cancel(): void                          [작성 취소]
```

## `AccidentDetail` [사고 상세 정보 - 재해 청구 시]

```
- accidentSubType: String         [사고 유형 - 일반재해/교통재해]
- content: String                 [사고 내용]
- date: Date                      [사고 날짜]
- location: String                [사고 장소]

+ AccidentDetail(): AccidentDetail        [생성자 - 빈 상태로 생성]
+ enter(subType: String, content: String, date: Date, location: String): void
                                          [상세 정보 입력]
```

## `RecipientInfo` [수령인 정보]

```
- name: String                    [수령인 이름]
- residentNo: String              [수령인 주민등록번호]
- contact: String                 [수령인 휴대전화번호]

+ RecipientInfo(customer: Customer): RecipientInfo
                                          [생성자 - 본인인증 결과로 자동 입력]
+ changeContact(contact: String): void    [연락처 변경]
```

## `BankAccount` [계좌 정보]

```
- bankName: String                [은행명]
- accountNo: String               [계좌번호]
- accountHolder: String           [예금주명]
- verified: boolean               [인증 결과]

+ BankAccount(): BankAccount              [생성자 - 빈 상태로 생성]
+ enter(bank: String, no: String, holder: String): void [입력]
+ verify(): boolean                       [본인 명의 계좌 검증 - E1]
```

## `DamageInvestigation` [손해 조사]

```
- investigationNo: String         [조사번호 - 생성 시 자동 부여]
🔗 claim: ClaimRequest             [대상 청구 - 생성자에서 주입]
🔗 handler: ClaimsHandler          [보상담당자 - 배정 시 채워짐]
- ourFaultRatio: double           [우리 고객 과실 비율]
- counterFaultRatio: double       [상대방 과실 비율]
- recognizedDamage: long          [총 인정 손해액]
- opinion: String                 [조사 의견 및 합의 내용]
- result: String                  [처리 결과 - 지급승인/면책]
- rejectReason: String            [면책 사유 - A3]
🔗 supplementRequest: SupplementRequest        [보완 서류 요청 - A1]
🔗 additionalInvestigation: AdditionalInvestigation [추가 조사 지시 - A2]
- investigatedAt: Date            [조사일시 - complete()에서 설정]
- status: String                  [상태 - 신규배정/조사중/조사완료/종결]

+ DamageInvestigation(claim: ClaimRequest): DamageInvestigation
                                          [생성자 - 조사번호 자동 부여, status="신규배정"]
+ assignHandler(handler: ClaimsHandler): void   [담당자 배정 - status="조사중"]
+ enterRecognizedDamage(amount: long): void [총 인정 손해액 입력]
+ enterFaultRatio(our: double, counter: double): void [과실 비율 입력]
+ validateFaultRatio(): boolean           [과실 비율 합 100% 검증 - E1]
+ enterOpinion(opinion: String): void     [조사 의견 입력]
+ selectResult(result: String): void      [처리 결과 선택]
+ enterRejectReason(reason: String): void [면책 사유 입력 - A3]
+ requestSupplement(items: List<String>, msg: String): SupplementRequest [보완 서류 요청 - A1]
+ requestAdditionalInvestigation(loc: String, schedule: Date, reason: String): AdditionalInvestigation
                                          [추가 조사 지시 - A2]
+ validateRequired(): boolean             [필수 입력 검증 - E2]
+ complete(): ClaimCalculation            [조사 완료 및 산출 이관]
+ closeAsRejected(): void                 [면책 종결 처리]
```

## `SupplementRequest` [보완 서류 요청]

```
- requestedItems: List<String>    [요청 서류 항목]
- message: String                 [메시지]
- sentAt: Date                    [발송일시 - send()에서 설정]

+ SupplementRequest(items: List<String>, message: String): SupplementRequest
                                          [생성자]
+ send(): void                            [고객에게 알림톡/문자 발송]
```

## `AdditionalInvestigation` [추가 조사 지시]

```
- visitLocation: String           [방문지 - 현장/정비소/병원]
- schedule: Date                  [일정]
- reason: String                  [추가 조사 사유]
- registeredAt: Date              [등록일시]

+ AdditionalInvestigation(location: String, schedule: Date, reason: String): AdditionalInvestigation
                                          [생성자 - registeredAt=now()]
```

## `ClaimCalculation` [보험금 산출]

```
- calculationNo: String           [산출번호 - 생성 시 자동 부여]
🔗 investigation: DamageInvestigation  [손해 조사 - 생성자에서 주입]
- recognizedDamage: long          [총 인정 손해액 - 자동 로드]
- faultRatio: double              [적용 고객 과실 비율 - 자동 로드]
- deductible: long                [자기부담금 - 약관에서 자동 로드]
- coverageLimit: long             [최대 보장 한도 - 약관에서 자동 로드]
- finalAmount: long               [최종 산출액]
- adjusted: boolean               [한도 조정 여부 - E2]
- exceededDeductible: boolean     [자기부담금 초과 여부 - E1]
🔗 approver: Employee              [결재권자 - A1에서 채워짐]
- approvalRequired: boolean       [결재 필요 여부]
- calculatedAt: Date              [산출일시 - 생성 시 자동 설정]
- status: String                  [상태 - 산출완료/결재대기/승인완료/종결]

+ ClaimCalculation(investigation: DamageInvestigation): ClaimCalculation
                                          [생성자 - 산출번호 자동 부여, 산출 데이터 자동 로드, calculate() 자동 호출]
+ loadCalculationData(): void             [손해액·과실비율·약관 자동 로드]
+ calculate(): long                       [공식 적용 산출]
+ applyCoverageLimit(): void              [보장 한도 초과 시 조정 - E2]
+ checkDeductibleExceeded(): boolean      [자기부담금 초과 여부 - E1]
+ selectApprover(approver: Employee): void [결재선 지정 - A1]
+ submitForApproval(): void               [결재 상신 - A1]
+ approve(): ClaimPayment                 [지급 승인 및 이관 - ClaimPayment 생성]
+ closeAsExceeded(): void                 [공제액 초과 종결 처리 - E1]
+ goBack(): void                          [이전 페이지 이동 - A2]
```

## `ClaimPayment` [보험금 지급]

```
- paymentNo: String               [지급번호 - 생성 시 자동 부여]
🔗 calculation: ClaimCalculation   [산출 건 - 생성자에서 주입]
🔗 recipient: RecipientInfo        [수령인 - 자동 로드]
🔗 account: BankAccount            [수령 계좌 - 자동 로드]
- finalAmount: long               [최종 확정 지급액]
- paymentType: String             [지급 유형 - 즉시/예약]
- scheduledAt: Date               [예약 일시 - A1]
- paidAt: Date                    [실제 지급일시 - execute() 성공 시 설정]
- otpInput: String                [입력된 OTP/비밀번호]
- otpVerified: boolean            [OTP 인증 결과]
- noticeOption: List<String>      [안내 메시지 옵션 - 알림톡/문자]
- noticeSent: boolean             [발송 여부]
- transferFailed: boolean         [이체 실패 여부 - E2]
- failureReason: String           [실패 사유]
- status: String                  [상태 - 대기/예약/완료/실패/종결]

+ ClaimPayment(calculation: ClaimCalculation): ClaimPayment
                                          [생성자 - 지급번호 자동 부여, 수령인·계좌 자동 로드]
+ selectPaymentType(type: String): void   [지급 유형 선택 - A1]
+ setScheduledDateTime(dt: Date): void    [예약 일시 지정 - A1]
+ setNoticeOption(options: List<String>): void [안내 메시지 옵션 선택]
+ enterOTP(otp: String): void             [OTP/비밀번호 입력]
+ verifyOTP(): boolean                    [OTP 검증 - E1]
+ execute(): void                         [이체 실행 - paidAt=now()]
+ schedule(): void                        [예약 등록 - A1]
+ handleTransferFailure(reason: String): void [이체 실패 처리 - E2]
+ sendAccountChangeNotice(): void         [계좌 변경 안내 알림톡 - E2]
+ sendCompletionNotice(): void            [지급 완료 안내 메시지]
+ goBack(): void                          [이전 페이지 이동 - A2]
+ close(): void                           [사고건 종결 처리]
```

### 🔗 7️⃣ 도메인 관계

```
AccidentReport [사고 접수] "*" ────▶ "1" Customer [고객]                       : 사고자
AccidentReport [사고 접수] "1" ────▶ "0..1" Dispatch [현장 출동]               : 유발

Dispatch [현장 출동] "*" ────▶ "1" AccidentReport [사고 접수]                  : 대상 사고
Dispatch [현장 출동] "*" ────▶ "0..1" DispatchAgent [현장출동 직원]            : 배정 직원
Dispatch [현장 출동] "1" ────▶ "0..1" DispatchRecord [현장 출동 기록]          : 산출

DispatchRecord [현장 출동 기록] "*" ────▶ "1" Dispatch [현장 출동]             : 대상 출동
DispatchRecord [현장 출동 기록] "1" ◇────▶ "*" Attachment [사진/첨부파일]      : 가진다

ClaimRequest [보험금 청구] "*" ────▶ "1" Customer [고객]                       : 청구자
ClaimRequest [보험금 청구] "*" ────▶ "1" InsuranceContract [보험계약]          : 대상 계약
ClaimRequest [보험금 청구] "*" ────▶ "1" Customer [피보험자]                   : 피보험자
ClaimRequest [보험금 청구] "1" ◆────▶ "1" RecipientInfo [수령인 정보]          : 구성된다
ClaimRequest [보험금 청구] "1" ◆────▶ "1" BankAccount [계좌 정보]              : 구성된다
ClaimRequest [보험금 청구] "1" ◆────▶ "0..1" AccidentDetail [사고 상세]        : 구성된다 (재해 시)
ClaimRequest [보험금 청구] "1" ────▶ "0..*" Attachment [첨부파일]              : 첨부
ClaimRequest [보험금 청구] "1" ────▶ "0..1" DamageInvestigation [손해 조사]    : 조사받는다

DamageInvestigation [손해 조사] "*" ────▶ "1" ClaimRequest [보험금 청구]       : 대상 청구
DamageInvestigation [손해 조사] "*" ────▶ "0..1" ClaimsHandler [보상담당자]    : 담당자
DamageInvestigation [손해 조사] "1" ◆────▶ "0..1" SupplementRequest [보완 서류 요청]    : 발생시킨다
DamageInvestigation [손해 조사] "1" ◆────▶ "0..1" AdditionalInvestigation [추가 조사]   : 발생시킨다
DamageInvestigation [손해 조사] "1" ────▶ "0..1" ClaimCalculation [보험금 산출] : 이어진다

ClaimCalculation [보험금 산출] "*" ────▶ "1" DamageInvestigation [손해 조사]   : 대상 조사
ClaimCalculation [보험금 산출] "*" ────▶ "0..1" Employee [사원]                : 결재권자
ClaimCalculation [보험금 산출] "1" ────▶ "0..1" ClaimPayment [보험금 지급]     : 이어진다

ClaimPayment [보험금 지급] "*" ────▶ "1" ClaimCalculation [보험금 산출]        : 대상 산출
ClaimPayment [보험금 지급] "*" ────▶ "1" RecipientInfo [수령인 정보]           : 수령인
ClaimPayment [보험금 지급] "*" ────▶ "1" BankAccount [계좌 정보]               : 수령 계좌
```

---

# 8️⃣ 납입 / 환급 도메인

## `Payment` [보험료 납입]

```
- paymentNo: String               [납입 신청번호 - 생성 시 자동 부여]
🔗 customer: Customer              [고객 - 생성자에서 주입]
🔗 items: List<PaymentItem>        [납입 항목 목록 - N:M 매핑]
- paymentMethod: String           [납입 방법 - 즉시이체/가상계좌]
🔗 account: BankAccount            [납입 계좌]
- totalAmount: long               [총 신청 금액]
- discountedAmount: long          [할인 적용 금액]
- earlyDiscount: long             [선납 할인액]
- requestedAt: Date               [신청일시 - submit()에서 설정]
- status: String                  [상태 - 작성중/완료/오류]

+ Payment(customer: Customer): Payment
                                          [생성자 - 납입 신청번호 자동 부여]
+ selectContracts(contracts: List<InsuranceContract>): void
                                          [납입 대상 계약 1건 이상 선택 - PaymentItem 생성]
+ enterPaymentCount(item: PaymentItem, count: int): void
                                          [계약별 납입 횟수 입력]
+ validatePaymentCount(): boolean         [잔여 납입 횟수 이하 검증]
+ selectPaymentMethod(method: String): void [납입 방법 선택 - A2]
+ selectExistingAccount(account: BankAccount): void [등록된 계좌 선택]
+ registerNewAccount(bank: String, no: String, holder: String): void
                                          [새 계좌 입력 - A3]
+ verifyAccount(): boolean                [계좌 인증]
+ calculateTotal(): long                  [총 신청 금액 + 선납 할인 산출]
+ submit(): void                          [신청 - requestedAt=now()]
+ handleProcessingError(): void           [납입 처리 오류 - E1]
+ cancel(): void                          [취소]
```

## `PaymentItem` [납입 항목] *(N:M 매핑 클래스)*

```
🔗 payment: Payment                [납입 건 - 생성자에서 주입]
🔗 contract: InsuranceContract     [대상 계약 - 생성자에서 주입]
- premiumPerCount: long           [회당 보험료 - contract에서 자동 로드]
- count: int                      [납입 횟수]
- subtotal: long                  [소계 금액]

+ PaymentItem(payment: Payment, contract: InsuranceContract): PaymentItem
                                          [생성자 - 회당 보험료 자동 로드]
+ setCount(count: int): void              [납입 횟수 설정]
+ calculateSubtotal(): long               [소계 산출 = premiumPerCount × count]
```

## `PaymentRecord` [납부 내역]

```
- recordNo: String                [결제번호 - 결제 시 자동 부여]
🔗 contract: InsuranceContract     [대상 계약 - 생성자에서 주입]
- paymentDate: Date               [결제 일자]
- amount: long                    [결제 금액]
- method: String                  [결제 수단 - 카드/계좌이체/가상계좌]
- status: String                  [수납 상태 - 대기/완료/반려]
- installmentNo: int              [회차]
- lateFee: long                   [연체료]
- approvalNo: String              [결제 승인 번호]
- rejectCategory: String          [반려 분류 - 오류결제/이중납부/계약불일치/기타]
- rejectReason: String            [상세 반려 사유]
- confirmedAt: Date               [수납 확정일시 - confirm()에서 설정]
- rejectedAt: Date                [반려일시 - reject()에서 설정]

+ PaymentRecord(contract: InsuranceContract, amount: long, method: String): PaymentRecord
                                          [생성자 - 결제번호 자동 부여, paymentDate=now()]
+ load(): void                            [상세 정보 로드]
+ confirm(): void                         [수납 확정 및 장부 반영 - confirmedAt=now(), status="완료"]
+ recordOnLedger(): void                  [수납 원장 반영]
+ updateContractStatus(): void            [계약 상태 자동 업데이트]
+ enterRejectInfo(category: String, reason: String): void
                                          [반려 사유 입력 - A3]
+ reject(): void                          [수납 반려 확정 - rejectedAt=now(), status="반려"]
+ handleProcessingError(): void           [확정 처리 오류 - E1]
```

## `OverdueNoticeSetting` [미납 알림 자동 발송 설정]

```
- enabled: boolean                [자동 발송 활성화 여부 - 기본 false]
- daysAfterDue: int               [발송 기준일 - 납입일 경과 일수]
- messageTemplate: String         [발송 메시지 템플릿]
- savedAt: Date                   [저장일시]

+ OverdueNoticeSetting(): OverdueNoticeSetting
                                          [생성자 - 기본값 enabled=false]
+ toggleEnabled(enabled: boolean): void   [자동 발송 활성화 토글]
+ setDaysAfterDue(days: int): void        [발송 기준일 입력]
+ previewMessage(): String                [메시지 템플릿 미리보기]
+ save(): void                            [설정 저장]
```

## `RefundCalculation` [해약 환급금 산출]

```
- refundNo: String                [환급 접수번호 - 생성 시 자동 부여]
🔗 cancellation: Cancellation      [해지 건 - 생성자에서 주입]
- totalPaidPremium: long          [총 납입 보험료 - 자동 로드]
- paymentPeriod: String           [납입 기간 - 자동 로드]
- reserveAmount: long             [책임준비금 - 자동 로드]
- appliedRate: double             [적용 이율 - 자동 로드]
- baseRefund: long                [기본 해약 환급금]
- unpaidPremium: long             [미납 보험료 - 자동 로드]
- loanPrincipal: long             [약관 대출 원금 - 자동 로드]
- loanInterest: long              [약관 대출 이자 - 자동 로드]
- finalRefund: long               [실지급 해약 환급금]
🔗 adjustments: List<DeductionAdjustment>  [수기 조정 내역]
- status: String                  [상태 - 산출대기/산출완료/지급완료]
- calculatedAt: Date              [산출일시 - 생성 시 자동 설정]
- confirmedAt: Date               [확정일시 - confirm()에서 설정]

+ RefundCalculation(cancellation: Cancellation): RefundCalculation
                                          [생성자 - 환급 접수번호 자동 부여, 데이터 자동 로드 후 calculate() 호출]
+ loadContractData(): void                [계약 데이터 자동 로드]
+ validateRequiredData(): boolean         [필수 데이터 누락 검증 - E1]
+ calculateBaseRefund(): long             [기본 환급금 산출]
+ calculateDeductions(): long             [공제 내역 산출]
+ calculateFinalRefund(): long            [실지급액 산출]
+ adjustDeduction(item: String, amount: long, note: String): DeductionAdjustment
                                          [공제 항목 수기 조정 - A1]
+ recalculate(): void                     [조정 후 재산출]
+ exportPDF(): File                       [산출 내역서 PDF 다운로드 - A2]
+ confirm(): RefundPayment                [환급금 확정 및 지급 이관 - RefundPayment 생성]
+ handleConfirmError(): void              [확정 저장 오류 - E2]
+ goBackToList(): void                    [목록으로 돌아가기 - A3]
```

## `DeductionAdjustment` [공제 수기 조정 내역]

```
- itemName: String                [공제 항목명]
- originalAmount: long            [원래 금액]
- adjustedAmount: long            [조정 금액]
- adjustedAt: Date                [조정일시 - 생성 시 자동 설정]
🔗 adjustedBy: FinanceManager      [조정자 - 생성자에서 주입]
- note: String                    [조정 메모]

+ DeductionAdjustment(item: String, original: long, adjusted: long, adjustedBy: FinanceManager, note: String): DeductionAdjustment
                                          [생성자 - adjustedAt=now()]
+ apply(): void                           [조정 적용]
```

## `RefundPayment` [환급금 지급]

```
- paymentNo: String               [지급번호 - 생성 시 자동 부여]
🔗 refund: RefundCalculation       [산출 건 - 생성자에서 주입]
🔗 account: BankAccount            [수령 계좌 - 자동 로드]
- finalAmount: long               [최종 확정 환급금 - 자동 로드]
- otpInput: String                [입력된 OTP]
- otpVerified: boolean            [OTP 인증 결과]
- otpFailCount: int               [OTP 실패 횟수 - E1]
- locked: boolean                 [잠금 여부 - 5회 실패 시 - E1]
- transferredAt: Date             [이체 완료 일시 - execute() 성공 시 설정]
- noticeSent: boolean             [알림톡 발송 여부]
- noticeFailureMessage: String    [알림톡 발송 실패 메시지 - E3]
- status: String                  [상태 - 대기/완료/실패/잠금]

+ RefundPayment(refund: RefundCalculation): RefundPayment
                                          [생성자 - 지급번호 자동 부여, 계좌·금액 자동 로드]
+ loadAccountInfo(): void                 [수령 계좌·지급금액 로드]
+ enterOTP(otp: String): void             [OTP 입력]
+ verifyOTP(): boolean                    [OTP 검증 - E1, otpFailCount 증가]
+ lockOnFailure(): void                   [5회 실패 시 잠금 - E1]
+ execute(): void                         [이체 실행 - transferredAt=now()]
+ handleTransferFailure(): void           [이체 처리 오류 - E2]
+ sendNotice(): void                      [고객 알림톡 발송]
+ handleNoticeFailure(): void             [알림톡 발송 실패 - E3]
+ goBackToList(): void                    [목록으로 돌아가기 - A1]
```

### 🔗 8️⃣ 도메인 관계

```
Payment [보험료 납입] "*" ────▶ "1" Customer [고객]                            : 신청자
Payment [보험료 납입] "1" ◆────▶ "1..*" PaymentItem [납입 항목]                : 구성된다
Payment [보험료 납입] "*" ────▶ "0..1" BankAccount [납입 계좌]                 : 사용 계좌

PaymentItem [납입 항목] "*" ────▶ "1" Payment [보험료 납입]                    : 소속
PaymentItem [납입 항목] "*" ────▶ "1" InsuranceContract [보험계약]             : 대상 계약

PaymentRecord [납부 내역] "*" ────▶ "1" InsuranceContract [보험계약]           : 대상 계약

RefundCalculation [해약 환급금 산출] "*" ────▶ "1" Cancellation [해지]         : 대상 해지
RefundCalculation [해약 환급금 산출] "1" ◇────▶ "*" DeductionAdjustment [공제 조정 내역] : 가진다

DeductionAdjustment [공제 조정 내역] "*" ────▶ "1" FinanceManager [재무회계 담당자] : 조정자

RefundPayment [환급금 지급] "*" ────▶ "1" RefundCalculation [해약 환급금 산출] : 대상 산출
RefundPayment [환급금 지급] "*" ────▶ "1" BankAccount [계좌 정보]              : 수령 계좌
```

---

# 9️⃣ 활동 / 고객 / 문의 도메인

## `ActivityPlan` [활동 계획]

```
- planNo: String                  [계획번호 - 자동 부여]
🔗 author: SalesChannel            [작성자 - 생성자에서 주입]
🔗 visits: List<Visit>             [방문/상담 일정 목록]
🔗 goal: SalesGoal                 [영업 목표]
🔗 proposedProducts: List<ProposedProduct>      [제안 상품 목록]
🔗 reviewer: SalesManager          [검토자 - 제출 시 채워짐]
- title: String                   [계획명]
- periodStart: Date               [계획 시작일]
- periodEnd: Date                 [계획 종료일]
- memo: String                    [메모]
- submittedAt: Date               [제출일시 - submit() 시 설정]
- status: String                  [상태 - 임시저장/검토중/확정]

+ ActivityPlan(author: SalesChannel): ActivityPlan
                                          [생성자 - 계획번호 자동 부여, 작성자 자동 입력]
+ enterBasicInfo(title: String, start: Date, end: Date, memo: String): void
                                          [기본 정보 입력]
+ validatePeriod(): boolean               [기간 유효성 검증 - A1]
+ addVisit(visit: Visit): void            [방문/상담 일정 추가]
+ removeVisit(visit: Visit): void         [일정 삭제]
+ setGoal(goal: SalesGoal): void          [영업 목표 설정]
+ addProposedProduct(product: ProposedProduct): void [제안 상품 추가]
+ saveDraft(): void                       [임시저장 - A3]
+ validateRequiredFields(): boolean       [필수 항목 검증 - E1]
+ submit(): void                          [제출 - submittedAt=now(), status="검토중"]
+ requestReview(): void                   [영업 관리자에게 검토 요청 알림]
```

## `Visit` [방문/상담 일정]

```
🔗 plan: ActivityPlan              [활동 계획 - 생성자에서 주입]
🔗 targetCustomer: Customer        [대상 고객 - 생성자에서 주입]
- activityType: String            [활동 유형 - 방문/상담/전화]
- dateTime: Date                  [활동 일시]
- location: String                [활동 장소]
- memo: String                    [메모]

+ Visit(plan: ActivityPlan, customer: Customer): Visit    [생성자]
+ enter(type: String, dateTime: Date, location: String, memo: String): void
                                          [정보 입력]
+ delete(): void                          [삭제]
```

## `SalesGoal` [영업 목표]

```
🔗 plan: ActivityPlan              [활동 계획 - 생성자에서 주입]
- targetContractCount: int        [목표 계약 건수]
- targetContractAmount: long      [목표 계약 금액]
- targetNewCustomerCount: int     [목표 신규 고객 수]

+ SalesGoal(plan: ActivityPlan): SalesGoal               [생성자]
+ enter(contractCount: int, amount: long, newCustomers: int): void
                                          [목표 입력]
```

## `ProposedProduct` [제안 상품]

```
🔗 plan: ActivityPlan              [활동 계획 - 생성자에서 주입]
🔗 targetCustomer: Customer        [대상 고객]
- productType: String             [제안 보험 종류]
- reason: String                  [제안 사유]

+ ProposedProduct(plan: ActivityPlan, customer: Customer, type: String, reason: String): ProposedProduct
                                          [생성자]
```

## `CustomerInfo` [고객 정보 등록 - 판매채널이 등록]

```
- customerNo: String              [고객번호 - 저장 시 자동 부여]
🔗 registrar: SalesChannel         [등록자 - 생성자에서 주입]
🔗 contractInfo: ContractInfo      [계약 정보]
- name: String                    [이름]
- residentNo: String              [주민등록번호 - 마스킹]
- contact: String                 [연락처]
- address: String                 [주소]
- editing: boolean                [편집 모드 여부]
- registeredAt: Date              [등록일시 - register() 시 설정]

+ CustomerInfo(registrar: SalesChannel): CustomerInfo
                                          [생성자]
+ enableEdit(): void                      [편집 모드 진입]
+ enterBasicInfo(name: String, residentNo: String, contact: String, address: String): void
                                          [기본 정보 입력]
+ enterContractInfo(info: ContractInfo): void [계약 정보 입력]
+ previewResidentNo(): String             [주민번호 미리보기 - A1]
+ validate(): boolean                     [유효성 검증 - 필수항목·형식 - E1]
+ checkDuplicate(): boolean               [중복 검증 - E2]
+ register(): void                        [등록 - 고객번호·계약번호 자동 부여, registeredAt=now()]
```

## `ContractInfo` [계약 정보 - 등록 시]

```
- contractNo: String              [계약번호 - 저장 시 자동 부여]
🔗 customerInfo: CustomerInfo      [고객 정보 - 생성자에서 주입]
- insuranceType: String           [보험종류 - 생명/건강/자동차/화재]
- contractDate: Date              [계약일]
- expiryDate: Date                [만료일]
- monthlyPremium: long            [월 보험료]
- riderInfo: List<String>         [특약 정보 - 다중]

+ ContractInfo(customerInfo: CustomerInfo): ContractInfo  [생성자]
+ enter(type: String, contractDate: Date, expiryDate: Date, premium: long): void
                                          [정보 입력]
+ addRider(rider: String): void           [특약 추가 - A2]
+ validate(): boolean                     [유효성 검증]
```

## `Inquiry` [문의]

```
- inquiryNo: String               [문의번호 - 자동 부여]
🔗 customer: Customer              [문의 고객 - 생성자에서 주입]
🔗 attachments: List<Attachment>   [첨부 파일 목록]
- type: String                    [문의 유형 - 보험료/보험금/계약변경/해지/기타]
- title: String                   [제목 - 최대 50자]
- content: String                 [내용 - 최대 1000자]
- receivedAt: Date                [접수 일시 - submit() 시 설정]
- status: String                  [처리 상태 - 답변대기/답변완료]

+ Inquiry(customer: Customer): Inquiry
                                          [생성자 - 문의번호 자동 부여]
+ selectType(type: String): void          [문의 유형 선택]
+ enterTitle(title: String): void         [제목 입력]
+ enterContent(content: String): void     [내용 입력]
+ attachFile(file: Attachment): boolean   [파일 첨부 - A3, E2]
+ removeAttachment(file: Attachment): void [첨부 삭제]
+ validateRequiredFields(): boolean       [필수 항목 검증 - E1]
+ submit(): void                          [제출 - receivedAt=now()]
+ sendReceiptNotice(): void               [접수 확인 문자 발송]
```

## `Answer` [답변]

```
- answerNo: String                [답변번호 - 자동 부여]
🔗 inquiry: Inquiry                [문의 - 생성자에서 주입]
🔗 agent: Employee                 [답변 담당자 - 생성자에서 주입]
- content: String                 [답변 내용]
- answeredAt: Date                [답변 일시 - register() 시 설정]

+ Answer(inquiry: Inquiry, agent: Employee): Answer
                                          [생성자 - 답변번호 자동 부여]
+ enterContent(content: String): void     [답변 내용 입력]
+ register(): void                        [등록 - answeredAt=now(), 문의 status="답변완료"]
```

## `FAQ` [자주 묻는 질문]

```
- faqId: String                   [FAQ ID - 자동 부여]
- category: String                [카테고리 - 보험료/보험금/계약변경/해지/기타]
- question: String                [질문]
- answer: String                  [답변]

+ FAQ(category: String, question: String, answer: String): FAQ   [생성자]
+ {static} search(category: String): List<FAQ>     [카테고리별 조회]
+ {static} getAll(): List<FAQ>                     [전체 조회]
```

## `Attachment` [첨부파일 - 공통]

```
- fileId: String                  [파일 ID - 자동 부여]
- fileName: String                [파일명]
- fileSize: long                  [파일 크기]
- filePath: String                [저장 경로]
- mimeType: String                [파일 형식]
- uploadedAt: Date                [업로드 일시]

+ Attachment(file: File): Attachment      [생성자 - 파일 ID 자동 부여, 업로드일시=now()]
+ validateSize(maxSize: long): boolean    [용량 검증 - 예: 10MB]
+ delete(): void                          [삭제]
+ download(): File                        [다운로드]
```

### 🔗 9️⃣ 도메인 관계

```
ActivityPlan [활동 계획] "*" ────▶ "1" SalesChannel [판매채널]                : 작성자
ActivityPlan [활동 계획] "*" ────▶ "0..1" SalesManager [영업 관리자]          : 검토자
ActivityPlan [활동 계획] "1" ◆────▶ "*" Visit [방문/상담 일정]                : 구성된다
ActivityPlan [활동 계획] "1" ◆────▶ "1" SalesGoal [영업 목표]                 : 구성된다
ActivityPlan [활동 계획] "1" ◆────▶ "*" ProposedProduct [제안 상품]           : 구성된다

Visit [일정] "*" ────▶ "1" Customer [고객]                                    : 대상 고객
ProposedProduct [제안 상품] "*" ────▶ "1" Customer [고객]                     : 대상 고객

CustomerInfo [고객 정보 등록] "*" ────▶ "1" SalesChannel [판매채널]           : 등록자
CustomerInfo [고객 정보 등록] "1" ◆────▶ "1" ContractInfo [계약 정보]         : 구성된다

Inquiry [문의] "*" ────▶ "1" Customer [고객]                                  : 문의 고객
Inquiry [문의] "1" ────▶ "0..1" Answer [답변]                                 : 가진다
Inquiry [문의] "1" ────▶ "0..*" Attachment [첨부파일]                         : 첨부

Answer [답변] "*" ────▶ "1" Inquiry [문의]                                    : 대상 문의
Answer [답변] "*" ────▶ "1" Employee [사원]                                   : 답변 담당자
```

---

# 🎯 전체 핵심 관계 요약 (한눈에)

## [상속 / Generalization]

```
User [사용자] ◁── Customer [고객]
User [사용자] ◁── Employee [사원]
Employee [사원] ◁── SalesChannel [판매채널] ◁── Planner [설계사] / Agency [대리점]
Employee [사원] ◁── SalesManager [영업 관리자] / TrainingManager [영업교육 담당자] / Underwriter [보험 심사자]
Employee [사원] ◁── ClaimsHandler [보상담당자] / DispatchAgent [현장출동 직원]
Employee [사원] ◁── FinanceManager [재무회계 담당자] / ContractManager [계약관리 담당자] / HRManager [인사 담당자]
```

## [합성 / Composition - 강한 포함]

```
TrainingExecution [교육 진행] ◆── Attendance [출석]
ActivityPlan [활동 계획] ◆── Visit [일정], SalesGoal [목표], ProposedProduct [제안 상품]
ClaimRequest [보험금 청구] ◆── RecipientInfo [수령인 정보], BankAccount [계좌 정보], AccidentDetail [사고 상세]
DamageInvestigation [손해 조사] ◆── SupplementRequest [보완 서류], AdditionalInvestigation [추가 조사]
CustomerInfo [고객 정보] ◆── ContractInfo [계약 정보]
InsuranceContract [보험계약] ◆── PaymentStatus [납입 현황]
Payment [보험료 납입] ◆── PaymentItem [납입 항목]
```

## [집약 / Aggregation - 약한 포함]

```
SalesActivity [영업활동] ◇── ImprovementOrder [개선지시]
Recruitment [모집공고] ◇── Applicant [지원자]
InsuranceProduct [보험상품] ◇── Rider [특약]
MaturityManagement [만기 계약 관리] ◇── NoticeRecord [안내 기록]
DispatchRecord [현장 출동 기록] ◇── Attachment [사진]
RefundCalculation [환급금 산출] ◇── DeductionAdjustment [공제 조정]
```

## [연관 / Association - 주요 흐름]

```
[보험 가입 흐름]
Customer [고객] ──▶ InsuranceContract [보험계약] ──▶ InsuranceProduct [보험상품]
Application [청약서] ──▶ Underwriting [인수 심사] ──▶ UnderwritingResult [심사 결과]

[보험금 처리 흐름]
Customer [고객] ──▶ ClaimRequest [청구] ──▶ DamageInvestigation [조사] ──▶ ClaimCalculation [산출] ──▶ ClaimPayment [지급]
AccidentReport [사고 접수] ──▶ Dispatch [현장 출동] ──▶ DispatchRecord [현장 기록]

[해지/환급 흐름]
Customer [고객] ──▶ Cancellation [해지] ──▶ RefundCalculation [환급 산출] ──▶ RefundPayment [환급 지급]

[상담/면담 흐름]
SalesChannel [판매채널] ──▶ Interview [면담] ──▶ InterviewRecord [면담 기록] ──▶ ProductProposal [상품 제안]
```

---