package org.dpbe.global.auth.dto;

public record PasswordChangeRequest(
        String currentPassword,
        String newPassword
) {
}
