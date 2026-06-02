package org.dpbe.domain.sales.service;

import java.util.List;
import java.util.stream.Collectors;
import org.dpbe.domain.common.enums.ActivityType;
import org.dpbe.domain.common.enums.InsuranceType;
import org.dpbe.domain.common.enums.PlanStatus;
import org.dpbe.domain.sales.dto.ActivityPlanRequest;
import org.dpbe.domain.sales.dto.ActivityPlanResponse;
import org.dpbe.domain.sales.entity.ActivityPlan;
import org.dpbe.domain.sales.entity.ScheduleItem;
import org.dpbe.domain.sales.repository.ActivityPlanRepository;
import org.dpbe.global.auth.service.AuthAccessService;
import org.dpbe.global.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityPlanService {

    private final ActivityPlanRepository repository;
    private final AuthAccessService authAccessService;

    public ActivityPlanService(ActivityPlanRepository repository,
                               AuthAccessService authAccessService) {
        this.repository = repository;
        this.authAccessService = authAccessService;
    }

    @Transactional(readOnly = true)
    public List<ActivityPlanResponse> findAll() {
        authAccessService.requireSalesOperationAccess();
        return repository.findAll().stream()
                .map(ActivityPlanResponse::from)
                .collect(Collectors.toList());
    }

    private Long parseId(String planNo) {
        try {
            return Long.parseLong(planNo.replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            throw ApiException.badRequest("유효하지 않은 계획번호: " + planNo);
        }
    }

    @Transactional(readOnly = true)
    public ActivityPlanResponse findByPlanNo(String planNo) {
        authAccessService.requireSalesOperationAccess();
        ActivityPlan plan = repository.findById(parseId(planNo));
        if (plan == null) {
            throw ApiException.notFound("활동 계획을 찾을 수 없습니다: " + planNo);
        }
        return ActivityPlanResponse.from(plan);
    }

    @Transactional
    public ActivityPlanResponse create(ActivityPlanRequest request) {
        authAccessService.requireSalesOperationAccess();
        if (request.planName() == null || request.startDate() == null
                || request.endDate() == null
                || request.targetContractCount() == null || request.targetContractCount() <= 0
                || request.targetContractAmount() == null || request.targetContractAmount() <= 0
                || request.proposedCustomerId() == null
                || request.proposedInsuranceType() == null) {
            throw ApiException.badRequest("필수 항목 누락: planName, startDate, endDate, targetContractCount, targetContractAmount, proposedCustomerId, proposedInsuranceType");
        }
        if (!request.endDate().isAfter(request.startDate())) {
            throw ApiException.badRequest("종료일은 시작일 이후여야 합니다.");
        }

        ActivityPlan p = new ActivityPlan();
        p.setPlanName(request.planName());
        p.setStartDate(request.startDate());
        p.setEndDate(request.endDate());
        p.setAuthor(request.author());
        p.setMemo(request.memo());
        p.setTargetContractCount(request.targetContractCount());
        p.setTargetContractAmount(request.targetContractAmount());
        p.setTargetNewCustomer(request.targetNewCustomer());
        p.setProposedCustomerId(request.proposedCustomerId());
        p.setProposalReason(request.proposalReason());

        try {
            p.setProposedInsuranceType(InsuranceType.valueOf(request.proposedInsuranceType()));
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("유효하지 않은 proposedInsuranceType: " + request.proposedInsuranceType());
        }

        PlanStatus status = PlanStatus.TEMP_SAVE;
        if (request.status() != null) {
            try { status = PlanStatus.valueOf(request.status()); }
            catch (IllegalArgumentException e) {
                throw ApiException.badRequest("유효하지 않은 status: " + request.status());
            }
        }
        p.setStatus(status);

        if (request.schedules() != null) {
            for (var s : request.schedules()) {
                if (s.customerId() == null || s.activityType() == null || s.activityDateTime() == null) {
                    throw ApiException.badRequest("일정 항목 필수값 누락: customerId, activityType, activityDateTime");
                }
                ActivityType at;
                try { at = ActivityType.valueOf(s.activityType()); }
                catch (IllegalArgumentException e) {
                    throw ApiException.badRequest("유효하지 않은 activityType: " + s.activityType());
                }
                p.addSchedule(new ScheduleItem(s.customerId(), at, s.activityDateTime(),
                        s.location(), s.memo()));
            }
        }

        repository.save(p);
        return ActivityPlanResponse.from(p);
    }
}
