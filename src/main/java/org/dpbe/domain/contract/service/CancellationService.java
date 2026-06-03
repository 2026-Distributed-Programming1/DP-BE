package org.dpbe.domain.contract.service;

import java.util.List;
import org.dpbe.domain.common.enums.ContractStatus;
import org.dpbe.domain.contract.dto.CancellationRequest;
import org.dpbe.domain.contract.dto.CancellationResponse;
import org.dpbe.domain.contract.entity.Cancellation;
import org.dpbe.domain.contract.entity.Contract;
import org.dpbe.domain.contract.repository.CancellationRepository;
import org.dpbe.domain.contract.repository.ContractRepository;
import org.dpbe.global.auth.dto.AuthenticatedUser;
import org.dpbe.global.auth.service.AuthAccessService;
import org.dpbe.global.dto.PageResponse;
import org.dpbe.global.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CancellationService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final ContractRepository contractRepository;
    private final CancellationRepository cancellationRepository;
    private final AuthAccessService authAccessService;

    public CancellationService(ContractRepository contractRepository,
                               CancellationRepository cancellationRepository,
                               AuthAccessService authAccessService) {
        this.contractRepository = contractRepository;
        this.cancellationRepository = cancellationRepository;
        this.authAccessService = authAccessService;
    }

    private Long parseId(String no) {
        try {
            return Long.parseLong(no.replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            throw ApiException.badRequest("유효하지 않은 번호: " + no);
        }
    }

    /** 해지 신청 — contract.status CANCELLED 업데이트 포함, 같은 트랜잭션 */
    @Transactional
    public CancellationResponse cancel(String contractNo, CancellationRequest req) {
        if (!req.noticeAgreed()) {
            throw ApiException.badRequest("해지 유의사항에 동의해야 합니다.");
        }
        if (req.reason() == null || req.reason().isBlank()) {
            throw ApiException.badRequest("해지 사유를 입력해야 합니다.");
        }

        Contract contract = contractRepository.findById(parseId(contractNo));
        if (contract == null) {
            throw ApiException.notFound("계약을 찾을 수 없습니다: " + contractNo);
        }
        authAccessService.requireContractAccess(contract);

        Cancellation c = new Cancellation(contract);
        c.selectReason(req.reason());
        if (req.detailReason() != null && !req.detailReason().isBlank()) {
            c.enterDetailReason(req.detailReason());
        }
        if (!c.validateReasonInput()) {
            throw ApiException.badRequest("'기타' 선택 시 상세 사유를 입력해야 합니다.");
        }

        c.agreeToNotice();
        c.calculateExpectedRefund();
        c.submit();

        cancellationRepository.save(c);
        contractRepository.updateStatus(contract.getId(), ContractStatus.CANCELLED);

        return toResponse(c);
    }

    @Transactional(readOnly = true)
    public PageResponse<CancellationResponse> getAll(int page, int size) {
        AuthenticatedUser user = authAccessService.currentUser();
        String customerNo = authAccessService.isCustomer() ? user.linkedCustomerNo() : null;
        if (authAccessService.isCustomer() && customerNo == null) {
            return emptyPage(page, size);
        }

        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);
        int offset = (normalizedPage - 1) * normalizedSize;
        int total = cancellationRepository.countByCustomerNo(customerNo);
        List<CancellationResponse> items = cancellationRepository
                .findPageByCustomerNo(customerNo, normalizedSize, offset)
                .stream()
                .map(this::toResponse)
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

    @Transactional(readOnly = true)
    public CancellationResponse getOne(String cancellationNo) {
        return cancellationRepository.findById(parseId(cancellationNo))
                .map(c -> {
                    authAccessService.requireContractAccess(c.getContract());
                    return toResponse(c);
                })
                .orElseThrow(() -> ApiException.notFound("해지 건을 찾을 수 없습니다: " + cancellationNo));
    }

    private CancellationResponse toResponse(Cancellation c) {
        String contractNo   = c.getContract() != null ? c.getContract().getContractNo() : null;
        String customerName = c.getContract() != null && c.getContract().getCustomer() != null
                ? c.getContract().getCustomer().getName() : null;
        long   premium      = c.getContract() != null ? c.getContract().getMonthlyPremium() : 0L;
        return new CancellationResponse(
                c.getCancellationNo(),
                contractNo,
                customerName,
                premium,
                c.getReason(),
                c.getDetailReason(),
                c.getExpectedRefund(),
                c.getStatus(),
                c.getCanceledAt());
    }
}
