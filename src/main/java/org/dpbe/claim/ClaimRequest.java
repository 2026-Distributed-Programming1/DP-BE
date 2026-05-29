package org.dpbe.claim;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.dpbe.actor.Customer;
import org.dpbe.common.Attachment;
import org.dpbe.common.BankAccount;
import org.dpbe.contract.Contract;
import org.dpbe.enums.AuthMethod;
import org.dpbe.enums.ClaimRequestStatus;
import org.dpbe.enums.ClaimType;
import org.dpbe.enums.NoticeMethod;

/**
 * 보험금 청구 (ClaimRequest)
 *
 * 고객이 가입한 보험계약을 대상으로 보험금을 청구하기 위해 생성되는 클래스이다.
 * 「보험금을 요청한다」 유스케이스의 중심 클래스로, 본인인증부터 시작해 청구 사유 선택,
 * 진단명 입력, 계좌 인증, 서류 첨부에 이르기까지 단계별로 정보를 입력받는다.
 */
public class ClaimRequest {

    private static int sequence = 0;             // 청구번호 자동 부여용

    private String claimNo;                      // 청구번호
    private Customer customer;                   // 청구 고객
    private Contract contract;          // 대상 계약
    private Customer insured;                    // 피보험자
    private AuthMethod authMethod;               // 본인인증 방법
    private boolean authenticated;               // 본인인증 성공 여부
    private boolean personalInfoAgreed;          // 개인정보 수집·이용 동의
    private ClaimType claimType;                 // 청구 유형
    private List<String> claimReasons;           // 청구 사유 - 다중
    private String diagnosis;                    // 병명/진단명
    private AccidentDetail accidentDetail;       // 사고 상세 - 재해인 경우만
    private RecipientInfo recipientInfo;         // 수령인 정보
    private BankAccount bankAccount;             // 지급 계좌
    private NoticeMethod noticeMethod;           // 안내 방법
    private boolean progressNoticeAgreed;        // 진행과정 안내 받기 동의
    private boolean fpNoticeAgreed;              // 담당 FP 통지 동의
    private List<Attachment> attachments;        // 첨부 서류
    private LocalDateTime requestedAt;           // 청구일시
    private ClaimRequestStatus status;           // 상태

    /** 생성자 - 청구번호 자동 부여 */
    public ClaimRequest(Customer customer, Contract contract) {
        sequence += 1;
        this.claimNo = "CLM" + String.format("%05d", sequence);
        this.customer = customer;
        this.contract = contract;
        this.claimReasons = new ArrayList<>();
        this.attachments = new ArrayList<>();
        this.status = ClaimRequestStatus.DRAFT;
    }

    /** DB 로딩용 생성자 */
    public ClaimRequest(String claimNo, Customer customer, Contract contract, ClaimRequestStatus status) {
        this.claimNo = claimNo;
        this.customer = customer;
        this.contract = contract;
        this.status = status;
        this.claimReasons = new ArrayList<>();
        this.attachments = new ArrayList<>();
    }

    /** 개인정보 동의 */
    public void agreePersonalInfoTerms() {
        this.personalInfoAgreed = true;
    }

    /** 인증 방법 선택 */
    public void selectAuthMethod(AuthMethod method) {
        this.authMethod = method;
    }

    /**
     * 본인인증
     * 외부 시스템 연동이 필요하므로 더미로 처리한다.
     */
    public boolean authenticate() {
        if (authMethod != null) {
            this.authenticated = true;
            return true;
        }
        return false;
    }

    /** 수령인 정보 확인 */
    public void confirmRecipientInfo() {
        if (this.recipientInfo == null && this.customer != null) {
            this.recipientInfo = new RecipientInfo(this.customer);
        }
    }

    /** 수령인 연락처 변경 */
    public void changeRecipientContact(String contact) {
        if (this.recipientInfo != null) {
            this.recipientInfo.changeContact(contact);
        }
    }

    /** 피보험자 선택 */
    public void selectInsured(Customer insured) {
        this.insured = insured;
    }

