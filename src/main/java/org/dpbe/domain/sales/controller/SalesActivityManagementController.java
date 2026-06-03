package org.dpbe.domain.sales.controller;

import java.time.LocalDate;
import org.dpbe.domain.sales.dto.SalesActivityManagementRequest;
import org.dpbe.domain.sales.dto.SalesActivityManagementResponse;
import org.dpbe.domain.sales.service.SalesActivityManagementService;
import org.dpbe.global.dto.PageResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sales-activity-managements")
public class SalesActivityManagementController {

    private final SalesActivityManagementService service;

    public SalesActivityManagementController(SalesActivityManagementService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<SalesActivityManagementResponse> findAll(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String channelType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.findAll(startDate, endDate, channelType, page, size);
    }

    @PostMapping
    public ResponseEntity<SalesActivityManagementResponse> create(
            @RequestBody SalesActivityManagementRequest request) {
        return ResponseEntity.status(201).body(service.create(request));
    }
}