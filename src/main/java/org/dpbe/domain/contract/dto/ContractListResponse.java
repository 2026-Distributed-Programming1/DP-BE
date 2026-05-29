package org.dpbe.domain.contract.dto;

import java.util.List;

/** 페이지 단위 계약 목록 응답 */
public record ContractListResponse(
        int page,
        int size,
        int total,
        List<ContractSummaryResponse> items
) {
}