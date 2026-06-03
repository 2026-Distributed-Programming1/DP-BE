package org.dpbe.domain.customer.service;

import java.util.List;
import java.util.stream.Collectors;
import org.dpbe.domain.actor.Customer;
import org.dpbe.domain.customer.dto.CustomerDetailResponse;
import org.dpbe.domain.customer.dto.CustomerSummary;
import org.dpbe.domain.customer.repository.CustomerRepository;
import org.dpbe.global.auth.service.AuthAccessService;
import org.dpbe.global.dto.PageResponse;
import org.dpbe.global.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository repository;
    private final AuthAccessService authAccessService;

    public CustomerService(CustomerRepository repository, AuthAccessService authAccessService) {
        this.repository = repository;
        this.authAccessService = authAccessService;
    }

    public PageResponse<CustomerSummary> search(String keyword, int page, int size) {
        authAccessService.requireStaffOrAdmin();

        String normalizedKeyword = normalize(keyword);
        int normalizedPage = page < 1 ? 1 : page;
        int normalizedSize = size < 1 ? 20 : Math.min(size, 100);
        int offset = (normalizedPage - 1) * normalizedSize;

        int total = repository.countByKeyword(normalizedKeyword);
        List<Customer> customers = repository.findByKeyword(normalizedKeyword, normalizedSize, offset);
        List<CustomerSummary> items = customers.stream()
                .map(c -> new CustomerSummary(
                        c.getId(), c.getCustomerId(), c.getName(),
                        c.getContact(), c.getEmail()))
                .collect(Collectors.toList());

        return new PageResponse<>(normalizedPage, normalizedSize, total, items);
    }

    public CustomerDetailResponse detail(String customerId) {
        Customer c = repository.findById(customerId);
        if (c == null) {
            throw ApiException.notFound("고객을 찾을 수 없습니다: " + customerId);
        }
        authAccessService.requireCustomerAccess(c);

        return new CustomerDetailResponse(
                c.getId(), c.getCustomerId(), c.getName(),
                c.getContact(), c.getEmail(), c.getAddress(),
                c.getBirthDate(), c.getRegisteredAt());
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
