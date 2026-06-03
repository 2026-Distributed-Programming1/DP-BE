package org.dpbe.domain.contract.controller;

import org.dpbe.domain.contract.dto.ExpiringContractSummaryResponse;
import org.dpbe.domain.contract.dto.NoticeCreateRequest;
import org.dpbe.domain.contract.dto.NoticeResponse;
import org.dpbe.domain.contract.dto.NoticeResponseRequest;
import org.dpbe.domain.contract.service.ExpiringContractManagementService;
import org.dpbe.global.dto.PageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExpiringContractManagementController {

    private final ExpiringContractManagementService expiringService;

    public ExpiringContractManagementController(ExpiringContractManagementService expiringService) {
        this.expiringService = expiringService;
    }

    /** 만기 임박(D-30) 계약 목록 */
    @GetMapping("/api/expiring-contracts")
    public PageResponse<ExpiringContractSummaryResponse> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return expiringService.getExpiringContracts(page, size);
    }

    /** 안내 기록 저장 */
    @PostMapping("/api/expiring-contracts/{contractNo}/notice")
    public NoticeResponse createNotice(@PathVariable String contractNo,
                                       @RequestBody NoticeCreateRequest req) {
        return expiringService.createNotice(contractNo, req);
    }

    /** 안내 기록 목록 (contractNo 필터 선택) */
    @GetMapping("/api/expiring-notices")
    public PageResponse<NoticeResponse> notices(
            @RequestParam(required = false) String contractNo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return expiringService.getNotices(contractNo, page, size);
    }

    /** 고객 응답 기록 */
    @PostMapping("/api/expiring-notices/{noticeNo}/response")
    public NoticeResponse recordResponse(@PathVariable String noticeNo,
                                         @RequestBody NoticeResponseRequest req) {
        return expiringService.recordResponse(noticeNo, req);
    }
}