    /** 청구 유형 선택 (A2) */
    public void selectClaimType(ClaimType type) {
        this.claimType = type;
    }

    /** 청구 사유 선택 - 다중 */
    public void selectClaimReasons(List<String> reasons) {
        this.claimReasons = new ArrayList<>(reasons);
    }

    /** 진단명 입력 */
    public void enterDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    /** 재해 사고 상세 입력 (A2) */
    public void enterAccidentDetail(AccidentDetail detail) {
        this.accidentDetail = detail;
    }

    /**
     * 실손 의료비 청구 확인 (A3)
     * 외부 시스템 연동이 필요한 부분이므로 더미로 처리한다.
     */
    public void confirmInsuranceBenefits() {
        System.out.println("[ClaimRequest] 실손 의료비 청구 확인 완료");
    }

    /** 등록된 계좌 선택 (A4) */
    public void selectExistingAccount(BankAccount account) {
        this.bankAccount = account;
    }

    /** 새 계좌 등록 */
    public void registerNewAccount(String bank, String no) {
        BankAccount newAccount = new BankAccount();
        // 예금주명은 청구 고객의 이름으로 설정
        String holder = (customer != null) ? customer.getName() : null;
        newAccount.enter(bank, no, holder);
        this.bankAccount = newAccount;
    }

    /** 계좌 인증 (E1) */
    public boolean verifyAccount() {
        if (this.bankAccount != null) {
            return this.bankAccount.verify();
        }
        return false;
    }

    /** 안내 방법 선택 */
    public void selectNoticeMethod(NoticeMethod method) {
        this.noticeMethod = method;
    }

    /** 진행과정 안내 동의 */
    public void setProgressNoticeAgreed(boolean agreed) {
        this.progressNoticeAgreed = agreed;
    }

    /** FP 통지 동의 */
    public void setFpNoticeAgreed(boolean agreed) {
        this.fpNoticeAgreed = agreed;
    }

    /** 서류 첨부 */
    public void attachDocument(Attachment doc) {
        this.attachments.add(doc);
    }

    /** 첨부 삭제 */
    public void removeAttachment(Attachment doc) {
        this.attachments.remove(doc);
    }

    /** 최종 확인 검증 */
    public boolean validateBeforeSubmit() {
        return personalInfoAgreed && authenticated && claimType != null
                && !claimReasons.isEmpty() && bankAccount != null && bankAccount.isVerified();
    }

    /** 청구 제출 - requestedAt=now() */
    public void submit() {
        if (validateBeforeSubmit()) {
            this.requestedAt = LocalDateTime.now();
            this.status = ClaimRequestStatus.RECEIVED;
            System.out.println("[ClaimRequest] 보험금 청구 접수 완료: " + claimNo);
        }
    }

    /** 작성 취소 */
    public void cancel() {
        // 다이어그램에 별도 취소 상태가 없으므로 단순히 작성 취소 행위만 표현
        System.out.println("[ClaimRequest] 청구 작성 취소: " + claimNo);
    }

    // Getter
    public String getClaimNo() { return claimNo; }
    public Customer getCustomer() { return customer; }
    public Contract getContract() { return contract; }
    public Customer getInsured() { return insured; }
    public AuthMethod getAuthMethod() { return authMethod; }
    public boolean isAuthenticated() { return authenticated; }
    public boolean isPersonalInfoAgreed() { return personalInfoAgreed; }
    public ClaimType getClaimType() { return claimType; }
    public List<String> getClaimReasons() { return claimReasons; }
    public String getDiagnosis() { return diagnosis; }
    public AccidentDetail getAccidentDetail() { return accidentDetail; }
    public RecipientInfo getRecipientInfo() { return recipientInfo; }
    public BankAccount getBankAccount() { return bankAccount; }
    public NoticeMethod getNoticeMethod() { return noticeMethod; }
    public boolean isProgressNoticeAgreed() { return progressNoticeAgreed; }
    public boolean isFpNoticeAgreed() { return fpNoticeAgreed; }
    public List<Attachment> getAttachments() { return attachments; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
    public ClaimRequestStatus getStatus() { return status; }
}
