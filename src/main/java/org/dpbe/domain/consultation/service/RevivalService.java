package org.dpbe.domain.consultation.service;

import java.util.List;
import org.dpbe.domain.actor.Customer;
import org.dpbe.global.auth.service.AuthAccessService;
import org.dpbe.domain.consultation.dto.RevivalRequest;
import org.dpbe.domain.consultation.dto.RevivalResponse;
import org.dpbe.global.dto.PageResponse;
import org.dpbe.domain.consultation.entity.Revival;
import org.dpbe.domain.consultation.repository.RevivalRepository;
import org.dpbe.domain.contract.repository.ContractRepository;
import org.dpbe.domain.customer.repository.CustomerRepository;
import org.dpbe.global.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RevivalService {

    private final RevivalRepository revivalRepo;
    private final ContractRepository contractRepo;
    private final CustomerRepository customerRepo;
    private final AuthAccessService authAccessService;

    public RevivalService(RevivalRepository revivalRepo,
                          ContractRepository contractRepo,
                          CustomerRepository customerRepo,
                          AuthAccessService authAccessService) {
        this.revivalRepo = revivalRepo;
        this.contractRepo = contractRepo;
        this.customerRepo = customerRepo;
        this.authAccessService = authAccessService;
    }

    public PageResponse<RevivalResponse> findAll(int page, int size) {
        authAccessService.requireConsultationManageAccess();
        int total = revivalRepo.countAll();
        int offset = (page - 1) * size;
        List<RevivalResponse> items = revivalRepo.findPage(size, offset).stream()
                .map(RevivalResponse::from).toList();
        return new PageResponse<>(page, size, total, items);
    }

    public RevivalResponse findByNo(String revivalNo) {
        authAccessService.requireConsultationManageAccess();
        var r = revivalRepo.findByNo(revivalNo);
        if (r == null) throw org.dpbe.global.exception.ApiException.notFound("부활신청을 찾을 수 없습니다: " + revivalNo);
        return RevivalResponse.from(r);
    }

    private Long parseContractId(String contractNo) {
        try {
            return Long.parseLong(contractNo.replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            throw ApiException.notFound("유효하지 않은 계약번호: " + contractNo);
        }
    }

    /** 부활 신청 — 계약 존재 확인 후 저장. */
    @Transactional
    public RevivalResponse create(RevivalRequest req) {
        if (req.customerId() == null || req.customerId().isBlank())
            throw ApiException.badRequest("고객 ID는 필수입니다.");
        if (req.contractNo() == null || req.contractNo().isBlank())
            throw ApiException.badRequest("계약번호는 필수입니다.");
        if (req.contact() == null || req.contact().isBlank())
            throw ApiException.badRequest("연락처는 필수입니다.");
        if (req.paymentMethod() == null || req.paymentMethod().isBlank())
            throw ApiException.badRequest("납입방법은 필수입니다.");

        Customer customer = customerRepo.findById(req.customerId());
        if (customer == null)
            throw ApiException.notFound("고객을 찾을 수 없습니다: " + req.customerId());
        authAccessService.requireCustomerAccess(customer);

        var contract = contractRepo.findById(parseContractId(req.contractNo()));
        if (contract == null)
            throw ApiException.notFound("계약을 찾을 수 없습니다: " + req.contractNo());
        authAccessService.requireContractAccess(contract);

        Revival r = new Revival();
        r.setCustomer(customer);
        r.setContractNo(req.contractNo());
        r.setContact(req.contact());
        r.setUnpaidAmount(req.unpaidAmount());
        r.pay(req.paymentMethod());
        r.submit();
        revivalRepo.save(r);
        return RevivalResponse.from(r);
    }
}
