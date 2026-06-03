package org.dpbe.domain.contract.service;

import java.util.List;
import org.dpbe.domain.contract.dto.ContractStatisticsResponse;
import org.dpbe.domain.contract.entity.ContractStatistics;
import org.dpbe.domain.contract.repository.ContractStatisticsRepository;
import org.dpbe.global.auth.service.AuthAccessService;
import org.dpbe.global.dto.PageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContractStatisticsService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final ContractStatisticsRepository statsRepository;
    private final AuthAccessService authAccessService;

    public ContractStatisticsService(ContractStatisticsRepository statsRepository,
                                     AuthAccessService authAccessService) {
        this.statsRepository = statsRepository;
        this.authAccessService = authAccessService;
    }

    /** 현재 집계를 실시간 반환 — DB 저장 없음 */
    @Transactional(readOnly = true)
    public ContractStatisticsResponse getCurrent() {
        authAccessService.requireContractOperationAccess();
        return toResponse(statsRepository.aggregate());
    }

    /** 현재 집계를 스냅샷으로 저장 후 반환 */
    @Transactional
    public ContractStatisticsResponse snapshot() {
        authAccessService.requireContractOperationAccess();
        ContractStatistics s = statsRepository.aggregate();
        s.setCreatedAt(java.time.LocalDateTime.now());
        statsRepository.save(s);
        return toResponse(s);
    }

    /** 저장된 스냅샷 이력 */
    @Transactional(readOnly = true)
    public PageResponse<ContractStatisticsResponse> getHistory(int page, int size) {
        authAccessService.requireContractOperationAccess();
        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);
        int offset = (normalizedPage - 1) * normalizedSize;
        int total = statsRepository.countAll();
        List<ContractStatisticsResponse> items = statsRepository.findPage(normalizedSize, offset).stream()
                .map(this::toResponse)
                .toList();
        return new PageResponse<>(normalizedPage, normalizedSize, total, items);
    }

    private int normalizePage(int page) {
        return page < 1 ? DEFAULT_PAGE : page;
    }

    private int normalizeSize(int size) {
        if (size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
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
