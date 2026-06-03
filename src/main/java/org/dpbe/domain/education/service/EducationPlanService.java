package org.dpbe.domain.education.service;

import org.dpbe.domain.education.dto.EducationPlanRejectRequest;
import org.dpbe.domain.education.dto.EducationPlanRequest;
import org.dpbe.domain.education.dto.EducationPlanResponse;
import org.dpbe.domain.education.entity.EducationPlan;
import org.dpbe.domain.education.repository.EducationPlanRepository;
import org.dpbe.global.auth.service.AuthAccessService;
import org.dpbe.global.dto.PageResponse;
import org.dpbe.global.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EducationPlanService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final EducationPlanRepository repository;
    private final AuthAccessService authAccessService;

    public EducationPlanService(EducationPlanRepository repository,
                                AuthAccessService authAccessService) {
        this.repository = repository;
        this.authAccessService = authAccessService;
    }

    @Transactional(readOnly = true)
    public PageResponse<EducationPlanResponse> getPlans(String status, int page, int size) {
        authAccessService.requireEducationOperationAccess();
        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);
        int offset = (normalizedPage - 1) * normalizedSize;
        int total = repository.countByStatus(status);
        var items = repository.findPageByStatus(status, normalizedSize, offset).stream()
                .map(EducationPlanResponse::from)
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

    private Long parseId(String planNo) {
        try {
            return Long.parseLong(planNo.replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            throw ApiException.badRequest("유효하지 않은 계획안 번호: " + planNo);
        }
    }

    @Transactional(readOnly = true)
    public EducationPlanResponse getPlan(String planNo) {
        authAccessService.requireEducationOperationAccess();
        EducationPlan plan = repository.findById(parseId(planNo));
        if (plan == null) throw ApiException.notFound("교육 계획안을 찾을 수 없습니다: " + planNo);
        return EducationPlanResponse.from(plan);
    }

    @Transactional
    public EducationPlanResponse createPlan(EducationPlanRequest req) {
        authAccessService.requireEducationOperationAccess();
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
        authAccessService.requireEducationOperationAccess();
        EducationPlan plan = repository.findById(parseId(planNo));
        if (plan == null) throw ApiException.notFound("교육 계획안을 찾을 수 없습니다: " + planNo);
        plan.approve();
        repository.updateStatus(plan);
        return EducationPlanResponse.from(plan);
    }

    @Transactional
    public EducationPlanResponse rejectPlan(String planNo, EducationPlanRejectRequest req) {
        authAccessService.requireEducationOperationAccess();
        EducationPlan plan = repository.findById(parseId(planNo));
        if (plan == null) throw ApiException.notFound("교육 계획안을 찾을 수 없습니다: " + planNo);
        if (req.reason() == null || req.reason().isBlank()) {
            throw ApiException.badRequest("반려 사유를 입력해주세요.");
        }
        plan.reject(req.reason());
        repository.updateStatus(plan);
        return EducationPlanResponse.from(plan);
    }
}
