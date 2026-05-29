package org.dpbe.domain.payment.dto;

import java.time.LocalDateTime;
import java.util.List;

/** 납입 신청 완료 결과 */
public record PaymentResultResponse(
        String paymentNo,
        LocalDateTime requestedAt,
        long totalAmount,
        long earlyDiscount,
        long discountedAmount,
        String paymentMethod,
        List<PaymentRecordResponse> records
) {
}