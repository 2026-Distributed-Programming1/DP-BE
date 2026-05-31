package org.dpbe.domain.contract.dto;

import java.time.LocalDateTime;

public record ContractStatisticsResponse(
        String statsNo,
        int totalCount,
        int activeCount,
        int expiredCount,
        int cancelledCount,
        LocalDateTime createdAt) {}