package org.dpbe.domain.payment.dto;

import java.time.LocalDateTime;
import org.dpbe.domain.payment.entity.RefundPayment;

public record RefundPaymentResponse(
        String paymentNo,
        String refundNo,
        long finalAmount,
        LocalDateTime transferredAt,
        boolean noticeSent,
        int otpFailCount,
        String status
) {
    public static RefundPaymentResponse from(RefundPayment p) {
        return new RefundPaymentResponse(
                p.getPaymentNo(),
                p.getRefund() != null ? p.getRefund().getRefundNo() : null,
                p.getFinalAmount(),
                p.getTransferredAt(),
                p.isNoticeSent(),
                p.getOtpFailCount(),
                p.getStatus() != null ? p.getStatus().name() : null
        );
    }
}