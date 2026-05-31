package org.dpbe.domain.consultation.dto;

public record ProposalCreateRequest(
        String customerName,
        String productName
) {}