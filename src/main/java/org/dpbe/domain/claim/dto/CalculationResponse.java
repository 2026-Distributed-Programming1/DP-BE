package org.dpbe.domain.claim.dto;

import org.dpbe.domain.claim.entity.ClaimCalculation;

/**
 * 보험금 산출 조회/등록 결과.
 * 산출은 조사 결과(인정손해액·과실비율)와 약관 기본값으로 자동 계산된다(엔터티 calculate()).
 */
public record CalculationResponse(
        String calculationNo,
        String investigationNo,
        long recognizedDamage,
        double faultRatio,
        long deductible,
        long coverageLimit,
        long finalAmount,
        boolean exceededDeductible,
        boolean adjusted,
        String status
) {
    public static CalculationResponse from(ClaimCalculation c) {
        return new CalculationResponse(
                c.getCalculationNo(),
                c.getInvestigation() != null ? c.getInvestigation().getInvestigationNo() : null,
                c.getRecognizedDamage(),
                c.getFaultRatio(),
                c.getDeductible(),
                c.getCoverageLimit(),
                c.getFinalAmount(),
                c.isExceededDeductible(),
                c.isAdjusted(),
                c.getStatus() != null ? c.getStatus().name() : null);
    }
}