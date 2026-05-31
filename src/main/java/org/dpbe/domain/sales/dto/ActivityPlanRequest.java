package org.dpbe.domain.sales.dto;

import java.time.LocalDate;
import java.util.List;

public record ActivityPlanRequest(
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
        List<ScheduleItemRequest> schedules
) {}