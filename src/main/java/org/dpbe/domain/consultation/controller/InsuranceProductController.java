package org.dpbe.domain.consultation.controller;

import java.util.List;
import org.dpbe.domain.consultation.dto.InsuranceProductResponse;
import org.dpbe.domain.consultation.service.InsuranceProductService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/insurance-products")
public class InsuranceProductController {

    private final InsuranceProductService productService;

    public InsuranceProductController(InsuranceProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<InsuranceProductResponse> findAll() {
        return productService.findAll();
    }
}