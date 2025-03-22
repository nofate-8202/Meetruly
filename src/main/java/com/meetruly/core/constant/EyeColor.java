package com.meetruly.core.constant;


import lombok.Getter;

@Getter
public enum EyeColor {
    BLUE("Blue"),
    GREEN("Green"),
    BROWN("Brown"),
    HAZEL("Hazel"),
    GREY("Grey"),
    BLACK("Black"),
    AMBER("Amber"),
    OTHER("Other");

    private final String displayName;

    EyeColor(String displayName) {
        this.displayName = displayName;
    }

}
