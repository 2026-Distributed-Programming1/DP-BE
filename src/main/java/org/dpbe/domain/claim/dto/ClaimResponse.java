package org.dpbe.domain.claim.dto;

import java.time.LocalDateTime;
import java.util.List;
import org.dpbe.domain.claim.entity.ClaimRequest;

/** 보험금 청구 조회/등록 결과 */
public record ClaimResponse(
        String claimNo,
        String customerId,
        String customerName,
        String contractNo,
        String claimType,
        List<String> claimReasons,
        String diagnosis,
        String bankName,
        String accountNo,
        String accountHolder,
        boolean personalInfoAgreed,
        LocalDateTime requestedAt,
        String status
) {
    public static ClaimResponse from(ClaimRequest r) {
        var account = r.getBankAccount();
        var customer = r.getCustomer();
        var contract = r.getContract();
        return new ClaimResponse(
                r.getClaimNo(),
                customer != null ? customer.getCustomerId() : null,
                customer != null ? customer.getName() : null,
                contract != null ? contract.getContractNo() : null,
                r.getClaimType() != null ? r.getClaimType().name() : null,
                r.getClaimReasons(),
                r.getDiagnosis(),
                account != null ? account.getBankName() : null,
                account != null ? account.getAccountNo() : null,
                account != null ? account.getAccountHolder() : null,
                r.isPersonalInfoAgreed(),
                r.getRequestedAt(),
                r.getStatus() != null ? r.getStatus().name() : null);
    }
}