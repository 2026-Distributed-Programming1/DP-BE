package org.dpbe.domain.payment.entity;

import java.time.LocalDateTime;
import org.dpbe.domain.common.entity.BankAccount;
import org.dpbe.domain.common.enums.RefundPaymentStatus;

/**
 * 환급금 지급 (RefundPayment)
 *
 * 확정된 해약 환급금을 고객 계좌로 실제 이체하는 행위를 관리하는 클래스이다.
 * 「해약 환급금을 지급한다」 유스케이스의 중심 클래스로, 생성 시점에 RefundCalculation으로부터
 * 계좌와 금액 정보가 자동 로드된다. 보안 관점에서 OTP 인증을 5회 실패하면 잠금 처리된다.
 */
public class RefundPayment {

    private static int sequence = 0;            // 지급번호 자동 부여용
    private static final int MAX_OTP_FAIL = 5;  // OTP 최대 실패 횟수

    private String paymentNo;                   // 지급번호
    private RefundCalculation refund;           // 산출 건
    private BankAccount account;                // 수령 계좌 - 자동 로드
    private long finalAmount;                   // 최종 확정 환급금 - 자동 로드
    private String otpInput;                    // 입력된 OTP
    private boolean otpVerified;                // OTP 인증 결과
    private int otpFailCount;                   // OTP 실패 횟수 (E1)
    private boolean locked;                     // 잠금 여부 (E1)
    private LocalDateTime transferredAt;        // 이체 완료 일시
    private boolean noticeSent;                 // 알림톡 발송 여부
    private String noticeFailureMessage;        // 알림톡 발송 실패 메시지 (E3)
    private RefundPaymentStatus status;         // 상태

    /** DB 로딩용 생성자 - 자동 로드 없이 필드 직접 설정 */
    public RefundPayment(String paymentNo, RefundCalculation refund,
                         long finalAmount, RefundPaymentStatus status) {
        this.paymentNo = paymentNo;
        this.refund = refund;
        this.finalAmount = finalAmount;
        this.status = status;
    }

    /** 생성자 - 지급번호 자동 부여, 계좌·금액 자동 로드 */
    public RefundPayment(RefundCalculation refund) {
        sequence += 1;
        this.paymentNo = "RPY" + String.format("%05d", sequence);
        this.refund = refund;
        this.otpFailCount = 0;
        this.locked = false;
        this.status = RefundPaymentStatus.WAITING;
        loadAccountInfo();
    }

    /** 수령 계좌·지급금액 로드 */
    public void loadAccountInfo() {
        if (refund != null) {
            this.finalAmount = refund.getFinalRefund();
            // 계좌 정보는 해지 건의 계약자 등록 계좌에서 가져오는 것이 자연스러우나,
            // 본 구현에서는 외부에서 setAccount() 또는 loadAccountInfo 호출 후 별도 주입이 필요할 수 있다.
            if (refund.getCancellation() != null
                    && refund.getCancellation().getContract() != null
                    && refund.getCancellation().getContract().getCustomer() != null
                    && !refund.getCancellation().getContract().getCustomer().getRegisteredAccounts().isEmpty()) {
                this.account = refund.getCancellation().getContract().getCustomer()
                        .getRegisteredAccounts().get(0);
            }
        }
    }

    /** OTP 입력 */
    public void enterOTP(String otp) {
        this.otpInput = otp;
    }

    /**
     * OTP 검증 (E1) - otpFailCount 증가
     * 외부 시스템 연동이 필요하므로 더미로 처리한다 (6자리 입력 시 성공으로 간주).
     */
    public boolean verifyOTP() {
        if (locked) {
            System.out.println("[RefundPayment] 계정이 잠겨 있어 OTP 검증 불가");
            return false;
        }
        if (otpInput != null && otpInput.length() == 6) {
            this.otpVerified = true;
            return true;
        }
        this.otpFailCount += 1;
        if (this.otpFailCount >= MAX_OTP_FAIL) {
            lockOnFailure();
        }
        return false;
    }

    /** 5회 실패 시 잠금 (E1) */
    public void lockOnFailure() {
        this.locked = true;
        this.status = RefundPaymentStatus.LOCKED;
        System.out.println("[RefundPayment] OTP 5회 실패로 잠금 처리: " + paymentNo);
    }

    /**
     * 이체 실행 - transferredAt=now()
     * 외부 시스템 연동이 필요하므로 더미로 처리한다.
     */
    public void execute() {
        if (!otpVerified) {
            System.out.println("[RefundPayment] OTP 미인증 상태로 이체 불가");
            return;
        }
        if (account == null || !account.isVerified()) {
            handleTransferFailure();
            return;
        }
        try {
            this.transferredAt = LocalDateTime.now();
            this.status = RefundPaymentStatus.COMPLETED;
            System.out.println("[RefundPayment] 환급금 이체 완료: " + paymentNo + ", 금액: " + finalAmount);
        } catch (Exception e) {
            handleTransferFailure();
        }
    }

    /** 이체 처리 오류 (E2) */
    public void handleTransferFailure() {
        this.status = RefundPaymentStatus.FAILED;
        System.out.println("[RefundPayment] 이체 처리 오류: " + paymentNo);
    }

    /**
     * 고객 알림톡 발송
     * 외부 시스템 연동이 필요하므로 더미로 처리한다.
     */
    public void sendNotice() {
        try {
            if (status == RefundPaymentStatus.COMPLETED) {
                this.noticeSent = true;
                System.out.println("[RefundPayment] 알림톡 발송 완료: " + paymentNo);
            }
        } catch (Exception e) {
            handleNoticeFailure();
        }
    }

    /** 알림톡 발송 실패 (E3) */
    public void handleNoticeFailure() {
        this.noticeFailureMessage = "알림톡 발송에 실패했습니다.";
        System.out.println("[RefundPayment] " + noticeFailureMessage);
    }

    /** 외부에서 계좌를 명시적으로 주입할 때 사용 (loadAccountInfo가 채우지 못한 경우 대비) */
    public void setAccount(BankAccount account) {
        this.account = account;
    }

    public void setOtpFailCount(int otpFailCount) { this.otpFailCount = otpFailCount; }
    public void setTransferredAt(LocalDateTime transferredAt) { this.transferredAt = transferredAt; }
    public void setNoticeSent(boolean noticeSent) { this.noticeSent = noticeSent; }

    // Getter
    public String getPaymentNo() { return paymentNo; }
    public RefundCalculation getRefund() { return refund; }
    public BankAccount getAccount() { return account; }
    public long getFinalAmount() { return finalAmount; }
    public String getOtpInput() { return otpInput; }
    public boolean isOtpVerified() { return otpVerified; }
    public int getOtpFailCount() { return otpFailCount; }
    public boolean isLocked() { return locked; }
    public LocalDateTime getTransferredAt() { return transferredAt; }
    public boolean isNoticeSent() { return noticeSent; }
    public String getNoticeFailureMessage() { return noticeFailureMessage; }
    public RefundPaymentStatus getStatus() { return status; }
}
