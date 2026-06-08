package org.dpbe.domain.contract.controller;

import org.dpbe.domain.contract.dto.ContractDetailResponse;
import org.dpbe.domain.contract.dto.ContractSummaryResponse;
import org.dpbe.domain.contract.service.SubscribedInsuranceService;
import org.dpbe.global.dto.PageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** UC '가입 보험을 조회한다' REST 엔드포인트 */
@RestController
@RequestMapping("/api/subscribed-insurances")
public class SubscribedInsuranceController {

    private final SubscribedInsuranceService subscribedInsuranceService;

    public SubscribedInsuranceController(SubscribedInsuranceService subscribedInsuranceService) {
        this.subscribedInsuranceService = subscribedInsuranceService;
    }

    /** 고객 본인 가입 보험 목록 */
    @GetMapping
    public PageResponse<ContractSummaryResponse> list(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return subscribedInsuranceService.list(type, page, size);
    }

    /** 고객 본인 가입 보험 상세 */
    @GetMapping("/{contractNo}")
    public ContractDetailResponse detail(@PathVariable String contractNo) {
        return subscribedInsuranceService.detail(contractNo);
    }
}
