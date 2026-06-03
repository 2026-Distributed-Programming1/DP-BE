package org.dpbe.domain.consultation.controller;

import org.dpbe.domain.consultation.dto.PolicyApplicationRequest;
import org.dpbe.domain.consultation.dto.PolicyApplicationResponse;
import org.dpbe.domain.consultation.service.PolicyApplicationService;
import org.dpbe.global.dto.PageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/policy-applications")
public class PolicyApplicationController {

    private final PolicyApplicationService policyService;

    public PolicyApplicationController(PolicyApplicationService policyService) {
        this.policyService = policyService;
    }

    @GetMapping
    public PageResponse<PolicyApplicationResponse> findAll(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return policyService.findAll(page, size);
    }

    @GetMapping("/{applicationNo}")
    public PolicyApplicationResponse findByNo(@PathVariable String applicationNo) {
        return policyService.findByNo(applicationNo);
    }

    @PostMapping
    public ResponseEntity<PolicyApplicationResponse> create(
            @RequestBody PolicyApplicationRequest request) {
        return ResponseEntity.status(201).body(policyService.create(request));
    }
}