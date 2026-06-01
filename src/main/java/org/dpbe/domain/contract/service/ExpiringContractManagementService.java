package org.dpbe.domain.contract.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import org.dpbe.domain.common.enums.CustomerResponse;
import org.dpbe.domain.contract.dto.ExpiringContractSummaryResponse;
import org.dpbe.domain.contract.dto.NoticeCreateRequest;
import org.dpbe.domain.contract.dto.NoticeResponse;
import org.dpbe.domain.contract.dto.NoticeResponseRequest;
import org.dpbe.domain.contract.entity.Contract;
import org.dpbe.domain.contract.entity.ExpiringContractManagement;
import org.dpbe.domain.contract.repository.ContractRepository;
import org.dpbe.domain.contract.repository.ExpiringContractManagementRepository;
import org.dpbe.global.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpiringContractManagementService {

    private final ContractRepository contractRepository;
    private final ExpiringContractManagementRepository noticeRepository;

    public ExpiringContractManagementService(ContractRepository contractRepository,
                                             ExpiringContractManagementRepository noticeRepository) {
        this.contractRepository = contractRepository;
        this.noticeRepository = noticeRepository;
    }

    /** 만기 임박(D-30 이내) 계약 목록 */
    @Transactional(readOnly = true)
    public List<ExpiringContractSummaryResponse> getExpiringContracts() {
        return contractRepository.findAll().stream()
                .filter(Contract::isMaturityNear)
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    private Long parseId(String no) {
        try {
            return Long.parseLong(no.replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            throw ApiException.badRequest("유효하지 않은 번호: " + no);
        }
    }

    /** 안내 기록 저장 */
    @Transactional
    public NoticeResponse createNotice(String contractNo, NoticeCreateRequest req) {
        Contract contract = contractRepository.findById(parseId(contractNo));
        if (contract == null) {
            throw ApiException.notFound("계약을 찾을 수 없습니다: " + contractNo);
        }

        ExpiringContractManagement m = new ExpiringContractManagement();
        m.setContractNo(contractNo);
        m.setContractorName(contract.getCustomer() != null ? contract.getCustomer().getName() : null);
        m.setExpiryDate(contract.getExpiryDate());
        m.setPhone(req.phone());
        m.setEmail(req.email() != null && !req.email().isBlank() ? req.email() : null);
        m.setIsRenewable(req.isRenewable());
        m.setExpectedPremium(req.expectedPremium());
        m.setNoticeMemo(req.noticeMemo() != null && !req.noticeMemo().isBlank() ? req.noticeMemo() : null);
        m.setNoticeDate(LocalDateTime.now());

        noticeRepository.save(m);
        return toNoticeResponse(m);
    }

    /** 고객 응답 기록 — RENEWAL 시 contract.monthly_premium 업데이트도 같은 트랜잭션 */
    @Transactional
    public NoticeResponse recordResponse(String noticeNo, NoticeResponseRequest req) {
        ExpiringContractManagement m = noticeRepository.findById(parseId(noticeNo))
                .orElseThrow(() -> ApiException.notFound("안내 기록을 찾을 수 없습니다: " + noticeNo));

        CustomerResponse response;
        try {
            response = CustomerResponse.valueOf(req.customerResponse());
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("유효하지 않은 응답값입니다 (RENEWAL/TERMINATION/PENDING): " + req.customerResponse());
        }

        Long renewalPremium = null;
        Long premiumDiff    = null;

        if (response == CustomerResponse.RENEWAL) {
            if (req.renewalPremium() == null || req.renewalPremium() <= 0) {
                throw ApiException.badRequest("갱신 시 갱신 보험료를 입력해야 합니다.");
            }
            Contract contract = contractRepository.findById(parseId(m.getContractNo()));
            if (contract == null) {
                throw ApiException.notFound("연결된 계약을 찾을 수 없습니다: " + m.getContractNo());
            }
            renewalPremium = req.renewalPremium();
            premiumDiff    = renewalPremium - contract.getMonthlyPremium();
            contractRepository.updatePremium(contract.getId(), renewalPremium);
        }

        noticeRepository.updateResponse(m.getId(), response.name(), renewalPremium, premiumDiff);

        m.setCustomerResponse(response);
        m.setRenewalPremium(renewalPremium);
        m.setPremiumDiff(premiumDiff);
        return toNoticeResponse(m);
    }

    /** 안내 기록 목록 (contractNo 필터 선택) */
    @Transactional(readOnly = true)
    public List<NoticeResponse> getNotices(String contractNo) {
        List<ExpiringContractManagement> list = contractNo != null && !contractNo.isBlank()
                ? noticeRepository.findByContractNo(contractNo)
                : noticeRepository.findAll();
        return list.stream().map(this::toNoticeResponse).collect(Collectors.toList());
    }

    private ExpiringContractSummaryResponse toSummary(Contract c) {
        long remainingDays = c.getExpiryDate() != null
                ? ChronoUnit.DAYS.between(LocalDate.now(), c.getExpiryDate()) : -1;
        String name = c.getCustomer() != null ? c.getCustomer().getName() : null;
        String status = c.getStatus() != null ? c.getStatus().name() : "NORMAL";
        return new ExpiringContractSummaryResponse(
                c.getContractNo(), name, c.getInsuranceType(),
                c.getExpiryDate(), remainingDays, c.getMonthlyPremium(), status);
    }

    private NoticeResponse toNoticeResponse(ExpiringContractManagement m) {
        String cr = m.getCustomerResponse() != null ? m.getCustomerResponse().name() : null;
        long expected = m.getExpectedPremium() != null ? m.getExpectedPremium() : 0L;
        return new NoticeResponse(
                m.getNoticeNo(), m.getContractNo(), m.getContractorName(),
                m.getExpiryDate(), m.getPhone(), m.getEmail(),
                Boolean.TRUE.equals(m.getIsRenewable()), expected,
                m.getNoticeDate(), m.getNoticeMemo(),
                cr, m.getRenewalPremium(), m.getPremiumDiff());
    }
}