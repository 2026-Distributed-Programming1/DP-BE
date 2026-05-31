package org.dpbe.domain.sales.controller;

import java.util.List;
import org.dpbe.domain.sales.dto.SalesActivityManagementRequest;
import org.dpbe.domain.sales.dto.SalesActivityManagementResponse;
import org.dpbe.domain.sales.service.SalesActivityManagementService;
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
    public List<SalesActivityManagementResponse> findAll() {
        return service.findAll();
    }

    @PostMapping
    public ResponseEntity<SalesActivityManagementResponse> create(
            @RequestBody SalesActivityManagementRequest request) {
        return ResponseEntity.status(201).body(service.create(request));
    }
}