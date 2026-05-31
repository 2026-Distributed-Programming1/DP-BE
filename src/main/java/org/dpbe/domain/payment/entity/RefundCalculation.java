package org.dpbe.domain.payment.entity;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.dpbe.domain.contract.entity.Cancellation;
import org.dpbe.domain.contract.entity.Contract;
import org.dpbe.domain.common.enums.RefundStatus;

/**
 * 해약 환급금 산출 (RefundCalculation)
 *
 * 보험계약이 해지된 후 고객에게 돌려줄 환급금을 산출하는 클래스이다.
 * 「해약 환급금을 산출한다」 유스케이스의 중심 클래스로, 생성과 동시에 계약 데이터를
 * 자동 로드하고 calculate()가 호출되어 산출 결과가 즉시 도출된다.
 */
public class RefundCalculation {

    private static int sequence = 0;                // 환급 접수번호 자동 부여용

    private static final double DEFAULT_RATE = 0.025;

    private Long id;
    private String refundNo;                        // 환급 접수번호
    private Cancellation cancellation;              // 해지 건
    private long totalPaidPremium;                  // 총 납입 보험료 - 자동 로드
    private String paymentPeriod;                   // 납입 기간 - 자동 로드
    private long reserveAmount;                     // 책임준비금 - 자동 로드
    private double appliedRate;                     // 적용 이율 - 자동 로드
    private long baseRefund;                        // 기본 해약 환급금
    private long unpaidPremium;                     // 미납 보험료 - 자동 로드
    private long loanPrincipal;                     // 약관 대출 원금 - 자동 로드
    private long loanInterest;                      // 약관 대출 이자 - 자동 로드
    private long finalRefund;                       // 실지급 해약 환급금
    private List<DeductionAdjustment> adjustments;  // 수기 조정 내역
    private RefundStatus status;                    // 상태
    private LocalDateTime calculatedAt;             // 산출일시
    private LocalDateTime confirmedAt;              // 확정일시

    /** DB 로딩용 생성자 - 자동 산출 없이 필드 직접 설정 */
    public RefundCalculation(String refundNo, Cancellation cancellation,
                             long totalPaidPremium, String paymentPeriod, long reserveAmount,
                             double appliedRate, long baseRefund, long unpaidPremium,
                             long finalRefund, RefundStatus status) {
        this.refundNo = refundNo;
        this.cancellation = cancellation;
        this.totalPaidPremium = totalPaidPremium;
        this.paymentPeriod = paymentPeriod;
        this.reserveAmount = reserveAmount;
        this.appliedRate = appliedRate;
        this.baseRefund = baseRefund;
        this.unpaidPremium = unpaidPremium;
        this.finalRefund = finalRefund;
        this.status = status;
        this.adjustments = new ArrayList<>();
    }

    /** 생성자 - 환급 접수번호 자동 부여, 데이터 자동 로드 후 calculate() 호출 */
    public RefundCalculation(Cancellation cancellation) {
        sequence += 1;
        this.refundNo = "RFC" + String.format("%05d", sequence);
        this.cancellation = cancellation;
        this.adjustments = new ArrayList<>();
        this.calculatedAt = LocalDateTime.now();
        this.status = RefundStatus.CALCULATION_PENDING;

        loadContractData();
        if (validateRequiredData()) {
            calculateBaseRefund();
            calculateDeductions();
            calculateFinalRefund();
            this.status = RefundStatus.CALCULATED;
        }
    }

    /**
     * 계약 데이터 자동 로드
     * 본 구현에서는 단순화하여 계약 정보로부터 가상의 값을 도출한다.
     * (실제로는 외부 시스템의 계약 이력에서 가져오는 부분)
     */
    public void loadContractData() {
        if (cancellation != null) {
            Contract contract = cancellation.getContract();
            if (contract != null) {
                long premium = contract.getMonthlyPremium();
                // 단순화된 가상 데이터 산출
                this.totalPaidPremium = premium * 24;       // 2년 납입 가정
                this.paymentPeriod = "24개월";
                this.reserveAmount = (long) (totalPaidPremium * 0.7);
                this.appliedRate = DEFAULT_RATE;
                this.unpaidPremium = 0;
                this.loanPrincipal = 0;
                this.loanInterest = 0;
            }
        }
    }

