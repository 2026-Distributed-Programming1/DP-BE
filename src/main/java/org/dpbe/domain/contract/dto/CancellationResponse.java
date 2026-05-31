package org.dpbe.domain.contract.dto;

import java.time.LocalDateTime;

public record CancellationResponse(
        String cancellationNo,
        String contractNo,
        String customerName,
        long monthlyPremium,
        String reason,
        String detailReason,
        long expectedRefund,
        String status,
        LocalDateTime canceledAt) {}