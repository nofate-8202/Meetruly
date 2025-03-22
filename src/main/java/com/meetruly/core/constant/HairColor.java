package com.meetruly.core.constant;

import lombok.Getter;

@Getter

public enum HairColor {
    BLACK("Black"),
    BROWN("Brown"),
    BLONDE("Blonde"),
    RED("Red"),
    GINGER("Ginger"),
    GREY("Grey"),
    WHITE("White"),
    DYED_BLUE("Dyed Blue"),
    DYED_GREEN("Dyed Green"),
    DYED_PINK("Dyed Pink"),
    DYED_PURPLE("Dyed Purple"),
    DYED_RED("Dyed Red"),
    OTHER("Other");

    private final String displayName;

    HairColor(String displayName) {
        this.displayName = displayName;
    }

}