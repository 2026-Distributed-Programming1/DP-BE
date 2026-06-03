package org.dpbe.domain.consultation.dto;

import org.dpbe.domain.common.enums.ApplicationType;

public record UnderwritingRequest(
        ApplicationType applicationType,
        String appNo,
        String customerName,
        String reviewType,
        String reviewOpinion,
        String riskGrade,
        String result,
        String resultCondition,
        String rejectionReason
) {}