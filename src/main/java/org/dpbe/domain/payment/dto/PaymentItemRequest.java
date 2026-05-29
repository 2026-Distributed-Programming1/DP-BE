package org.dpbe.domain.payment.dto;

/** 계약별 납입 항목 입력 (N:M) */
public record PaymentItemRequest(
        String contractNo,
        int count
) {
}