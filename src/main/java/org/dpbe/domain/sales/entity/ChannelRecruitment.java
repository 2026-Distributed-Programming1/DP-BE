package org.dpbe.domain.sales.entity;

import org.dpbe.domain.common.enums.ChannelType;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 판매채널 모집 (ChannelRecruitment)
 * 영업 관리자가 새로운 판매채널을 모집하기 위해 공고를 등록하고 관리하는 클래스이다.
 */
public class ChannelRecruitment {
    private Long id;
    private String recruitmentNo;           // 모집번호
    private String managerName;             // 등록 영업 관리자명
    private ChannelType channelType;        // 채널 유형 - 설계사/대리점 (enum) 필수
    private Integer recruitCount;           // 모집 인원 필수
    private LocalDate startDate;            // 모집 기간 시작일 필수
    private LocalDate endDate;              // 모집 기간 종료일 필수
    private String condition;               // 모집 조건 선택
    private LocalDateTime registeredAt;     // 등록 일시

    public ChannelRecruitment() {}

    public static ChannelRecruitment fromDb(String recruitmentNo, ChannelType channelType,
                                             String managerName, LocalDateTime registeredAt) {
        ChannelRecruitment r = new ChannelRecruitment();
        r.recruitmentNo = recruitmentNo;
        r.channelType   = channelType;
        r.managerName   = managerName;
        r.registeredAt  = registeredAt;
        return r;
    }

    public void loadRecruitmentList() {}
    public void openRegistrationForm() {}

    public Boolean validateRequired() {
        return channelType != null && recruitCount != null && recruitCount > 0
                && startDate != null && endDate != null;
    }

    public void highlightError() {}

    public void save() {
        this.registeredAt = LocalDateTime.now();
        this.recruitmentNo = "RC-" + registeredAt.toString().replaceAll("[^0-9]", "").substring(0, 14);
    }

    public void showSaveSuccess() {}
    public void showSaveResult() {}
    public void cancel() {}
    public void showCancelConfirm() {}
    public void close() {}
    public void returnToActivityManagement() {}
    public void openCalendar() {}
    public void showRequiredError() {}

    // Getters / Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRecruitmentNo() { return recruitmentNo; }
    public String getManagerName() { return managerName; }
    public void setManagerName(String managerName) { this.managerName = managerName; }
    public ChannelType getChannelType() { return channelType; }
    public void setChannelType(ChannelType channelType) { this.channelType = channelType; }
    public Integer getRecruitCount() { return recruitCount; }
    public void setRecruitCount(Integer recruitCount) { this.recruitCount = recruitCount; }
    public LocalDate getLocalStartDate() { return startDate; }
    public void setLocalStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getLocalEndDate() { return endDate; }
    public void setLocalEndDate(LocalDate endDate) { this.endDate = endDate; }
    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }
}