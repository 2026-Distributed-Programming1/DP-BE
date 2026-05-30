package org.dpbe.domain.claim.dto;

/**
 * 손해 조사 등록 요청 (보상담당자가 한 청구 건에 대해 조사 결과를 제출).
 * result: APPROVED(지급승인) | REJECTED(면책). REJECTED면 rejectReason 사용.
 * 과실비율 합은 100%여야 한다(E1). handlerEmpId는 선택.
 */
public record InvestigationCreateRequest(
        String handlerEmpId,
        String handlerName,
        long recognizedDamage,
        double ourFaultRatio,
        double counterFaultRatio,
        String opinion,
        String result,
        String rejectReason
) {
}