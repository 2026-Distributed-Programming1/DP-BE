package org.dpbe.domain.contract.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.dpbe.domain.common.enums.ContractStatus;
import org.dpbe.domain.contract.dto.ContractDetailResponse;
import org.dpbe.domain.contract.dto.ContractSummaryResponse;
import org.dpbe.domain.contract.entity.Contract;
import org.dpbe.domain.contract.repository.ContractRepository;
import org.dpbe.global.auth.dto.AuthenticatedUser;
import org.dpbe.global.auth.service.AuthAccessService;
import org.dpbe.global.dto.PageResponse;
import org.dpbe.global.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** UC '가입 보험을 조회한다' API 서비스. */
@Service
@Transactional(readOnly = true)
public class SubscribedInsuranceService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final ContractRepository contractRepository;
    private final AuthAccessService authAccessService;

    public SubscribedInsuranceService(ContractRepository contractRepository,
                                      AuthAccessService authAccessService) {
        this.contractRepository = contractRepository;
        this.authAccessService = authAccessService;
    }

    /** 고객 본인 가입 보험 목록 */
    public PageResponse<ContractSummaryResponse> list(String type, int page, int size) {
        requireCustomer();
        AuthenticatedUser user = authAccessService.currentUser();
        String customerNo = user.linkedCustomerNo();
        if (customerNo == null) {
            return emptyPage(page, size);
        }

        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);
        int offset = (normalizedPage - 1) * normalizedSize;
        int total = contractRepository.countByFilters(type, customerNo);
        List<ContractSummaryResponse> items = contractRepository
                .findPageByFilters(type, customerNo, normalizedSize, offset)
                .stream()
                .map(this::toSummary)
                .toList();

        return new PageResponse<>(normalizedPage, normalizedSize, total, items);
    }

    /** 고객 본인 가입 보험 상세 */
    public ContractDetailResponse detail(String contractNo) {
        requireCustomer();
        Contract c = contractRepository.findById(parseId(contractNo));
        if (c == null) {
            throw ApiException.notFound("계약을 찾을 수 없습니다: " + contractNo);
        }
        authAccessService.requireContractAccess(c);
        return toDetail(c);
    }

    private void requireCustomer() {
        if (!authAccessService.isCustomer()) {
            throw ApiException.forbidden("고객만 가입 보험을 조회할 수 있습니다.");
        }
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

    private Long parseId(String contractNo) {
        try {
            return Long.parseLong(contractNo.replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            throw ApiException.notFound("유효하지 않은 계약번호: " + contractNo);
        }
    }

    private ContractDetailResponse toDetail(Contract c) {
        LocalDate endDate = c.getEndDate();
        long daysUntilExpiry = endDate != null
                ? ChronoUnit.DAYS.between(LocalDate.now(), endDate) : -1;
        boolean expiringSoon = daysUntilExpiry >= 0 && daysUntilExpiry <= 30;

        return new ContractDetailResponse(
                c.getContractNo(),
                c.getPolicyNo(),
                c.getCustomer() != null ? c.getCustomer().getName() : null,
                c.getCustomer() != null ? c.getCustomer().getContact() : null,
                c.getInsuranceType(),
                c.getStartDate(),
                c.getEndDate(),
                c.getMonthlyPremium(),
                statusName(c.getStatus()),
                statusLabel(c.getStatus()),
                c.getPaidCount() != null ? c.getPaidCount() : 0,
                c.getTotalPayCount() != null ? c.getTotalPayCount() : 0,
                c.getLastPaymentDate(),
                Boolean.TRUE.equals(c.getIsOverdue()),
                c.getOverdueCount() != null ? c.getOverdueCount() : 0,
                expiringSoon,
                daysUntilExpiry,
                c.getSpecialClauses());
    }

    private ContractSummaryResponse toSummary(Contract c) {
        return new ContractSummaryResponse(
                c.getContractNo(),
                c.getCustomer() != null ? c.getCustomer().getName() : null,
                c.getInsuranceType(),
                c.getStartDate(),
                c.getEndDate(),
                c.getMonthlyPremium(),
                c.getPaidCount() != null ? c.getPaidCount() : 0,
                c.getTotalPayCount() != null ? c.getTotalPayCount() : 0,
                statusName(c.getStatus()),
                statusLabel(c.getStatus()));
    }

    private String statusName(ContractStatus status) {
        return (status != null ? status : ContractStatus.NORMAL).name();
    }

    private String statusLabel(ContractStatus status) {
        switch (status != null ? status : ContractStatus.NORMAL) {
            case EXPIRED:   return "만기";
            case CANCELLED: return "해지";
            case LAPSED:    return "실효";
            default:        return "정상";
        }
    }
}
