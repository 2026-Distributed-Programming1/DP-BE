package org.dpbe.domain.sales.controller;

import org.dpbe.domain.sales.dto.ChannelScreeningRequest;
import org.dpbe.domain.sales.dto.ChannelScreeningResponse;
import org.dpbe.domain.sales.dto.ScreeningRejectRequest;
import org.dpbe.domain.sales.service.ChannelScreeningService;
import org.dpbe.global.dto.PageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/channel-screenings")
public class ChannelScreeningController {

    private final ChannelScreeningService service;

    public ChannelScreeningController(ChannelScreeningService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<ChannelScreeningResponse> findAll(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.findAll(page, size);
    }

    @PostMapping
    public ResponseEntity<ChannelScreeningResponse> register(
            @RequestBody ChannelScreeningRequest request) {
        return ResponseEntity.status(201).body(service.register(request));
    }

    @PostMapping("/{screeningNo}/approve")
    public ChannelScreeningResponse approve(@PathVariable String screeningNo) {
        return service.approve(screeningNo);
    }

    @PostMapping("/{screeningNo}/reject")
    public ChannelScreeningResponse reject(
            @PathVariable String screeningNo,
            @RequestBody ScreeningRejectRequest request) {
        return service.reject(screeningNo, request);
    }
}
