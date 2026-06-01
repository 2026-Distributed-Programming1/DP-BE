package org.dpbe.global.auth.dto;

public record CustomerAccountCreateResponse(
        AuthUserResponse user,
        String temporaryPassword
) {
}
