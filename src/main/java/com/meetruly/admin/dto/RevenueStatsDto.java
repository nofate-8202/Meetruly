package com.meetruly.admin.dto;

import com.meetruly.core.constant.SubscriptionPlan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevenueStatsDto {

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private double totalRevenue;
    private Map<SubscriptionPlan, Double> revenueByPlan;
    private Map<SubscriptionPlan, Long> subscriptionCountByPlan;

    private double growthPercentage;
    private long activeSubscriptions;


    private double averageRevenuePerUser;
    private double revenuePerDay;
    private Map<String, Double> revenueByDay;
    private Map<String, Double> revenueByMonth;
}