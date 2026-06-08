package org.dpbe.domain.consultation.service;

import java.util.List;
import org.dpbe.domain.consultation.dto.PendingApplicationResponse;
import org.dpbe.domain.consultation.repository.UnderwritingRepository;
import org.dpbe.global.auth.service.AuthAccessService;
import org.dpbe.global.dto.PageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UnderwritingService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final UnderwritingRepository underwritingRepo;
    private final AuthAccessService authAccessService;

    public UnderwritingService(UnderwritingRepository underwritingRepo,
                               AuthAccessService authAccessService) {
        this.underwritingRepo = underwritingRepo;
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

}
