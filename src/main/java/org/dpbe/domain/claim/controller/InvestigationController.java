package org.dpbe.domain.claim.controller;

import org.dpbe.domain.claim.dto.InvestigationCreateRequest;
import org.dpbe.domain.claim.dto.InvestigationResponse;
import org.dpbe.domain.claim.service.DamageInvestigationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** UC '손해 조사를 한다' REST 엔드포인트 (청구 건 기준 무상태) */
@RestController
@RequestMapping("/api/claims/{claimNo}/investigation")
public class InvestigationController {

    private final DamageInvestigationService investigationService;

    public InvestigationController(DamageInvestigationService investigationService) {
        this.investigationService = investigationService;
    }

    /** 조사 등록 */
    @PostMapping
    public InvestigationResponse create(@PathVariable String claimNo,
                                        @RequestBody InvestigationCreateRequest request) {
        return investigationService.create(claimNo, request);
    }

    /** 조사 조회 */
    @GetMapping
    public InvestigationResponse detail(@PathVariable String claimNo) {
        return investigationService.findByClaimNo(claimNo);
    }
}