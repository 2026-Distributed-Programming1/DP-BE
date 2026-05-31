package org.dpbe.domain.sales.dto;

import java.time.LocalDate;

public record SalesActivityManagementRequest(
        String managerName,
        String channelName,
        String channelType,
        LocalDate startDate,
        LocalDate endDate,
        Integer visitCount,
        Integer contractCount,
        Double achievementRate,
        String improvementContent,
        Integer revisedTarget
) {}