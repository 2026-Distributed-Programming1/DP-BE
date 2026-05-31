package org.dpbe.domain.claim.dto;

import java.time.LocalDateTime;
import java.util.List;
import org.dpbe.domain.claim.entity.DispatchRecord;

/** 현장 출동 기록 조회/등록 결과 (업로드된 사진 파일명 포함) */
public record DispatchRecordResponse(
        String recordNo,
        String dispatchNo,
        boolean policeRequired,
        boolean towingRequired,
        String notes,
        List<String> photoNames,
        LocalDateTime transmittedAt,
        String status
) {
    public static DispatchRecordResponse from(DispatchRecord r, List<String> photoNames) {
        return new DispatchRecordResponse(
                r.getRecordId(),
                r.getDispatch() != null ? r.getDispatch().getDispatchNo() : null,
                r.isPoliceRequired(),
                r.isTowingRequired(),
                r.getNotes(),
                photoNames,
                r.getTransmittedAt(),
                r.getStatus() != null ? r.getStatus().name() : null);
    }
}