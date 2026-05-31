package org.dpbe.domain.sales.controller;

import java.util.List;
import org.dpbe.domain.sales.dto.CustomerRegistrationRequest;
import org.dpbe.domain.sales.dto.CustomerRegistrationResponse;
import org.dpbe.domain.sales.service.CustomerRegistrationService;
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
    public List<CustomerRegistrationResponse> findAll() {
        return service.findAll();
    }

    @PostMapping
    public ResponseEntity<CustomerRegistrationResponse> register(
            @RequestBody CustomerRegistrationRequest request) {
        return ResponseEntity.status(201).body(service.register(request));
    }
}