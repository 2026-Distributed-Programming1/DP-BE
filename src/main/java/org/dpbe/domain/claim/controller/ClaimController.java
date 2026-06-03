package org.dpbe.domain.claim.controller;

import org.dpbe.domain.claim.dto.ClaimCreateRequest;
import org.dpbe.domain.claim.dto.ClaimResponse;
import org.dpbe.domain.claim.service.ClaimRequestService;
import org.dpbe.global.dto.PageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** UC '보험금을 요청한다' REST 엔드포인트 (클라이언트 주도·무상태) */
@RestController
@RequestMapping("/api/claims")
public class ClaimController {

    private final ClaimRequestService claimRequestService;

    public ClaimController(ClaimRequestService claimRequestService) {
        this.claimRequestService = claimRequestService;
    }

    /** 청구 등록 */
    @PostMapping
    public ClaimResponse create(@RequestBody ClaimCreateRequest request) {
        return claimRequestService.create(request);
    }

    /** 청구 목록 */
    @GetMapping
    public PageResponse<ClaimResponse> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return claimRequestService.findAll(page, size);
    }

    /** 청구 상세 */
    @GetMapping("/{claimNo}")
    public ClaimResponse detail(@PathVariable String claimNo) {
        return claimRequestService.findByClaimNo(claimNo);
    }
}
