package org.dpbe.domain.contract.dto;

import java.time.LocalDate;

public record ExpiringContractSummaryResponse(
        String contractNo,
        String contractorName,
        String insuranceType,
        LocalDate expiryDate,
        long remainingDays,
        long monthlyPremium,
        String status) {}