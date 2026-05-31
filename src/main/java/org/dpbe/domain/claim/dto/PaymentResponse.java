package org.dpbe.domain.claim.dto;

import java.time.LocalDateTime;
import org.dpbe.domain.claim.entity.ClaimPayment;

/** 보험금 지급 조회/생성/실행 결과 */
public record PaymentResponse(
        String paymentNo,
        String calculationNo,
        long finalAmount,
        String recipientName,
        String accountNo,
        String paymentType,
        LocalDateTime scheduledAt,
        LocalDateTime paidAt,
        String failureReason,
        String status
) {
    public static PaymentResponse from(ClaimPayment p) {
        return new PaymentResponse(
                p.getPaymentNo(),
                p.getCalculation() != null ? p.getCalculation().getCalculationNo() : null,
                p.getFinalAmount(),
                p.getRecipient() != null ? p.getRecipient().getName() : null,
                p.getAccount() != null ? p.getAccount().getAccountNo() : null,
                p.getPaymentType() != null ? p.getPaymentType().name() : null,
                p.getScheduledAt(),
                p.getPaidAt(),
                p.getFailureReason(),
                p.getStatus() != null ? p.getStatus().name() : null);
    }
}