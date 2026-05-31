package org.dpbe.domain.consultation.controller;

import org.dpbe.domain.consultation.dto.RevivalRequest;
import org.dpbe.domain.consultation.dto.RevivalResponse;
import org.dpbe.domain.consultation.service.RevivalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/revivals")
public class RevivalController {

    private final RevivalService revivalService;

    public RevivalController(RevivalService revivalService) {
        this.revivalService = revivalService;
    }

    @PostMapping
    public ResponseEntity<RevivalResponse> create(@RequestBody RevivalRequest request) {
        return ResponseEntity.status(201).body(revivalService.create(request));
    }
}