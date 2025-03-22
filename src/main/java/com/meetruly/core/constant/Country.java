package com.meetruly.core.constant;

import lombok.Getter;

@Getter
public enum Country {
    BULGARIA("Bulgaria"),
    GERMANY("Germany"),
    FRANCE("France"),
    SPAIN("Spain"),
    ITALY("Italy"),
    UNITED_KINGDOM("United Kingdom"),
    USA("USA"),
    CANADA("Canada"),
    AUSTRALIA("Australia"),
    JAPAN("Japan"),
    CHINA("China"),
    RUSSIA("Russia"),
    GREECE("Greece"),
    TURKEY("Turkey"),
    ROMANIA("Romania"),
    SERBIA("Serbia"),
    NORTH_MACEDONIA("North Macedonia"),
    CROATIA("Croatia"),
    SLOVENIA("Slovenia"),
    AUSTRIA("Austria");

    private final String displayName;

    Country(String displayName) {
        this.displayName = displayName;
    }
}
