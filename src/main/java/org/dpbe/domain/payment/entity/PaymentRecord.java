package org.dpbe.domain.payment.entity;

import org.dpbe.domain.contract.entity.Contract;
import org.dpbe.domain.common.enums.PaymentRecordStatus;
import org.dpbe.domain.common.enums.RejectCategory;
import org.dpbe.global.exception.ApiException;
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

    private Long id;                          // DB 대리키(PK)
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

    /** 신규 결제 기록 생성자 */
    public PaymentRecord(Contract contract, long amount, String method) {
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

    /** 수납 확정 및 장부 반영 - confirmedAt=now(), status="완료" */
    public void confirm() {
        if (this.status != PaymentRecordStatus.WAITING) {
            throw ApiException.badRequest("대기 상태인 납부 내역만 확정할 수 있습니다. 현재 상태: " + this.status);
        }
        try {
            this.confirmedAt = LocalDateTime.now();
            this.status = PaymentRecordStatus.COMPLETED;
            updateContractStatus();
              // 처리 필요
        } catch (Exception e) {
            handleProcessingError();
        }
    }

    /**
     * 계약 상태 자동 업데이트
     * 도메인의 Contract 미완 구현 상태이므로 더미로 처리하는 게 맞는지 확인 필요
     */
    public void updateContractStatus() {
        if (this.contract != null) {
            // 처리 필요
        }
    }

    /** 반려 사유 입력 (A3) */
    public void enterRejectInfo(RejectCategory category, String reason) {
        this.rejectCategory = category;
        this.rejectReason = reason;
    }

    /** 수납 반려 확정 - rejectedAt=now(), status="반려" */
    public void reject() {
        if (this.status != PaymentRecordStatus.WAITING) {
            throw ApiException.badRequest("대기 상태인 납부 내역만 반려할 수 있습니다. 현재 상태: " + this.status);
        }
        if (this.rejectCategory == null) {
            // 처리 필요
            return;
        }
        this.rejectedAt = LocalDateTime.now();
        this.status = PaymentRecordStatus.REJECTED;
        // 처리 필요
    }

    /** 확정 처리 오류 (E1) */
    public void handleProcessingError() {
      // 처리 필요
    }

    // Getter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRecordNo() { return recordNo; }
    public void setRecordNo(String recordNo) { this.recordNo = recordNo; }
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
