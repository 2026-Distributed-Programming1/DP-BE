package org.dpbe.domain.sales.service;

import java.time.LocalDateTime;
import org.dpbe.domain.common.enums.ChannelType;
import org.dpbe.domain.common.enums.EvaluationGrade;
import org.dpbe.domain.sales.dto.BonusRequestRequest;
import org.dpbe.domain.sales.dto.BonusRequestResponse;
import org.dpbe.domain.sales.entity.BonusRequest;
import org.dpbe.domain.sales.repository.BonusRequestRepository;
import org.dpbe.global.auth.service.AuthAccessService;
import org.dpbe.global.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BonusRequestService {

    private final BonusRequestRepository repository;
    private final AuthAccessService authAccessService;

    public BonusRequestService(BonusRequestRepository repository,
                               AuthAccessService authAccessService) {
        this.repository = repository;
        this.authAccessService = authAccessService;
    }

    @Transactional
    public BonusRequestResponse create(BonusRequestRequest request) {
        authAccessService.requireBonusRequestManageAccess();
        if (request.channelName() == null || request.evaluationGrade() == null
                || request.baseSalary() == null) {
            throw ApiException.badRequest("필수 항목 누락: channelName, evaluationGrade, baseSalary");
        }

        EvaluationGrade grade;
        try { grade = EvaluationGrade.valueOf(request.evaluationGrade()); }
        catch (IllegalArgumentException e) {
            throw ApiException.badRequest("유효하지 않은 evaluationGrade: " + request.evaluationGrade());
        }
        if (grade != EvaluationGrade.S && grade != EvaluationGrade.A) {
            throw ApiException.badRequest("성과급 지급 대상은 S·A 등급만 가능합니다.");
        }

        BonusRequest r = new BonusRequest();
        r.setEvaluationNo(request.evaluationNo());
        r.setChannelName(request.channelName());
        if (request.channelType() != null) {
            try { r.setChannelType(ChannelType.valueOf(request.channelType())); }
            catch (IllegalArgumentException e) {
                throw ApiException.badRequest("유효하지 않은 channelType: " + request.channelType());
            }
        }
        r.setEvaluationGrade(grade);
        r.setBaseSalary(request.baseSalary());
        r.calculateBonus();
        r.setRequestReason(request.requestReason());
        r.setRequestedAt(LocalDateTime.now());

        repository.save(r);
        return BonusRequestResponse.from(r);
    }
}
