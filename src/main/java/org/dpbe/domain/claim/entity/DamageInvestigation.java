package org.dpbe.domain.claim.entity;

import org.dpbe.domain.actor.ClaimsHandler;
import org.dpbe.domain.common.enums.InvestigationResult;
import org.dpbe.domain.common.enums.InvestigationStatus;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 손해 조사 (DamageInvestigation)
 *
 * 보상담당자가 보험금 청구 건에 대해 손해 정도를 조사하고 지급 여부를 결정하는 클래스이다.
 * 「손해 조사를 한다」 유스케이스의 중심 클래스로, 조사 진행 중 추가 서류가 필요하면
 * SupplementRequest를, 추가 현장 조사가 필요하면 AdditionalInvestigation을 생성한다.
 */
public class DamageInvestigation {

    private Long id;                                            // DB 대리키(PK)
    private String investigationNo;                             // 조사번호
    private ClaimRequest claim;                                 // 대상 청구
    private ClaimsHandler handler;                              // 보상담당자
    private double ourFaultRatio;                               // 우리 고객 과실 비율
    private double counterFaultRatio;                           // 상대방 과실 비율
    private long recognizedDamage;                              // 총 인정 손해액
    private String opinion;                                     // 조사 의견 및 합의 내용
    private InvestigationResult result;                         // 처리 결과 - 지급승인/면책
    private String rejectReason;                                // 면책 사유 (A3)
    private SupplementRequest supplementRequest;                // 보완 서류 요청 (A1)
    private AdditionalInvestigation additionalInvestigation;    // 추가 조사 지시 (A2)
    private LocalDateTime investigatedAt;                       // 조사일시
    private InvestigationStatus status;                         // 상태

    /** 신규 조사 생성자 */
    public DamageInvestigation(ClaimRequest claim) {
        this.claim = claim;
        this.status = InvestigationStatus.NEW_ASSIGNED;
    }

    /** DB 로딩용 생성자 */
    public DamageInvestigation(String investigationNo, ClaimRequest claim, String handlerName,
                               double ourFaultRatio, double counterFaultRatio,
                               long recognizedDamage, InvestigationStatus status) {
        this.investigationNo = investigationNo;
        this.claim = claim;
        this.ourFaultRatio = ourFaultRatio;
        this.counterFaultRatio = counterFaultRatio;
        this.recognizedDamage = recognizedDamage;
        this.status = status;
    }

    /** 담당자 배정 - status="조사중" */
    public void assignHandler(ClaimsHandler handler) {
        this.handler = handler;
        this.status = InvestigationStatus.INVESTIGATING;
    }

    /** DB 복원용 — 상태 전이 없이 handler 셸만 주입 */
    public void setHandlerShell(ClaimsHandler handler) {
        this.handler = handler;
    }

    /** 총 인정 손해액 입력 */
    public void enterRecognizedDamage(long amount) {
        this.recognizedDamage = amount;
    }

    /** 과실 비율 입력 */
    public void enterFaultRatio(double our, double counter) {
        this.ourFaultRatio = our;
        this.counterFaultRatio = counter;
    }

    /** 과실 비율 합 100% 검증 (E1) */
    public boolean validateFaultRatio() {
        return Math.abs((ourFaultRatio + counterFaultRatio) - 100.0) < 0.0001;
    }

    /** 조사 의견 입력 */
    public void enterOpinion(String opinion) {
        this.opinion = opinion;
    }

    /** 처리 결과 선택 */
    public void selectResult(InvestigationResult result) {
        this.result = result;
    }

    /** 면책 사유 입력 (A3) */
    public void enterRejectReason(String reason) {
        this.rejectReason = reason;
    }

    /** 보완 서류 요청 (A1) */
    public SupplementRequest requestSupplement(List<String> items, String msg) {
        this.supplementRequest = new SupplementRequest(items, msg);
        this.supplementRequest.send();
        return this.supplementRequest;
    }

    /** 추가 조사 지시 (A2) */
    public AdditionalInvestigation requestAdditionalInvestigation(String loc, LocalDateTime schedule, String reason) {
        this.additionalInvestigation = new AdditionalInvestigation(loc, schedule, reason);
        return this.additionalInvestigation;
    }

    /** 필수 입력 검증 (E2) */
    public boolean validateRequired() {
        return validateFaultRatio() && opinion != null && result != null && recognizedDamage > 0;
    }

    /** 조사 완료 및 산출 이관 */
    public ClaimCalculation complete() {
        if (validateRequired() && result == InvestigationResult.APPROVED) {
            this.investigatedAt = LocalDateTime.now();
            this.status = InvestigationStatus.INVESTIGATED;
            System.out.println("[DamageInvestigation] 손해 조사 완료, 산출 이관: " + investigationNo);
            return new ClaimCalculation(this);
        }
        return null;
    }

    /** 면책 종결 처리 */
    public void closeAsRejected() {
        if (result == InvestigationResult.REJECTED) {
            this.investigatedAt = LocalDateTime.now();
            this.status = InvestigationStatus.CLOSED;
            System.out.println("[DamageInvestigation] 면책 종결: " + investigationNo);
        }
    }

    // Getter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getInvestigationNo() { return investigationNo; }
    public void setInvestigationNo(String investigationNo) { this.investigationNo = investigationNo; }
    public ClaimRequest getClaim() { return claim; }
    public ClaimsHandler getHandler() { return handler; }
    public double getOurFaultRatio() { return ourFaultRatio; }
    public double getCounterFaultRatio() { return counterFaultRatio; }
    public long getRecognizedDamage() { return recognizedDamage; }
    public String getOpinion() { return opinion; }
    public InvestigationResult getResult() { return result; }
    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
    public SupplementRequest getSupplementRequest() { return supplementRequest; }
    public AdditionalInvestigation getAdditionalInvestigation() { return additionalInvestigation; }
    public LocalDateTime getInvestigatedAt() { return investigatedAt; }
    public void setInvestigatedAt(LocalDateTime investigatedAt) { this.investigatedAt = investigatedAt; }
    public InvestigationStatus getStatus() { return status; }
}
