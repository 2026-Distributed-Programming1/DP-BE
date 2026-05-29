package org.dpbe.domain.payment.dto;

import java.util.List;

/**
 * 납입 신청 제출 (클라이언트 주도 — 완성된 상태를 한 번에 전송).
 * paymentMethod: IMMEDIATE_TRANSFER | VIRTUAL_ACCOUNT
 * 계좌는 신규 입력 정보를 전달한다(인증 후 저장).
 */
public record PaymentSubmitRequest(
        String customerId,
        List<PaymentItemRequest> items,
        String paymentMethod,
        String bankName,
        String accountNo,
        String accountHolder
) {
}