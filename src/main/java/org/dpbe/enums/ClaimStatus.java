package org.dpbe.enums;
/**
 * 처리 상태 (ClaimStatus)
 * [지급 완료, 심사중, 반려]
 */
public enum ClaimStatus {
    PAID,       // 지급 완료
    UNDER_REVIEW, // 심사중
    REJECTED    // 반려
}