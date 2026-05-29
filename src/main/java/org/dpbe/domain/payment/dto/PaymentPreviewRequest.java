package org.dpbe.domain.payment.dto;

import java.util.List;

/** 납입 미리보기 요청 — 계약·횟수만으로 총액/선납할인 계산 (저장 없음) */
public record PaymentPreviewRequest(
        List<PaymentItemRequest> items
) {
}