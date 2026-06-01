package org.dpbe.global.auth.dto;

public record LoginRequest(
        String username,
        String password
) {
}
