package org.dpbe.domain.consultation.service;

import java.util.List;
import java.util.stream.Collectors;
import org.dpbe.domain.consultation.dto.ProposalCreateRequest;
import org.dpbe.domain.consultation.dto.ProposalResponse;
import org.dpbe.domain.consultation.entity.InsuranceProduct;
import org.dpbe.domain.consultation.entity.Proposal;
import org.dpbe.domain.consultation.repository.InsuranceProductRepository;
import org.dpbe.domain.consultation.repository.ProposalRepository;
import org.dpbe.global.auth.service.AuthAccessService;
import org.dpbe.global.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProposalService {

    private final ProposalRepository proposalRepo;
    private final InsuranceProductRepository productRepo;
    private final AuthAccessService authAccessService;

    public ProposalService(ProposalRepository proposalRepo,
                           InsuranceProductRepository productRepo,
                           AuthAccessService authAccessService) {
        this.proposalRepo = proposalRepo;
        this.productRepo = productRepo;
        this.authAccessService = authAccessService;
    }

    public List<ProposalResponse> findAll() {
        authAccessService.requireProposalManageAccess();
        return proposalRepo.findAll().stream()
                .map(ProposalResponse::from)
                .collect(Collectors.toList());
    }

    /** 제안서 발송 — 상품 존재 확인 후 저장. */
    @Transactional
    public ProposalResponse create(ProposalCreateRequest req) {
        authAccessService.requireProposalManageAccess();
        if (req.customerName() == null || req.customerName().isBlank())
            throw ApiException.badRequest("고객명은 필수입니다.");
        if (req.productName() == null || req.productName().isBlank())
            throw ApiException.badRequest("상품명은 필수입니다.");

        InsuranceProduct product = productRepo.findByProductName(req.productName());
        if (product == null)
            throw ApiException.notFound("보험상품을 찾을 수 없습니다: " + req.productName());

        Proposal proposal = new Proposal();
        proposal.setCustomerName(req.customerName());
        proposal.selectProduct(product);
        proposal.send();
        proposalRepo.save(proposal);
        return ProposalResponse.from(proposal);
    }
}
