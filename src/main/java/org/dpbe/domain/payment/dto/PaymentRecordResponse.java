package org.dpbe.domain.payment.dto;

/** 제출 결과로 생성된 납부 내역 */
public record PaymentRecordResponse(
        String recordNo,
        String contractNo,
        long amount
) {
}