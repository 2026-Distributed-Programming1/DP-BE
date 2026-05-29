package org.dpbe.domain.contract.dto;

import java.time.LocalDate;

/** 계약 목록 행 (UC '계약 정보를 조회한다' Basic 2) */
public record ContractSummaryResponse(
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