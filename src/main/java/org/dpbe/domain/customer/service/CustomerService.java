package org.dpbe.domain.customer.service;

import java.util.List;
import java.util.stream.Collectors;
import org.dpbe.domain.actor.Customer;
import org.dpbe.domain.customer.dto.CustomerDetailResponse;
import org.dpbe.domain.customer.dto.CustomerListResponse;
import org.dpbe.domain.customer.dto.CustomerSummary;
import org.dpbe.domain.customer.repository.CustomerRepository;
import org.dpbe.global.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository repository;

    public CustomerService(CustomerRepository repository) {
        this.repository = repository;
    }

    public CustomerListResponse search(String keyword, int page, int size) {
        if (page < 1) page = 1;
        if (size < 1) size = 20;

        List<Customer> all = (keyword == null || keyword.isBlank())
                ? repository.findAll()
                : repository.findByKeyword(keyword);

        int total = all.size();
        int from = Math.min((page - 1) * size, total);
        int to = Math.min(from + size, total);

        List<CustomerSummary> items = all.subList(from, to).stream()
                .map(c -> new CustomerSummary(
                        c.getId(), c.getCustomerId(), c.getName(),
                        c.getContact(), c.getEmail()))
                .collect(Collectors.toList());

        return new CustomerListResponse(page, size, total, items);
    }

    public CustomerDetailResponse detail(String customerId) {
        Customer c = repository.findById(customerId);
        if (c == null) {
            throw ApiException.notFound("고객을 찾을 수 없습니다: " + customerId);
        }
        return new CustomerDetailResponse(
                c.getId(), c.getCustomerId(), c.getName(),
                c.getContact(), c.getEmail(), c.getAddress(),
                c.getBirthDate(), c.getRegisteredAt());
    }
}
