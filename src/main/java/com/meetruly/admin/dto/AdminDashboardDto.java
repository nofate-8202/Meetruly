package com.meetruly.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDashboardDto {

    private long totalUsers;
    private long pendingApprovalUsers;
    private long activeUsers;
    private long blockedUsers;
    private long registrationsToday;
    private long registrationsThisMonth;

    private long totalMatches;
    private long totalLikes;
    private long totalMessages;
    private long totalProfileViews;
    private long messagesLast24Hours;
    private long likesLast24Hours;

    private double totalRevenue;
    private double revenueThisMonth;
    private double revenuePreviousMonth;
    private double growthPercentage;
    private Map<String, Integer> subscriptionsByPlan;
    private Map<String, Double> revenueByPlan;

    private long pendingReports;
    private long totalReports;
    private Map<String, Long> reportsByType;

    private List<UserReportDto> recentReports;
    private List<UserDto> recentPendingUsers;
}