package org.dpbe.domain.consultation.controller;

import java.util.List;
import org.dpbe.domain.consultation.dto.ConsultationCreateRequest;
import org.dpbe.domain.consultation.dto.ConsultationResponse;
import org.dpbe.domain.consultation.service.ConsultationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/consultations")
public class ConsultationController {

    private final ConsultationService consultationService;

    public ConsultationController(ConsultationService consultationService) {
        this.consultationService = consultationService;
    }

    @GetMapping
    public List<ConsultationResponse> findAll() {
        return consultationService.findAll();
    }

    @GetMapping("/{consultNo}")
    public ConsultationResponse findByConsultNo(@PathVariable String consultNo) {
        return consultationService.findByConsultNo(consultNo);
    }

    @PostMapping
    public ResponseEntity<ConsultationResponse> create(@RequestBody ConsultationCreateRequest request) {
        return ResponseEntity.status(201).body(consultationService.create(request));
    }

    @PostMapping("/{consultNo}/accept")
    public ConsultationResponse accept(@PathVariable String consultNo) {
        return consultationService.accept(consultNo);
    }
}