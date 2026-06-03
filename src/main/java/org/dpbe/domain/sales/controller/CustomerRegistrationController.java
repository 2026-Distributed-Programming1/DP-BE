package org.dpbe.domain.sales.controller;

import org.dpbe.domain.sales.dto.CustomerRegistrationRequest;
import org.dpbe.domain.sales.dto.CustomerRegistrationResponse;
import org.dpbe.domain.sales.service.CustomerRegistrationService;
import org.dpbe.global.dto.PageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer-registrations")
public class CustomerRegistrationController {

    private final CustomerRegistrationService service;

    public CustomerRegistrationController(CustomerRegistrationService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<CustomerRegistrationResponse> findAll(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.findAll(page, size);
    }

    @PostMapping
    public ResponseEntity<CustomerRegistrationResponse> register(
            @RequestBody CustomerRegistrationRequest request) {
        return ResponseEntity.status(201).body(service.register(request));
    }
}
