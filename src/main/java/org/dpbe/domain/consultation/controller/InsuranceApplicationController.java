package org.dpbe.domain.consultation.controller;

import org.dpbe.domain.consultation.dto.InsuranceApplicationRequest;
import org.dpbe.domain.consultation.dto.InsuranceApplicationResponse;
import org.dpbe.domain.consultation.service.InsuranceApplicationService;
import org.dpbe.global.dto.PageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/insurance-applications")
public class InsuranceApplicationController {

    private final InsuranceApplicationService appService;

    public InsuranceApplicationController(InsuranceApplicationService appService) {
        this.appService = appService;
    }

    @GetMapping
    public PageResponse<InsuranceApplicationResponse> findAll(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return appService.findAll(page, size);
    }

    @GetMapping("/{applicationNo}")
    public InsuranceApplicationResponse findByNo(@PathVariable String applicationNo) {
        return appService.findByNo(applicationNo);
    }

    @PostMapping
    public ResponseEntity<InsuranceApplicationResponse> create(
            @RequestBody InsuranceApplicationRequest request) {
        return ResponseEntity.status(201).body(appService.create(request));
    }
}