package org.dpbe.domain.claim.service;

import java.util.List;
import org.dpbe.domain.actor.Customer;
import org.dpbe.global.auth.service.AuthAccessService;
import org.dpbe.domain.claim.dto.ClaimCreateRequest;
import org.dpbe.domain.claim.dto.ClaimResponse;
import org.dpbe.domain.claim.entity.ClaimRequest;
import org.dpbe.domain.claim.repository.ClaimRequestRepository;
import org.dpbe.domain.common.enums.AuthMethod;
import org.dpbe.domain.common.enums.ClaimType;
import org.dpbe.domain.contract.entity.Contract;
import org.dpbe.domain.contract.repository.ContractRepository;
import org.dpbe.domain.customer.repository.CustomerRepository;
import org.dpbe.global.auth.dto.AuthenticatedUser;
import org.dpbe.global.dto.PageResponse;
import org.dpbe.global.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC '보험금을 요청한다' API 서비스.
 * 보험금 청구 규칙·검증을 처리한다(클라이언트 주도·무상태).
 * 본인인증·개인정보 동의는 요청 DTO 플래그로 받아 검증하며, authMethod는 저장하지 않는다(휘발).
 */
@Service
@Transactional(readOnly = true)
public class ClaimRequestService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final ClaimRequestRepository claimRequestRepository;
    private final CustomerRepository customerRepository;
    private final ContractRepository contractRepository;
    private final AuthAccessService authAccessService;

    public ClaimRequestService(ClaimRequestRepository claimRequestRepository,
                               CustomerRepository customerRepository,
                               ContractRepository contractRepository,
                               AuthAccessService authAccessService) {
        this.claimRequestRepository = claimRequestRepository;
        this.customerRepository = customerRepository;
        this.contractRepository = contractRepository;
        this.authAccessService = authAccessService;
    }

    private Long parseId(String claimNo) {
        try {
            return Long.parseLong(claimNo.replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            throw ApiException.notFound("유효하지 않은 청구번호: " + claimNo);
        }
    }

    private Long parseContractId(String contractNo) {
        try {
            return Long.parseLong(contractNo.replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            throw ApiException.notFound("유효하지 않은 계약번호: " + contractNo);
        }
    }

    public PageResponse<ClaimResponse> findAll(int page, int size) {
        AuthenticatedUser user = authAccessService.currentUser();
        String customerNo = authAccessService.isCustomer() ? user.linkedCustomerNo() : null;
        if (authAccessService.isCustomer() && customerNo == null) {
            return emptyPage(page, size);
        }

        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);
        int offset = (normalizedPage - 1) * normalizedSize;
        int total = claimRequestRepository.countByCustomerNo(customerNo);
        List<ClaimResponse> items = claimRequestRepository
                .findPageByCustomerNo(customerNo, normalizedSize, offset)
                .stream()
                .map(ClaimResponse::from)
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

    public ClaimResponse findByClaimNo(String claimNo) {
        ClaimRequest r = claimRequestRepository.findById(parseId(claimNo));
        if (r == null) {
            throw ApiException.notFound("청구를 찾을 수 없습니다: " + claimNo);
        }
        authAccessService.requireCustomerAccess(r.getCustomer());
        return ClaimResponse.from(r);
    }

    /** 청구 등록 — 검증 후 claim_requests에 저장(@Transactional). */
    @Transactional
    public ClaimResponse create(ClaimCreateRequest request) {
        Customer customer = customerRepository.findById(request.customerId());
        if (customer == null) {
            throw ApiException.notFound("고객을 찾을 수 없습니다: " + request.customerId());
        }
        authAccessService.requireCustomerAccess(customer);
        Contract contract = contractRepository.findById(parseContractId(request.contractNo()));
        if (contract == null) {
            throw ApiException.notFound("계약을 찾을 수 없습니다: " + request.contractNo());
        }
        authAccessService.requireContractAccess(contract);
        if (request.claimReasons() == null || request.claimReasons().isEmpty()) {
            throw ApiException.badRequest("청구 사유를 1건 이상 선택해야 합니다.");
        }

        ClaimRequest claim = new ClaimRequest(customer, contract);

        // 개인정보 동의 (필수)
        if (!request.personalInfoAgreed()) {
            throw ApiException.badRequest("개인정보 수집·이용 동의가 필요합니다.");
        }
        claim.agreePersonalInfoTerms();

        // 본인인증 (검증용 — authMethod는 저장하지 않음)
        claim.selectAuthMethod(parseAuthMethod(request.authMethod()));
        if (!claim.authenticate()) {
            throw ApiException.badRequest("본인인증에 실패했습니다.");
        }

        claim.selectClaimType(parseClaimType(request.claimType()));
        claim.selectClaimReasons(request.claimReasons());
        if (request.diagnosis() != null) {
            claim.enterDiagnosis(request.diagnosis());
        }

        // 계좌 입력 + 인증 (E1)
        claim.registerNewAccount(request.bankName(), request.accountNo());
        if (!claim.verifyAccount()) {
            throw ApiException.badRequest("계좌 인증에 실패했습니다.");
        }

        if (!claim.validateBeforeSubmit()) {
            throw ApiException.badRequest("필수 입력값이 누락되어 청구를 제출할 수 없습니다.");
        }
        claim.submit();
        claimRequestRepository.save(claim);
        return ClaimResponse.from(claim);
    }

    private AuthMethod parseAuthMethod(String method) {
        if (method == null) {
            throw ApiException.badRequest("본인인증 방법을 선택해야 합니다.");
        }
        try {
            return AuthMethod.valueOf(method);
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("알 수 없는 인증 방법입니다: " + method);
        }
    }

    private ClaimType parseClaimType(String type) {
        if (type == null) {
            throw ApiException.badRequest("청구 유형을 선택해야 합니다.");
        }
        try {
            return ClaimType.valueOf(type);
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("알 수 없는 청구 유형입니다: " + type);
        }
    }
}
