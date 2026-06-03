package org.dpbe.domain.sales.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.dpbe.domain.common.enums.ChannelType;
import org.dpbe.domain.common.enums.EvaluationGrade;
import org.dpbe.domain.sales.dto.SalesOrgEvaluationListResponse;
import org.dpbe.domain.sales.dto.SalesOrgEvaluationRequest;
import org.dpbe.domain.sales.dto.SalesOrgEvaluationResponse;
import org.dpbe.domain.sales.entity.SalesOrgEvaluation;
import org.dpbe.domain.sales.repository.SalesOrgEvaluationRepository;
import org.dpbe.global.auth.service.AuthAccessService;
import org.dpbe.global.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SalesOrgEvaluationService {

    private final SalesOrgEvaluationRepository repository;
    private final AuthAccessService authAccessService;

    public SalesOrgEvaluationService(SalesOrgEvaluationRepository repository,
                                     AuthAccessService authAccessService) {
        this.repository = repository;
        this.authAccessService = authAccessService;
    }

    @Transactional(readOnly = true)
    public SalesOrgEvaluationListResponse findAll(
            LocalDate startDate, LocalDate endDate, String channelType, int page, int size) {
        authAccessService.requireSalesOperationAccess();
        if (page < 1) page = 1;
        if (size < 1) size = 20;

        List<SalesOrgEvaluation> filtered = repository.findAll().stream()
                .filter(e -> startDate == null || (e.getEvaluatedAt() != null
                        && !e.getEvaluatedAt().toLocalDate().isBefore(startDate)))
                .filter(e -> endDate == null || (e.getEvaluatedAt() != null
                        && !e.getEvaluatedAt().toLocalDate().isAfter(endDate)))
                .filter(e -> channelType == null || channelType.isBlank()
                        || (e.getChannelType() != null && e.getChannelType().name().equalsIgnoreCase(channelType)))
                .sorted((a, b) -> {
                    double ra = a.getAchievementRate() != null ? a.getAchievementRate() : 0;
                    double rb = b.getAchievementRate() != null ? b.getAchievementRate() : 0;
                    return Double.compare(ra, rb);
                })
                .collect(Collectors.toList());

        int total = filtered.size();
        int from = Math.min((page - 1) * size, total);
        int to = Math.min(from + size, total);

        List<SalesOrgEvaluationResponse> items = filtered.subList(from, to).stream()
                .map(SalesOrgEvaluationResponse::from)
                .collect(Collectors.toList());

        return new SalesOrgEvaluationListResponse(page, size, total, items);
    }

    @Transactional
    public SalesOrgEvaluationResponse create(SalesOrgEvaluationRequest request) {
        authAccessService.requireSalesOperationAccess();
        if (request.channelName() == null || request.evaluationGrade() == null) {
            throw ApiException.badRequest("필수 항목 누락: channelName, evaluationGrade");
        }

        SalesOrgEvaluation e = new SalesOrgEvaluation();
        e.setChannelName(request.channelName());
        if (request.channelType() != null) {
            try { e.setChannelType(ChannelType.valueOf(request.channelType())); }
            catch (IllegalArgumentException ex) {
                throw ApiException.badRequest("유효하지 않은 channelType: " + request.channelType());
            }
        }
        try { e.setEvaluationGrade(EvaluationGrade.valueOf(request.evaluationGrade())); }
        catch (IllegalArgumentException ex) {
            throw ApiException.badRequest("유효하지 않은 evaluationGrade: " + request.evaluationGrade());
        }
        e.setSalesResult(request.salesResult());
        e.setContractCount(request.contractCount());
        e.setAchievementRate(request.achievementRate());
        e.setEvaluationComment(request.evaluationComment());
        e.setEvaluatedAt(LocalDateTime.now());

        repository.save(e);
        return SalesOrgEvaluationResponse.from(e);
    }
}
