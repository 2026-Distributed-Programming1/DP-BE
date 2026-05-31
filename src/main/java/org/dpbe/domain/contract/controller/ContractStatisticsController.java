package org.dpbe.domain.contract.controller;

import java.util.List;
import org.dpbe.domain.contract.dto.ContractStatisticsResponse;
import org.dpbe.domain.contract.service.ContractStatisticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contract-statistics")
public class ContractStatisticsController {

    private final ContractStatisticsService statsService;

    public ContractStatisticsController(ContractStatisticsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping
    public ContractStatisticsResponse getCurrent() {
        return statsService.getCurrent();
    }

    @PostMapping
    public ContractStatisticsResponse snapshot() {
        return statsService.snapshot();
    }

    @GetMapping("/history")
    public List<ContractStatisticsResponse> history() {
        return statsService.getHistory();
    }
}