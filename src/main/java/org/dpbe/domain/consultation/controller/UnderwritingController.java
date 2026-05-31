package org.dpbe.domain.consultation.controller;

import java.util.List;
import org.dpbe.domain.consultation.dto.PendingApplicationResponse;
import org.dpbe.domain.consultation.dto.UnderwritingRequest;
import org.dpbe.domain.consultation.dto.UnderwritingResponse;
import org.dpbe.domain.consultation.service.UnderwritingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/underwriting")
public class UnderwritingController {

    private final UnderwritingService underwritingService;

    public UnderwritingController(UnderwritingService underwritingService) {
        this.underwritingService = underwritingService;
    }

    @GetMapping("/pending")
    public List<PendingApplicationResponse> findPending() {
        return underwritingService.findPending();
    }

    @PostMapping
    public ResponseEntity<UnderwritingResponse> complete(@RequestBody UnderwritingRequest request) {
        return ResponseEntity.status(201).body(underwritingService.complete(request));
    }
}