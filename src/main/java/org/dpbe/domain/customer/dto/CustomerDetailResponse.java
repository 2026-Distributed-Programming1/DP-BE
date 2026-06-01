package org.dpbe.domain.customer.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CustomerDetailResponse(
        Long id,
        String customerId,
        String name,
        String phone,
        String email,
        String address,
        LocalDate birthDate,
        LocalDateTime registeredAt
) {}
