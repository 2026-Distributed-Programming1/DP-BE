package org.dpbe.domain.consultation.dto;

import java.time.LocalDateTime;
import org.dpbe.domain.consultation.entity.Proposal;

public record ProposalResponse(
        String proposalNo,
        String customerName,
        String productName,
        long monthlyPremium,
        LocalDateTime sentAt
) {
    public static ProposalResponse from(Proposal p) {
        String productName = p.getInsuranceProduct() != null ? p.getInsuranceProduct().getProductName() : null;
        long premium = p.getInsuranceProduct() != null ? p.getInsuranceProduct().getMonthlyPremium() : 0L;
        return new ProposalResponse(p.getProposalNo(), p.getCustomerName(), productName, premium, p.getSentAt());
    }
}