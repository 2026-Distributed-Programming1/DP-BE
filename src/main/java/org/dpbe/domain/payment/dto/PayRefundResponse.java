package org.dpbe.domain.payment.dto;

import java.time.LocalDateTime;
import org.dpbe.domain.payment.entity.RefundPayment;

public record PayRefundResponse(
        String paymentNo,
        String refundNo,
        long finalAmount,
        LocalDateTime transferredAt,
        boolean noticeSent,
        int otpFailCount,
        String status
) {
    public static PayRefundResponse from(RefundPayment p) {
        return new PayRefundResponse(
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
