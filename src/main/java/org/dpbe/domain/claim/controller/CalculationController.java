package org.dpbe.domain.claim.controller;

import org.dpbe.domain.claim.dto.CalculationResponse;
import org.dpbe.domain.claim.service.ClaimCalculationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * UC '보험금을 산출한다' REST 엔드포인트 (무상태).
 * 산출은 조사 기준으로 생성·조회하고, 승인은 산출번호 기준으로 전이한다.
 */
@RestController
@RequestMapping("/api")
public class CalculationController {

    private final ClaimCalculationService calculationService;

    public CalculationController(ClaimCalculationService calculationService) {
        this.calculationService = calculationService;
    }

    /** 산출 등록 (조사 결과로 자동 계산) */
    @PostMapping("/investigations/{investigationNo}/calculation")
    public CalculationResponse create(@PathVariable String investigationNo) {
        return calculationService.create(investigationNo);
    }

    /** 산출 조회 */
    @GetMapping("/investigations/{investigationNo}/calculation")
    public CalculationResponse detail(@PathVariable String investigationNo) {
        return calculationService.findByInvestigationNo(investigationNo);
    }

    /** 산출 승인 (CALCULATED → APPROVED, 지급 가능 상태로 전이) */
    @PostMapping("/calculations/{calculationNo}/approve")
    public CalculationResponse approve(@PathVariable String calculationNo) {
        return calculationService.approve(calculationNo);
    }
}