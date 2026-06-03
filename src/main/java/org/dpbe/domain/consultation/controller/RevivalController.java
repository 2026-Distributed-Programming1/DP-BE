package org.dpbe.domain.consultation.controller;

import org.dpbe.domain.consultation.dto.RevivalRequest;
import org.dpbe.domain.consultation.dto.RevivalResponse;
import org.dpbe.domain.consultation.service.RevivalService;
import org.dpbe.global.dto.PageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/revivals")
public class RevivalController {

    private final RevivalService revivalService;

    public RevivalController(RevivalService revivalService) {
        this.revivalService = revivalService;
    }

    @GetMapping
    public PageResponse<RevivalResponse> findAll(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return revivalService.findAll(page, size);
    }

    @GetMapping("/{revivalNo}")
    public RevivalResponse findByNo(@PathVariable String revivalNo) {
        return revivalService.findByNo(revivalNo);
    }

    @PostMapping
    public ResponseEntity<RevivalResponse> create(@RequestBody RevivalRequest request) {
        return ResponseEntity.status(201).body(revivalService.create(request));
    }
}