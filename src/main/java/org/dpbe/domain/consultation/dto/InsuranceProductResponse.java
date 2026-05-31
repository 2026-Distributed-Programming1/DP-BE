package org.dpbe.domain.consultation.dto;

import org.dpbe.domain.consultation.entity.InsuranceProduct;

public record InsuranceProductResponse(
        String productName,
        String category,
        long monthlyPremium,
        String coverageSummary,
        String exclusionSummary
) {
    public static InsuranceProductResponse from(InsuranceProduct p) {
        return new InsuranceProductResponse(
                p.getProductName(), p.getType(), p.getMonthlyPremium(),
                p.getCoverage(), p.getSpecialTerms());
    }
}