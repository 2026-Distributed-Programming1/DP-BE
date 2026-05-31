package org.dpbe.domain.consultation.dto;

public record UnderwritingRequest(
        String applicationType,
        String appNo,
        String customerName,
        String reviewType,
        String reviewOpinion,
        String riskGrade,
        String result,
        String resultCondition,
        String rejectionReason
) {}