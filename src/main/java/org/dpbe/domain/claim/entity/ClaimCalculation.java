package org.dpbe.domain.claim.entity;

import org.dpbe.domain.actor.Employee;
import org.dpbe.domain.common.enums.CalculationStatus;
import java.time.LocalDateTime;

/**
 * 보험금 산출 (ClaimCalculation)
 *
 * 손해 조사 결과를 바탕으로 실제 지급할 보험금을 산출하는 클래스이다.
 * 「보험금을 산출한다」 유스케이스의 중심 클래스로, 생성과 동시에 loadCalculationData()와
 * calculate()가 자동 호출되어 산출 결과가 즉시 도출된다.
 */
public class ClaimCalculation {

    private static int sequence = 0;          // 산출번호 자동 부여용

    // 본 구현에서 사용하는 가상의 약관 기본값 (시나리오·다이어그램 외부의 외부 시스템 데이터)
    // 기본 공제액
    private static final long DEFAULT_DEDUCTIBLE = 100_000L;
    // 기본 보장한도
    private static final long DEFAULT_COVERAGE_LIMIT = 100_000_000L;

    private String calculationNo;             // 산출번호
    private DamageInvestigation investigation; // 손해 조사
    private long recognizedDamage;            // 총 인정 손해액 - 자동 로드
    private double faultRatio;                // 적용 고객 과실 비율 - 자동 로드
    private long deductible;                  // 자기부담금 - 약관에서 자동 로드
    private long coverageLimit;               // 최대 보장 한도 - 약관에서 자동 로드
    private long finalAmount;                 // 최종 산출액
    private boolean adjusted;                 // 한도 조정 여부 (E2)
    private boolean exceededDeductible;       // 자기부담금 초과 여부 (E1)
    private Employee approver;                // 결재권자 (A1)
    private boolean approvalRequired;         // 결재 필요 여부
    private LocalDateTime calculatedAt;       // 산출일시
    private CalculationStatus status;         // 상태

    /** 생성자 - 산출번호 자동 부여, 산출 데이터 자동 로드, calculate() 자동 호출 */
    public ClaimCalculation(DamageInvestigation investigation) {
        sequence += 1;
        this.calculationNo = "CAL" + String.format("%05d", sequence);
        this.investigation = investigation;
        this.calculatedAt = LocalDateTime.now();
        loadCalculationData();
        calculate();
    }

    /** DB 로딩용 생성자 - 자동 산출 없이 필드 직접 설정 */
    public ClaimCalculation(String calculationNo, DamageInvestigation investigation,
                            long recognizedDamage, double faultRatio, long finalAmount,
                            boolean exceededDeductible, boolean adjusted, CalculationStatus status) {
        this.calculationNo = calculationNo;
        this.investigation = investigation;
        this.recognizedDamage = recognizedDamage;
        this.faultRatio = faultRatio;
        this.finalAmount = finalAmount;
        this.exceededDeductible = exceededDeductible;
        this.adjusted = adjusted;
        this.status = status;
    }

    /** 손해액·과실비율·약관 자동 로드 */
    public void loadCalculationData() {
        if (this.investigation != null) {
            this.recognizedDamage = investigation.getRecognizedDamage();
            this.faultRatio = investigation.getOurFaultRatio();
        }
        // 약관 정보 (외부 시스템에서 로드되는 부분이므로 기본값 사용)
        this.deductible = DEFAULT_DEDUCTIBLE;
        this.coverageLimit = DEFAULT_COVERAGE_LIMIT;
    }

    /** 공식 적용 산출: 인정 손해액 × 과실비율(%) - 자기부담금 */
    public long calculate() {
        long calculated = (long) (recognizedDamage * (faultRatio / 100.0)) - deductible;
        if (calculated < 0) {
            calculated = 0;
        }
        this.finalAmount = calculated;
        this.applyCoverageLimit();
        this.exceededDeductible = checkDeductibleExceeded();
        this.status = CalculationStatus.CALCULATED;
        return this.finalAmount;
    }

    /** 보장 한도 초과 시 조정 (E2) */
    public void applyCoverageLimit() {
        if (this.finalAmount > this.coverageLimit) {
            this.finalAmount = this.coverageLimit;
            this.adjusted = true;
        }
    }

    /** 자기부담금 초과 여부 (E1) */
    public boolean checkDeductibleExceeded() {
        // 인정 손해액의 고객 과실 비율 적용 결과가 자기부담금 이하면 지급할 금액이 없음
        long damageAfterFault = (long) (recognizedDamage * (faultRatio / 100.0));
        return damageAfterFault <= deductible;
    }

    /** 결재선 지정 (A1) */
    public void selectApprover(Employee approver) {
        this.approver = approver;
        this.approvalRequired = true;
    }

    /** 결재 상신 (A1) */
    public void submitForApproval() {
        if (this.approvalRequired) {
            this.status = CalculationStatus.APPROVAL_PENDING;
            System.out.println("[ClaimCalculation] 결재 상신: " + calculationNo
                    + " (결재권자: " + (approver != null ? approver.getName() : "미지정") + ")");
        }
    }

    /** 지급 승인 및 이관 - ClaimPayment 생성 */
    public ClaimPayment approve() {
        this.status = CalculationStatus.APPROVED;
        System.out.println("[ClaimCalculation] 지급 승인 및 이관: " + calculationNo);
        return new ClaimPayment(this);
    }

    /** 공제액 초과 종결 처리 (E1) */
    public void closeAsExceeded() {
        if (this.exceededDeductible) {
            this.status = CalculationStatus.CLOSED;
            System.out.println("[ClaimCalculation] 공제액 초과 종결: " + calculationNo);
        }
    }

    // Getter
    public String getCalculationNo() { return calculationNo; }
    public DamageInvestigation getInvestigation() { return investigation; }
    public long getRecognizedDamage() { return recognizedDamage; }
    public double getFaultRatio() { return faultRatio; }
    public long getDeductible() { return deductible; }
    public long getCoverageLimit() { return coverageLimit; }
    public long getFinalAmount() { return finalAmount; }
    public boolean isAdjusted() { return adjusted; }
    public boolean isExceededDeductible() { return exceededDeductible; }
    public Employee getApprover() { return approver; }
    public boolean isApprovalRequired() { return approvalRequired; }
    public LocalDateTime getCalculatedAt() { return calculatedAt; }
    public CalculationStatus getStatus() { return status; }
}
