package org.dpbe.domain.consultation.service;

import org.dpbe.domain.actor.Customer;
import org.dpbe.domain.consultation.dto.InsuranceApplicationRequest;
import org.dpbe.domain.consultation.dto.InsuranceApplicationResponse;
import org.dpbe.domain.consultation.entity.InsuranceApplication;
import org.dpbe.domain.consultation.entity.InsuranceProduct;
import org.dpbe.domain.consultation.repository.InsuranceApplicationRepository;
import org.dpbe.domain.consultation.repository.InsuranceProductRepository;
import org.dpbe.domain.customer.repository.CustomerRepository;
import org.dpbe.global.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class InsuranceApplicationService {

    private final InsuranceApplicationRepository appRepo;
    private final InsuranceProductRepository productRepo;
    private final CustomerRepository customerRepo;

    public InsuranceApplicationService(InsuranceApplicationRepository appRepo,
                                       InsuranceProductRepository productRepo,
                                       CustomerRepository customerRepo) {
        this.appRepo = appRepo;
        this.productRepo = productRepo;
        this.customerRepo = customerRepo;
    }

    /** 보험 가입 신청. */
    @Transactional
    public InsuranceApplicationResponse create(InsuranceApplicationRequest req) {
        if (req.customerId() == null || req.customerId().isBlank())
            throw ApiException.badRequest("고객 ID는 필수입니다.");
        if (req.productName() == null || req.productName().isBlank())
            throw ApiException.badRequest("상품명은 필수입니다.");
        if (req.paymentMethod() == null || req.paymentMethod().isBlank())
            throw ApiException.badRequest("납입방법은 필수입니다.");

        Customer customer = customerRepo.findById(req.customerId());
        if (customer == null)
            throw ApiException.notFound("고객을 찾을 수 없습니다: " + req.customerId());

        InsuranceProduct product = productRepo.findByProductName(req.productName());
        if (product == null)
            throw ApiException.notFound("보험상품을 찾을 수 없습니다: " + req.productName());

        InsuranceApplication a = new InsuranceApplication();
        a.setCustomer(customer);
        a.setProduct(product);
        a.selectPaymentMethod(req.paymentMethod());
        a.apply();
        a.setStatus("신청");
        appRepo.save(a);
        return InsuranceApplicationResponse.from(a);
    }
}