    /** 필수 데이터 누락 검증 (E1) */
    public boolean validateRequiredData() {
        return cancellation != null && totalPaidPremium > 0;
    }

    /** 기본 환급금 산출 */
    public long calculateBaseRefund() {
        // 책임준비금에 적용 이율을 더한 금액
        this.baseRefund = (long) (this.reserveAmount * (1 + this.appliedRate));
        return this.baseRefund;
    }

    /** 공제 내역 산출 */
    public long calculateDeductions() {
        // 미납 보험료 + 대출 원금 + 대출 이자 + 수기 조정 합계
        long total = this.unpaidPremium + this.loanPrincipal + this.loanInterest;
        for (DeductionAdjustment adj : adjustments) {
            total += adj.getAdjustedAmount();
        }
        return total;
    }

    /** 실지급액 산출 */
    public long calculateFinalRefund() {
        long deductions = calculateDeductions();
        this.finalRefund = this.baseRefund - deductions;
        if (this.finalRefund < 0) {
            this.finalRefund = 0;
        }
        return this.finalRefund;
    }

    /** 공제 항목 수기 조정 (A1) */
    public DeductionAdjustment adjustDeduction(String item, long amount, String note) {
        // 본 구현에서는 적용자(FinanceManager) 정보가 없는 컨텍스트도 허용
        DeductionAdjustment adj = new DeductionAdjustment(item, 0L, amount, null, note);
        adj.apply();
        this.adjustments.add(adj);
        return adj;
    }

    /** 조정 후 재산출 */
    public void recalculate() {
        calculateFinalRefund();
        System.out.println("[RefundCalculation] 재산출 완료, 최종 환급금: " + finalRefund);
    }

    /**
     * 산출 내역서 PDF 다운로드 (A2)
     * 외부 시스템 연동이 필요하므로 더미로 처리한다.
     */
    public File exportPDF() {
        System.out.println("[RefundCalculation] 산출 내역서 PDF 다운로드: " + refundNo);
        return new File(refundNo + ".pdf");
    }

    /** 환급금 확정 및 지급 이관 - RefundPayment 생성 */
    public RefundPayment confirm() {
        try {
            this.confirmedAt = LocalDateTime.now();
            this.status = RefundStatus.CALCULATED;
            System.out.println("[RefundCalculation] 환급금 확정 및 지급 이관: " + refundNo);
            return new RefundPayment(this);
        } catch (Exception e) {
            handleConfirmError();
            return null;
        }
    }

    /** 확정 저장 오류 (E2) */
    public void handleConfirmError() {
        System.out.println("[RefundCalculation] 확정 저장 오류 발생: " + refundNo);
    }

    // Getter / Setter
    public Long getId()              { return id; }
    public void setId(Long id)       { this.id = id; }
    public String getRefundNo()      { return refundNo; }
    public void setRefundNo(String refundNo) { this.refundNo = refundNo; }
    public Cancellation getCancellation() { return cancellation; }
    public long getTotalPaidPremium() { return totalPaidPremium; }
    public String getPaymentPeriod() { return paymentPeriod; }
    public long getReserveAmount() { return reserveAmount; }
    public double getAppliedRate() { return appliedRate; }
    public long getBaseRefund() { return baseRefund; }
    public long getUnpaidPremium() { return unpaidPremium; }
    public long getLoanPrincipal() { return loanPrincipal; }
    public long getLoanInterest() { return loanInterest; }
    public long getFinalRefund() { return finalRefund; }
    public List<DeductionAdjustment> getAdjustments() { return adjustments; }
    public RefundStatus getStatus() { return status; }
    public LocalDateTime getCalculatedAt() { return calculatedAt; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
}
