package org.dpbe.domain.consultation.controller;

import org.dpbe.domain.consultation.dto.UnderwritingRequest;
import org.dpbe.domain.consultation.dto.UnderwritingResponse;
import org.dpbe.domain.consultation.service.DeliverUnderwritingResultService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** UC '심사 결과를 전달한다' REST 엔드포인트 */
@RestController
@RequestMapping("/api/underwriting")
public class DeliverUnderwritingResultController {

    private final DeliverUnderwritingResultService deliverUnderwritingResultService;

    public DeliverUnderwritingResultController(DeliverUnderwritingResultService deliverUnderwritingResultService) {
        this.deliverUnderwritingResultService = deliverUnderwritingResultService;
    }

    @PostMapping
    public ResponseEntity<UnderwritingResponse> complete(@RequestBody UnderwritingRequest request) {
        return ResponseEntity.status(201).body(deliverUnderwritingResultService.complete(request));
    }
}
