package org.dpbe.domain.consultation.dto;

import java.time.LocalDateTime;
import org.dpbe.domain.consultation.entity.InsuranceApplication;

public record InsuranceApplicationResponse(
        String applicationNo,
        String customerName,
        String productName,
        String paymentMethod,
        String status,
        LocalDateTime appliedAt
) {
    public static InsuranceApplicationResponse from(InsuranceApplication a) {
        String productName = a.getProduct() != null ? a.getProduct().getProductName() : null;
        String customerName = a.getCustomer() != null ? a.getCustomer().getName() : null;
        return new InsuranceApplicationResponse(
                a.getApplicationNo(), customerName, productName,
                a.getPaymentMethod(), a.getStatus(), a.getAppliedAt());
    }
}