package com.meetruly.core.constant;

import lombok.Getter;

@Getter
public enum UserRole {
    ADMIN("Administrator"),
    USER("User");

    private final String displayValue;

    UserRole(String displayValue) {
        this.displayValue = displayValue;
    }

}
