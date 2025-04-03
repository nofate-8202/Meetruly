package com.meetruly.subscription.service;

import com.meetruly.core.constant.SubscriptionPlan;
import com.meetruly.core.exception.MeetrulyException;
import com.meetruly.subscription.dto.*;
import com.meetruly.subscription.model.Subscription;
import com.meetruly.subscription.model.Transaction;
import com.meetruly.subscription.repository.SubscriptionRepository;
import com.meetruly.subscription.repository.TransactionRepository;
import com.meetruly.subscription.service.impl.SubscriptionServiceImpl;
import com.meetruly.user.model.User;
import com.meetruly.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SubscriptionServiceImplTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SubscriptionServiceImpl subscriptionService;

    private UUID userId;
    private User user;
    private Subscription activeSubscription;
    private Transaction transaction;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder()
                .id(userId)
                .username("testuser")
                .email("test@example.com")
                .build();

        LocalDateTime now = LocalDateTime.now();
        activeSubscription = Subscription.builder()
                .id(UUID.randomUUID())
                .user(user)
                .plan(SubscriptionPlan.SILVER)
                .startDate(now)
                .endDate(now.plusMonths(1))
                .active(true)
                .autoRenew(true)
                .dailyMessageCount(2)
                .profileViewsCount(3)
                .messageCountResetDate(now.plusDays(1))
                .profileViewsResetDate(now.plusDays(1))
                .createdAt(now)
                .updatedAt(now)
                .build();

        transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .user(user)
                .transactionReference("TR-12345678")
                .plan(SubscriptionPlan.SILVER)
                .amount(SubscriptionPlan.SILVER.getPrice())
                .currency("EUR")
                .status(Transaction.TransactionStatus.COMPLETED)
                .type(Transaction.TransactionType.SUBSCRIPTION_PURCHASE)
                .paymentMethod("credit_card")
                .createdAt(now)
                .processedAt(now)
                .build();
    }

    @Test
    void createSubscription_ShouldCreateNewSubscription() {
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByUserAndActive(user, true)).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        
        Subscription result = subscriptionService.createSubscription(userId, SubscriptionPlan.GOLD, true);

        
        assertNotNull(result);
        assertEquals(SubscriptionPlan.GOLD, result.getPlan());
        assertEquals(user, result.getUser());
        assertTrue(result.isActive());
        assertTrue(result.isAutoRenew());
        verify(subscriptionRepository).save(any(Subscription.class));
    }

    @Test
    void createSubscription_WhenUserHasActiveSubscription_ShouldDeactivateOldAndCreateNew() {
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByUserAndActive(user, true)).thenReturn(Optional.of(activeSubscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        
        Subscription result = subscriptionService.createSubscription(userId, SubscriptionPlan.GOLD, true);

        
        assertNotNull(result);
        assertEquals(SubscriptionPlan.GOLD, result.getPlan());
        assertEquals(user, result.getUser());
        assertTrue(result.isActive());
        assertTrue(result.isAutoRenew());

        
        verify(subscriptionRepository, times(2)).save(any(Subscription.class));
        assertFalse(activeSubscription.isActive());
    }

    @Test
    void createSubscription_WhenUserNotFound_ShouldThrowException() {
        
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        
        MeetrulyException exception = assertThrows(MeetrulyException.class,
                () -> subscriptionService.createSubscription(userId, SubscriptionPlan.GOLD, true));
        assertEquals("User not found with id: " + userId, exception.getMessage());
    }

    @Test
    void getCurrentSubscription_WhenUserHasActiveSubscription_ShouldReturnIt() {
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByUserAndActive(user, true)).thenReturn(Optional.of(activeSubscription));

        
        SubscriptionDto result = subscriptionService.getCurrentSubscription(userId);

        
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(activeSubscription.getPlan(), result.getPlan());
        assertEquals(activeSubscription.isAutoRenew(), result.isAutoRenew());
        assertEquals(activeSubscription.getDailyMessageCount(), result.getDailyMessageCount());
    }

    @Test
    void getCurrentSubscription_WhenUserHasNoActiveSubscription_ShouldReturnFreePlan() {
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByUserAndActive(user, true)).thenReturn(Optional.empty());

        
        SubscriptionDto result = subscriptionService.getCurrentSubscription(userId);

        
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(SubscriptionPlan.FREE, result.getPlan());
        assertFalse(result.isAutoRenew());
    }

    @Test
    void getCurrentSubscription_WhenUserNotFound_ShouldThrowException() {
        
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        
        MeetrulyException exception = assertThrows(MeetrulyException.class,
                () -> subscriptionService.getCurrentSubscription(userId));
        assertEquals("User not found with id: " + userId, exception.getMessage());
    }

    @Test
    void getUserSubscriptionHistory_ShouldReturnUserSubscriptions() {
        
        Subscription oldSubscription = Subscription.builder()
                .id(UUID.randomUUID())
                .user(user)
                .plan(SubscriptionPlan.FREE)
                .startDate(LocalDateTime.now().minusMonths(2))
                .endDate(LocalDateTime.now().minusMonths(1))
                .active(false)
                .build();

        List<Subscription> subscriptions = Arrays.asList(oldSubscription, activeSubscription);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByUser(user)).thenReturn(subscriptions);

        
        List<SubscriptionDto> results = subscriptionService.getUserSubscriptionHistory(userId);

        
        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals(SubscriptionPlan.FREE, results.get(0).getPlan());
        assertEquals(SubscriptionPlan.SILVER, results.get(1).getPlan());
    }

    @Test
    void cancelAutoRenewal_ShouldUpdateSubscription() {
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByUserAndActive(user, true)).thenReturn(Optional.of(activeSubscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(activeSubscription);

        
        subscriptionService.cancelAutoRenewal(userId);

        
        assertFalse(activeSubscription.isAutoRenew());
        verify(subscriptionRepository).save(activeSubscription);
    }

    @Test
    void enableAutoRenewal_ShouldUpdateSubscription() {
        
        activeSubscription.setAutoRenew(false);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByUserAndActive(user, true)).thenReturn(Optional.of(activeSubscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(activeSubscription);

        
        subscriptionService.enableAutoRenewal(userId);

        
        assertTrue(activeSubscription.isAutoRenew());
        verify(subscriptionRepository).save(activeSubscription);
    }

    @Test
    void enableAutoRenewal_OnFreePlan_ShouldThrowException() {
        
        activeSubscription.setPlan(SubscriptionPlan.FREE);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByUserAndActive(user, true)).thenReturn(Optional.of(activeSubscription));

        
        MeetrulyException exception = assertThrows(MeetrulyException.class,
                () -> subscriptionService.enableAutoRenewal(userId));
        assertEquals("Cannot enable auto-renewal for FREE plan", exception.getMessage());
    }

    @Test
    void cancelSubscription_ShouldDeactivateAndCreateFreePlan() {
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByUserAndActive(user, true)).thenReturn(Optional.of(activeSubscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        
        SubscriptionServiceImpl spyService = spy(subscriptionService);

        
        
        Subscription freeSubscription = new Subscription();
        doReturn(freeSubscription).when(spyService).createSubscription(eq(userId), eq(SubscriptionPlan.FREE), eq(false));

        
        spyService.cancelSubscription(userId);

        
        assertFalse(activeSubscription.isActive());
        assertFalse(activeSubscription.isAutoRenew());

        
        verify(subscriptionRepository).save(any(Subscription.class));
        
        verify(spyService).createSubscription(userId, SubscriptionPlan.FREE, false);
    }

    @Test
    void cancelSubscription_OnFreePlan_ShouldThrowException() {
        
        activeSubscription.setPlan(SubscriptionPlan.FREE);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByUserAndActive(user, true)).thenReturn(Optional.of(activeSubscription));

        
        MeetrulyException exception = assertThrows(MeetrulyException.class,
                () -> subscriptionService.cancelSubscription(userId));
        assertEquals("Cannot cancel FREE plan", exception.getMessage());
    }

    @Test
    void canSendMessage_WhenUnderLimit_ShouldReturnTrue() {
        
        SubscriptionDto subscriptionDto = SubscriptionDto.builder()
                .userId(userId)
                .plan(SubscriptionPlan.SILVER)
                .dailyMessageCount(2)
                .dailyMessageLimit(15)
                .expired(false)
                .build();

        
        SubscriptionServiceImpl spyService = spy(subscriptionService);

        
        doReturn(subscriptionDto).when(spyService).getCurrentSubscription(userId);

        
        boolean result = spyService.canSendMessage(userId);

        
        assertTrue(result);
    }

    @Test
    void canSendMessage_WhenAtLimit_ShouldReturnFalse() {
        
        SubscriptionDto subscriptionDto = SubscriptionDto.builder()
                .userId(userId)
                .plan(SubscriptionPlan.SILVER)
                .dailyMessageCount(15)
                .dailyMessageLimit(15)
                .expired(false)
                .build();

        
        SubscriptionServiceImpl spyService = spy(subscriptionService);

        
        doReturn(subscriptionDto).when(spyService).getCurrentSubscription(userId);

        
        boolean result = spyService.canSendMessage(userId);

        
        assertFalse(result);
    }

    @Test
    void incrementMessageCount_ShouldIncrementCounter() {
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByUserAndActive(user, true)).thenReturn(Optional.of(activeSubscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(activeSubscription);

        int initialCount = activeSubscription.getDailyMessageCount();

        
        subscriptionService.incrementMessageCount(userId);

        
        assertEquals(initialCount + 1, activeSubscription.getDailyMessageCount());
        verify(subscriptionRepository).save(activeSubscription);
    }

    @Test
    void incrementMessageCount_WhenResetTimeReached_ShouldResetCounter() {
        
        activeSubscription.setMessageCountResetDate(LocalDateTime.now().minusDays(1));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByUserAndActive(user, true)).thenReturn(Optional.of(activeSubscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(activeSubscription);

        
        subscriptionService.incrementMessageCount(userId);

        
        assertEquals(1, activeSubscription.getDailyMessageCount());
        assertTrue(activeSubscription.getMessageCountResetDate().isAfter(LocalDateTime.now()));
        verify(subscriptionRepository).save(activeSubscription);
    }

    @Test
    void processSubscriptionPurchase_ShouldCreateTransactionAndSubscription() {
        
        SubscriptionPurchaseRequest request = SubscriptionPurchaseRequest.builder()
                .plan(SubscriptionPlan.GOLD)
                .autoRenew(true)
                .paymentMethod("credit_card")
                .cardNumber("4111111111111111")
                .cardExpiryDate("12/25")
                .cardCVV("123")
                .cardHolderName("Test User")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            if (t.getId() == null) {
                t.setId(UUID.randomUUID());
            }
            return t;
        });

        
        SubscriptionServiceImpl spyService = spy(subscriptionService);

        
        doReturn(activeSubscription).when(spyService).createSubscription(
                eq(userId), eq(SubscriptionPlan.GOLD), eq(true));

        
        Transaction result = spyService.processSubscriptionPurchase(userId, request);

        
        assertNotNull(result);
        assertEquals(Transaction.TransactionStatus.COMPLETED, result.getStatus());
        assertEquals(SubscriptionPlan.GOLD, result.getPlan());
        assertEquals(SubscriptionPlan.GOLD.getPrice(), result.getAmount());
        assertNotNull(result.getProcessedAt());

        verify(transactionRepository, times(2)).save(any(Transaction.class));
        verify(spyService).createSubscription(userId, SubscriptionPlan.GOLD, true);
    }

    @Test
    void processSubscriptionPurchase_ForFreePlan_ShouldThrowException() {
        
        SubscriptionPurchaseRequest request = SubscriptionPurchaseRequest.builder()
                .plan(SubscriptionPlan.FREE)
                .autoRenew(false)
                .paymentMethod("credit_card")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        
        MeetrulyException exception = assertThrows(MeetrulyException.class,
                () -> subscriptionService.processSubscriptionPurchase(userId, request));
        assertEquals("Cannot purchase FREE plan", exception.getMessage());
    }

    @Test
    void getSubscriptionSummary_ShouldReturnCompleteSummary() {
        
        SubscriptionDto subscriptionDto = SubscriptionDto.builder()
                .userId(userId)
                .plan(SubscriptionPlan.SILVER)
                .dailyMessageCount(5)
                .dailyMessageLimit(15)
                .profileViewsCount(3)
                .profileViewsLimit(10)
                .endDate(LocalDateTime.now().plusDays(20))
                .autoRenew(true)
                .build();

        
        SubscriptionServiceImpl spyService = spy(subscriptionService);

        
        doReturn(subscriptionDto).when(spyService).getCurrentSubscription(userId);

        
        SubscriptionSummaryDto result = spyService.getSubscriptionSummary(userId);

        
        assertNotNull(result);
        assertEquals(SubscriptionPlan.SILVER, result.getCurrentPlan());
        assertEquals(10, result.getRemainingMessages());
        assertEquals(7, result.getRemainingProfileViews());
        assertTrue(result.isAutoRenew());

        
        assertEquals(SubscriptionPlan.FREE.getDailyMessageLimit(), result.getFreeDailyMessageLimit());
        assertEquals(SubscriptionPlan.SILVER.getDailyMessageLimit(), result.getSilverDailyMessageLimit());
        assertEquals(SubscriptionPlan.GOLD.getDailyMessageLimit(), result.getGoldDailyMessageLimit());

        assertEquals(SubscriptionPlan.FREE.getProfileViewsLimit(), result.getFreeProfileViewsLimit());
        assertEquals(SubscriptionPlan.SILVER.getProfileViewsLimit(), result.getSilverProfileViewsLimit());
        assertEquals(SubscriptionPlan.GOLD.getProfileViewsLimit(), result.getGoldProfileViewsLimit());

        assertEquals(SubscriptionPlan.SILVER.getPrice(), result.getSilverPrice());
        assertEquals(SubscriptionPlan.GOLD.getPrice(), result.getGoldPrice());
    }

    @Test
    void canViewFullProfile_ForSilverPlan_ShouldReturnPlanCapability() {
        
        SubscriptionDto subscriptionDto = SubscriptionDto.builder()
                .userId(userId)
                .plan(SubscriptionPlan.SILVER)
                .build();

        
        SubscriptionServiceImpl spyService = spy(subscriptionService);

        
        doReturn(subscriptionDto).when(spyService).getCurrentSubscription(userId);

        
        boolean result = spyService.canViewFullProfile(userId);

        
        assertEquals(SubscriptionPlan.SILVER.isCanViewFullProfiles(), result);
    }

    @Test
    void hasReachedProfileViewLimit_ForGoldPlan_ShouldReturnFalse() {
        
        SubscriptionDto subscriptionDto = SubscriptionDto.builder()
                .userId(userId)
                .plan(SubscriptionPlan.GOLD)
                .profileViewsCount(100) 
                .profileViewsLimit(50)  
                .build();

        
        SubscriptionServiceImpl spyService = spy(subscriptionService);

        
        doReturn(subscriptionDto).when(spyService).getCurrentSubscription(userId);

        
        boolean result = spyService.hasReachedProfileViewLimit(userId);

        
        assertFalse(result);
    }

    @Test
    void hasReachedProfileViewLimit_ForSilverPlanUnderLimit_ShouldReturnFalse() {
        
        SubscriptionDto subscriptionDto = SubscriptionDto.builder()
                .userId(userId)
                .plan(SubscriptionPlan.SILVER)
                .profileViewsCount(5)
                .profileViewsLimit(10)
                .build();

        
        SubscriptionServiceImpl spyService = spy(subscriptionService);

        
        doReturn(subscriptionDto).when(spyService).getCurrentSubscription(userId);

        
        boolean result = spyService.hasReachedProfileViewLimit(userId);

        
        assertFalse(result);
    }

    @Test
    void hasReachedProfileViewLimit_ForSilverPlanAtLimit_ShouldReturnTrue() {
        
        SubscriptionDto subscriptionDto = SubscriptionDto.builder()
                .userId(userId)
                .plan(SubscriptionPlan.SILVER)
                .profileViewsCount(10)
                .profileViewsLimit(10)
                .build();

        
        SubscriptionServiceImpl spyService = spy(subscriptionService);

        
        doReturn(subscriptionDto).when(spyService).getCurrentSubscription(userId);

        
        boolean result = spyService.hasReachedProfileViewLimit(userId);

        
        assertTrue(result);
    }

    @Test
    void incrementProfileViewCount_ForGoldPlan_ShouldNotIncrement() {
        
        activeSubscription.setPlan(SubscriptionPlan.GOLD);
        int initialCount = activeSubscription.getProfileViewsCount();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByUserAndActive(user, true)).thenReturn(Optional.of(activeSubscription));

        
        subscriptionService.incrementProfileViewCount(userId);

        
        assertEquals(initialCount, activeSubscription.getProfileViewsCount());
        verify(subscriptionRepository, never()).save(any(Subscription.class));
    }

    @Test
    void incrementProfileViewCount_ForSilverPlan_ShouldIncrement() {
        
        int initialCount = activeSubscription.getProfileViewsCount();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByUserAndActive(user, true)).thenReturn(Optional.of(activeSubscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(activeSubscription);

        
        subscriptionService.incrementProfileViewCount(userId);

        
        assertEquals(initialCount + 1, activeSubscription.getProfileViewsCount());
        verify(subscriptionRepository).save(activeSubscription);
    }

    @Test
    void incrementProfileViewCount_WhenResetTimeReached_ShouldResetCounter() {
        
        activeSubscription.setProfileViewsResetDate(LocalDateTime.now().minusDays(1));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByUserAndActive(user, true)).thenReturn(Optional.of(activeSubscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(activeSubscription);

        
        subscriptionService.incrementProfileViewCount(userId);

        
        assertEquals(1, activeSubscription.getProfileViewsCount());
        assertTrue(activeSubscription.getProfileViewsResetDate().isAfter(LocalDateTime.now()));
        verify(subscriptionRepository).save(activeSubscription);
    }

    @Test
    void getUserTransactions_ShouldReturnUserTransactions() {
        
        Transaction oldTransaction = Transaction.builder()
                .id(UUID.randomUUID())
                .user(user)
                .transactionReference("TR-OLD12345")
                .plan(SubscriptionPlan.SILVER)
                .amount(SubscriptionPlan.SILVER.getPrice())
                .currency("EUR")
                .status(Transaction.TransactionStatus.COMPLETED)
                .type(Transaction.TransactionType.SUBSCRIPTION_PURCHASE)
                .createdAt(LocalDateTime.now().minusMonths(1))
                .processedAt(LocalDateTime.now().minusMonths(1))
                .build();

        List<Transaction> transactions = Arrays.asList(oldTransaction, transaction);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(transactionRepository.findByUser(user)).thenReturn(transactions);

        
        List<TransactionDto> results = subscriptionService.getUserTransactions(userId);

        
        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals("TR-OLD12345", results.get(0).getTransactionReference());
        assertEquals("TR-12345678", results.get(1).getTransactionReference());
    }

    @Test
    void getRevenueStats_ShouldCalculateCorrectStats() {
        
        LocalDateTime startDate = LocalDateTime.now().minusMonths(1);
        LocalDateTime endDate = LocalDateTime.now();

        Double totalRevenue = 1000.0;
        Double silverRevenue = 400.0;
        Double goldRevenue = 600.0;
        Double previousPeriodRevenue = 800.0;

        when(transactionRepository.calculateRevenueForPeriod(startDate, endDate)).thenReturn(totalRevenue);
        when(transactionRepository.calculateRevenueByPlanForPeriod(eq(SubscriptionPlan.SILVER), eq(startDate), eq(endDate))).thenReturn(silverRevenue);
        when(transactionRepository.calculateRevenueByPlanForPeriod(eq(SubscriptionPlan.GOLD), eq(startDate), eq(endDate))).thenReturn(goldRevenue);

        
        LocalDateTime previousPeriodStart = startDate.minusDays(30);
        LocalDateTime previousPeriodEnd = startDate;
        when(transactionRepository.calculateRevenueForPeriod(any(LocalDateTime.class), eq(startDate))).thenReturn(previousPeriodRevenue);

        when(transactionRepository.countCompletedByPlan(SubscriptionPlan.SILVER)).thenReturn(40L);
        when(transactionRepository.countCompletedByPlan(SubscriptionPlan.GOLD)).thenReturn(30L);

        when(subscriptionRepository.countActiveSubscriptions()).thenReturn(50L);

        
        RevenueStatsDto result = subscriptionService.getRevenueStats(startDate, endDate);

        
        assertNotNull(result);
        assertEquals(totalRevenue, result.getTotalRevenue());
        assertEquals(silverRevenue, result.getRevenueByPlan().get(SubscriptionPlan.SILVER));
        assertEquals(goldRevenue, result.getRevenueByPlan().get(SubscriptionPlan.GOLD));
        assertEquals(40L, result.getSubscriptionCountByPlan().get(SubscriptionPlan.SILVER));
        assertEquals(30L, result.getSubscriptionCountByPlan().get(SubscriptionPlan.GOLD));
        assertEquals(50L, result.getActiveSubscriptions());

        
        double expectedGrowth = ((totalRevenue - previousPeriodRevenue) / previousPeriodRevenue) * 100;
        assertEquals(expectedGrowth, result.getGrowthPercentage(), 0.01);
    }

    @Test
    void getRevenueStats_WithNullRevenue_ShouldHandleNulls() {
        
        LocalDateTime startDate = LocalDateTime.now().minusMonths(1);
        LocalDateTime endDate = LocalDateTime.now();

        when(transactionRepository.calculateRevenueForPeriod(startDate, endDate)).thenReturn(null);
        when(transactionRepository.calculateRevenueByPlanForPeriod(any(SubscriptionPlan.class), eq(startDate), eq(endDate))).thenReturn(null);
        when(transactionRepository.calculateRevenueForPeriod(any(LocalDateTime.class), eq(startDate))).thenReturn(null);

        when(subscriptionRepository.countActiveSubscriptions()).thenReturn(0L);

        
        RevenueStatsDto result = subscriptionService.getRevenueStats(startDate, endDate);

        
        assertNotNull(result);
        assertEquals(0.0, result.getTotalRevenue());
        assertNotNull(result.getRevenueByPlan());
        assertNotNull(result.getSubscriptionCountByPlan());
        assertEquals(0.0, result.getGrowthPercentage());
    }

    @Test
    void countActiveSubscriptionsByPlan_ShouldCallRepository() {
        
        when(subscriptionRepository.countActiveByPlan(SubscriptionPlan.SILVER)).thenReturn(10L);

        
        long result = subscriptionService.countActiveSubscriptionsByPlan(SubscriptionPlan.SILVER);

        
        assertEquals(10L, result);
        verify(subscriptionRepository).countActiveByPlan(SubscriptionPlan.SILVER);
    }

    @Test
    void countAllActiveSubscriptions_ShouldCallRepository() {
        
        when(subscriptionRepository.countActiveSubscriptions()).thenReturn(30L);

        
        long result = subscriptionService.countAllActiveSubscriptions();

        
        assertEquals(30L, result);
        verify(subscriptionRepository).countActiveSubscriptions();
    }

    @Test
    void processAutomaticRenewals_ShouldRenewSubscriptions() {
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dayEnd = now.withHour(23).withMinute(59).withSecond(59);

        Subscription sub1 = Subscription.builder()
                .id(UUID.randomUUID())
                .user(user)
                .plan(SubscriptionPlan.SILVER)
                .autoRenew(true)
                .endDate(now.plusHours(2))
                .build();

        User user2 = User.builder()
                .id(UUID.randomUUID())
                .username("user2")
                .build();

        Subscription sub2 = Subscription.builder()
                .id(UUID.randomUUID())
                .user(user2)
                .plan(SubscriptionPlan.GOLD)
                .autoRenew(true)
                .endDate(now.plusHours(5))
                .build();

        List<Subscription> subscriptionsToRenew = Arrays.asList(sub1, sub2);

        when(subscriptionRepository.findSubscriptionsToRenew(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(subscriptionsToRenew);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(i -> i.getArgument(0));

        
        SubscriptionServiceImpl spyService = spy(subscriptionService);
        doReturn(new Subscription()).when(spyService).createSubscription(any(UUID.class), any(SubscriptionPlan.class), eq(true));

        
        spyService.processAutomaticRenewals();

        
        verify(transactionRepository, times(2)).save(any(Transaction.class));
        verify(subscriptionRepository, times(2)).save(any(Subscription.class));
        verify(spyService).createSubscription(sub1.getUser().getId(), sub1.getPlan(), true);
        verify(spyService).createSubscription(sub2.getUser().getId(), sub2.getPlan(), true);

        assertFalse(sub1.isActive());
        assertFalse(sub2.isActive());
    }

    @Test
    void resetDailyMessageCounts_ShouldResetMessages() {
        
        LocalDateTime now = LocalDateTime.now();

        Subscription sub1 = Subscription.builder()
                .id(UUID.randomUUID())
                .dailyMessageCount(5)
                .messageCountResetDate(now.minusHours(1))
                .build();

        Subscription sub2 = Subscription.builder()
                .id(UUID.randomUUID())
                .dailyMessageCount(10)
                .messageCountResetDate(now.minusHours(2))
                .build();

        List<Subscription> subscriptions = Arrays.asList(sub1, sub2);

        when(subscriptionRepository.findSubscriptionsToResetMessageCount(any(LocalDateTime.class)))
                .thenReturn(subscriptions);
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(i -> i.getArgument(0));

        
        subscriptionService.resetDailyMessageCounts();

        
        assertEquals(0, sub1.getDailyMessageCount());
        assertEquals(0, sub2.getDailyMessageCount());
        assertTrue(sub1.getMessageCountResetDate().isAfter(now));
        assertTrue(sub2.getMessageCountResetDate().isAfter(now));
        verify(subscriptionRepository, times(2)).save(any(Subscription.class));
    }

    @Test
    void resetProfileViewCounts_ShouldResetViews() {
        
        LocalDateTime now = LocalDateTime.now();

        Subscription sub1 = Subscription.builder()
                .id(UUID.randomUUID())
                .profileViewsCount(3)
                .profileViewsResetDate(now.minusHours(1))
                .build();

        Subscription sub2 = Subscription.builder()
                .id(UUID.randomUUID())
                .profileViewsCount(7)
                .profileViewsResetDate(now.minusHours(2))
                .build();

        List<Subscription> subscriptions = Arrays.asList(sub1, sub2);

        when(subscriptionRepository.findSubscriptionsToResetProfileViews(any(LocalDateTime.class)))
                .thenReturn(subscriptions);
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(i -> i.getArgument(0));

        
        subscriptionService.resetProfileViewCounts();

        
        assertEquals(0, sub1.getProfileViewsCount());
        assertEquals(0, sub2.getProfileViewsCount());
        assertTrue(sub1.getProfileViewsResetDate().isAfter(now));
        assertTrue(sub2.getProfileViewsResetDate().isAfter(now));
        verify(subscriptionRepository, times(2)).save(any(Subscription.class));
    }

    @Test
    void canSendMessage_WithExpiredSubscriptionAndAutoRenew_ShouldRenew() {
        
        SubscriptionDto expiredSubscription = SubscriptionDto.builder()
                .userId(userId)
                .plan(SubscriptionPlan.SILVER)
                .dailyMessageCount(5)
                .dailyMessageLimit(15)
                .expired(true)
                .autoRenew(true)
                .build();

        SubscriptionDto renewedSubscription = SubscriptionDto.builder()
                .userId(userId)
                .plan(SubscriptionPlan.SILVER)
                .dailyMessageCount(0)
                .dailyMessageLimit(15)
                .expired(false)
                .autoRenew(true)
                .build();

        
        SubscriptionServiceImpl spyService = spy(subscriptionService);

        
        doReturn(expiredSubscription, renewedSubscription)
                .when(spyService).getCurrentSubscription(userId);

        
        doNothing().when(spyService).processSubscriptionRenewal(userId, SubscriptionPlan.SILVER);

        
        boolean result = spyService.canSendMessage(userId);

        
        assertTrue(result);
        verify(spyService).processSubscriptionRenewal(userId, SubscriptionPlan.SILVER);
        verify(spyService, times(2)).getCurrentSubscription(userId);
    }
    @Test
    void canSendMessage_WithExpiredSubscriptionWithoutAutoRenew_ShouldSwitchToFree() {
        
        SubscriptionDto expiredSubscription = SubscriptionDto.builder()
                .userId(userId)
                .plan(SubscriptionPlan.SILVER)
                .dailyMessageCount(5)
                .dailyMessageLimit(15)
                .expired(true)
                .autoRenew(false)
                .build();

        SubscriptionDto freeSubscription = SubscriptionDto.builder()
                .userId(userId)
                .plan(SubscriptionPlan.FREE)
                .dailyMessageCount(0)
                .dailyMessageLimit(3)
                .expired(false)
                .autoRenew(false)
                .build();

        
        SubscriptionServiceImpl spyService = spy(subscriptionService);

        
        doReturn(expiredSubscription, freeSubscription)
                .when(spyService).getCurrentSubscription(userId);

        
        doNothing().when(spyService).switchToFreePlan(userId);

        
        boolean result = spyService.canSendMessage(userId);

        
        assertTrue(result);
        verify(spyService).switchToFreePlan(userId);
        verify(spyService, times(2)).getCurrentSubscription(userId);
    }

    @Test
    void deactivateExpiredSubscription_ShouldDeactivateAndCreateNewFree() {
        
        Subscription expiredSubscription = Subscription.builder()
                .id(UUID.randomUUID())
                .user(user)
                .plan(SubscriptionPlan.SILVER)
                .active(true)
                .endDate(LocalDateTime.now().minusDays(1))
                .build();

        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(expiredSubscription);

        
        SubscriptionServiceImpl spyService = spy(subscriptionService);

        
        doReturn(new Subscription()).when(spyService).createSubscription(userId, SubscriptionPlan.FREE, false);

        
        spyService.deactivateExpiredSubscription(expiredSubscription);

        
        assertFalse(expiredSubscription.isActive());
        verify(subscriptionRepository).save(expiredSubscription);
        verify(spyService).createSubscription(userId, SubscriptionPlan.FREE, false);
    }

    @Test
    void processSubscriptionRenewal_Indirectly_ThroughCanSendMessage() {
        
        SubscriptionDto expiredSubscription = SubscriptionDto.builder()
                .userId(userId)
                .plan(SubscriptionPlan.SILVER)
                .dailyMessageCount(5)
                .dailyMessageLimit(15)
                .expired(true)
                .autoRenew(true)
                .build();

        SubscriptionDto renewedSubscription = SubscriptionDto.builder()
                .userId(userId)
                .plan(SubscriptionPlan.SILVER)
                .dailyMessageCount(0)
                .dailyMessageLimit(15)
                .expired(false)
                .autoRenew(true)
                .build();

        
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            if (t.getId() == null) {
                t.setId(UUID.randomUUID());
            }
            return t;
        });

        
        SubscriptionServiceImpl spyService = spy(subscriptionService);

        
        doReturn(expiredSubscription, renewedSubscription)
                .when(spyService).getCurrentSubscription(userId);

        
        doReturn(new Subscription()).when(spyService).createSubscription(
                eq(userId), eq(SubscriptionPlan.SILVER), eq(true));

        
        boolean canSend = spyService.canSendMessage(userId);

        
        assertTrue(canSend);
        verify(transactionRepository).save(any(Transaction.class));
        verify(spyService).createSubscription(userId, SubscriptionPlan.SILVER, true);
    }

    @Test
    void switchToFreePlan_ShouldDeactivateCurrentAndCreateFreePlan() {
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByUserAndActive(user, true)).thenReturn(Optional.of(activeSubscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(activeSubscription);

        
        SubscriptionServiceImpl spyService = spy(subscriptionService);

        
        doReturn(new Subscription()).when(spyService).createSubscription(userId, SubscriptionPlan.FREE, false);

        
        try {
            java.lang.reflect.Method method = SubscriptionServiceImpl.class.getDeclaredMethod(
                    "switchToFreePlan", UUID.class);
            method.setAccessible(true);
            method.invoke(spyService, userId);
        } catch (Exception e) {
            fail("Failed to invoke private method: " + e.getMessage());
        }

        
        assertFalse(activeSubscription.isActive());
        verify(subscriptionRepository).save(activeSubscription);
        verify(spyService).createSubscription(userId, SubscriptionPlan.FREE, false);
    }
}