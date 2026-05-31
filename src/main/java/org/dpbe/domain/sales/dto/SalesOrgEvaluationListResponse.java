package org.dpbe.domain.sales.dto;

import java.util.List;

public record SalesOrgEvaluationListResponse(
        int page,
        int size,
        int total,
        List<SalesOrgEvaluationResponse> items
) {}
