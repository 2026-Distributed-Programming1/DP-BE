package org.dpbe.domain.sales.controller;

import java.util.List;
import org.dpbe.domain.sales.dto.SalesOrgEvaluationRequest;
import org.dpbe.domain.sales.dto.SalesOrgEvaluationResponse;
import org.dpbe.domain.sales.service.SalesOrgEvaluationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sales-org-evaluations")
public class SalesOrgEvaluationController {

    private final SalesOrgEvaluationService service;

    public SalesOrgEvaluationController(SalesOrgEvaluationService service) {
        this.service = service;
    }

    @GetMapping
    public List<SalesOrgEvaluationResponse> findAll() {
        return service.findAll();
    }

    @PostMapping
    public ResponseEntity<SalesOrgEvaluationResponse> create(
            @RequestBody SalesOrgEvaluationRequest request) {
        return ResponseEntity.status(201).body(service.create(request));
    }
}