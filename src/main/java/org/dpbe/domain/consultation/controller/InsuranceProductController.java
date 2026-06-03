package org.dpbe.domain.consultation.controller;

import org.dpbe.domain.consultation.dto.InsuranceProductResponse;
import org.dpbe.domain.consultation.service.InsuranceProductService;
import org.dpbe.global.dto.ItemsResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/insurance-products")
public class InsuranceProductController {

    private final InsuranceProductService productService;

    public InsuranceProductController(InsuranceProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ItemsResponse<InsuranceProductResponse> findAll() {
        return new ItemsResponse<>(productService.findAll());
    }
}
