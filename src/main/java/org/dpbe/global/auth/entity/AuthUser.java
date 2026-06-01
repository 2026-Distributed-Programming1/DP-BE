package org.dpbe.global.auth.entity;

import java.time.LocalDateTime;

public class AuthUser {

    private final Long id;
    private final String username;
    private final String passwordHash;
    private final UserRole role;
    private final Long linkedCustomerId;
    private final String linkedCustomerNo;
    private final String displayName;
    private final boolean enabled;
    private final boolean passwordChangeRequired;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public AuthUser(Long id,
                    String username,
                    String passwordHash,
                    UserRole role,
                    Long linkedCustomerId,
                    String linkedCustomerNo,
                    String displayName,
                    boolean enabled,
                    boolean passwordChangeRequired,
                    LocalDateTime createdAt,
                    LocalDateTime updatedAt) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.linkedCustomerId = linkedCustomerId;
        this.linkedCustomerNo = linkedCustomerNo;
        this.displayName = displayName;
        this.enabled = enabled;
        this.passwordChangeRequired = passwordChangeRequired;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserRole getRole() {
        return role;
    }

    public Long getLinkedCustomerId() {
        return linkedCustomerId;
    }

    public String getLinkedCustomerNo() {
        return linkedCustomerNo;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isPasswordChangeRequired() {
        return passwordChangeRequired;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
