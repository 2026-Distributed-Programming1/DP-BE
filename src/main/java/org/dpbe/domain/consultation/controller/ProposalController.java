package org.dpbe.domain.consultation.controller;

import java.util.List;
import org.dpbe.domain.consultation.dto.ProposalCreateRequest;
import org.dpbe.domain.consultation.dto.ProposalResponse;
import org.dpbe.domain.consultation.service.ProposalService;
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
    public List<ProposalResponse> findAll() {
        return proposalService.findAll();
    }

    @PostMapping
    public ResponseEntity<ProposalResponse> create(@RequestBody ProposalCreateRequest request) {
        return ResponseEntity.status(201).body(proposalService.create(request));
    }
}