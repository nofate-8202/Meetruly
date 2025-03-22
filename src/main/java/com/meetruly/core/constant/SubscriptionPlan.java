package com.meetruly.core.constant;

import lombok.Getter;

@Getter

public enum SubscriptionPlan {
    FREE("Free", 0.0, 3, 0, false, false),
    SILVER("Silver", 9.99, 15, 10, true, false),
    GOLD("Gold", 19.99, Integer.MAX_VALUE, Integer.MAX_VALUE, true, true);

    private final String displayName;
    private final double price;
    private final int dailyMessageLimit;
    private final int profileViewsLimit;
    private final boolean canViewFullProfiles;
    private final boolean priorityMessaging;

    SubscriptionPlan(String displayName, double price, int dailyMessageLimit, int profileViewsLimit,
                     boolean canViewFullProfiles, boolean priorityMessaging) {
        this.displayName = displayName;
        this.price = price;
        this.dailyMessageLimit = dailyMessageLimit;
        this.profileViewsLimit = profileViewsLimit;
        this.canViewFullProfiles = canViewFullProfiles;
        this.priorityMessaging = priorityMessaging;
    }
}