package org.dpbe.domain.sales.dto;

public record BonusRequestRequest(
        String evaluationNo,
        String channelName,
        String channelType,
        String evaluationGrade,
        Long baseSalary,
        String requestReason
) {}