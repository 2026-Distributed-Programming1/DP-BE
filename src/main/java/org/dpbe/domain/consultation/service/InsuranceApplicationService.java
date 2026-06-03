package org.dpbe.domain.consultation.service;

import java.util.List;
import org.dpbe.domain.actor.Customer;
import org.dpbe.global.auth.service.AuthAccessService;
import org.dpbe.domain.consultation.dto.InsuranceApplicationRequest;
import org.dpbe.domain.consultation.dto.InsuranceApplicationResponse;
import org.dpbe.global.dto.PageResponse;
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
    private final AuthAccessService authAccessService;

    public InsuranceApplicationService(InsuranceApplicationRepository appRepo,
                                       InsuranceProductRepository productRepo,
                                       CustomerRepository customerRepo,
                                       AuthAccessService authAccessService) {
        this.appRepo = appRepo;
        this.productRepo = productRepo;
        this.customerRepo = customerRepo;
        this.authAccessService = authAccessService;
    }

    public PageResponse<InsuranceApplicationResponse> findAll(int page, int size) {
        authAccessService.requireConsultationManageAccess();
        int total = appRepo.countAll();
        int offset = (page - 1) * size;
        List<InsuranceApplicationResponse> items = appRepo.findPage(size, offset).stream()
                .map(InsuranceApplicationResponse::from).toList();
        return new PageResponse<>(page, size, total, items);
    }

    public InsuranceApplicationResponse findByNo(String applicationNo) {
        authAccessService.requireConsultationManageAccess();
        var a = appRepo.findByNo(applicationNo);
        if (a == null) throw org.dpbe.global.exception.ApiException.notFound("청약신청을 찾을 수 없습니다: " + applicationNo);
        return InsuranceApplicationResponse.from(a);
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
        authAccessService.requireCustomerAccess(customer);

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
