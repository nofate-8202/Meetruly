package com.meetruly.core.constant;

import lombok.Getter;

@Getter
public enum ReportStatus {
    PENDING("Pending"),
    REVIEWED("Reviewed"),
    RESOLVED("Resolved"),
    DISMISSED("Dismissed");

    private final String displayName;

    ReportStatus(String displayName) {
        this.displayName = displayName;
    }
}
