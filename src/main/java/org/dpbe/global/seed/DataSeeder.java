package org.dpbe.global.seed;

import java.time.LocalDate;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.dpbe.domain.actor.ClaimsHandler;
import org.dpbe.domain.actor.Customer;
import org.dpbe.domain.claim.entity.ClaimCalculation;
import org.dpbe.domain.claim.entity.ClaimPayment;
import org.dpbe.domain.claim.entity.ClaimRequest;
import org.dpbe.domain.claim.entity.DamageInvestigation;
import org.dpbe.domain.claim.repository.ClaimCalculationRepository;
import org.dpbe.domain.claim.repository.ClaimPaymentRepository;
import org.dpbe.domain.claim.repository.ClaimRequestRepository;
import org.dpbe.domain.claim.repository.DamageInvestigationRepository;
import org.dpbe.domain.common.entity.BankAccount;
import org.dpbe.domain.common.enums.AuthMethod;
import org.dpbe.domain.common.enums.CalculationStatus;
import org.dpbe.domain.common.enums.ClaimPaymentStatus;
import org.dpbe.domain.common.enums.ClaimRequestStatus;
import org.dpbe.domain.common.enums.ClaimType;
import org.dpbe.domain.common.enums.ContractStatus;
import org.dpbe.domain.common.enums.InquiryStatus;
import org.dpbe.domain.common.enums.InquiryType;
import org.dpbe.domain.common.enums.InvestigationResult;
import org.dpbe.domain.common.enums.PaymentMethod;
import org.dpbe.domain.common.enums.PaymentRecordStatus;
import org.dpbe.domain.common.enums.PaymentStatus;
import org.dpbe.domain.common.enums.PaymentType;
import org.dpbe.domain.common.enums.PlanStatus;
import org.dpbe.domain.common.enums.RejectCategory;
import org.dpbe.domain.contract.entity.Contract;
import org.dpbe.domain.contract.repository.ContractRepository;
import org.dpbe.domain.customer.repository.CustomerRepository;
import org.dpbe.domain.education.entity.Attendance;
import org.dpbe.domain.education.entity.EducationExecution;
import org.dpbe.domain.education.entity.EducationPlan;
import org.dpbe.domain.education.entity.EducationPreparation;
import org.dpbe.domain.education.repository.EducationExecutionRepository;
import org.dpbe.domain.education.repository.EducationPlanRepository;
import org.dpbe.domain.education.repository.EducationPreparationRepository;
import org.dpbe.domain.inquiry.entity.Inquiry;
import org.dpbe.domain.inquiry.repository.InquiryRepository;
import org.dpbe.domain.consultation.entity.InsuranceProduct;
import org.dpbe.domain.consultation.repository.InsuranceProductRepository;
import org.dpbe.domain.payment.entity.Payment;
import org.dpbe.domain.payment.entity.PaymentRecord;
import org.dpbe.domain.payment.repository.PaymentRecordRepository;
import org.dpbe.domain.payment.repository.PaymentRepository;
import org.dpbe.global.auth.entity.UserRole;
import org.dpbe.global.auth.repository.AuthUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 웹(Spring) 기동 시 초기 데이터 시더.
 *
 * 개발·검증 환경에서 Spring 기동 시 필요한 기본 데이터를 적재한다.
 * Repository + @Transactional + DataSource 경로로 실제 MySQL에 적재한다.
 *
 * - 멱등: 고객 데이터가 이미 있으면 건너뛴다(재기동 시 중복 방지).
 * - 토글: {@code app.seed.enabled=false} 로 비활성화(실 DB 보호).
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {

    private final CustomerRepository customerRepository;
    private final ContractRepository contractRepository;
    private final InsuranceProductRepository insuranceProductRepository;
    private final AuthUserRepository authUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final PaymentRepository paymentRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final ClaimRequestRepository claimRequestRepository;
    private final DamageInvestigationRepository damageInvestigationRepository;
    private final ClaimCalculationRepository claimCalculationRepository;
    private final ClaimPaymentRepository claimPaymentRepository;
    private final EducationPlanRepository educationPlanRepository;
    private final EducationPreparationRepository educationPreparationRepository;
    private final EducationExecutionRepository educationExecutionRepository;
    private final InquiryRepository inquiryRepository;

    public DataSeeder(CustomerRepository customerRepository,
                      ContractRepository contractRepository,
                      InsuranceProductRepository insuranceProductRepository,
                      AuthUserRepository authUserRepository,
                      PasswordEncoder passwordEncoder,
                      PaymentRepository paymentRepository,
                      PaymentRecordRepository paymentRecordRepository,
                      ClaimRequestRepository claimRequestRepository,
                      DamageInvestigationRepository damageInvestigationRepository,
                      ClaimCalculationRepository claimCalculationRepository,
                      ClaimPaymentRepository claimPaymentRepository,
                      EducationPlanRepository educationPlanRepository,
                      EducationPreparationRepository educationPreparationRepository,
                      EducationExecutionRepository educationExecutionRepository,
                      InquiryRepository inquiryRepository) {
        this.customerRepository = customerRepository;
        this.contractRepository = contractRepository;
        this.insuranceProductRepository = insuranceProductRepository;
        this.authUserRepository = authUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.paymentRepository = paymentRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.claimRequestRepository = claimRequestRepository;
        this.damageInvestigationRepository = damageInvestigationRepository;
        this.claimCalculationRepository = claimCalculationRepository;
        this.claimPaymentRepository = claimPaymentRepository;
        this.educationPlanRepository = educationPlanRepository;
        this.educationPreparationRepository = educationPreparationRepository;
        this.educationExecutionRepository = educationExecutionRepository;
        this.inquiryRepository = inquiryRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!customerRepository.findAll().isEmpty()) {
            log.info("[seed] 기존 고객 데이터 존재 — 시드 건너뜀");
            return;
        }

        log.info("[seed] 검증용 초기 데이터 적재 시작");
        LocalDate today = LocalDate.now();

        Customer c1 = saveCustomer("김고객", "900101-1234567", "010-1111-2222", "kim@test.com",
                "서울시 강남구 테헤란로 123", LocalDate.of(1990, 1, 1));
        Customer c2 = saveCustomer("이고객", "850515-2345678", "010-3333-4444", "lee@test.com",
                "서울시 서초구 반포대로 45", LocalDate.of(1985, 5, 15));
        Customer c3 = saveCustomer("최고객", "950820-1456789", "010-5555-6666", "choi@test.com",
                "경기도 성남시 분당구", LocalDate.of(1995, 8, 20));

        seedAuthUsers(c1, c2, c3);

        insuranceProductRepository.save(new InsuranceProduct("실손의료보험", "건강", 50_000L, "의료비 전액 보장", "치과 제외"));
        insuranceProductRepository.save(new InsuranceProduct("종신보험", "생명", 150_000L, "사망 시 1억 지급", "없음"));
        insuranceProductRepository.save(new InsuranceProduct("자동차보험", "손해", 80_000L, "대인/대물 무제한", "음주운전 제외"));
        insuranceProductRepository.save(new InsuranceProduct("건강보험", "건강", 65_000L, "입원·통원 치료 보장", "미용 목적 치료 제외"));

        Contract c1Expiring = saveContract(c1, today.minusYears(1), today.plusDays(15), 50_000L,
                "실손의료보험", ContractStatus.NORMAL);
        Contract c1LongTerm = saveContract(c1, today.minusYears(2), today.plusYears(10), 120_000L,
                "종신보험", ContractStatus.NORMAL);
        Contract c2Expired = saveContract(c2, today.minusYears(3), today.minusDays(10), 80_000L,
                "자동차보험", ContractStatus.EXPIRED);
        Contract c3Cancelled = saveContract(c3, today.minusYears(1), today.plusYears(2), 90_000L,
                "건강보험", ContractStatus.CANCELLED);

        seedPayment(c1, List.of(c1Expiring, c1LongTerm));
        seedPaymentRecordCompleted(c1Expiring, 100_000L, "카드");
        seedPaymentRecordRejected(c2Expired, 80_000L, "계좌이체");

        seedClaimDraft(c2, c2Expired);
        seedClaimFlow(c1, c1Expiring);

        seedEducationFlow(c1, c2, c3);
        seedInquiries(c1, c2);

        log.info("[seed] 완료 — 고객 3명, 계약 4건, 보험상품 4개, 검증용 흐름 데이터 적재 완료");
    }

    private Customer saveCustomer(String name, String residentNo, String phone, String email,
                                  String address, LocalDate birthDate) {
        Customer customer = new Customer("TMP-" + name, name, residentNo, phone, email);
        customer.enterAddress(address);
        customer.enterBirthDate(birthDate);
        return customerRepository.saveNew(customer);
    }

    private void seedAuthUsers(Customer c1, Customer c2, Customer c3) {
        ensureAuthUser("admin", "admin1234", UserRole.ADMIN, null, "관리자", true, false);
        ensureAuthUser("claim_staff", "claim1234", UserRole.CLAIM_STAFF, null, "보상담당", true, false);
        ensureAuthUser("finance_staff", "finance1234", UserRole.FINANCE_STAFF, null, "재무담당", true, false);
        ensureAuthUser("kim.customer", "customer1234", UserRole.CUSTOMER, c1.getId(), c1.getName(), true, false);
        ensureAuthUser("lee.customer", "customer1234", UserRole.CUSTOMER, c2.getId(), c2.getName(), true, false);
        ensureAuthUser("choi.customer", "customer1234", UserRole.CUSTOMER, c3.getId(), c3.getName(), true, false);
    }

    private void ensureAuthUser(String username, String rawPassword, UserRole role, Long linkedCustomerId,
                                String displayName, boolean enabled, boolean passwordChangeRequired) {
        if (authUserRepository.existsByUsername(username)) {
            return;
        }
        authUserRepository.save(
                username,
                passwordEncoder.encode(rawPassword),
                role,
                linkedCustomerId,
                displayName,
                enabled,
                passwordChangeRequired);
    }

    private Contract saveContract(Customer customer, LocalDate contractDate, LocalDate expiryDate,
                                  long monthlyPremium, String insuranceType, ContractStatus status) {
        Contract contract = new Contract(customer, contractDate, expiryDate, monthlyPremium);
        contract.setInsuranceType(insuranceType);
        contract.setStatus(status);
        contractRepository.save(contract);
        return contract;
    }

    private void seedPayment(Customer customer, List<Contract> contracts) {
        Payment payment = new Payment(customer);
        payment.selectContracts(contracts);
        for (int i = 0; i < payment.getItems().size(); i++) {
            payment.enterPaymentCount(payment.getItems().get(i), i == 0 ? 2 : 1);
        }
        payment.selectPaymentMethod(PaymentMethod.IMMEDIATE_TRANSFER);
        payment.registerNewAccount("국민은행", "123-45-678901", customer.getName());
        payment.getAccount().verify();
        payment.submit();
        paymentRepository.save(payment);
    }

    private void seedPaymentRecordCompleted(Contract contract, long amount, String method) {
        PaymentRecord record = new PaymentRecord(contract, amount, method);
        record.setInstallmentNo(1);
        record.confirm();
        paymentRecordRepository.save(record);
    }

    private void seedPaymentRecordRejected(Contract contract, long amount, String method) {
        PaymentRecord record = new PaymentRecord(contract, amount, method);
        record.setInstallmentNo(2);
        record.enterRejectInfo(RejectCategory.DUPLICATE_PAYMENT, "동일 계약에 대한 중복 납부");
        record.reject();
        paymentRecordRepository.save(record);
    }

    private void seedClaimDraft(Customer customer, Contract contract) {
        ClaimRequest draft = new ClaimRequest(customer, contract);
        draft.selectAuthMethod(AuthMethod.SIMPLE);
        draft.authenticate();
        draft.agreePersonalInfoTerms();
        draft.selectClaimType(ClaimType.ACCIDENT);
        draft.selectClaimReasons(List.of("차량 파손"));
        draft.enterDiagnosis("차량 수리 필요");
        BankAccount account = verifiedAccount(customer.getName());
        draft.selectExistingAccount(account);
        draft.confirmRecipientInfo();
        claimRequestRepository.save(draft);
    }

    private void seedClaimFlow(Customer customer, Contract contract) {
        ClaimRequest claim = new ClaimRequest(customer, contract);
        claim.selectAuthMethod(AuthMethod.CERTIFICATE);
        claim.authenticate();
        claim.agreePersonalInfoTerms();
        claim.selectClaimType(ClaimType.DISEASE);
        claim.selectClaimReasons(List.of("입원 치료비", "약제비"));
        claim.enterDiagnosis("급성 위염");
        claim.selectExistingAccount(verifiedAccount(customer.getName()));
        claim.confirmRecipientInfo();
        claim.submit();
        claimRequestRepository.save(claim);

        ClaimsHandler handler = new ClaimsHandler("CH-001", "박보상", "보상", "대리", 500_000_000L);
        DamageInvestigation investigation = new DamageInvestigation(claim);
        investigation.assignHandler(handler);
        investigation.enterFaultRatio(20.0, 80.0);
        investigation.enterRecognizedDamage(2_500_000L);
        investigation.enterOpinion("과실 비율 협의 완료");
        investigation.selectResult(InvestigationResult.APPROVED);
        ClaimCalculation calculation = investigation.complete();
        damageInvestigationRepository.save(investigation);

        ClaimPayment claimPayment = calculation.approve();
        claimCalculationRepository.save(calculation);

        claimPayment.selectPaymentType(PaymentType.IMMEDIATE);
        claimPayment.enterOTP("123456");
        claimPayment.verifyOTP();
        claimPayment.execute();
        claimPaymentRepository.save(claimPayment);
    }

    private void seedEducationFlow(Customer c1, Customer c2, Customer c3) {
        EducationPlan pendingPlan = new EducationPlan();
        pendingPlan.setTrainerName("홍강사");
        pendingPlan.enterPlanInfo("보험 기초 교육", todayMinus(7), todayPlus(7), "오프라인", 20, 1_500_000L);
        pendingPlan.enterContentInfo("보험 기초 지식 습득", "상품 구조와 보장 범위 이해", "교재 A, 교재 B");
        pendingPlan.tempSave();
        educationPlanRepository.save(pendingPlan);

        EducationPlan approvedPlan = new EducationPlan();
        approvedPlan.setTrainerName("홍강사");
        approvedPlan.enterPlanInfo("고객 응대 교육", todayPlus(3), todayPlus(5), "온라인", 15, 900_000L);
        approvedPlan.enterContentInfo("고객 응대 품질 향상", "클레임 대응, 문의 응대", "교재 C");
        approvedPlan.requestApproval();
        approvedPlan.approve();
        educationPlanRepository.save(approvedPlan);

        EducationPreparation prep = new EducationPreparation();
        prep.setPlanNo(approvedPlan.getPlanNo());
        prep.enterPreparationInfo("서울 본사 3층 강당", "홍강사", "현장 공지 없음");
        prep.setTextbookStatus("교재 준비 완료");
        prep.addAttendee(c1.getName());
        prep.addAttendee(c2.getName());
        prep.addAttendee(c3.getName());
        prep.save();
        educationPreparationRepository.save(prep);

        EducationExecution exec = new EducationExecution(prep);
        exec.markAttendance(c1.getName(), true);
        exec.markAttendance(c2.getName(), true);
        exec.markAttendance(c3.getName(), false);
        exec.complete();
        educationExecutionRepository.save(exec);
    }

    private void seedInquiries(Customer c1, Customer c2) {
        Inquiry pending = new Inquiry();
        pending.setCustomerId(c1.getId());
        pending.setCustomerName(c1.getName());
        pending.setInquiryType(InquiryType.CLAIM);
        pending.setTitle("청구 서류가 더 필요한가요?");
        pending.setContent("보험금 청구를 제출하려고 하는데 추가 서류가 있는지 확인하고 싶습니다.");
        pending.setAttachmentFileName("claim-guide.pdf");
        pending.setAttachmentFileSize(42_000L);
        pending.submit();
        inquiryRepository.save(pending);

        Inquiry answered = new Inquiry();
        answered.setCustomerId(c2.getId());
        answered.setCustomerName(c2.getName());
        answered.setInquiryType(InquiryType.CONTRACT_CHANGE);
        answered.setTitle("계약 변경 절차가 어떻게 되나요?");
        answered.setContent("보험료 납입 방법을 변경하려고 합니다.");
        answered.submit();
        inquiryRepository.save(answered);
        answered.answer("고객센터 또는 계약 상세 화면에서 변경 요청이 가능합니다.");
        inquiryRepository.updateAnswer(answered);
    }

    private BankAccount verifiedAccount(String holder) {
        BankAccount account = new BankAccount();
        account.enter("국민은행", "123-45-678901", holder);
        account.verify();
        return account;
    }

    private LocalDate todayMinus(int days) {
        return LocalDate.now().minusDays(days);
    }

    private LocalDate todayPlus(int days) {
        return LocalDate.now().plusDays(days);
    }
}
