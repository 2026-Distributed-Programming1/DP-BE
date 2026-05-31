package org.dpbe.domain.claim.dto;

/**
 * 보험금 이체 실행 요청 (OTP 인증 후 이체).
 * 현재 OTP 검증은 더미(6자리)이며, 실제 OTP 도입 시 이 엔드포인트의 검증만 교체한다.
 */
public record PaymentExecuteRequest(
        String otp
) {
}