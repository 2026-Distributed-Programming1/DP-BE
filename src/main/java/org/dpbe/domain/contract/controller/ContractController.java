package org.dpbe.domain.contract.controller;

import org.dpbe.domain.contract.dto.ContractDetailResponse;
import org.dpbe.domain.contract.dto.ContractSummaryResponse;
import org.dpbe.domain.contract.service.ContractService;
import org.dpbe.global.dto.PageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** UC '계약 정보를 조회한다' REST 엔드포인트 */
@RestController
@RequestMapping("/api/contracts")
public class ContractController {

    private final ContractService contractService;

    public ContractController(ContractService contractService) {
        this.contractService = contractService;
    }

    /** 필터·페이징 계약 목록 (type 비우면 전체 조회) */
    @GetMapping
    public PageResponse<ContractSummaryResponse> list(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return contractService.list(type, page, size);
    }

    /** 계약 상세 (만기 D-day, 특약 포함) */
    @GetMapping("/{contractNo}")
    public ContractDetailResponse detail(@PathVariable String contractNo) {
        return contractService.detail(contractNo);
    }
}
