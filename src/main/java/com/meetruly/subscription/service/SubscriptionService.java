package com.meetruly.subscription.service;

import com.meetruly.core.constant.SubscriptionPlan;
import com.meetruly.subscription.dto.*;
import com.meetruly.subscription.model.Subscription;
import com.meetruly.subscription.model.Transaction;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionService {


    Subscription createSubscription(UUID userId, SubscriptionPlan plan, boolean autoRenew);

    SubscriptionDto getCurrentSubscription(UUID userId);

    List<SubscriptionDto> getUserSubscriptionHistory(UUID userId);

    void cancelAutoRenewal(UUID userId);

    void enableAutoRenewal(UUID userId);

    void cancelSubscription(UUID userId);


    boolean canSendMessage(UUID userId);

    void incrementMessageCount(UUID userId);

    boolean canViewFullProfile(UUID userId);

    boolean hasReachedProfileViewLimit(UUID userId);

    void incrementProfileViewCount(UUID userId);


    Transaction processSubscriptionPurchase(UUID userId, SubscriptionPurchaseRequest request);

    List<TransactionDto> getUserTransactions(UUID userId);

    RevenueStatsDto getRevenueStats(LocalDateTime startDate, LocalDateTime endDate);

    long countActiveSubscriptionsByPlan(SubscriptionPlan plan);

    long countAllActiveSubscriptions();

    SubscriptionSummaryDto getSubscriptionSummary(UUID userId);

    void processAutomaticRenewals();

    void resetDailyMessageCounts();

    void resetProfileViewCounts();
}