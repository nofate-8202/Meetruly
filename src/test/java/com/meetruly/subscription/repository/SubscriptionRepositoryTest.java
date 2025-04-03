package com.meetruly.subscription.repository;

import com.meetruly.core.constant.SubscriptionPlan;
import com.meetruly.subscription.model.Subscription;
import com.meetruly.user.model.User;
import com.meetruly.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Transactional
public class SubscriptionRepositoryTest {

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private Subscription testSubscription;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        
        subscriptionRepository.deleteAll();

        
        now = LocalDateTime.now();

        
        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password")
                .gender(com.meetruly.core.constant.Gender.MALE)
                .role(com.meetruly.core.constant.UserRole.USER)
                .accountNonLocked(true)
                .approved(true)
                .emailVerified(true)
                .enabled(true)
                .profileCompleted(false)
                .createdAt(now)
                .updatedAt(now)
                .build();
        userRepository.save(testUser);

        
        testSubscription = Subscription.builder()
                .user(testUser)
                .plan(SubscriptionPlan.SILVER)
                .startDate(now)
                .endDate(now.plusMonths(1))
                .active(true)
                .autoRenew(true)
                .dailyMessageCount(0)
                .profileViewsCount(0)
                .messageCountResetDate(now.plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0))
                .profileViewsResetDate(now.plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0))
                .createdAt(now)
                .updatedAt(now)
                .build();

        subscriptionRepository.save(testSubscription);
    }

    @Test

    void testFindByUserAndActive() {
        
        Optional<Subscription> result = subscriptionRepository.findByUserAndActive(testUser, true);

        
        assertTrue(result.isPresent());
        assertEquals(testSubscription.getId(), result.get().getId());
        assertEquals(SubscriptionPlan.SILVER, result.get().getPlan());
    }

    @Test

    void testFindByUserAndActive_WhenNoActiveSubscription() {
        
        testSubscription.setActive(false);
        subscriptionRepository.save(testSubscription);

        
        Optional<Subscription> result = subscriptionRepository.findByUserAndActive(testUser, true);

        
        assertFalse(result.isPresent());
    }

    @Test

    void testFindByUser() {
        
        Subscription inactiveSubscription = Subscription.builder()
                .user(testUser)
                .plan(SubscriptionPlan.FREE)
                .startDate(now.minusMonths(2))
                .endDate(now.minusMonths(1))
                .active(false)
                .autoRenew(false)
                .createdAt(now.minusMonths(2))
                .updatedAt(now.minusMonths(2))
                .build();
        subscriptionRepository.save(inactiveSubscription);

        
        List<Subscription> results = subscriptionRepository.findByUser(testUser);

        
        assertEquals(2, results.size());
    }

    @Test

    void testFindActiveSubscriptionByUserId() {
        
        Optional<Subscription> result = subscriptionRepository.findActiveSubscriptionByUserId(testUser.getId());

        
        assertTrue(result.isPresent());
        assertEquals(testSubscription.getId(), result.get().getId());
        assertEquals(SubscriptionPlan.SILVER, result.get().getPlan());
    }

    @Test

    void testFindSubscriptionsToRenew() {
        
        LocalDateTime startRenewalPeriod = now.plusDays(15);
        LocalDateTime endRenewalPeriod = now.plusDays(30);

        
        Subscription subscriptionToRenew = Subscription.builder()
                .user(testUser)
                .plan(SubscriptionPlan.GOLD)
                .startDate(now)
                .endDate(now.plusDays(20)) 
                .active(true)
                .autoRenew(true)
                .createdAt(now)
                .updatedAt(now)
                .build();
        subscriptionRepository.save(subscriptionToRenew);

        
        List<Subscription> results = subscriptionRepository.findSubscriptionsToRenew(startRenewalPeriod, endRenewalPeriod);

        
        assertTrue(results.stream().anyMatch(s -> s.getId().equals(subscriptionToRenew.getId())));
    }

    @Test

    void testFindSubscriptionsToResetMessageCount() {
        
        
        testSubscription.setMessageCountResetDate(now.minusDays(1));
        subscriptionRepository.save(testSubscription);

        
        List<Subscription> results = subscriptionRepository.findSubscriptionsToResetMessageCount(now);

        
        assertTrue(results.stream().anyMatch(s -> s.getId().equals(testSubscription.getId())));
    }

    @Test

    void testFindSubscriptionsToResetProfileViews() {
        
        
        testSubscription.setProfileViewsResetDate(now.minusDays(1));
        subscriptionRepository.save(testSubscription);

        
        List<Subscription> results = subscriptionRepository.findSubscriptionsToResetProfileViews(now);

        
        assertTrue(results.stream().anyMatch(s -> s.getId().equals(testSubscription.getId())));
    }

    @Test

    void testCountActiveByPlan() {
        
        
        Subscription anotherSubscription = Subscription.builder()
                .user(testUser)
                .plan(SubscriptionPlan.SILVER)
                .startDate(now)
                .endDate(now.plusMonths(1))
                .active(true)
                .autoRenew(true)
                .createdAt(now)
                .updatedAt(now)
                .build();
        subscriptionRepository.save(anotherSubscription);

        
        long count = subscriptionRepository.countActiveByPlan(SubscriptionPlan.SILVER);

        
        assertEquals(2, count);
    }

    @Test

    void testCountActiveSubscriptions() {
        
        
        Subscription anotherSubscription = Subscription.builder()
                .user(testUser)
                .plan(SubscriptionPlan.GOLD)
                .startDate(now)
                .endDate(now.plusMonths(1))
                .active(true)
                .autoRenew(true)
                .createdAt(now)
                .updatedAt(now)
                .build();
        subscriptionRepository.save(anotherSubscription);

        
        long count = subscriptionRepository.countActiveSubscriptions();

        
        assertEquals(2, count);
    }

    @Test

    void testFindByEndDateBefore() {
        
        
        Subscription expiredSubscription = Subscription.builder()
                .user(testUser)
                .plan(SubscriptionPlan.FREE)
                .startDate(now.minusMonths(2))
                .endDate(now.minusDays(1)) 
                .active(true) 
                .autoRenew(false)
                .createdAt(now.minusMonths(2))
                .updatedAt(now.minusMonths(2))
                .build();
        subscriptionRepository.save(expiredSubscription);

        
        List<Subscription> results = subscriptionRepository.findByEndDateBefore(now);

        
        assertTrue(results.stream().anyMatch(s -> s.getId().equals(expiredSubscription.getId())));
        assertFalse(results.stream().anyMatch(s -> s.getId().equals(testSubscription.getId())));
    }
}