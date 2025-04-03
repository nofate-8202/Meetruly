package com.meetruly.subscription.service.impl;

import com.meetruly.core.constant.SubscriptionPlan;
import com.meetruly.core.exception.MeetrulyException;
import com.meetruly.subscription.dto.*;
import com.meetruly.subscription.model.Subscription;
import com.meetruly.subscription.model.Transaction;
import com.meetruly.subscription.repository.SubscriptionRepository;
import com.meetruly.subscription.repository.TransactionRepository;
import com.meetruly.subscription.service.SubscriptionService;
import com.meetruly.user.model.User;
import com.meetruly.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public Subscription createSubscription(UUID userId, SubscriptionPlan plan, boolean autoRenew) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MeetrulyException("User not found with id: " + userId));

        Optional<Subscription> activeSubscriptionOpt = subscriptionRepository.findByUserAndActive(user, true);

        if (activeSubscriptionOpt.isPresent()) {
            Subscription activeSubscription = activeSubscriptionOpt.get();
            activeSubscription.setActive(false);
            activeSubscription.setUpdatedAt(LocalDateTime.now());
            subscriptionRepository.save(activeSubscription);
        }

        LocalDateTime now = LocalDateTime.now();
        Subscription subscription = Subscription.builder()
                .user(user)
                .plan(plan)
                .startDate(now)
                .endDate(now.plusMonths(1))
                .active(true)
                .autoRenew(autoRenew)
                .dailyMessageCount(0)
                .profileViewsCount(0)
                .messageCountResetDate(now.plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0))
                .profileViewsResetDate(now.plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0))
                .createdAt(now)
                .updatedAt(now)
                .build();

        return subscriptionRepository.save(subscription);
    }

    @Override
    public SubscriptionDto getCurrentSubscription(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MeetrulyException("User not found with id: " + userId));

        Optional<Subscription> activeSubscriptionOpt = subscriptionRepository.findByUserAndActive(user, true);

        if (activeSubscriptionOpt.isPresent()) {
            Subscription subscription = activeSubscriptionOpt.get();

            if (subscription.isExpired() && !subscription.isAutoRenew()) {
                deactivateExpiredSubscription(subscription);
                return createFreeSubscriptionDto(user);
            }

            return convertToSubscriptionDto(subscription);
        } else {

            return createFreeSubscriptionDto(user);
        }
    }

    @Transactional
    public void deactivateExpiredSubscription(Subscription subscription) {
        subscription.setActive(false);
        subscription.setUpdatedAt(LocalDateTime.now());
        subscriptionRepository.save(subscription);

        createSubscription(subscription.getUser().getId(), SubscriptionPlan.FREE, false);
    }

    private SubscriptionDto createFreeSubscriptionDto(User user) {
        SubscriptionPlan freePlan = SubscriptionPlan.FREE;
        LocalDateTime now = LocalDateTime.now();

        return SubscriptionDto.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .plan(freePlan)
                .startDate(now)
                .endDate(now.plusYears(100))
                .active(true)
                .autoRenew(false)
                .dailyMessageCount(0)
                .profileViewsCount(0)
                .dailyMessageLimit(freePlan.getDailyMessageLimit())
                .profileViewsLimit(freePlan.getProfileViewsLimit())
                .messageCountResetDate(now.plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0))
                .profileViewsResetDate(now.plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0))
                .createdAt(now)
                .updatedAt(now)
                .expired(false)
                .hasReachedMessageLimit(false)
                .hasReachedProfileViewLimit(false)
                .build();
    }

    @Override
    public List<SubscriptionDto> getUserSubscriptionHistory(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MeetrulyException("User not found with id: " + userId));

        return subscriptionRepository.findByUser(user).stream()
                .map(this::convertToSubscriptionDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void cancelAutoRenewal(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MeetrulyException("User not found with id: " + userId));

        Subscription subscription = subscriptionRepository.findByUserAndActive(user, true)
                .orElseThrow(() -> new MeetrulyException("No active subscription found for user"));

        subscription.setAutoRenew(false);
        subscription.setUpdatedAt(LocalDateTime.now());
        subscriptionRepository.save(subscription);
    }

    @Override
    @Transactional
    public void enableAutoRenewal(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MeetrulyException("User not found with id: " + userId));

        Subscription subscription = subscriptionRepository.findByUserAndActive(user, true)
                .orElseThrow(() -> new MeetrulyException("No active subscription found for user"));

        if (subscription.getPlan() == SubscriptionPlan.FREE) {
            throw new MeetrulyException("Cannot enable auto-renewal for FREE plan");
        }

        subscription.setAutoRenew(true);
        subscription.setUpdatedAt(LocalDateTime.now());
        subscriptionRepository.save(subscription);
    }

    @Override
    @Transactional
    public void cancelSubscription(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MeetrulyException("User not found with id: " + userId));

        Subscription subscription = subscriptionRepository.findByUserAndActive(user, true)
                .orElseThrow(() -> new MeetrulyException("No active subscription found for user"));

        if (subscription.getPlan() == SubscriptionPlan.FREE) {
            throw new MeetrulyException("Cannot cancel FREE plan");
        }

        subscription.setActive(false);
        subscription.setAutoRenew(false);
        subscription.setUpdatedAt(LocalDateTime.now());
        subscriptionRepository.save(subscription);

        createSubscription(userId, SubscriptionPlan.FREE, false);
    }

    @Override
    public boolean canSendMessage(UUID userId) {
        SubscriptionDto subscription = getCurrentSubscription(userId);

        if (subscription.getPlan() != SubscriptionPlan.FREE && subscription.isExpired()) {
            if (subscription.isAutoRenew()) {

                processSubscriptionRenewal(userId, subscription.getPlan());
                subscription = getCurrentSubscription(userId);
            } else {

                switchToFreePlan(userId);
                subscription = getCurrentSubscription(userId);
            }
        }

        return subscription.getDailyMessageCount() < subscription.getDailyMessageLimit();
    }

    @Transactional
    public void processSubscriptionRenewal(UUID userId, SubscriptionPlan plan) {

        Transaction transaction = Transaction.builder()
                .user(userRepository.findById(userId).orElseThrow())
                .transactionReference("RENEWAL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .plan(plan)
                .amount(plan.getPrice())
                .currency("EUR")
                .status(Transaction.TransactionStatus.COMPLETED)
                .type(Transaction.TransactionType.SUBSCRIPTION_RENEWAL)
                .paymentMethod("Auto-renewal")
                .createdAt(LocalDateTime.now())
                .processedAt(LocalDateTime.now())
                .build();

        transactionRepository.save(transaction);

        createSubscription(userId, plan, true);
    }

    @Transactional
    public void switchToFreePlan(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MeetrulyException("User not found with id: " + userId));

        subscriptionRepository.findByUserAndActive(user, true).ifPresent(subscription -> {
            subscription.setActive(false);
            subscription.setUpdatedAt(LocalDateTime.now());
            subscriptionRepository.save(subscription);
        });

        createSubscription(userId, SubscriptionPlan.FREE, false);
    }

    @Override
    @Transactional
    public void incrementMessageCount(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MeetrulyException("User not found with id: " + userId));

        Subscription subscription = subscriptionRepository.findByUserAndActive(user, true)
                .orElseThrow(() -> new MeetrulyException("No active subscription found for user"));

        subscription.setDailyMessageCount(subscription.getDailyMessageCount() + 1);

        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(subscription.getMessageCountResetDate())) {
            subscription.setDailyMessageCount(1);
            subscription.setMessageCountResetDate(now.plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0));
        }

        subscription.setUpdatedAt(LocalDateTime.now());
        subscriptionRepository.save(subscription);
    }

    @Override
    public boolean canViewFullProfile(UUID userId) {
        SubscriptionDto subscription = getCurrentSubscription(userId);

        return subscription.getPlan().isCanViewFullProfiles();
    }

    @Override
    public boolean hasReachedProfileViewLimit(UUID userId) {
        SubscriptionDto subscription = getCurrentSubscription(userId);

        if (subscription.getPlan() == SubscriptionPlan.GOLD) {
            return false;
        }

        return subscription.getProfileViewsCount() >= subscription.getProfileViewsLimit();
    }

    @Override
    @Transactional
    public void incrementProfileViewCount(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MeetrulyException("User not found with id: " + userId));

        Subscription subscription = subscriptionRepository.findByUserAndActive(user, true)
                .orElseThrow(() -> new MeetrulyException("No active subscription found for user"));

        if (subscription.getPlan() == SubscriptionPlan.GOLD) {
            return;
        }

        subscription.setProfileViewsCount(subscription.getProfileViewsCount() + 1);

        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(subscription.getProfileViewsResetDate())) {
            subscription.setProfileViewsCount(1);
            subscription.setProfileViewsResetDate(now.plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0));
        }

        subscription.setUpdatedAt(LocalDateTime.now());
        subscriptionRepository.save(subscription);
    }

    @Override
    @Transactional
    public Transaction processSubscriptionPurchase(UUID userId, SubscriptionPurchaseRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MeetrulyException("User not found with id: " + userId));

        if (request.getPlan() == SubscriptionPlan.FREE) {
            throw new MeetrulyException("Cannot purchase FREE plan");
        }

        Transaction transaction = Transaction.builder()
                .user(user)
                .plan(request.getPlan())
                .amount(request.getPlan().getPrice())
                .currency("EUR")
                .status(Transaction.TransactionStatus.PENDING)
                .type(Transaction.TransactionType.SUBSCRIPTION_PURCHASE)
                .paymentMethod(request.getPaymentMethod())
                .paymentDetails(buildPaymentDetails(request))
                .createdAt(LocalDateTime.now())
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);

        savedTransaction.setStatus(Transaction.TransactionStatus.COMPLETED);
        savedTransaction.setProcessedAt(LocalDateTime.now());
        transactionRepository.save(savedTransaction);

        createSubscription(userId, request.getPlan(), request.isAutoRenew());

        return savedTransaction;
    }

    private String buildPaymentDetails(SubscriptionPurchaseRequest request) {

        if ("credit_card".equals(request.getPaymentMethod()) && request.getCardNumber() != null) {
            String maskedCardNumber = "xxxx-xxxx-xxxx-" + request.getCardNumber().substring(Math.max(0, request.getCardNumber().length() - 4));
            return "Card: " + maskedCardNumber + ", Exp: " + request.getCardExpiryDate();
        } else if ("paypal".equals(request.getPaymentMethod())) {
            return "PayPal payment";
        } else {
            return "Other payment method";
        }
    }

    @Override
    public List<TransactionDto> getUserTransactions(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MeetrulyException("User not found with id: " + userId));

        return transactionRepository.findByUser(user).stream()
                .map(this::convertToTransactionDto)
                .collect(Collectors.toList());
    }

    @Override
    public RevenueStatsDto getRevenueStats(LocalDateTime startDate, LocalDateTime endDate) {

        Double totalRevenue = transactionRepository.calculateRevenueForPeriod(startDate, endDate);
        if (totalRevenue == null) {
            totalRevenue = 0.0;
        }

        Map<SubscriptionPlan, Double> revenueByPlan = new HashMap<>();
        for (SubscriptionPlan plan : SubscriptionPlan.values()) {
            if (plan != SubscriptionPlan.FREE) {
                Double planRevenue = transactionRepository.calculateRevenueByPlanForPeriod(plan, startDate, endDate);
                revenueByPlan.put(plan, planRevenue != null ? planRevenue : 0.0);
            }
        }

        Map<SubscriptionPlan, Long> subscriptionCountByPlan = new HashMap<>();
        for (SubscriptionPlan plan : SubscriptionPlan.values()) {
            if (plan != SubscriptionPlan.FREE) {
                long count = transactionRepository.countCompletedByPlan(plan);
                subscriptionCountByPlan.put(plan, count);
            }
        }

        long periodDays = java.time.Duration.between(startDate, endDate).toDays();
        LocalDateTime previousPeriodStart = startDate.minusDays(periodDays);
        LocalDateTime previousPeriodEnd = startDate;

        Double previousRevenue = transactionRepository.calculateRevenueForPeriod(previousPeriodStart, previousPeriodEnd);
        if (previousRevenue == null) {
            previousRevenue = 0.0;
        }

        double growthPercentage = 0.0;
        if (previousRevenue > 0) {
            growthPercentage = ((totalRevenue - previousRevenue) / previousRevenue) * 100;
        }

        long activeSubscriptions = countAllActiveSubscriptions();

        return RevenueStatsDto.builder()
                .startDate(startDate)
                .endDate(endDate)
                .totalRevenue(totalRevenue)
                .revenueByPlan(revenueByPlan)
                .subscriptionCountByPlan(subscriptionCountByPlan)
                .growthPercentage(growthPercentage)
                .activeSubscriptions(activeSubscriptions)
                .build();
    }

    @Override
    public long countActiveSubscriptionsByPlan(SubscriptionPlan plan) {
        return subscriptionRepository.countActiveByPlan(plan);
    }

    @Override
    public long countAllActiveSubscriptions() {
        return subscriptionRepository.countActiveSubscriptions();
    }

    @Override
    public SubscriptionSummaryDto getSubscriptionSummary(UUID userId) {
        SubscriptionDto subscription = getCurrentSubscription(userId);

        return SubscriptionSummaryDto.builder()
                .currentPlan(subscription.getPlan())
                .remainingMessages(subscription.getDailyMessageLimit() - subscription.getDailyMessageCount())
                .remainingProfileViews(subscription.getProfileViewsLimit() - subscription.getProfileViewsCount())
                .validUntil(subscription.getEndDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
                .autoRenew(subscription.isAutoRenew())
                .freeDailyMessageLimit(SubscriptionPlan.FREE.getDailyMessageLimit())
                .silverDailyMessageLimit(SubscriptionPlan.SILVER.getDailyMessageLimit())
                .goldDailyMessageLimit(SubscriptionPlan.GOLD.getDailyMessageLimit())
                .freeProfileViewsLimit(SubscriptionPlan.FREE.getProfileViewsLimit())
                .silverProfileViewsLimit(SubscriptionPlan.SILVER.getProfileViewsLimit())
                .goldProfileViewsLimit(SubscriptionPlan.GOLD.getProfileViewsLimit())
                .silverPrice(SubscriptionPlan.SILVER.getPrice())
                .goldPrice(SubscriptionPlan.GOLD.getPrice())
                .build();
    }

    @Override
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void processAutomaticRenewals() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dayEnd = now.withHour(23).withMinute(59).withSecond(59);

        List<Subscription> subscriptionsToRenew = subscriptionRepository.findSubscriptionsToRenew(now, dayEnd);

        for (Subscription subscription : subscriptionsToRenew) {
            try {

                Transaction transaction = Transaction.builder()
                        .user(subscription.getUser())
                        .transactionReference("AUTORENEWAL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                        .plan(subscription.getPlan())
                        .amount(subscription.getPlan().getPrice())
                        .currency("EUR")
                        .status(Transaction.TransactionStatus.COMPLETED)
                        .type(Transaction.TransactionType.SUBSCRIPTION_RENEWAL)
                        .paymentMethod("Auto-renewal")
                        .createdAt(now)
                        .processedAt(now)
                        .build();

                transactionRepository.save(transaction);

                subscription.setActive(false);
                subscription.setUpdatedAt(now);
                subscriptionRepository.save(subscription);

                createSubscription(subscription.getUser().getId(), subscription.getPlan(), true);

                log.info("Auto-renewed subscription for user: {}", subscription.getUser().getUsername());
            } catch (Exception e) {
                log.error("Failed to auto-renew subscription for user: {}", subscription.getUser().getUsername(), e);
            }
        }
    }

    @Override
    @Scheduled(cron = "0 5 0 * * ?")
    @Transactional
    public void resetDailyMessageCounts() {
        LocalDateTime now = LocalDateTime.now();

        List<Subscription> subscriptionsToReset = subscriptionRepository.findSubscriptionsToResetMessageCount(now);

        for (Subscription subscription : subscriptionsToReset) {
            subscription.setDailyMessageCount(0);
            subscription.setMessageCountResetDate(now.plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0));
            subscription.setUpdatedAt(now);
            subscriptionRepository.save(subscription);
        }

        log.info("Reset daily message counts for {} subscriptions", subscriptionsToReset.size());
    }

    @Override
    @Scheduled(cron = "0 10 0 * * ?")
    @Transactional
    public void resetProfileViewCounts() {
        LocalDateTime now = LocalDateTime.now();

        List<Subscription> subscriptionsToReset = subscriptionRepository.findSubscriptionsToResetProfileViews(now);

        for (Subscription subscription : subscriptionsToReset) {
            subscription.setProfileViewsCount(0);
            subscription.setProfileViewsResetDate(now.plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0));
            subscription.setUpdatedAt(now);
            subscriptionRepository.save(subscription);
        }

        log.info("Reset profile view counts for {} subscriptions", subscriptionsToReset.size());
    }

    private SubscriptionDto convertToSubscriptionDto(Subscription subscription) {
        return SubscriptionDto.builder()
                .id(subscription.getId())
                .userId(subscription.getUser().getId())
                .username(subscription.getUser().getUsername())
                .plan(subscription.getPlan())
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .active(subscription.isActive())
                .autoRenew(subscription.isAutoRenew())
                .dailyMessageCount(subscription.getDailyMessageCount())
                .profileViewsCount(subscription.getProfileViewsCount())
                .dailyMessageLimit(subscription.getPlan().getDailyMessageLimit())
                .profileViewsLimit(subscription.getPlan().getProfileViewsLimit())
                .messageCountResetDate(subscription.getMessageCountResetDate())
                .profileViewsResetDate(subscription.getProfileViewsResetDate())
                .createdAt(subscription.getCreatedAt())
                .updatedAt(subscription.getUpdatedAt())
                .expired(subscription.isExpired())
                .hasReachedMessageLimit(subscription.hasReachedMessageLimit())
                .hasReachedProfileViewLimit(subscription.hasReachedProfileViewLimit())
                .build();
    }

    private TransactionDto convertToTransactionDto(Transaction transaction) {
        return TransactionDto.builder()
                .id(transaction.getId())
                .userId(transaction.getUser().getId())
                .username(transaction.getUser().getUsername())
                .transactionReference(transaction.getTransactionReference())
                .plan(transaction.getPlan())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .status(transaction.getStatus())
                .type(transaction.getType())
                .paymentMethod(transaction.getPaymentMethod())
                .paymentDetails(transaction.getPaymentDetails())
                .createdAt(transaction.getCreatedAt())
                .processedAt(transaction.getProcessedAt())
                .build();
    }
}