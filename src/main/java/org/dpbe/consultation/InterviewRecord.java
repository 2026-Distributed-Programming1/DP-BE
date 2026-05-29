package org.dpbe.consultation;

import java.time.LocalDateTime;

/**
 * 면담기록 (InterviewRecord)
 * UC: 면담 결과를 기록한다
 */
public class InterviewRecord {

    private static int sequence = 0;

    private int recordNumber;
    private String customerName;
    private LocalDateTime interviewedAt;
    private LocalDateTime recordedAt;
    private LocalDateTime modifiedAt;
    private String content;
    private String customerReaction;
    private String followUpAction;

    public InterviewRecord(int recordNumber, String customerName, LocalDateTime interviewedAt,
                           String content, String customerReaction, String followUpAction) {
        this.recordNumber = recordNumber;
        this.customerName = customerName;
        this.interviewedAt = interviewedAt;
        this.content = content;
        this.customerReaction = customerReaction;
        this.followUpAction = followUpAction;
    }

    public InterviewRecord() {
        sequence += 1;
        this.recordNumber = sequence;
    }

    private InterviewRecord(boolean fromDb) {}

    public static InterviewRecord fromDb(int recordNumber, String customerName, String content,
                                         LocalDateTime interviewedAt, String customerReaction,
                                         String followUpAction) {
        InterviewRecord r = new InterviewRecord(true);
        r.recordNumber    = recordNumber;
        r.customerName    = customerName;
        r.content         = content;
        r.interviewedAt   = interviewedAt;
        r.customerReaction = customerReaction;
        r.followUpAction  = followUpAction;
        return r;
    }

    public void save(String content, String customerReaction, String followUpAction) {
        this.content = content;
        this.customerReaction = customerReaction;
        this.followUpAction = followUpAction;
        this.recordedAt = LocalDateTime.now();
    }

    public void modify(String content, String customerReaction, String followUpAction) {
        this.content = content;
        this.customerReaction = customerReaction;
        this.followUpAction = followUpAction;
        this.modifiedAt = LocalDateTime.now();
    }

    public boolean validateRequiredFields() {
        return content != null && !content.isEmpty()
                && customerReaction != null && !customerReaction.isEmpty();
    }

    public Proposal navigateToProposal() {
        return new Proposal();
    }

    public int getRecordNumber() { return recordNumber; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public LocalDateTime getInterviewedAt() { return interviewedAt; }
    public void setInterviewedAt(LocalDateTime interviewedAt) { this.interviewedAt = interviewedAt; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
    public LocalDateTime getModifiedAt() { return modifiedAt; }
    public String getContent() { return content; }
    public String getCustomerReaction() { return customerReaction; }
    public String getFollowUpAction() { return followUpAction; }
}