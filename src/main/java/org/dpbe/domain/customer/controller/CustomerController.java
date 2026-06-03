package org.dpbe.domain.customer.controller;

import org.dpbe.domain.customer.dto.CustomerDetailResponse;
import org.dpbe.domain.customer.dto.CustomerSummary;
import org.dpbe.domain.customer.service.CustomerService;
import org.dpbe.global.dto.PageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService service;

    public CustomerController(CustomerService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<CustomerSummary> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.search(keyword, page, size);
    }

    @GetMapping("/{customerId}")
    public CustomerDetailResponse detail(@PathVariable String customerId) {
        return service.detail(customerId);
    }
}
