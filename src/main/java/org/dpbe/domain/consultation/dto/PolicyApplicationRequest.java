package org.dpbe.domain.consultation.dto;

import java.time.LocalDateTime;

public record PolicyApplicationRequest(
        String customerId,
        String customerName,
        String productName,
        int period,
        String paymentMethod,
        LocalDateTime uploadedAt
) {}