package org.dpbe.domain.payment.dto;

import java.util.List;

/** 납입 미리보기 결과 — 총액/선납할인/최종결제액 */
public record PaymentPreviewResponse(
        long totalAmount,
        long earlyDiscount,
        long discountedAmount,
        List<PaymentLineResponse> lines
) {
}