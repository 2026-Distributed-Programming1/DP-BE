package org.dpbe.domain.consultation.controller;

import org.dpbe.domain.consultation.dto.PendingApplicationResponse;
import org.dpbe.domain.consultation.dto.UnderwritingRequest;
import org.dpbe.domain.consultation.dto.UnderwritingResponse;
import org.dpbe.domain.consultation.service.UnderwritingService;
import org.dpbe.global.dto.PageResponse;
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
    public PageResponse<PendingApplicationResponse> findPending(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return underwritingService.findPending(page, size);
    }

    @PostMapping
    public ResponseEntity<UnderwritingResponse> complete(@RequestBody UnderwritingRequest request) {
        return ResponseEntity.status(201).body(underwritingService.complete(request));
    }
}
