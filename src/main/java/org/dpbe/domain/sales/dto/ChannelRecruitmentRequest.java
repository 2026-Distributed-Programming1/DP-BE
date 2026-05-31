package org.dpbe.domain.sales.dto;

import java.time.LocalDate;

public record ChannelRecruitmentRequest(
        String managerName,
        String channelType,
        Integer recruitCount,
        LocalDate startDate,
        LocalDate endDate,
        String condition
) {}