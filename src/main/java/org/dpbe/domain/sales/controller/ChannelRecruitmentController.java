package org.dpbe.domain.sales.controller;

import java.util.List;
import org.dpbe.domain.sales.dto.ChannelRecruitmentRequest;
import org.dpbe.domain.sales.dto.ChannelRecruitmentResponse;
import org.dpbe.domain.sales.service.ChannelRecruitmentService;
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
    public List<ChannelRecruitmentResponse> findAll() {
        return service.findAll();
    }

    @PostMapping
    public ResponseEntity<ChannelRecruitmentResponse> create(
            @RequestBody ChannelRecruitmentRequest request) {
        return ResponseEntity.status(201).body(service.create(request));
    }
}