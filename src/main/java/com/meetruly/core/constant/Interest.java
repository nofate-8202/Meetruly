package com.meetruly.core.constant;

import lombok.Getter;

@Getter
public enum Interest {
    MUSIC("Music"),
    MOVIES("Movies"),
    BOOKS("Books"),
    SPORT("Sport"),
    TRAVEL("Travel"),
    ART("Art"),
    COOKING("Cooking"),
    PHOTOGRAPHY("Photography"),
    DANCING("Dancing"),
    GAMING("Gaming"),
    HIKING("Hiking"),
    FITNESS("Fitness"),
    YOGA("Yoga"),
    TECHNOLOGY("Technology"),
    FASHION("Fashion"),
    ANIMALS("Animals"),
    NATURE("Nature"),
    GARDENING("Gardening"),
    POLITICS("Politics"),
    SCIENCE("Science"),
    HISTORY("History"),
    CARS("Cars"),
    MOTORCYCLES("Motorcycles"),
    FISHING("Fishing"),
    HUNTING("Hunting"),
    ASTROLOGY("Astrology"),
    SPIRITUALITY("Spirituality"),
    VOLUNTEERING("Volunteering"),
    LANGUAGES("Languages"),
    POETRY("Poetry");

    private final String displayName;

    Interest(String displayName) {
        this.displayName = displayName;
    }

}
