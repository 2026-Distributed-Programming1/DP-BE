package org.dpbe.domain.common.enums;

/**
 * 고객 응답 (CustomerResponse)
 * 만기 계약 안내 후 고객의 응답 유형이다.
 */
public enum CustomerResponse {
    RENEWAL,    // 갱신
    TERMINATION, // 해지
    PENDING     // 추후 결정
}
