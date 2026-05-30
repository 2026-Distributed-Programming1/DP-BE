package org.dpbe.domain.claim.controller;

import org.dpbe.domain.claim.dto.CalculationResponse;
import org.dpbe.domain.claim.service.ClaimCalculationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** UC '보험금을 산출한다' REST 엔드포인트 (조사 기준 무상태) */
@RestController
@RequestMapping("/api/investigations/{investigationNo}/calculation")
public class CalculationController {

    private final ClaimCalculationService calculationService;

    public CalculationController(ClaimCalculationService calculationService) {
        this.calculationService = calculationService;
    }

    /** 산출 등록 (조사 결과로 자동 계산) */
    @PostMapping
    public CalculationResponse create(@PathVariable String investigationNo) {
        return calculationService.create(investigationNo);
    }

    /** 산출 조회 */
    @GetMapping
    public CalculationResponse detail(@PathVariable String investigationNo) {
        return calculationService.findByInvestigationNo(investigationNo);
    }
}