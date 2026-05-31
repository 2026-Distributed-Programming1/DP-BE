package org.dpbe.domain.sales.dto;

import java.time.LocalDateTime;
import org.dpbe.domain.sales.entity.SalesOrgEvaluation;

public record SalesOrgEvaluationResponse(
        Long id,
        String evaluationNo,
        String channelName,
        String channelType,
        Long salesResult,
        Integer contractCount,
        Double achievementRate,
        String evaluationGrade,
        String evaluationComment,
        LocalDateTime evaluatedAt
) {
    public static SalesOrgEvaluationResponse from(SalesOrgEvaluation e) {
        return new SalesOrgEvaluationResponse(
                e.getId(),
                e.getEvaluationNo(),
                e.getChannelName(),
                e.getChannelType() != null ? e.getChannelType().name() : null,
                e.getSalesResult(),
                e.getContractCount(),
                e.getAchievementRate(),
                e.getEvaluationGrade() != null ? e.getEvaluationGrade().name() : null,
                e.getEvaluationComment(),
                e.getEvaluatedAt());
    }
}