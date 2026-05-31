package org.dpbe.domain.consultation.dto;

public record InsuranceApplicationRequest(
        String customerId,
        String customerName,
        String productName,
        String paymentMethod
) {}