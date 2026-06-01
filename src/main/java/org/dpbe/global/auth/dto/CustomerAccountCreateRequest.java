package org.dpbe.global.auth.dto;

public record CustomerAccountCreateRequest(
        String customerId,
        String username
) {
}
