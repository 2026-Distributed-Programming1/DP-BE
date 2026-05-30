package org.dpbe.domain.claim.dto;

import java.time.LocalDateTime;
import org.dpbe.domain.claim.entity.DamageInvestigation;

/** 손해 조사 조회/등록 결과 */
public record InvestigationResponse(
        String investigationNo,
        String claimNo,
        String handlerName,
        double ourFaultRatio,
        double counterFaultRatio,
        long recognizedDamage,
        String opinion,
        String result,
        String rejectReason,
        LocalDateTime investigatedAt,
        String status
) {
    public static InvestigationResponse from(DamageInvestigation inv) {
        return new InvestigationResponse(
                inv.getInvestigationNo(),
                inv.getClaim() != null ? inv.getClaim().getClaimNo() : null,
                inv.getHandler() != null ? inv.getHandler().getName() : null,
                inv.getOurFaultRatio(),
                inv.getCounterFaultRatio(),
                inv.getRecognizedDamage(),
                inv.getOpinion(),
                inv.getResult() != null ? inv.getResult().name() : null,
                inv.getRejectReason(),
                inv.getInvestigatedAt(),
                inv.getStatus() != null ? inv.getStatus().name() : null);
    }
}