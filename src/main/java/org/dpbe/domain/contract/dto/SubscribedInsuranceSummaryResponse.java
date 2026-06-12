package org.dpbe.domain.contract.dto;

import java.time.LocalDate;

/** 가입 보험 목록 행 (UC '가입 보험을 조회한다') */
public record SubscribedInsuranceSummaryResponse(
        String contractNo,
        String customerName,
        String insuranceType,
        LocalDate startDate,
        LocalDate endDate,
        long monthlyPremium,
        int paidCount,
        int totalPayCount,
        String status,
        String statusLabel
) {
}
