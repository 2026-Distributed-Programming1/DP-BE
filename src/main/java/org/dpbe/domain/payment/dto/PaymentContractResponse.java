package org.dpbe.domain.payment.dto;

/** 납입 가능한 고객 계약 옵션 (UC '보험료를 납입한다' 계약 선택 화면) */
public record PaymentContractResponse(
        String contractNo,
        String insuranceType,
        long monthlyPremium
) {
}