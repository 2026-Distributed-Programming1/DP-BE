package org.dpbe.domain.consultation.dto;

public record PendingApplicationResponse(
        String applicationType,
        String applicationNo,
        String customerName,
        String productName,
        String paymentMethod,
        String status
) {}