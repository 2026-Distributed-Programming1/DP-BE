package org.dpbe.domain.consultation.dto;

import java.time.LocalDateTime;
import org.dpbe.domain.consultation.entity.Revival;

public record RevivalResponse(
        String revivalNo,
        String contractNo,
        String customerName,
        String contact,
        long unpaidAmount,
        String paymentMethod,
        LocalDateTime appliedAt
) {
    public static RevivalResponse from(Revival r) {
        String customerName = r.getCustomer() != null ? r.getCustomer().getName() : null;
        return new RevivalResponse(
                r.getRevivalNo(), r.getContractNo(), customerName,
                r.getContact(), r.getUnpaidAmount(), r.getPaymentMethod(), r.getAppliedAt());
    }
}