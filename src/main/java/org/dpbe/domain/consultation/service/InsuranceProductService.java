package org.dpbe.domain.consultation.service;

import java.util.List;
import java.util.stream.Collectors;
import org.dpbe.domain.consultation.dto.InsuranceProductResponse;
import org.dpbe.domain.consultation.repository.InsuranceProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class InsuranceProductService {

    private final InsuranceProductRepository productRepo;

    public InsuranceProductService(InsuranceProductRepository productRepo) {
        this.productRepo = productRepo;
    }

    public List<InsuranceProductResponse> findAll() {
        return productRepo.findAll().stream()
                .map(InsuranceProductResponse::from)
                .collect(Collectors.toList());
    }
}