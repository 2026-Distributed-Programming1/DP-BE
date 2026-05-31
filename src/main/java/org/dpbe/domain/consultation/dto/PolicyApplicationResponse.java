package org.dpbe.domain.consultation.dto;

import java.time.LocalDateTime;
import org.dpbe.domain.consultation.entity.PolicyApplication;

public record PolicyApplicationResponse(
        String applicationNo,
        String customerName,
        String productName,
        int period,
        String paymentMethod,
        String status,
        LocalDateTime submittedAt,
        LocalDateTime uploadedAt
) {
    public static PolicyApplicationResponse from(PolicyApplication p) {
        return new PolicyApplicationResponse(
                p.getApplicationNo(), p.getCustomerName(), p.getProductName(),
                p.getPeriod(), p.getPaymentMethod(), p.getStatus(),
                p.getSubmittedAt(), p.getUploadedAt());
    }
}