package org.dpbe.domain.contract.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import org.dpbe.global.exception.ApiException;
import org.dpbe.domain.contract.dto.ContractDetailResponse;
import org.dpbe.domain.contract.dto.ContractListResponse;
import org.dpbe.domain.contract.dto.ContractSummaryResponse;
import org.dpbe.domain.contract.repository.ContractRepository;
import org.dpbe.domain.contract.entity.Contract;
import org.dpbe.domain.common.enums.ContractStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC '계약 정보를 조회한다' API 서비스.
 * 계약 조회·필터·상세 로직을 처리한다.
 * 조회 전용이므로 readOnly 트랜잭션.
 */
@Service
@Transactional(readOnly = true)
public class ContractService {

    private final ContractRepository contractRepository;

    public ContractService(ContractRepository contractRepository) {
        this.contractRepository = contractRepository;
    }

    /** Basic 2 / A1(필터 없음) / A2(결과 없음) — 필터 + 페이징 목록 */
    public ContractListResponse list(String type, int page, int size) {
        if (page < 1) page = 1;
        if (size < 1) size = 20;

        List<Contract> filtered = contractRepository.findAll().stream()
                .filter(c -> type == null || type.isBlank()
                        || (c.getInsuranceType() != null && c.getInsuranceType().contains(type)))
                .collect(Collectors.toList());

        int total = filtered.size();
        int from = Math.min((page - 1) * size, total);
        int to = Math.min(from + size, total);

        List<ContractSummaryResponse> items = filtered.subList(from, to).stream()
                .map(this::toSummary)
                .collect(Collectors.toList());

        return new ContractListResponse(page, size, total, items);
    }

    private Long parseId(String contractNo) {
        try {
            return Long.parseLong(contractNo.replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            throw ApiException.notFound("유효하지 않은 계약번호: " + contractNo);
        }
    }

    /** Basic 4 / A3(만기임박 D-day) / A5(특약 없음) — 상세 */
    public ContractDetailResponse detail(String contractNo) {
        Contract c = contractRepository.findById(parseId(contractNo));
        if (c == null) {
            throw ApiException.notFound("계약을 찾을 수 없습니다: " + contractNo);
        }

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
