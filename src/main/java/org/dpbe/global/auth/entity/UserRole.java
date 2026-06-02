package org.dpbe.global.auth.entity;

public enum UserRole {
    CUSTOMER,
    STAFF,
    CONTRACT_STAFF,
    CLAIM_STAFF,
    UNDERWRITING_STAFF,
    SALES_STAFF,
    EDUCATION_STAFF,
    FINANCE_STAFF,
    DISPATCH_STAFF,
    ADMIN;

    public boolean isStaffLike() {
        return this == STAFF
                || this == CONTRACT_STAFF
                || this == CLAIM_STAFF
                || this == UNDERWRITING_STAFF
                || this == SALES_STAFF
                || this == EDUCATION_STAFF
                || this == FINANCE_STAFF
                || this == DISPATCH_STAFF;
    }
}
