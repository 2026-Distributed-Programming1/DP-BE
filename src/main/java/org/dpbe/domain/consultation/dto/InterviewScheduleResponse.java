package org.dpbe.domain.consultation.dto;

import java.time.LocalDateTime;
import org.dpbe.domain.consultation.entity.InterviewSchedule;

public record InterviewScheduleResponse(
        String scheduleNo,
        String customerName,
        String designerName,
        String interviewType,
        LocalDateTime scheduledAt,
        String location,
        String preparation,
        String status,
        LocalDateTime registeredAt,
        LocalDateTime modifiedAt,
        LocalDateTime cancelledAt
) {
    public static InterviewScheduleResponse from(InterviewSchedule s) {
        return new InterviewScheduleResponse(
                s.getScheduleNo(), s.getCustomerName(), s.getDesignerName(),
                s.getType(), s.getScheduledAt(), s.getLocation(), s.getPreparation(),
                s.getStatus(), s.getRegisteredAt(), s.getModifiedAt(), s.getCancelledAt());
    }
}