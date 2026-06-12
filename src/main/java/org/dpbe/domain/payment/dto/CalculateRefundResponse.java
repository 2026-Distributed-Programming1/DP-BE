package org.dpbe.domain.payment.dto;

import org.dpbe.domain.payment.entity.RefundCalculation;

public record CalculateRefundResponse(
        String refundNo,
        String cancellationNo,
        long totalPaidPremium,
        String paymentPeriod,
        long reserveAmount,
        double appliedRate,
        long baseRefund,
        long unpaidPremium,
        long finalRefund,
        String status
) {
    public static CalculateRefundResponse from(RefundCalculation r) {
        return new CalculateRefundResponse(
                r.getRefundNo(),
                r.getCancellation() != null ? r.getCancellation().getCancellationNo() : null,
                r.getTotalPaidPremium(),
                r.getPaymentPeriod(),
                r.getReserveAmount(),
                r.getAppliedRate(),
                r.getBaseRefund(),
                r.getUnpaidPremium(),
                r.getFinalRefund(),
                r.getStatus() != null ? r.getStatus().name() : null
        );
    }
}
