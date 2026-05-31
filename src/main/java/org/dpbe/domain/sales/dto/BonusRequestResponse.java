package org.dpbe.domain.sales.dto;

import java.time.LocalDateTime;
import org.dpbe.domain.sales.entity.BonusRequest;

public record BonusRequestResponse(
        Long id,
        String requestNo,
        String evaluationNo,
        String channelName,
        String channelType,
        String evaluationGrade,
        Double bonusRatio,
        Double bonusAmount,
        String requestReason,
        LocalDateTime requestedAt
) {
    public static BonusRequestResponse from(BonusRequest r) {
        return new BonusRequestResponse(
                r.getId(),
                r.getRequestNo(),
                r.getEvaluationNo(),
                r.getChannelName(),
                r.getChannelType() != null ? r.getChannelType().name() : null,
                r.getEvaluationGrade() != null ? r.getEvaluationGrade().name() : null,
                r.getBonusRatio(),
                r.getBonusAmount(),
                r.getRequestReason(),
                r.getRequestedAt());
    }
}