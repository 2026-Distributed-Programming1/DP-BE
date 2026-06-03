package org.dpbe.domain.consultation.controller;

import org.dpbe.domain.consultation.dto.ProposalCreateRequest;
import org.dpbe.domain.consultation.dto.ProposalResponse;
import org.dpbe.domain.consultation.service.ProposalService;
import org.dpbe.global.dto.PageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/proposals")
public class ProposalController {

    private final ProposalService proposalService;

    public ProposalController(ProposalService proposalService) {
        this.proposalService = proposalService;
    }

    @GetMapping
    public PageResponse<ProposalResponse> findAll(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return proposalService.findAll(page, size);
    }

    @PostMapping
    public ResponseEntity<ProposalResponse> create(@RequestBody ProposalCreateRequest request) {
        return ResponseEntity.status(201).body(proposalService.create(request));
    }
}
