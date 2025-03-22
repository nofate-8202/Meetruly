package com.meetruly.subscription.dto;

import com.meetruly.core.constant.SubscriptionPlan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionSummaryDto {

    private SubscriptionPlan currentPlan;
    private int remainingMessages;
    private int remainingProfileViews;
    private String validUntil;
    private boolean autoRenew;

    private int freeDailyMessageLimit;
    private int silverDailyMessageLimit;
    private int goldDailyMessageLimit;

    private int freeProfileViewsLimit;
    private int silverProfileViewsLimit;
    private int goldProfileViewsLimit;

    private double silverPrice;
    private double goldPrice;
}
