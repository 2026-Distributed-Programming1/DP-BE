package org.dpbe.domain.sales.dto;

public record SalesOrgEvaluationRequest(
        String channelName,
        String channelType,
        Long salesResult,
        Integer contractCount,
        Double achievementRate,
        String evaluationGrade,
        String evaluationComment
) {}