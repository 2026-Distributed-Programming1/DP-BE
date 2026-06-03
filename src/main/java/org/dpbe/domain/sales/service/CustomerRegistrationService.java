package org.dpbe.domain.sales.service;

import java.util.List;
import java.util.stream.Collectors;
import org.dpbe.domain.common.enums.InsuranceType;
import org.dpbe.domain.sales.dto.CustomerRegistrationRequest;
import org.dpbe.domain.sales.dto.CustomerRegistrationResponse;
import org.dpbe.domain.sales.entity.CustomerRegistration;
import org.dpbe.domain.sales.repository.CustomerRegistrationRepository;
import org.dpbe.global.auth.service.AuthAccessService;
import org.dpbe.global.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerRegistrationService {

    private final CustomerRegistrationRepository repository;
    private final AuthAccessService authAccessService;

    public CustomerRegistrationService(CustomerRegistrationRepository repository,
                                       AuthAccessService authAccessService) {
        this.repository = repository;
        this.authAccessService = authAccessService;
    }

    @Transactional(readOnly = true)
    public List<CustomerRegistrationResponse> findAll() {
        authAccessService.requireSalesOperationAccess();
        return repository.findAll().stream()
                .map(CustomerRegistrationResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public CustomerRegistrationResponse register(CustomerRegistrationRequest request) {
        authAccessService.requireSalesOperationAccess();
        if (request.name() == null || request.ssn() == null || request.phone() == null
                || request.insuranceType() == null || request.contractDate() == null
                || request.expiryDate() == null || request.monthlyPremium() == null) {
            throw ApiException.badRequest("필수 항목 누락: name, ssn, phone, insuranceType, contractDate, expiryDate, monthlyPremium");
        }
        if (request.monthlyPremium() <= 0) {
            throw ApiException.badRequest("월 보험료는 0보다 커야 합니다.");
        }

        CustomerRegistration r = new CustomerRegistration();
        r.setName(request.name());
        r.setSsn(request.ssn().replaceAll("-", ""));
        r.setPhone(request.phone());
        r.setAddress(request.address());
        try {
            r.setInsuranceType(InsuranceType.valueOf(request.insuranceType()));
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("유효하지 않은 insuranceType: " + request.insuranceType());
        }
        r.setContractDate(request.contractDate());
        r.setExpiryDate(request.expiryDate());
        r.setMonthlyPremium(request.monthlyPremium());

        repository.save(r);
        return CustomerRegistrationResponse.from(r);
    }
}
