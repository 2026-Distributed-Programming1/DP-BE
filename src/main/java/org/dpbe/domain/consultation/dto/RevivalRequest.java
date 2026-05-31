package org.dpbe.domain.consultation.dto;

public record RevivalRequest(
        String customerId,
        String customerName,
        String contractNo,
        String contact,
        long unpaidAmount,
        String paymentMethod
) {}