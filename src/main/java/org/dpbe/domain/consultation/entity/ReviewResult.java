package org.dpbe.domain.consultation.entity;

import java.time.LocalDateTime;

/**
 * 심사결과 (ReviewResult)
 * Underwriting 과 Composition 관계
 */
public class ReviewResult {

    private String result;
    private String condition;
    private String rejectionReason;
    private LocalDateTime confirmedAt;
    private String processingNo;

    public ReviewResult(String result, String condition, String rejectionReason, LocalDateTime confirmedAt) {
        this.result = result;
        this.condition = condition;
        this.rejectionReason = rejectionReason;
        this.confirmedAt = confirmedAt;
    }

    public ReviewResult(String result, String condition, String rejectionReason) {
        this.result = result;
        this.condition = condition;
        this.rejectionReason = rejectionReason;
    }

    public void deliver() {
      // 처리 필요
    }

    public void confirm() {
        this.confirmedAt = LocalDateTime.now();
        this.processingNo = "PR-" + confirmedAt.toString().replaceAll("[^0-9]", "").substring(0, 14);
        // 처리 필요
    }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public String getCoverage() { return condition; } // 기존 원본에 condition 필드를 getCoverage로 리턴하던 구조 100% 보존
    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public String getProcessingNo() { return processingNo; }
}