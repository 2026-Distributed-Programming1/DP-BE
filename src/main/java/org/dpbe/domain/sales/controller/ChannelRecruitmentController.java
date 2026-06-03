package org.dpbe.domain.sales.controller;

import org.dpbe.domain.sales.dto.ChannelRecruitmentRequest;
import org.dpbe.domain.sales.dto.ChannelRecruitmentResponse;
import org.dpbe.domain.sales.service.ChannelRecruitmentService;
import org.dpbe.global.dto.PageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/channel-recruitments")
public class ChannelRecruitmentController {

    private final ChannelRecruitmentService service;

    public ChannelRecruitmentController(ChannelRecruitmentService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<ChannelRecruitmentResponse> findAll(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.findAll(page, size);
    }

    @PostMapping
    public ResponseEntity<ChannelRecruitmentResponse> create(
            @RequestBody ChannelRecruitmentRequest request) {
        return ResponseEntity.status(201).body(service.create(request));
    }
}
