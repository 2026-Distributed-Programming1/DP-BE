package org.dpbe.domain.contract.dto;

import java.time.LocalDate;
import java.util.List;

/** 계약 상세 패널 (UC '계약 정보를 조회한다' Basic 4, A3 만기임박, A5 특약) */
public record ContractDetailResponse(
        String contractNo,
        String policyNo,
        String customerName,
        String customerContact,
        String insuranceType,
        LocalDate startDate,
        LocalDate endDate,
        long monthlyPremium,
        String status,
        String statusLabel,
        int paidCount,
        int totalPayCount,
        LocalDate lastPaymentDate,
        boolean isOverdue,
        int overdueCount,
        boolean isExpiringSoon,
        long daysUntilExpiry,
        List<String> specialClauses
) {
}