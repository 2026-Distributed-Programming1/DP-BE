package org.dpbe.domain.claim.service;

import org.dpbe.domain.claim.dto.CalculationResponse;
import org.dpbe.domain.claim.entity.ClaimCalculation;
import org.dpbe.domain.claim.entity.DamageInvestigation;
import org.dpbe.domain.claim.repository.ClaimCalculationRepository;
import org.dpbe.domain.claim.repository.DamageInvestigationRepository;
import org.dpbe.domain.common.enums.CalculationStatus;
import org.dpbe.domain.common.enums.InvestigationResult;
import org.dpbe.global.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC '보험금을 산출한다' API 서비스.
 * 콘솔 ClaimCalculationRunner의 규칙만 이관한다. 산출은 조사 결과로 자동 계산되며
 * (엔터티 생성자 calculate()), E1(자기부담금 초과)이면 종결 처리한다.
 */
@Service
@Transactional(readOnly = true)
public class ClaimCalculationService {

    private final ClaimCalculationRepository calculationRepository;
    private final DamageInvestigationRepository investigationRepository;

    public ClaimCalculationService(ClaimCalculationRepository calculationRepository,
                                   DamageInvestigationRepository investigationRepository) {
        this.calculationRepository = calculationRepository;
        this.investigationRepository = investigationRepository;
    }

    private Long parseId(String no) {
        try {
            return Long.parseLong(no.replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            throw ApiException.notFound("유효하지 않은 번호: " + no);
        }
    }

    /** 산출 승인 — CALCULATED → APPROVED 전이(지급 가능 상태). 지급건은 별도 생성. */
    @Transactional
    public CalculationResponse approve(String calculationNo) {
        ClaimCalculation calc = calculationRepository.findById(parseId(calculationNo));
        if (calc == null) {
            throw ApiException.notFound("산출을 찾을 수 없습니다: " + calculationNo);
        }
        if (calc.getStatus() != CalculationStatus.CALCULATED) {
            throw ApiException.badRequest("산출완료(CALCULATED) 상태만 승인할 수 있습니다: " + calculationNo);
        }
        calc.approve();   // 상태 APPROVED (반환 ClaimPayment는 지급 단계에서 별도 생성하므로 폐기)
        calculationRepository.updateStatus(calc);
        return CalculationResponse.from(calc);
    }

    public CalculationResponse findByInvestigationNo(String investigationNo) {
        ClaimCalculation c = calculationRepository.findByInvestigationNo(investigationNo);
        if (c == null) {
            throw ApiException.notFound("해당 조사의 산출 건이 없습니다: " + investigationNo);
        }
        return CalculationResponse.from(c);
    }

    /** 산출 등록 — 조사 결과로 자동 산출 후 저장(@Transactional). */
    @Transactional
    public CalculationResponse create(String investigationNo) {
        DamageInvestigation inv = investigationRepository.findById(parseId(investigationNo));
        if (inv == null) {
            throw ApiException.notFound("조사를 찾을 수 없습니다: " + investigationNo);
        }
        if (inv.getResult() != InvestigationResult.APPROVED) {
            throw ApiException.badRequest("지급 승인된 조사만 산출할 수 있습니다: " + investigationNo);
        }
        if (calculationRepository.findByInvestigationNo(investigationNo) != null) {
            throw ApiException.badRequest("이미 산출된 조사입니다: " + investigationNo);
        }

        // 생성자가 손해액·과실비율·약관 기본값을 읽어 calculate() 자동 실행
        ClaimCalculation calc = new ClaimCalculation(inv);

        // E1: 자기부담금 초과 시 지급할 금액 없음 → 종결 처리
        if (calc.isExceededDeductible()) {
            calc.closeAsExceeded();
        }

        calculationRepository.save(calc);
        return CalculationResponse.from(calc);
    }
}