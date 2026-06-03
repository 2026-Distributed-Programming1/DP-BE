package org.dpbe.domain.common.enums;

public enum ApplicationType {
    POLICY("청약"),
    INSURANCE("보험신청");

    private final String label;

    ApplicationType(String label) { this.label = label; }

    public String getLabel() { return label; }
}