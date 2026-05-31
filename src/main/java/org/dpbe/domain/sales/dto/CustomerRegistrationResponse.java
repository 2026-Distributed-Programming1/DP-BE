package org.dpbe.domain.sales.dto;

import java.time.LocalDate;
import org.dpbe.domain.sales.entity.CustomerRegistration;

public record CustomerRegistrationResponse(
        Long id,
        String customerId,
        String name,
        String maskedSsn,
        String phone,
        String address,
        String insuranceType,
        LocalDate contractDate,
        LocalDate expiryDate,
        Long monthlyPremium
) {
    public static CustomerRegistrationResponse from(CustomerRegistration r) {
        return new CustomerRegistrationResponse(
                r.getId(),
                r.getCustomerId(),
                r.getName(),
                r.getSsnMasked() != null ? r.getSsnMasked() : r.getMaskedSsn(),
                r.getPhone(),
                r.getAddress(),
                r.getInsuranceType() != null ? r.getInsuranceType().name() : null,
                r.getContractDate(),
                r.getExpiryDate(),
                r.getMonthlyPremium());
    }
}