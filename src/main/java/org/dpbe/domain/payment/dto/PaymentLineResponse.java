package org.dpbe.domain.payment.dto;

/** 미리보기/결과의 계약별 소계 라인 */
public record PaymentLineResponse(
        String contractNo,
        long premiumPerCount,
        int count,
        long subtotal
) {
}