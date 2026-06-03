package org.dpbe.domain.consultation.service;

import java.time.LocalDateTime;
import java.util.List;
import org.dpbe.domain.consultation.dto.PendingApplicationResponse;
import org.dpbe.domain.consultation.dto.UnderwritingRequest;
import org.dpbe.domain.consultation.dto.UnderwritingResponse;
import org.dpbe.domain.consultation.entity.Underwriting;
import org.dpbe.domain.consultation.repository.InsuranceApplicationRepository;
import org.dpbe.domain.consultation.repository.PolicyApplicationRepository;
import org.dpbe.domain.consultation.repository.UnderwritingRepository;
import org.dpbe.global.auth.service.AuthAccessService;
import org.dpbe.global.dto.PageResponse;
import org.dpbe.global.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UnderwritingService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final UnderwritingRepository underwritingRepo;
    private final PolicyApplicationRepository policyAppRepo;
    private final InsuranceApplicationRepository insuranceAppRepo;
    private final AuthAccessService authAccessService;

    public UnderwritingService(UnderwritingRepository underwritingRepo,
                               PolicyApplicationRepository policyAppRepo,
                               InsuranceApplicationRepository insuranceAppRepo,
                               AuthAccessService authAccessService) {
        this.underwritingRepo = underwritingRepo;
        this.policyAppRepo = policyAppRepo;
        this.insuranceAppRepo = insuranceAppRepo;
        this.authAccessService = authAccessService;
    }

    /** 심사 대기 목록 — 청약(POL) + 보험신청(APP) 통합. */
    public PageResponse<PendingApplicationResponse> findPending(int page, int size) {
        authAccessService.requireUnderwritingOperationAccess();
        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);
        int offset = (normalizedPage - 1) * normalizedSize;
        int total = underwritingRepo.countPendingApplications();
        List<PendingApplicationResponse> items =
                underwritingRepo.findPendingApplicationsPage(normalizedSize, offset);
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

    /** 심사 완료 — 결과 저장 + 원본 신청 건 status 갱신. */
    @Transactional
    public UnderwritingResponse complete(UnderwritingRequest req) {
        authAccessService.requireUnderwritingOperationAccess();
        if (req.applicationType() == null || req.applicationType().isBlank())
            throw ApiException.badRequest("신청 유형(청약/보험신청)은 필수입니다.");
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
        if ("청약".equals(req.applicationType())) {
            policyAppRepo.findPending().stream()
                    .filter(p -> req.appNo().equals(p.getApplicationNo()))
                    .findFirst()
                    .ifPresent(p -> {
                        p.setStatus(req.result());
                        policyAppRepo.updateStatus(p);
                    });
        } else if ("보험신청".equals(req.applicationType())) {
            insuranceAppRepo.findPending().stream()
                    .filter(a -> req.appNo().equals(a.getApplicationNo()))
                    .findFirst()
                    .ifPresent(a -> {
                        a.setStatus(req.result());
                        insuranceAppRepo.updateStatus(a);
                    });
        } else {
            throw ApiException.badRequest("알 수 없는 신청 유형입니다: " + req.applicationType());
        }

        return UnderwritingResponse.from(u);
    }
}
