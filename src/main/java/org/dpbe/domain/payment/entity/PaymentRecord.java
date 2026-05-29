package org.dpbe.domain.payment.entity;

import org.dpbe.domain.contract.entity.Contract;
import org.dpbe.domain.common.enums.PaymentRecordStatus;
import org.dpbe.domain.common.enums.RejectCategory;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 납부 내역 (PaymentRecord)
 *
 * 실제로 결제가 발생한 납부 이력을 기록하는 클래스이다.
 * 「납부 내역을 관리한다」 유스케이스의 중심 클래스로, 재무회계 담당자가 납부 내역을 검토하여
 * confirm()으로 수납을 확정하거나 reject()로 반려한다.
 */
public class PaymentRecord {

    private static int sequence = 0;          // 결제번호 자동 부여용

    private String recordNo;                  // 결제번호
    private Contract contract;       // 대상 계약
    private LocalDate paymentDate;            // 결제 일자
    private long amount;                      // 결제 금액
    private String method;                    // 결제 수단 - 카드/계좌이체/가상계좌
    private PaymentRecordStatus status;       // 수납 상태
    private int installmentNo;                // 회차
    private long lateFee;                     // 연체료
    private String approvalNo;                // 결제 승인 번호
    private RejectCategory rejectCategory;    // 반려 분류
    private String rejectReason;              // 상세 반려 사유
    private LocalDateTime confirmedAt;        // 수납 확정일시
    private LocalDateTime rejectedAt;         // 반려일시

    /** 생성자 - 결제번호 자동 부여, paymentDate=now() */
    public PaymentRecord(Contract contract, long amount, String method) {
        sequence += 1;
        this.recordNo = "PRC" + String.format("%05d", sequence);
        this.contract = contract;
        this.amount = amount;
        this.method = method;
        this.paymentDate = LocalDate.now();
        this.status = PaymentRecordStatus.WAITING;
    }

    /** DB 로딩용 생성자 */
    public PaymentRecord(String recordNo, Contract contract, long amount, String method,
                         LocalDate paymentDate, PaymentRecordStatus status) {
        this.recordNo = recordNo;
        this.contract = contract;
        this.amount = amount;
        this.method = method;
        this.paymentDate = paymentDate;
        this.status = status;
    }

    /**
     * 상세 정보 로드
     * 시나리오상 화면 진입 시 데이터 로드 단계가 있으므로 메서드를 정의하지만,
     * 본 구현에서는 모든 필드가 이미 채워져 있으므로 실제 로드 작업은 수행하지 않는다.
     */
    public void load() {
        System.out.println("[PaymentRecord] 상세 정보 로드: " + recordNo);
    }

    /** 수납 확정 및 장부 반영 - confirmedAt=now(), status="완료" */
    public void confirm() {
        try {
            this.confirmedAt = LocalDateTime.now();
            this.status = PaymentRecordStatus.COMPLETED;
            recordOnLedger();
            updateContractStatus();
            System.out.println("[PaymentRecord] 수납 확정: " + recordNo);
        } catch (Exception e) {
            handleProcessingError();
        }
    }

    /**
     * 수납 원장 반영
     * 외부 시스템 연동이 필요하므로 더미로 처리한다.
     */
    public void recordOnLedger() {
        System.out.println("[PaymentRecord] 수납 원장 반영: " + recordNo);
    }

    /**
     * 계약 상태 자동 업데이트
     * 6️⃣ 도메인의 Contract 미완 구현 상태이므로 더미로 처리한다.
     */
    public void updateContractStatus() {
        if (this.contract != null) {
            System.out.println("[PaymentRecord] 계약 상태 업데이트: " + contract.getContractNo());
        }
    }

    /** 반려 사유 입력 (A3) */
    public void enterRejectInfo(RejectCategory category, String reason) {
        this.rejectCategory = category;
        this.rejectReason = reason;
    }

    /** 수납 반려 확정 - rejectedAt=now(), status="반려" */
    public void reject() {
        if (this.rejectCategory == null) {
            System.out.println("[PaymentRecord] 반려 사유 미입력으로 반려 불가");
            return;
        }
        this.rejectedAt = LocalDateTime.now();
        this.status = PaymentRecordStatus.REJECTED;
        System.out.println("[PaymentRecord] 수납 반려: " + recordNo + ", 사유: " + rejectReason);
    }

    /** 확정 처리 오류 (E1) */
    public void handleProcessingError() {
        System.out.println("[PaymentRecord] 확정 처리 오류 발생: " + recordNo);
    }

    // Getter
    public String getRecordNo() { return recordNo; }
    public Contract getContract() { return contract; }
    public LocalDate getPaymentDate() { return paymentDate; }
    public long getAmount() { return amount; }
    public String getMethod() { return method; }
    public PaymentRecordStatus getStatus() { return status; }
    public int getInstallmentNo() { return installmentNo; }
    public long getLateFee() { return lateFee; }
    public String getApprovalNo() { return approvalNo; }
    public RejectCategory getRejectCategory() { return rejectCategory; }
    public String getRejectReason() { return rejectReason; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public LocalDateTime getRejectedAt() { return rejectedAt; }

    public void setInstallmentNo(int installmentNo) { this.installmentNo = installmentNo; }
    public void setLateFee(long lateFee) { this.lateFee = lateFee; }
    public void setApprovalNo(String approvalNo) { this.approvalNo = approvalNo; }
    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }
    public void setRejectedAt(LocalDateTime rejectedAt) { this.rejectedAt = rejectedAt; }
    public void setRejectCategory(RejectCategory rejectCategory) { this.rejectCategory = rejectCategory; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
}
