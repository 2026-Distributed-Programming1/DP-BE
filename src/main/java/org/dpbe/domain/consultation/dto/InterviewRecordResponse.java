package org.dpbe.domain.consultation.dto;

import java.time.LocalDateTime;
import org.dpbe.domain.consultation.entity.InterviewRecord;

public record InterviewRecordResponse(
        String recordNo,
        String customerName,
        String content,
        String customerReaction,
        String followUpAction,
        LocalDateTime interviewedAt,
        LocalDateTime recordedAt,
        LocalDateTime modifiedAt
) {
    public static InterviewRecordResponse from(InterviewRecord r) {
        return new InterviewRecordResponse(
                r.getRecordNo(), r.getCustomerName(), r.getContent(),
                r.getCustomerReaction(), r.getFollowUpAction(),
                r.getInterviewedAt(), r.getRecordedAt(), r.getModifiedAt());
    }
}