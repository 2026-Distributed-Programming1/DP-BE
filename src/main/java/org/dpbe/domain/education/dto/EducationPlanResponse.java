package org.dpbe.domain.education.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.dpbe.domain.education.entity.EducationPlan;

public record EducationPlanResponse(
        Long id,
        String planNo,
        String trainerName,
        String educationName,
        String channelType,
        LocalDate startDate,
        LocalDate endDate,
        int targetCount,
        long budget,
        String educationGoal,
        String educationContent,
        String textbookList,
        String rejectReason,
        LocalDateTime approvedAt,
        String status
) {
    public static EducationPlanResponse from(EducationPlan plan) {
        return new EducationPlanResponse(
                plan.getId(), plan.getPlanNo(), plan.getTrainerName(),
                plan.getEducationName(), plan.getChannelType(),
                plan.getStartDate(), plan.getEndDate(),
                plan.getTargetCount(), plan.getBudget(),
                plan.getEducationGoal(), plan.getEducationContent(),
                plan.getTextbookList(), plan.getRejectReason(),
                plan.getApprovedAt(), plan.getStatus() != null ? plan.getStatus().name() : null
        );
    }
}