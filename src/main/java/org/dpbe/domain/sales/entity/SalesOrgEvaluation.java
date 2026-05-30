package org.dpbe.domain.sales.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.dpbe.domain.common.enums.ChannelType;
import org.dpbe.domain.common.enums.EvaluationGrade;

/**
 * 영업 조직 평가 (SalesOrgEvaluation)
 * 실적을 바탕으로 판매채널의 성과를 평가하고 등급을 부여하는 클래스이다.
 */
public class SalesOrgEvaluation {
    private Long id;
    private LocalDate filterStartDate;      // 평가 기간 시작일
    private LocalDate filterEndDate;        // 평가 기간 종료일
    private ChannelType channelType;        // 채널 유형 - 설계사/대리점 (enum)
    private String channelName;             // 채널명
    private Long salesResult;              // 매출실적
    private Integer contractCount;         // 계약건수
    private Double achievementRate;        // 목표달성률
    private EvaluationGrade evaluationGrade; // 평가 등급 - S/A/B/C/D 필수 (enum)
    private String evaluationComment;      // 평가 의견 선택
    private String evaluationNo;           // 평가번호
    private LocalDateTime evaluatedAt;     // 등록 일시

    public void loadPerformanceTable() {}

    public void search() {}

    public void showNoResultMessage() {}

    public void sortByAchievementRate() {}

    public void openDetailPanel() {}

    public void closeDetailPanel() {}

    public void openEvaluationForm() {}

    public Boolean validateRequired() {
        return evaluationGrade != null;
    }

    public void highlightError() {}

    public void saveEvaluation() {
        this.evaluatedAt = LocalDateTime.now();
        this.evaluationNo = "EV-" + evaluatedAt.toString().replaceAll("[^0-9]", "").substring(0, 14);
    }

    public void showEvaluationResult() {}

    public void cancelEvaluation() {}

    public void navigateToBonus() {}

    // Runner에서 실제 사용하는 getter/setter만 유지
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSalesResult() { return salesResult; }
    public Integer getContractCount() { return contractCount; }
    public String getEvaluationComment() { return evaluationComment; }
    public ChannelType getChannelType() { return channelType; }
    public String getEvaluationNo() { return evaluationNo; }
    public void setEvaluationNo(String evaluationNo) { this.evaluationNo = evaluationNo; }
    public void setFilterStartDate(LocalDate filterStartDate) { this.filterStartDate = filterStartDate; }
    public void setFilterEndDate(LocalDate filterEndDate) { this.filterEndDate = filterEndDate; }
    public void setChannelType(ChannelType channelType) { this.channelType = channelType; }
    public String getChannelName() { return channelName; }
    public void setChannelName(String channelName) { this.channelName = channelName; }
    public void setSalesResult(Long salesResult) { this.salesResult = salesResult; }
    public void setContractCount(Integer contractCount) { this.contractCount = contractCount; }
    public Double getAchievementRate() { return achievementRate; }
    public void setAchievementRate(Double achievementRate) { this.achievementRate = achievementRate; }
    public EvaluationGrade getEvaluationGrade() { return evaluationGrade; }
    public void setEvaluationGrade(EvaluationGrade evaluationGrade) { this.evaluationGrade = evaluationGrade; }
    public void setEvaluationComment(String evaluationComment) { this.evaluationComment = evaluationComment; }
    public LocalDateTime getEvaluatedAt() { return evaluatedAt; }
    public void setEvaluatedAt(LocalDateTime evaluatedAt) { this.evaluatedAt = evaluatedAt; }
}