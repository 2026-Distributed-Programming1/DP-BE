package org.dpbe.domain.claim.dto;

import java.util.List;

/**
 * 보험금 청구 등록 요청 (클라이언트 주도 — 완성된 상태를 한 번에 전송).
 * claimType: DISEASE | ACCIDENT, authMethod: MOBILE | SIMPLE | CERTIFICATE.
 * personalInfoAgreed·authMethod는 submit() 검증용 — authMethod는 저장되지 않는다(휘발).
 * 계좌는 신규 입력 정보를 평면으로 전달한다(인증 후 저장).
 */
public record ClaimCreateRequest(
        String customerId,
        String contractNo,
        String claimType,
        List<String> claimReasons,
        String diagnosis,
        String bankName,
        String accountNo,
        String accountHolder,
        boolean personalInfoAgreed,
        String authMethod
) {
}