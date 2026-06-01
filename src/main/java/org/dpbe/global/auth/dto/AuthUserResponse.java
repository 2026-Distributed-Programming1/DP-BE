package org.dpbe.global.auth.dto;

import org.dpbe.global.auth.entity.UserRole;

public record AuthUserResponse(
        Long id,
        String username,
        UserRole role,
        Long linkedCustomerId,
        String linkedCustomerNo,
        String displayName,
        boolean passwordChangeRequired
) {

    public static AuthUserResponse from(AuthenticatedUser user) {
        return new AuthUserResponse(
                user.id(),
                user.username(),
                user.role(),
                user.linkedCustomerId(),
                user.linkedCustomerNo(),
                user.displayName(),
                user.passwordChangeRequired());
    }
}
