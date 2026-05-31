package org.dpbe.domain.sales.dto;

import java.time.LocalDateTime;

public record ScheduleItemRequest(
        String customerId,
        String activityType,
        LocalDateTime activityDateTime,
        String location,
        String memo
) {}