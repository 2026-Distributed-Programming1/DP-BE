package org.dpbe.domain.consultation.service;

import org.dpbe.domain.consultation.dto.ProposalCreateRequest;
import org.dpbe.domain.consultation.dto.ProposalResponse;
import org.dpbe.domain.consultation.entity.InsuranceProduct;
import org.dpbe.domain.consultation.entity.Proposal;
import org.dpbe.domain.consultation.repository.InsuranceProductRepository;
import org.dpbe.domain.consultation.repository.ProposalRepository;
import org.dpbe.global.auth.service.AuthAccessService;
import org.dpbe.global.dto.PageResponse;
import org.dpbe.global.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProposalService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

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

    public PageResponse<ProposalResponse> findAll(int page, int size) {
        authAccessService.requireProposalManageAccess();
        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);
        int offset = (normalizedPage - 1) * normalizedSize;
        int total = proposalRepo.countAll();
        var items = proposalRepo.findPage(normalizedSize, offset).stream()
                .map(ProposalResponse::from)
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
