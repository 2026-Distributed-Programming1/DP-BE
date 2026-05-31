package org.dpbe.domain.claim.dto;

import java.time.LocalDateTime;

/**
 * 보험금 지급 생성 요청 (산출 건 기준).
 * paymentType: IMMEDIATE(즉시) | SCHEDULED(예약). SCHEDULED면 scheduledAt 필수.
 * 생성 시점엔 지급건만 만든다(WAITING/SCHEDULED). 실제 이체는 별도 execute 호출.
 * 수령인·계좌는 청구 단계 값을 조인으로 승계하므로 입력받지 않는다.
 */
public record PaymentCreateRequest(
        String paymentType,
        LocalDateTime scheduledAt
) {
}