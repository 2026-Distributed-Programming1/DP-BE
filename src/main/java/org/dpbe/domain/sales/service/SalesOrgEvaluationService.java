package org.dpbe.domain.sales.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.dpbe.domain.common.enums.ChannelType;
import org.dpbe.domain.common.enums.EvaluationGrade;
import org.dpbe.domain.sales.dto.SalesOrgEvaluationRequest;
import org.dpbe.domain.sales.dto.SalesOrgEvaluationResponse;
import org.dpbe.domain.sales.entity.SalesOrgEvaluation;
import org.dpbe.domain.sales.repository.SalesOrgEvaluationRepository;
import org.dpbe.global.auth.service.AuthAccessService;
import org.dpbe.global.dto.PageResponse;
import org.dpbe.global.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SalesOrgEvaluationService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final SalesOrgEvaluationRepository repository;
    private final AuthAccessService authAccessService;

    public SalesOrgEvaluationService(SalesOrgEvaluationRepository repository,
                                     AuthAccessService authAccessService) {
        this.repository = repository;
        this.authAccessService = authAccessService;
    }

    @Transactional(readOnly = true)
    public PageResponse<SalesOrgEvaluationResponse> findAll(
            LocalDate startDate, LocalDate endDate, String channelType, int page, int size) {
        authAccessService.requireSalesOperationAccess();

        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);
        int offset = (normalizedPage - 1) * normalizedSize;
        int total = repository.countByFilters(startDate, endDate, channelType);
        var items = repository.findPageByFilters(startDate, endDate, channelType, normalizedSize, offset).stream()
                .map(SalesOrgEvaluationResponse::from)
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
