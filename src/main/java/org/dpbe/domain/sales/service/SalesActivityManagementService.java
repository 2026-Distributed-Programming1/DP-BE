package org.dpbe.domain.sales.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.dpbe.domain.sales.dto.SalesActivityManagementListResponse;
import org.dpbe.domain.sales.dto.SalesActivityManagementRequest;
import org.dpbe.domain.sales.dto.SalesActivityManagementResponse;
import org.dpbe.domain.sales.entity.SalesActivityManagement;
import org.dpbe.domain.sales.repository.SalesActivityManagementRepository;
import org.dpbe.global.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SalesActivityManagementService {

    private final SalesActivityManagementRepository repository;

    public SalesActivityManagementService(SalesActivityManagementRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public SalesActivityManagementListResponse findAll(
            LocalDate startDate, LocalDate endDate, String channelType, int page, int size) {
        if (page < 1) page = 1;
        if (size < 1) size = 20;

        List<SalesActivityManagement> filtered = repository.findAll().stream()
                .filter(a -> startDate == null || (a.getStartDate() != null && !a.getStartDate().isBefore(startDate)))
                .filter(a -> endDate == null || (a.getEndDate() != null && !a.getEndDate().isAfter(endDate)))
                .filter(a -> channelType == null || channelType.isBlank()
                        || (a.getChannelType() != null && a.getChannelType().name().equalsIgnoreCase(channelType)))
                .sorted((a, b) -> {
                    double ra = a.getAchievementRate() != null ? a.getAchievementRate() : 0;
                    double rb = b.getAchievementRate() != null ? b.getAchievementRate() : 0;
                    return Double.compare(ra, rb);
                })
                .collect(Collectors.toList());

        int total = filtered.size();
        int from = Math.min((page - 1) * size, total);
        int to = Math.min(from + size, total);

        List<SalesActivityManagementResponse> items = filtered.subList(from, to).stream()
                .map(SalesActivityManagementResponse::from)
                .collect(Collectors.toList());

        return new SalesActivityManagementListResponse(page, size, total, items);
    }

    @Transactional
    public SalesActivityManagementResponse create(SalesActivityManagementRequest request) {
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