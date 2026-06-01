package org.dpbe.global.auth.dto;

public record CustomerSignupResponse(
        String customerId,
        String username,
        String message
) {
}
