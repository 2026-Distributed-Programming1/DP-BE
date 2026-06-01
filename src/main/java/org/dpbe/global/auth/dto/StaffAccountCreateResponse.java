package org.dpbe.global.auth.dto;

public record StaffAccountCreateResponse(
        AuthUserResponse user,
        String temporaryPassword
) {
}
