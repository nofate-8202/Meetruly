package com.meetruly.subscription.repository;

import com.meetruly.core.constant.SubscriptionPlan;
import com.meetruly.subscription.model.Subscription;
import com.meetruly.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    Optional<Subscription> findByUserAndActive(User user, boolean active);

    List<Subscription> findByUser(User user);

    @Query("SELECT s FROM Subscription s WHERE s.user.id = :userId AND s.active = true")
    Optional<Subscription> findActiveSubscriptionByUserId(@Param("userId") UUID userId);

    @Query("SELECT s FROM Subscription s WHERE s.endDate BETWEEN :startDate AND :endDate AND s.autoRenew = true")
    List<Subscription> findSubscriptionsToRenew(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT s FROM Subscription s WHERE s.messageCountResetDate <= :now")
    List<Subscription> findSubscriptionsToResetMessageCount(@Param("now") LocalDateTime now);

    @Query("SELECT s FROM Subscription s WHERE s.profileViewsResetDate <= :now")
    List<Subscription> findSubscriptionsToResetProfileViews(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(s) FROM Subscription s WHERE s.plan = :plan AND s.active = true")
    long countActiveByPlan(@Param("plan") SubscriptionPlan plan);

    @Query("SELECT COUNT(s) FROM Subscription s WHERE s.active = true")
    long countActiveSubscriptions();

    List<Subscription> findByEndDateBefore(LocalDateTime date);
}