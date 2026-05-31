package org.dpbe.domain.consultation.service;

import org.dpbe.domain.actor.Customer;
import org.dpbe.domain.consultation.dto.PolicyApplicationRequest;
import org.dpbe.domain.consultation.dto.PolicyApplicationResponse;
import org.dpbe.domain.consultation.entity.PolicyApplication;
import org.dpbe.domain.consultation.repository.InsuranceProductRepository;
import org.dpbe.domain.consultation.repository.PolicyApplicationRepository;
import org.dpbe.domain.customer.repository.CustomerRepository;
import org.dpbe.global.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PolicyApplicationService {

    private final PolicyApplicationRepository policyAppRepo;
    private final InsuranceProductRepository productRepo;
    private final CustomerRepository customerRepo;

    public PolicyApplicationService(PolicyApplicationRepository policyAppRepo,
                                    InsuranceProductRepository productRepo,
                                    CustomerRepository customerRepo) {
        this.policyAppRepo = policyAppRepo;
        this.productRepo = productRepo;
        this.customerRepo = customerRepo;
    }

    /** 청약서 제출. */
    @Transactional
    public PolicyApplicationResponse create(PolicyApplicationRequest req) {
        if (req.customerId() == null || req.customerId().isBlank())
            throw ApiException.badRequest("고객 ID는 필수입니다.");
        if (req.productName() == null || req.productName().isBlank())
            throw ApiException.badRequest("상품명은 필수입니다.");
        if (req.period() <= 0)
            throw ApiException.badRequest("보험기간은 1년 이상이어야 합니다.");
        if (req.paymentMethod() == null || req.paymentMethod().isBlank())
            throw ApiException.badRequest("납입방법은 필수입니다.");

        Customer customer = customerRepo.findById(req.customerId());
        if (customer == null)
            throw ApiException.notFound("고객을 찾을 수 없습니다: " + req.customerId());

        if (productRepo.findByProductName(req.productName()) == null)
            throw ApiException.notFound("보험상품을 찾을 수 없습니다: " + req.productName());

        PolicyApplication p = new PolicyApplication();
        p.setCustomer(customer);
        p.selectProduct(req.productName(), req.period(), req.paymentMethod());
        p.setUploadedAt(req.uploadedAt());
        p.submit();
        p.setStatus("신청");
        policyAppRepo.save(p);
        return PolicyApplicationResponse.from(p);
    }
}