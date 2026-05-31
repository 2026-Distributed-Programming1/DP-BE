package org.dpbe.domain.sales.dto;

import java.time.LocalDateTime;
import org.dpbe.domain.sales.entity.ScheduleItem;

public record ScheduleItemResponse(
        String customerId,
        String activityType,
        LocalDateTime activityDateTime,
        String location,
        String memo
) {
    public static ScheduleItemResponse from(ScheduleItem item) {
        return new ScheduleItemResponse(
                item.getCustomerId(),
                item.getActivityType() != null ? item.getActivityType().name() : null,
                item.getActivityDateTime(),
                item.getLocation(),
                item.getMemo());
    }
}