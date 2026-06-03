package org.dpbe.domain.consultation.controller;

import org.dpbe.domain.consultation.dto.InterviewRecordCreateRequest;
import org.dpbe.domain.consultation.dto.InterviewRecordResponse;
import org.dpbe.domain.consultation.dto.InterviewRecordUpdateRequest;
import org.dpbe.domain.consultation.service.InterviewRecordService;
import org.dpbe.global.dto.PageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/interview-records")
public class InterviewRecordController {

    private final InterviewRecordService recordService;

    public InterviewRecordController(InterviewRecordService recordService) {
        this.recordService = recordService;
    }

    @GetMapping
    public PageResponse<InterviewRecordResponse> findAll(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return recordService.findAll(page, size);
    }

    @GetMapping("/{recordNo}")
    public InterviewRecordResponse findByRecordNo(@PathVariable String recordNo) {
        return recordService.findByRecordNo(recordNo);
    }

    @PostMapping
    public ResponseEntity<InterviewRecordResponse> create(
            @RequestBody InterviewRecordCreateRequest request) {
        return ResponseEntity.status(201).body(recordService.create(request));
    }

    @PutMapping("/{recordNo}")
    public InterviewRecordResponse update(
            @PathVariable String recordNo,
            @RequestBody InterviewRecordUpdateRequest request) {
        return recordService.update(recordNo, request);
    }
}
