package org.dpbe.domain.sales.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.dpbe.domain.sales.entity.SalesActivityManagement;

public record SalesActivityManagementResponse(
        Long id,
        String activityNo,
        String managerName,
        String channelName,
        String channelType,
        LocalDate startDate,
        LocalDate endDate,
        Integer visitCount,
        Integer contractCount,
        Double conversionRate,
        Double achievementRate,
        String improvementContent,
        Integer revisedTarget,
        LocalDateTime registeredAt
) {
    public static SalesActivityManagementResponse from(SalesActivityManagement a) {
        return new SalesActivityManagementResponse(
                a.getId(),
                a.getActivityNo(),
                a.getManagerName(),
                a.getChannelName(),
                a.getChannelType() != null ? a.getChannelType().name() : null,
                a.getStartDate(),
                a.getEndDate(),
                a.getVisitCount(),
                a.getContractCount(),
                a.getConversionRate(),
                a.getAchievementRate(),
                a.getImprovementContent(),
                a.getRevisedTarget(),
                a.getRegisteredAt());
    }
}