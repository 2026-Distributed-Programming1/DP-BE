package org.dpbe.domain.claim.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.dpbe.domain.common.entity.BankAccount;
import org.dpbe.domain.common.enums.ClaimPaymentStatus;
import org.dpbe.domain.common.enums.NoticeMethod;
import org.dpbe.domain.common.enums.PaymentType;

/**
 * 보험금 지급 (ClaimPayment)
 *
 * 산출된 보험금을 수령인 계좌로 실제 이체하는 행위를 관리하는 클래스이다.
 * 「보험금을 지급한다」 유스케이스의 중심 클래스로, 생성 시점에 ClaimCalculation으로부터
 * 수령인과 계좌 정보가 자동 로드된다.
 */
public class ClaimPayment {

    private static int sequence = 0;            // 지급번호 자동 부여용

    private String paymentNo;                   // 지급번호
    private ClaimCalculation calculation;       // 산출 건
    private RecipientInfo recipient;            // 수령인 - 자동 로드
    private BankAccount account;                // 수령 계좌 - 자동 로드
    private long finalAmount;                   // 최종 확정 지급액
    private PaymentType paymentType;            // 지급 유형 - 즉시/예약
    private LocalDateTime scheduledAt;          // 예약 일시 (A1)
    private LocalDateTime paidAt;               // 실제 지급일시
    private String otpInput;                    // 입력된 OTP/비밀번호
    private boolean otpVerified;                // OTP 인증 결과
    private List<NoticeMethod> noticeOption;    // 안내 메시지 옵션
    private boolean noticeSent;                 // 발송 여부
    private boolean transferFailed;             // 이체 실패 여부 (E2)
    private String failureReason;               // 실패 사유
    private ClaimPaymentStatus status;          // 상태

    /** DB 로딩용 생성자 - 자동 로드 없이 필드 직접 설정 */
    public ClaimPayment(String paymentNo, ClaimCalculation calculation,
                        long finalAmount, ClaimPaymentStatus status) {
        this.paymentNo = paymentNo;
        this.calculation = calculation;
        this.finalAmount = finalAmount;
        this.status = status;
        this.noticeOption = new ArrayList<>();
    }

    /** 생성자 - 지급번호 자동 부여, 수령인·계좌 자동 로드 */
    public ClaimPayment(ClaimCalculation calculation) {
        sequence += 1;
        this.paymentNo = "CPY" + String.format("%05d", sequence);
        this.calculation = calculation;
        this.noticeOption = new ArrayList<>();
        this.status = ClaimPaymentStatus.WAITING;

        // 수령인·계좌·금액 자동 로드 (산출 건 → 손해 조사 → 청구 건 경유)
        if (calculation != null) {
            this.finalAmount = calculation.getFinalAmount();
            DamageInvestigation inv = calculation.getInvestigation();
            if (inv != null && inv.getClaim() != null) {
                this.recipient = inv.getClaim().getRecipientInfo();
                this.account = inv.getClaim().getBankAccount();
            }
        }
    }

    /** 지급 유형 선택 (A1) */
    public void selectPaymentType(PaymentType type) {
        this.paymentType = type;
    }

    /** 예약 일시 지정 (A1) */
    public void setScheduledDateTime(LocalDateTime dt) {
        this.scheduledAt = dt;
    }

    /** 안내 메시지 옵션 선택 */
    public void setNoticeOption(List<NoticeMethod> options) {
        this.noticeOption = new ArrayList<>(options);
    }

    /** OTP/비밀번호 입력 */
    public void enterOTP(String otp) {
        this.otpInput = otp;
    }

    /**
     * OTP 검증 (E1)
     * 외부 시스템 연동이 필요하므로 더미로 처리한다 (6자리 입력 시 성공으로 간주).
     */
    public boolean verifyOTP() {
        if (otpInput != null && otpInput.length() == 6) {
            this.otpVerified = true;
            return true;
        }
        return false;
    }

    /**
     * 이체 실행 - paidAt=now()
     * 외부 시스템 연동이 필요하므로 더미로 처리한다.
     */
    public void execute() {
        if (!otpVerified) {
            System.out.println("[ClaimPayment] OTP 미인증 상태로 이체 불가");
            return;
        }
        if (account == null || !account.isVerified()) {
            handleTransferFailure("계좌 정보 오류 또는 미인증");
            return;
        }
        this.paidAt = LocalDateTime.now();
        this.status = ClaimPaymentStatus.COMPLETED;
        System.out.println("[ClaimPayment] 보험금 이체 완료: " + paymentNo + ", 금액: " + finalAmount);
    }

    /** 예약 등록 (A1) */
    public void schedule() {
        if (paymentType == PaymentType.SCHEDULED && scheduledAt != null) {
            this.status = ClaimPaymentStatus.SCHEDULED;
            System.out.println("[ClaimPayment] 예약 등록: " + paymentNo + ", 예약일시: " + scheduledAt);
        }
    }

    /** 이체 실패 처리 (E2) */
    public void handleTransferFailure(String reason) {
        this.transferFailed = true;
        this.failureReason = reason;
        this.status = ClaimPaymentStatus.FAILED;
        System.out.println("[ClaimPayment] 이체 실패: " + reason);
    }

    /**
     * 계좌 변경 안내 알림톡 (E2)
     * 외부 시스템 연동이 필요하므로 더미로 처리한다.
     */
    public void sendAccountChangeNotice() {
        System.out.println("[ClaimPayment] 계좌 변경 안내 알림톡 발송");
    }

    /**
     * 지급 완료 안내 메시지
     * 외부 시스템 연동이 필요하므로 더미로 처리한다.
     */
    public void sendCompletionNotice() {
        if (status == ClaimPaymentStatus.COMPLETED) {
            this.noticeSent = true;
            System.out.println("[ClaimPayment] 지급 완료 안내 메시지 발송: 옵션 = " + noticeOption);
        }
    }

    /** 사고건 종결 처리 */
    public void close() {
        this.status = ClaimPaymentStatus.CLOSED;
        System.out.println("[ClaimPayment] 사고건 종결: " + paymentNo);
    }

    // Getter
    public String getPaymentNo() { return paymentNo; }
    public ClaimCalculation getCalculation() { return calculation; }
    public RecipientInfo getRecipient() { return recipient; }
    public BankAccount getAccount() { return account; }
    public long getFinalAmount() { return finalAmount; }
    public PaymentType getPaymentType() { return paymentType; }
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public String getOtpInput() { return otpInput; }
    public boolean isOtpVerified() { return otpVerified; }
    public List<NoticeMethod> getNoticeOption() { return noticeOption; }
    public boolean isNoticeSent() { return noticeSent; }
    public boolean isTransferFailed() { return transferFailed; }
    public String getFailureReason() { return failureReason; }
    public ClaimPaymentStatus getStatus() { return status; }

    // DB 복원용 setter
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public void setRecipientFromName(String name) {
        if (name != null) this.recipient = new RecipientInfo(name);
    }
    public void setAccountFromNo(String accountNo) {
        if (accountNo != null) {
            this.account = new BankAccount();
            this.account.enter(null, accountNo, null);
        }
    }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }
    public void setPaymentType(PaymentType paymentType) { this.paymentType = paymentType; }
}
