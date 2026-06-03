package org.dpbe.domain.consultation.dto;

import java.time.LocalDateTime;
import org.dpbe.domain.common.enums.ApplicationType;
import org.dpbe.domain.consultation.entity.Underwriting;

public record UnderwritingResponse(
        String underwritingNo,
        String appNo,
        ApplicationType applicationType,
        String customerName,
        String reviewType,
        String riskGrade,
        String reviewOpinion,
        String result,
        String resultCondition,
        String rejectionReason,
        LocalDateTime reviewedAt
) {
    public static UnderwritingResponse from(Underwriting u) {
        String result = u.getReviewResult() != null ? u.getReviewResult().getResult() : null;
        String condition = u.getReviewResult() != null ? u.getReviewResult().getCondition() : null;
        String rejection = u.getReviewResult() != null ? u.getReviewResult().getRejectionReason() : null;
        return new UnderwritingResponse(
                u.getUnderwritingNo(), u.getAppNo(), u.getApplicationType(), u.getCustomerName(),
                u.getReviewType(), u.getRiskGrade(), u.getReviewOpinion(),
                result, condition, rejection, u.getReviewedAt());
    }
}