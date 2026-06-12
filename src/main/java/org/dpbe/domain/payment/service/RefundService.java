package org.dpbe.domain.payment.service;

import java.util.List;
import org.dpbe.domain.contract.entity.Cancellation;
import org.dpbe.domain.contract.entity.Contract;
import org.dpbe.domain.contract.repository.CancellationRepository;
import org.dpbe.domain.payment.dto.RefundCalculationResponse;
import org.dpbe.domain.payment.entity.RefundCalculation;
import org.dpbe.domain.payment.repository.RefundCalculationRepository;
import org.dpbe.global.auth.dto.AuthenticatedUser;
import org.dpbe.global.auth.service.AuthAccessService;
import org.dpbe.global.dto.PageResponse;
import org.dpbe.global.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefundService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final CancellationRepository cancellationRepository;
    private final RefundCalculationRepository refundCalculationRepository;
    private final AuthAccessService authAccessService;

    public RefundService(CancellationRepository cancellationRepository,
                         RefundCalculationRepository refundCalculationRepository,
                         AuthAccessService authAccessService) {
        this.cancellationRepository = cancellationRepository;
        this.refundCalculationRepository = refundCalculationRepository;
        this.authAccessService = authAccessService;
    }

    private Long parseId(String businessNo) {
        try {
            return Long.parseLong(businessNo.replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            throw ApiException.badRequest("유효하지 않은 번호: " + businessNo);
        }
    }

    /** 환급금 산출 단건 조회 */
    @Transactional(readOnly = true)
    public RefundCalculationResponse getCalculation(String refundNo) {
        RefundCalculation refund = refundCalculationRepository.findById(parseId(refundNo))
                .orElseThrow(() -> ApiException.notFound("환급금 산출 건을 찾을 수 없습니다: " + refundNo));
        requireRefundAccess(refund);
        return RefundCalculationResponse.from(refund);
    }

    /** 환급금 산출 목록 */
    @Transactional(readOnly = true)
    public PageResponse<RefundCalculationResponse> getAllCalculations(int page, int size) {
        String customerNo = accessibleCustomerNo();
        if (!authAccessService.isCustomer()) {
            authAccessService.requireRefundOperationAccess();
        }

        if (authAccessService.isCustomer() && customerNo == null) {
            return emptyPage(page, size);
        }

        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);
        int offset = (normalizedPage - 1) * normalizedSize;
        int total = refundCalculationRepository.countByCustomerNo(customerNo);
        List<RefundCalculationResponse> items = refundCalculationRepository
                .findPageByCustomerNo(customerNo, normalizedSize, offset)
                .stream()
                .map(RefundCalculationResponse::from)
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

    private <T> PageResponse<T> emptyPage(int page, int size) {
        return new PageResponse<>(normalizePage(page), normalizeSize(size), 0, List.of());
    }

    private String accessibleCustomerNo() {
        AuthenticatedUser user = authAccessService.currentUser();
        return authAccessService.isCustomer() ? user.linkedCustomerNo() : null;
    }

    private void requireRefundAccess(RefundCalculation refund) {
        if (!authAccessService.isCustomer()) {
            authAccessService.requireRefundOperationAccess();
            return;
        }

        if (!canAccessRefund(refund)) {
            throw ApiException.forbidden("본인 환급 데이터만 접근할 수 있습니다.");
        }
    }

    private boolean canAccessRefund(RefundCalculation refund) {
        if (refund == null || refund.getCancellation() == null) {
            return !authAccessService.isCustomer();
        }
        Cancellation fullCancellation = cancellationRepository.findById(refund.getCancellation().getId())
                .orElse(null);
        Contract contract = fullCancellation != null ? fullCancellation.getContract() : null;
        return authAccessService.canAccessContract(contract);
    }
}
