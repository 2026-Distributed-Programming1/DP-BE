package org.dpbe.domain.contract.controller;

import java.util.List;
import org.dpbe.domain.contract.dto.CancellationRequest;
import org.dpbe.domain.contract.dto.CancellationResponse;
import org.dpbe.domain.contract.service.CancellationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CancellationController {

    private final CancellationService cancellationService;

    public CancellationController(CancellationService cancellationService) {
        this.cancellationService = cancellationService;
    }

    @PostMapping("/api/contracts/{contractNo}/cancellation")
    public CancellationResponse cancel(@PathVariable String contractNo,
                                       @RequestBody CancellationRequest req) {
        return cancellationService.cancel(contractNo, req);
    }

    @GetMapping("/api/cancellations")
    public List<CancellationResponse> list() {
        return cancellationService.getAll();
    }

    @GetMapping("/api/cancellations/{cancellationNo}")
    public CancellationResponse detail(@PathVariable String cancellationNo) {
        return cancellationService.getOne(cancellationNo);
    }
}