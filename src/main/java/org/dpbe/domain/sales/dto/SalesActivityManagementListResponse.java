package org.dpbe.domain.sales.dto;

import java.util.List;

public record SalesActivityManagementListResponse(
        int page,
        int size,
        int total,
        List<SalesActivityManagementResponse> items
) {}
