package org.dpbe.domain.consultation.service;

import java.time.LocalDateTime;
import org.dpbe.domain.common.enums.ApplicationType;
import org.dpbe.domain.consultation.dto.UnderwritingRequest;
import org.dpbe.domain.consultation.dto.UnderwritingResponse;
import org.dpbe.domain.consultation.entity.Underwriting;
import org.dpbe.domain.consultation.repository.InsuranceApplicationRepository;
import org.dpbe.domain.consultation.repository.PolicyApplicationRepository;
import org.dpbe.domain.consultation.repository.UnderwritingRepository;
import org.dpbe.global.auth.service.AuthAccessService;
import org.dpbe.global.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** UC '심사 결과를 전달한다' API 서비스. */
@Service
public class DeliverUnderwritingResultService {

    private final UnderwritingRepository underwritingRepo;
    private final PolicyApplicationRepository policyAppRepo;
    private final InsuranceApplicationRepository insuranceAppRepo;
    private final AuthAccessService authAccessService;

    public DeliverUnderwritingResultService(UnderwritingRepository underwritingRepo,
                                            PolicyApplicationRepository policyAppRepo,
                                            InsuranceApplicationRepository insuranceAppRepo,
                                            AuthAccessService authAccessService) {
        this.underwritingRepo = underwritingRepo;
        this.policyAppRepo = policyAppRepo;
        this.insuranceAppRepo = insuranceAppRepo;
        this.authAccessService = authAccessService;
    }

    /** 심사 완료 — 결과 저장 + 원본 신청 건 status 갱신. */
    @Transactional
    public UnderwritingResponse complete(UnderwritingRequest req) {
        authAccessService.requireUnderwritingOperationAccess();
        if (req.applicationType() == null)
            throw ApiException.badRequest("신청 유형(POLICY/INSURANCE)은 필수입니다.");
        if (req.appNo() == null || req.appNo().isBlank())
            throw ApiException.badRequest("심사 대상 신청번호는 필수입니다.");
        if (req.result() == null || req.result().isBlank())
            throw ApiException.badRequest("심사 결과는 필수입니다.");
        if ("조건부승인".equals(req.result()) && (req.resultCondition() == null || req.resultCondition().isBlank()))
            throw ApiException.badRequest("조건부승인 시 승인 조건을 입력해야 합니다.");
        if ("거절".equals(req.result()) && (req.rejectionReason() == null || req.rejectionReason().isBlank()))
            throw ApiException.badRequest("거절 시 거절 사유를 입력해야 합니다.");

        Underwriting u = new Underwriting();
        u.setAppNo(req.appNo());
        u.setApplicationType(req.applicationType());
        u.setCustomerName(req.customerName());
        u.startReview();
        if (req.reviewType() != null && !req.reviewType().isBlank()) {
            u.manualReview(req.reviewType(), req.reviewOpinion());
        } else {
            u.autoReview();
        }
        u.complete(req.result(), req.resultCondition(), req.rejectionReason());
        u.setReviewedAt(LocalDateTime.now());
        underwritingRepo.save(u);

        // 원본 신청 건 status 갱신 — applicationType으로 명시적 분기
        if (ApplicationType.POLICY == req.applicationType()) {
            var p = policyAppRepo.findByNo(req.appNo());
            if (p == null) throw ApiException.notFound("청약서를 찾을 수 없습니다: " + req.appNo());
            p.applyUnderwritingResult(req.result());
            policyAppRepo.updateStatus(p);
        } else {
            var a = insuranceAppRepo.findByNo(req.appNo());
            if (a == null) throw ApiException.notFound("청약신청을 찾을 수 없습니다: " + req.appNo());
            a.applyUnderwritingResult(req.result());
            insuranceAppRepo.updateStatus(a);
        }

        return UnderwritingResponse.from(u);
    }
}
