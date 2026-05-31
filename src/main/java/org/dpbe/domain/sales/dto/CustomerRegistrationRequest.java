package org.dpbe.domain.sales.dto;

import java.time.LocalDate;

public record CustomerRegistrationRequest(
        String name,
        String ssn,
        String phone,
        String address,
        String insuranceType,
        LocalDate contractDate,
        LocalDate expiryDate,
        Long monthlyPremium
) {}