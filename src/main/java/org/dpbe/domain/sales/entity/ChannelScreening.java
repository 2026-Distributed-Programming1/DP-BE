package org.dpbe.domain.sales.entity;

import org.dpbe.domain.common.enums.ChannelType;
import org.dpbe.domain.common.enums.ScreeningStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 판매채널 채용 심사 (ChannelScreening)
 * 지원자의 자격과 경력을 심사하여 채용 여부를 결정하는 클래스이다.
 */
public class ChannelScreening {
    private String applicantName;           // 지원자명
    private ChannelType channelType;        // 채널 유형 - 설계사/대리점 (enum)
    private LocalDate applicationDate = LocalDate.now();      // 지원일
    private String career;                  // 경력 사항
    private List<String> certifications = new ArrayList<>();    // 자격증 목록
    private ScreeningStatus screeningStatus = ScreeningStatus.PENDING; // 심사 상태 - 대기/승인/거절 (enum)
    private String approvalNo;              // 승인번호
    private LocalDateTime approvedAt;       // 승인 일시
    private String rejectionReason;         // 거절 사유
    private LocalDate filterStartDate;      // 조회 시작일
    private LocalDate filterEndDate;        // 조회 종료일

//    public ChannelScreening() {
//        this.certifications = new ArrayList<>();
//        this.screeningStatus = ScreeningStatus.PENDING;
//        this.applicationDate = LocalDate.now();
//    }

    public void loadApplicantList() {}

    public void search() {}

    public void showNoResultMessage() {}

    public void openDetailPanel() {}

    public void showApprovalConfirm() {}

    public void approve() {
        this.approvedAt = LocalDateTime.now();
        this.approvalNo = "AP-" + approvedAt.toString().replaceAll("[^0-9]", "").substring(0, 14);
        this.screeningStatus = ScreeningStatus.APPROVED;
    }

    public void notifyHr() {}

    public void showApprovalResult() {}

    public void showNotifyError() {}

    public void showRegistrationError() {}

    public void openRejectionPopup() {}

    public void reject() {
        this.screeningStatus = ScreeningStatus.REJECTED;
    }

    public void closeDetailPanel() {}

    // Getters / Setters
    public String getApplicantName() { return applicantName; }
    public void setApplicantName(String applicantName) { this.applicantName = applicantName; }
    public ChannelType getChannelType() { return channelType; }
    public void setChannelType(ChannelType channelType) { this.channelType = channelType; }
    public LocalDate getApplicationDate() { return applicationDate; }
    public void setApplicationDate(LocalDate applicationDate) { this.applicationDate = applicationDate; }
    public String getCareer() { return career; }
    public void setCareer(String career) { this.career = career; }
    public List<String> getCertifications() { return certifications; }
    public void addCertification(String cert) { this.certifications.add(cert); }
    public void setCertifications(List<String> certifications) {
        this.certifications = certifications != null ? certifications : new ArrayList<>();
    }
    public ScreeningStatus getScreeningStatus() { return screeningStatus; }
    public void setScreeningStatus(ScreeningStatus screeningStatus) { this.screeningStatus = screeningStatus; }
    public String getApprovalNo() { return approvalNo; }
    public void setApprovalNo(String approvalNo) { this.approvalNo = approvalNo; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public LocalDate getFilterStartDate() { return filterStartDate; }
    public void setFilterStartDate(LocalDate filterStartDate) { this.filterStartDate = filterStartDate; }
    public LocalDate getFilterEndDate() { return filterEndDate; }
    public void setFilterEndDate(LocalDate filterEndDate) { this.filterEndDate = filterEndDate; }
}