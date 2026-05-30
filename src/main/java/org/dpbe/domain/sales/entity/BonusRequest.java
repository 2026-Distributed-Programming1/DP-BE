package org.dpbe.domain.sales.entity;

import java.time.LocalDateTime;
import org.dpbe.domain.common.enums.ChannelType;
import org.dpbe.domain.common.enums.EvaluationGrade;

/**
 * 성과급 지급 요청 (BonusRequest)
 * 평가 등급에 따라 성과급 지급을 요청하는 클래스이다.
 */
public class BonusRequest {
    private Long id;
    private String evaluationNo;            // 평가번호
    private String channelName;             // 채널명
    private ChannelType channelType;        // 채널 유형 - 설계사/대리점 (enum)
    private EvaluationGrade evaluationGrade; // 평가 등급 - S/A (enum)
    private Double bonusRatio;              // 등급별 지급 비율 - S:150%, A:120%
    private Long baseSalary;               // 기본급
    private Double bonusAmount;              // 산출된 성과급 금액
    private String requestReason;          // 요청 사유 선택
    private String requestNo;              // 요청 번호
    private LocalDateTime requestedAt;     // 요청일시

    public void loadRequestScreen() {}

    public Double calculateBonus() {
        if (baseSalary == null || bonusRatio == null) return (double) 0L;
        this.bonusAmount = (double) Math.round(baseSalary * bonusRatio);
        return this.bonusAmount;
    }

    public void showConfirmPopup() {}

    public void submit() {
        this.requestedAt = LocalDateTime.now();
        this.requestNo = "BR-" + requestedAt.toString().replaceAll("[^0-9]", "").substring(0, 14);
    }

    public void showRequestResult() {}

    public void showRequestError() {}

    public void retry() {}

    public void cancel() {}

    public void returnToEvaluation() {}

    // Runner에서 실제 사용하는 getter/setter만 유지
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEvaluationNo() { return evaluationNo; }
    public void setEvaluationNo(String evaluationNo) { this.evaluationNo = evaluationNo; }
    public String getChannelName() { return channelName; }
    public void setChannelName(String channelName) { this.channelName = channelName; }
    public ChannelType getChannelType() { return channelType; }
    public void setChannelType(ChannelType channelType) { this.channelType = channelType; }
    public EvaluationGrade getEvaluationGrade() { return evaluationGrade; }
    public void setEvaluationGrade(EvaluationGrade evaluationGrade) {
        this.evaluationGrade = evaluationGrade;
        this.bonusRatio = (evaluationGrade == EvaluationGrade.S) ? 1.5 : 1.2;
    }
    public Double getBonusRatio() { return bonusRatio; }
    public void setBaseSalary(Long baseSalary) { this.baseSalary = baseSalary; }
    public Double getBonusAmount() { return bonusAmount; }
    public void setRequestReason(String requestReason) { this.requestReason = requestReason; }
    public String getRequestReason() { return requestReason; }
    public String getRequestNo() { return requestNo; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
}