package org.dpbe.domain.consultation.controller;

import org.dpbe.domain.consultation.dto.InsuranceApplicationRequest;
import org.dpbe.domain.consultation.dto.InsuranceApplicationResponse;
import org.dpbe.domain.consultation.service.InsuranceApplicationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/insurance-applications")
public class InsuranceApplicationController {

    private final InsuranceApplicationService appService;

    public InsuranceApplicationController(InsuranceApplicationService appService) {
        this.appService = appService;
    }

    @PostMapping
    public ResponseEntity<InsuranceApplicationResponse> create(
            @RequestBody InsuranceApplicationRequest request) {
        return ResponseEntity.status(201).body(appService.create(request));
    }
}