package org.dpbe.domain.education.service;

import java.util.List;
import org.dpbe.domain.education.dto.EducationPlanRejectRequest;
import org.dpbe.domain.education.dto.EducationPlanRequest;
import org.dpbe.domain.education.dto.EducationPlanResponse;
import org.dpbe.domain.education.entity.EducationPlan;
import org.dpbe.domain.education.repository.EducationPlanRepository;
import org.dpbe.global.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EducationPlanService {

    private final EducationPlanRepository repository;

    public EducationPlanService(EducationPlanRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<EducationPlanResponse> getPlans(String status) {
        List<EducationPlan> plans = (status != null && !status.isBlank())
                ? repository.findByStatus(status)
                : repository.findAll();
        return plans.stream().map(EducationPlanResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public EducationPlanResponse getPlan(String planNo) {
        EducationPlan plan = repository.findByPlanNo(planNo);
        if (plan == null) throw ApiException.notFound("교육 계획안을 찾을 수 없습니다: " + planNo);
        return EducationPlanResponse.from(plan);
    }

    @Transactional
    public EducationPlanResponse createPlan(EducationPlanRequest req) {
        EducationPlan plan = new EducationPlan();
        plan.setTrainerName(req.trainerName());
        plan.enterPlanInfo(req.educationName(), req.startDate(), req.endDate(),
                req.channelType(), req.targetCount(), req.budget());
        plan.enterContentInfo(req.educationGoal(), req.educationContent(), req.textbookList());

        if ("REQUEST_APPROVAL".equals(req.action())) {
            if (!plan.validateRequiredFields()) {
                throw ApiException.badRequest("필수 항목을 입력해주세요.");
            }
            plan.requestApproval();
        } else {
            plan.tempSave();
        }

        repository.save(plan);
        return EducationPlanResponse.from(plan);
    }

    @Transactional
    public EducationPlanResponse approvePlan(String planNo) {
        EducationPlan plan = repository.findByPlanNo(planNo);
        if (plan == null) throw ApiException.notFound("교육 계획안을 찾을 수 없습니다: " + planNo);
        if (!"승인요청".equals(plan.getStatus())) {
            throw ApiException.badRequest("승인 요청 상태의 계획안만 승인할 수 있습니다.");
        }
        plan.approve();
        repository.updateStatus(plan);
        return EducationPlanResponse.from(plan);
    }

    @Transactional
    public EducationPlanResponse rejectPlan(String planNo, EducationPlanRejectRequest req) {
        EducationPlan plan = repository.findByPlanNo(planNo);
        if (plan == null) throw ApiException.notFound("교육 계획안을 찾을 수 없습니다: " + planNo);
        if (!"승인요청".equals(plan.getStatus())) {
            throw ApiException.badRequest("승인 요청 상태의 계획안만 반려할 수 있습니다.");
        }
        if (req.reason() == null || req.reason().isBlank()) {
            throw ApiException.badRequest("반려 사유를 입력해주세요.");
        }
        plan.reject(req.reason());
        repository.updateStatus(plan);
        return EducationPlanResponse.from(plan);
    }
}