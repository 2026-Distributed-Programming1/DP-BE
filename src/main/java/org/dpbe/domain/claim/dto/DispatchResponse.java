package org.dpbe.domain.claim.dto;

import org.dpbe.domain.claim.entity.Dispatch;

/** 현장 출동 목록 항목 (출동요원이 기록할 대상 선택용) */
public record DispatchResponse(
        String dispatchNo,
        String accidentNo,
        String status
) {
    public static DispatchResponse from(Dispatch d) {
        return new DispatchResponse(
                d.getDispatchNo(),
                d.getAccident() != null ? d.getAccident().getReportNo() : null,
                d.getStatus() != null ? d.getStatus().name() : null);
    }
}