package com.meetruly.core.constant;

import lombok.Getter;

@Getter
public enum RelationshipType {
    DATING("Dating"),
    LONG_TERM("Long-term relationship"),
    MARRIAGE("Marriage"),
    FRIENDSHIP("Friendship"),
    CASUAL("Casual");

    private final String displayName;

    RelationshipType(String displayName) {
        this.displayName = displayName;
    }

}