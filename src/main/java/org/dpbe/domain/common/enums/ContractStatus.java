package org.dpbe.domain.common.enums;
/**
 * 계약 상태 (ContractStatus)
 * [정상, 만기]
 */
public enum ContractStatus {
    NORMAL,    // 정상
    EXPIRED,   // 만기
    CANCELLED, // 해지
    LAPSED     // 실효
}