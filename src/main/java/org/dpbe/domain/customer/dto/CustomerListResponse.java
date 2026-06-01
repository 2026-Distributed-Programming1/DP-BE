package org.dpbe.domain.customer.dto;

import java.util.List;

public record CustomerListResponse(
        int page,
        int size,
        int total,
        List<CustomerSummary> items
) {}
