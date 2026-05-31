package org.dpbe.domain.claim.dto;

import java.time.LocalDateTime;
import org.dpbe.domain.claim.entity.AccidentReport;

/**
 * 사고 접수 조회/등록 결과.
 * 접수 시 현장출동이 생성되면 dispatchNo가 채워진다(없으면 null).
 */
public record AccidentResponse(
        String accidentNo,
        String customerId,
        String customerName,
        String vehicleNo,
        String ownerName,
        String phoneNo,
        String accidentType,
        String damageType,
        String location,
        boolean needsDispatch,
        int casualtyCount,
        String injurySeverity,
        boolean emergencyReported,
        LocalDateTime reportedAt,
        String status,
        String dispatchNo
) {
    public static AccidentResponse from(AccidentReport r, String dispatchNo) {
        return new AccidentResponse(
                r.getReportNo(),
                r.getCustomer() != null ? r.getCustomer().getCustomerId() : null,
                r.getCustomer() != null ? r.getCustomer().getName() : null,
                r.getVehicleNo(),
                r.getOwnerName(),
                r.getPhoneNo(),
                r.getAccidentType() != null ? r.getAccidentType().name() : null,
                r.getDamageType(),
                r.getLocation(),
                r.isNeedsDispatch(),
                r.getCasualtyCount(),
                r.getInjurySeverity(),
                r.isEmergencyReported(),
                r.getReportedAt(),
                r.getStatus() != null ? r.getStatus().name() : null,
                dispatchNo);
    }

    public static AccidentResponse from(AccidentReport r) {
        return from(r, null);
    }
}