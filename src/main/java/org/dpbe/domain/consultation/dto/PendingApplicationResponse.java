package org.dpbe.domain.consultation.dto;

import org.dpbe.domain.common.enums.ApplicationType;

public record PendingApplicationResponse(
        ApplicationType applicationType,
        String applicationNo,
        String customerName,
        String productName,
        String paymentMethod,
        String status
) {}