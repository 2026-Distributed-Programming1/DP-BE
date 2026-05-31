package org.dpbe.domain.claim.dto;

/**
 * 사고 접수 요청 (고객이 사고 정보를 한 번에 제출).
 * accidentType: OBJECT(사물) | PERSON(사람). agreedTerms(위치기반 약관 동의) 필수.
 * needsDispatch=true면 접수 후 현장출동(dispatch)이 함께 생성된다.
 * casualty* 는 인명사고(A1) 시 추가 정보(선택).
 */
public record AccidentCreateRequest(
        String customerId,
        String vehicleNo,
        String ownerName,
        String phoneNo,
        String accidentType,
        String damageType,
        String location,
        boolean needsDispatch,
        boolean agreedTerms,
        int casualtyCount,
        String injurySeverity,
        boolean emergencyReported
) {
}