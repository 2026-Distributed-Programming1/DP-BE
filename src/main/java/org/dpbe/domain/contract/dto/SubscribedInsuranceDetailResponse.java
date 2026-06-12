package org.dpbe.domain.contract.dto;

import java.time.LocalDate;
import java.util.List;

/** 가입 보험 상세 (UC '가입 보험을 조회한다') */
public record SubscribedInsuranceDetailResponse(
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
