package org.dpbe.domain.sales.controller;

import java.time.LocalDate;
import org.dpbe.domain.sales.dto.SalesOrgEvaluationListResponse;
import org.dpbe.domain.sales.dto.SalesOrgEvaluationRequest;
import org.dpbe.domain.sales.dto.SalesOrgEvaluationResponse;
import org.dpbe.domain.sales.service.SalesOrgEvaluationService;
import org.springframework.format.annotation.DateTimeFormat;
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
    public SalesOrgEvaluationListResponse findAll(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String channelType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.findAll(startDate, endDate, channelType, page, size);
    }

    @PostMapping
    public ResponseEntity<SalesOrgEvaluationResponse> create(
            @RequestBody SalesOrgEvaluationRequest request) {
        return ResponseEntity.status(201).body(service.create(request));
    }
}