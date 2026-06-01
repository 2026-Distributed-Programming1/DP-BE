package org.dpbe.global.auth.dto;

import java.io.Serializable;
import org.dpbe.global.auth.entity.AuthUser;
import org.dpbe.global.auth.entity.UserRole;

public record AuthenticatedUser(
        Long id,
        String username,
        UserRole role,
        Long linkedCustomerId,
        String linkedCustomerNo,
        String displayName,
        boolean passwordChangeRequired
) implements Serializable {

    public static AuthenticatedUser from(AuthUser user) {
        return new AuthenticatedUser(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getLinkedCustomerId(),
                user.getLinkedCustomerNo(),
                user.getDisplayName(),
                user.isPasswordChangeRequired());
    }
}
