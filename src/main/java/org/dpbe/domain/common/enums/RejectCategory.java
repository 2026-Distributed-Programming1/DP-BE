package org.dpbe.domain.common.enums;

public enum RejectCategory {
    PAYMENT_ERROR,        // 오류결제
    DUPLICATE_PAYMENT,    // 이중납부
    CONTRACT_MISMATCH,    // 계약불일치
    OTHER                 // 기타
}
