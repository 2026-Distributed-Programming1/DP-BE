package org.dpbe.domain.sales.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.dpbe.domain.sales.dto.SalesActivityManagementRequest;
import org.dpbe.domain.sales.dto.SalesActivityManagementResponse;
import org.dpbe.domain.sales.entity.SalesActivityManagement;
import org.dpbe.domain.sales.repository.SalesActivityManagementRepository;
import org.dpbe.global.auth.service.AuthAccessService;
import org.dpbe.global.dto.PageResponse;
import org.dpbe.global.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SalesActivityManagementService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final SalesActivityManagementRepository repository;
    private final AuthAccessService authAccessService;

    public SalesActivityManagementService(SalesActivityManagementRepository repository,
                                          AuthAccessService authAccessService) {
        this.repository = repository;
        this.authAccessService = authAccessService;
    }

    @Transactional(readOnly = true)
    public PageResponse<SalesActivityManagementResponse> findAll(
            LocalDate startDate, LocalDate endDate, String channelType, int page, int size) {
        authAccessService.requireSalesOperationAccess();

        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);
        int offset = (normalizedPage - 1) * normalizedSize;
        int total = repository.countByFilters(startDate, endDate, channelType);
        var items = repository.findPageByFilters(startDate, endDate, channelType, normalizedSize, offset).stream()
                .map(SalesActivityManagementResponse::from)
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

    @Transactional
    public SalesActivityManagementResponse create(SalesActivityManagementRequest request) {
        authAccessService.requireSalesOperationAccess();
        if (request.channelName() == null || request.channelType() == null
                || request.startDate() == null || request.endDate() == null) {
            throw ApiException.badRequest("필수 항목 누락: channelName, channelType, startDate, endDate");
        }

        SalesActivityManagement a = new SalesActivityManagement();
        a.setManagerName(request.managerName());
        a.setChannelName(request.channelName());
        a.setActivityType(request.channelType());
        a.setStartDate(request.startDate());
        a.setEndDate(request.endDate());
        if (request.visitCount() != null) a.setVisitCount(request.visitCount());
        if (request.contractCount() != null) a.setContractCount(request.contractCount());
        if (request.achievementRate() != null) a.setAchievementRate(request.achievementRate());
        a.setImprovementContent(request.improvementContent());
        a.setRevisedTarget(request.revisedTarget());
        a.setRegisteredAt(LocalDateTime.now());

        repository.save(a);
        return SalesActivityManagementResponse.from(a);
    }
}
