package com.meetruly.subscription.dto;

import com.meetruly.core.constant.SubscriptionPlan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionDto {

    private UUID id;
    private UUID userId;
    private String username;
    private SubscriptionPlan plan;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private boolean active;
    private boolean autoRenew;
    private int dailyMessageCount;
    private int profileViewsCount;
    private int dailyMessageLimit;
    private int profileViewsLimit;
    private LocalDateTime messageCountResetDate;
    private LocalDateTime profileViewsResetDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean expired;
    private boolean hasReachedMessageLimit;
    private boolean hasReachedProfileViewLimit;
}