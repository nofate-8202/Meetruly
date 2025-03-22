package com.meetruly.core.constant;

import lombok.Getter;

@Getter

public enum RelationshipStatus {
    SINGLE("Single"),
    DIVORCED("Divorced"),
    WIDOWED("Widowed"),
    SEPARATED("Separated"),
    COMPLICATED("It's complicated"),
    OPEN_RELATIONSHIP("Open relationship");

    private final String displayName;

    RelationshipStatus(String displayName) {
        this.displayName = displayName;
    }


}