package org.dpbe.domain.sales.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.dpbe.domain.common.enums.ChannelType;

/**
 * 영업 활동 관리 (SalesActivityManagement)
 * 채널별 활동 현황을 모니터링하고 지표를 관리하는 클래스이다.
 */
public class SalesActivityManagement {
    private Long id;
    private LocalDate startDate;           // 관리 기간 시작일
    private LocalDate endDate;             // 관리 기간 종료일
    private ChannelType channelType;       // 채널 유형 - 설계사/대리점 (enum)
    private String channelName;            // 채널명
    private Integer visitCount;            // 방문건수
    private Integer contractCount;         // 계약건수
    private Double conversionRate;         // 전환율 - 계약건수/방문건수 %
    private Double achievementRate;        // 목표달성률 - 실적/목표 %
    private String improvementContent;     // 개선 지시 내용
    private Integer revisedTarget;         // 수정 목표
    private String managerName;            // 담당 영업 관리자명
    private String managementNo;           // 관리번호
    private LocalDateTime registeredAt;    // 등록 일시

    public void loadActivityTable() {} //전체 현황 테이블 출력

    public void search() {} //조건 필터링

    public void showNoResultMessage() {}

    public void sortByAchievementRate() {} //목표달성률 낮은 순 정렬

    public void highlightLowAchievement() {}

    public void openDetailPanel() {}

    public void closeDetailPanel() {}

    public void openImprovementForm() {}

    public void saveImprovement() {
        this.registeredAt = LocalDateTime.now();
        this.managementNo = "SA-" + registeredAt.toString().replaceAll("[^0-9]", "").substring(0, 14);
    }

    public void showSaveSuccess() {}

    public void showSaveError() {}

    public void cancelImprovement() {}

    public void navigateToRecruitment() {}

    // Runner에서 실제 사용하는 getter/setter만 유지
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ChannelType getChannelType() { return channelType; }
    public Integer getVisitCount() { return visitCount; }
    public Integer getContractCount() { return contractCount; }
    public Double getAchievementRate() { return achievementRate; }
    public String getImprovementContent() { return improvementContent; }
    public Integer getRevisedTarget() { return revisedTarget; }
    public String getManagerName() { return managerName; }
    public void setManagerName(String managerName) { this.managerName = managerName; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public void setChannelType(ChannelType channelType) { this.channelType = channelType; }
    public String getChannelName() { return channelName; }
    public void setChannelName(String channelName) { this.channelName = channelName; }
    public void setVisitCount(Integer visitCount) {
        this.visitCount = visitCount;
        recalcConversionRate();
    }
    public void setContractCount(Integer contractCount) {
        this.contractCount = contractCount;
        recalcConversionRate();
    }
    public Double getConversionRate() { return conversionRate; }
    public void setAchievementRate(Double achievementRate) { this.achievementRate = achievementRate; }
    public void setImprovementContent(String improvementContent) { this.improvementContent = improvementContent; }
    public void setRevisedTarget(Integer revisedTarget) { this.revisedTarget = revisedTarget; }
    public String getManagementNo() { return managementNo; }
    public void setManagementNo(String managementNo) { this.managementNo = managementNo; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }
    public String getActivityType() { return channelType != null ? channelType.name() : null; }
    public void setActivityType(String activityType) {
        if (activityType != null) {
            try { this.channelType = ChannelType.valueOf(activityType); }
            catch (IllegalArgumentException ignored) {}
        }
    }

    private void recalcConversionRate() {
        if (visitCount != null && visitCount > 0 && contractCount != null) {
            this.conversionRate = (double) contractCount / visitCount * 100;
        }
    }
}