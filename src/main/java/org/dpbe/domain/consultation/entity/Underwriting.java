package org.dpbe.domain.consultation.entity;

import java.time.LocalDateTime;

/**
 * 인수심사 (Underwriting)
 * UC: 인수 심사를 한다
 */
public class Underwriting {

    private static int sequence = 0;

    private int reviewNumber;
    private String appNo;
    private String customerName;
    private LocalDateTime reviewedAt;
    private String riskGrade;
    private String reviewType;
    private String reviewOpinion;
    private ReviewResult reviewResult;

    public Underwriting(int reviewNumber, LocalDateTime reviewedAt, String riskGrade, String reviewType,
                        String reviewOpinion, ReviewResult reviewResult) {
        this.reviewNumber = reviewNumber;
        this.reviewedAt = reviewedAt;
        this.riskGrade = riskGrade;
        this.reviewType = reviewType;
        this.reviewOpinion = reviewOpinion;
        this.reviewResult = reviewResult;
    }

    public Underwriting() {
        sequence += 1;
        this.reviewNumber = sequence;
    }

    public void startReview() {
        this.reviewedAt = LocalDateTime.now();
        System.out.println("  [시스템] 인수 심사를 시작합니다.");
    }

    public ReviewResult autoReview() {
        this.riskGrade = "일반";
        this.reviewResult = new ReviewResult("승인", null, null);
        System.out.println("  [시스템] 자동 심사가 완료되었습니다. 위험등급: " + riskGrade);
        return reviewResult;
    }

    public ReviewResult manualReview(String reviewType, String opinion) {
        this.reviewType = reviewType;
        this.reviewOpinion = opinion;
        this.reviewResult = new ReviewResult("승인", null, null);
        System.out.println("  [시스템] 수동 심사가 완료되었습니다.");
        return reviewResult;
    }

    public void attachDocument(String file) {
        System.out.println("  [시스템] 서류가 첨부되었습니다: " + file);
    }

    public void complete(String result, String condition, String rejectionReason) {
        this.reviewResult = new ReviewResult(result, condition, rejectionReason);
    }

    public int getReviewNumber() { return reviewNumber; }
    public String getAppNo() { return appNo; }
    public void setAppNo(String appNo) { this.appNo = appNo; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public String getRiskGrade() { return riskGrade; }
    public String getReviewType() { return reviewType; }
    public String getReviewOpinion() { return reviewOpinion; }
    public ReviewResult getReviewResult() { return reviewResult; }
}