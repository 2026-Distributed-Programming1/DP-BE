package org.dpbe.domain.sales.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.dpbe.domain.common.enums.ChannelType;
import org.dpbe.domain.common.enums.EvaluationGrade;
import org.dpbe.domain.sales.dto.SalesOrgEvaluationRequest;
import org.dpbe.domain.sales.dto.SalesOrgEvaluationResponse;
import org.dpbe.domain.sales.entity.SalesOrgEvaluation;
import org.dpbe.domain.sales.repository.SalesOrgEvaluationRepository;
import org.dpbe.global.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SalesOrgEvaluationService {

    private final SalesOrgEvaluationRepository repository;

    public SalesOrgEvaluationService(SalesOrgEvaluationRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<SalesOrgEvaluationResponse> findAll() {
        return repository.findAll().stream()
                .map(SalesOrgEvaluationResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public SalesOrgEvaluationResponse create(SalesOrgEvaluationRequest request) {
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