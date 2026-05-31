package org.dpbe.domain.sales.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
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
    public List<SalesActivityManagementResponse> findAll() {
        return repository.findAll().stream()
                .map(SalesActivityManagementResponse::from)
                .collect(Collectors.toList());
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