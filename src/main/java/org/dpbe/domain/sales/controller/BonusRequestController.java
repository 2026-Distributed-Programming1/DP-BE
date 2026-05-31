package org.dpbe.domain.sales.controller;

import org.dpbe.domain.sales.dto.BonusRequestRequest;
import org.dpbe.domain.sales.dto.BonusRequestResponse;
import org.dpbe.domain.sales.service.BonusRequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bonus-requests")
public class BonusRequestController {

    private final BonusRequestService service;

    public BonusRequestController(BonusRequestService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<BonusRequestResponse> create(@RequestBody BonusRequestRequest request) {
        return ResponseEntity.status(201).body(service.create(request));
    }
}