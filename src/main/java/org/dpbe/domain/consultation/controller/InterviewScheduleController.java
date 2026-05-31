package org.dpbe.domain.consultation.controller;

import java.util.List;
import org.dpbe.domain.consultation.dto.InterviewScheduleCreateRequest;
import org.dpbe.domain.consultation.dto.InterviewScheduleResponse;
import org.dpbe.domain.consultation.dto.InterviewScheduleUpdateRequest;
import org.dpbe.domain.consultation.service.InterviewScheduleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/interview-schedules")
public class InterviewScheduleController {

    private final InterviewScheduleService scheduleService;

    public InterviewScheduleController(InterviewScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping
    public List<InterviewScheduleResponse> findAll() {
        return scheduleService.findAll();
    }

    @GetMapping("/{scheduleNo}")
    public InterviewScheduleResponse findByScheduleNo(@PathVariable String scheduleNo) {
        return scheduleService.findByScheduleNo(scheduleNo);
    }

    @PostMapping
    public ResponseEntity<InterviewScheduleResponse> create(
            @RequestBody InterviewScheduleCreateRequest request) {
        return ResponseEntity.status(201).body(scheduleService.create(request));
    }

    @PutMapping("/{scheduleNo}")
    public InterviewScheduleResponse update(
            @PathVariable String scheduleNo,
            @RequestBody InterviewScheduleUpdateRequest request) {
        return scheduleService.update(scheduleNo, request);
    }

    @PostMapping("/{scheduleNo}/cancel")
    public InterviewScheduleResponse cancel(@PathVariable String scheduleNo) {
        return scheduleService.cancel(scheduleNo);
    }
}