package org.dpbe.domain.claim.service;

import org.dpbe.domain.actor.ClaimsHandler;
import org.dpbe.domain.claim.dto.InvestigationCreateRequest;
import org.dpbe.domain.claim.dto.InvestigationResponse;
import org.dpbe.domain.claim.entity.ClaimRequest;
import org.dpbe.domain.claim.entity.DamageInvestigation;
import org.dpbe.domain.claim.repository.ClaimRequestRepository;
import org.dpbe.domain.claim.repository.DamageInvestigationRepository;
import org.dpbe.domain.common.enums.InvestigationResult;
import org.dpbe.global.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC '손해 조사를 한다' API 서비스.
 * 손해 조사 규칙·검증(E1 과실비율 합 100%, E2 필수입력)을 처리한다.
 * 한 청구 건당 조사 1건. 결과 APPROVED면 complete(), REJECTED면 closeAsRejected().
 * (산출 단계는 별도 POST이므로 complete()가 반환하는 calc 객체는 여기서 저장하지 않는다.)
 */
@Service
@Transactional(readOnly = true)
public class DamageInvestigationService {

    private final DamageInvestigationRepository investigationRepository;
    private final ClaimRequestRepository claimRequestRepository;

    public DamageInvestigationService(DamageInvestigationRepository investigationRepository,
                                      ClaimRequestRepository claimRequestRepository) {
        this.investigationRepository = investigationRepository;
        this.claimRequestRepository = claimRequestRepository;
    }

    public InvestigationResponse findByClaimNo(String claimNo) {
        parseId(claimNo);
        DamageInvestigation inv = investigationRepository.findByClaimNo(claimNo);
        if (inv == null) {
            throw ApiException.notFound("해당 청구의 조사 건이 없습니다: " + claimNo);
        }
        return InvestigationResponse.from(inv);
    }

    private Long parseId(String no) {
        if (no == null || no.isBlank()) {
            throw ApiException.badRequest("유효하지 않은 번호: " + no);
        }
        try {
            return Long.parseLong(no.replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            throw ApiException.badRequest("유효하지 않은 번호: " + no);
        }
    }

    /** 조사 등록 — 청구 건에 대해 조사 결과를 제출하고 저장(@Transactional). */
    @Transactional
    public InvestigationResponse create(String claimNo, InvestigationCreateRequest request) {
        ClaimRequest claim = claimRequestRepository.findById(parseId(claimNo));
        if (claim == null) {
            throw ApiException.notFound("청구를 찾을 수 없습니다: " + claimNo);
        }
        if (investigationRepository.findByClaimNo(claimNo) != null) {
            throw ApiException.badRequest("이미 조사가 등록된 청구입니다: " + claimNo);
        }

        DamageInvestigation inv = new DamageInvestigation(claim);
        inv.assignHandler(new ClaimsHandler(
                request.handlerEmpId() != null ? request.handlerEmpId() : "?",
                request.handlerName() != null ? request.handlerName() : "",
                "", "", 0L));

        inv.enterRecognizedDamage(request.recognizedDamage());
        inv.enterFaultRatio(request.ourFaultRatio(), request.counterFaultRatio());
        if (!inv.validateFaultRatio()) {
            throw ApiException.badRequest("[E1] 과실 비율의 합이 100%가 아닙니다.");
        }
        if (request.opinion() != null) {
            inv.enterOpinion(request.opinion());
        }

        InvestigationResult result = parseResult(request.result());
        inv.selectResult(result);
        if (result == InvestigationResult.REJECTED) {
            if (request.rejectReason() == null || request.rejectReason().isBlank()) {
                throw ApiException.badRequest("면책 처리에는 면책 사유가 필요합니다.");
            }
            inv.enterRejectReason(request.rejectReason());
        }

        if (!inv.validateRequired()) {
            throw ApiException.badRequest("[E2] 필수 입력값이 누락되어 조사를 완료할 수 없습니다.");
        }

        if (result == InvestigationResult.APPROVED) {
            inv.complete();   // 상태 INVESTIGATED, investigatedAt 기록 (반환 calc는 산출 단계에서 별도 생성)
        } else {
            inv.closeAsRejected();   // 상태 CLOSED
        }

        investigationRepository.save(inv);
        return InvestigationResponse.from(inv);
    }

    private InvestigationResult parseResult(String result) {
        if (result == null) {
            throw ApiException.badRequest("처리 결과를 선택해야 합니다.");
        }
        try {
            return InvestigationResult.valueOf(result);
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("알 수 없는 처리 결과입니다: " + result);
        }
    }
}
