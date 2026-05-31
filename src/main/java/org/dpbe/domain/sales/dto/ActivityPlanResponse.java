package org.dpbe.domain.sales.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.dpbe.domain.sales.entity.ActivityPlan;

public record ActivityPlanResponse(
        Long id,
        String planNo,
        String planName,
        LocalDate startDate,
        LocalDate endDate,
        String author,
        String memo,
        Integer targetContractCount,
        Long targetContractAmount,
        Integer targetNewCustomer,
        String proposedCustomerId,
        String proposedInsuranceType,
        String proposalReason,
        String status,
        List<ScheduleItemResponse> schedules
) {
    public static ActivityPlanResponse from(ActivityPlan p) {
        return new ActivityPlanResponse(
                p.getId(),
                p.getPlanNo(),
                p.getPlanName(),
                p.getStartDate(),
                p.getEndDate(),
                p.getAuthor(),
                p.getMemo(),
                p.getTargetContractCount(),
                p.getTargetContractAmount(),
                p.getTargetNewCustomer(),
                p.getProposedCustomerId(),
                p.getProposedInsuranceType() != null ? p.getProposedInsuranceType().name() : null,
                p.getProposalReason(),
                p.getStatus() != null ? p.getStatus().name() : null,
                p.getSchedules().stream()
                        .map(ScheduleItemResponse::from)
                        .collect(Collectors.toList()));
    }
}