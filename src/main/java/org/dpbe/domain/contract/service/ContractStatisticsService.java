package org.dpbe.domain.contract.service;

import java.util.List;
import java.util.stream.Collectors;
import org.dpbe.domain.contract.dto.ContractStatisticsResponse;
import org.dpbe.domain.contract.entity.ContractStatistics;
import org.dpbe.domain.contract.repository.ContractStatisticsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContractStatisticsService {

    private final ContractStatisticsRepository statsRepository;

    public ContractStatisticsService(ContractStatisticsRepository statsRepository) {
        this.statsRepository = statsRepository;
    }

    /** 현재 집계를 실시간 반환 — DB 저장 없음 */
    @Transactional(readOnly = true)
    public ContractStatisticsResponse getCurrent() {
        return toResponse(statsRepository.aggregate());
    }

    /** 현재 집계를 스냅샷으로 저장 후 반환 */
    @Transactional
    public ContractStatisticsResponse snapshot() {
        ContractStatistics s = statsRepository.aggregate();
        s.setCreatedAt(java.time.LocalDateTime.now());
        statsRepository.save(s);
        return toResponse(s);
    }

    /** 저장된 스냅샷 이력 */
    @Transactional(readOnly = true)
    public List<ContractStatisticsResponse> getHistory() {
        return statsRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private ContractStatisticsResponse toResponse(ContractStatistics s) {
        return new ContractStatisticsResponse(
                s.getStatsNo(),
                s.getTotalCount(),
                s.getActiveCount(),
                s.getExpiredCount(),
                s.getCancelledCount(),
                s.getCreatedAt());
    }
}