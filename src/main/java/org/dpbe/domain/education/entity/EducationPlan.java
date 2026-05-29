package org.dpbe.domain.education.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 교육계획안 (EducationPlan)
 * UC: 교육 계획안을 작성한다
 */
public class EducationPlan {

    private static int sequence = 0;

    private int planNumber;
    private String trainerName;
    private String educationName;
    private LocalDate startDate;
    private LocalDate endDate;
    private String channelType;
    private int targetCount;
    private long budget;
    private String educationGoal;
    private String educationContent;
    private String textbookList;
    private String rejectReason;
    private LocalDateTime approvedAt;
    private String status;

    public EducationPlan(int planNumber, String educationName, LocalDate startDate, LocalDate endDate,
                         String channelType, int targetCount, long budget, String status) {
        this.planNumber = planNumber;
        this.educationName = educationName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.channelType = channelType;
        this.targetCount = targetCount;
        this.budget = budget;
        this.status = status;
    }

    public EducationPlan() {
        sequence += 1;
        this.planNumber = sequence;
        this.status = "작성중";
    }

    private EducationPlan(boolean fromDb) {}

    public static EducationPlan fromDb(int planNumber, String trainerName, String educationName,
                                        String channelType, LocalDate startDate, LocalDate endDate,
                                        int targetCount, long budget,
                                        String educationGoal, String educationContent,
                                        String textbookList, String rejectReason, String status) {
        EducationPlan p = new EducationPlan(true);
        p.planNumber      = planNumber;
        p.trainerName     = trainerName;
        p.educationName   = educationName;
        p.channelType     = channelType;
        p.startDate       = startDate;
        p.endDate         = endDate;
        p.targetCount     = targetCount;
        p.budget          = budget;
        p.educationGoal   = educationGoal;
        p.educationContent = educationContent;
        p.textbookList    = textbookList;
        p.rejectReason    = rejectReason;
        p.status          = status;
        return p;
    }

    public void enterPlanInfo(String educationName, LocalDate startDate, LocalDate endDate,
                              String channelType, int targetCount, long budget) {
        this.educationName = educationName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.channelType = channelType;
        this.targetCount = targetCount;
        this.budget = budget;
    }

    public void enterContentInfo(String educationGoal, String educationContent, String textbookList) {
        this.educationGoal    = educationGoal;
        this.educationContent = educationContent;
        this.textbookList     = textbookList;
    }

    public boolean validateRequiredFields() {
        return educationName != null && !educationName.isEmpty()
                && startDate != null && endDate != null
                && channelType != null && !channelType.isEmpty()
                && targetCount > 0 && budget > 0
                && educationGoal != null && !educationGoal.isEmpty()
                && educationContent != null && !educationContent.isEmpty();
    }

    public void requestApproval() {
        this.status = "승인요청";
    }

    public void tempSave() {
        this.status = "임시저장";
    }

    public void approve() {
        this.approvedAt = LocalDateTime.now();
        this.status = "승인";
    }

    public void reject(String reason) {
        this.rejectReason = reason;
        this.status = "반려";
    }

    public int getPlanNumber() { return planNumber; }
    public String getTrainerName() { return trainerName; }
    public void setTrainerName(String trainerName) { this.trainerName = trainerName; }
    public String getEducationName() { return educationName; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public String getChannelType() { return channelType; }
    public int getTargetCount() { return targetCount; }
    public long getBudget() { return budget; }
    public String getEducationGoal() { return educationGoal; }
    public String getEducationContent() { return educationContent; }
    public String getTextbookList() { return textbookList; }
    public String getRejectReason() { return rejectReason; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
    public String getStatus() { return status; }
}
