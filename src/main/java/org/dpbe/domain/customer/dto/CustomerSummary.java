package org.dpbe.domain.customer.dto;

public record CustomerSummary(
        Long id,
        String customerId,
        String name,
        String phone,
        String email
) {}
