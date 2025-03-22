package com.meetruly.subscription.repository;

import com.meetruly.core.constant.SubscriptionPlan;
import com.meetruly.subscription.model.Transaction;
import com.meetruly.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByTransactionReference(String transactionReference);

    List<Transaction> findByUser(User user);

    Page<Transaction> findByUser(User user, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.createdAt BETWEEN :startDate AND :endDate")
    List<Transaction> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT t FROM Transaction t WHERE t.createdAt BETWEEN :startDate AND :endDate AND t.status = 'COMPLETED'")
    List<Transaction> findCompletedByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.createdAt BETWEEN :startDate AND :endDate AND t.status = 'COMPLETED'")
    Double calculateRevenueForPeriod(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.plan = :plan AND t.createdAt BETWEEN :startDate AND :endDate AND t.status = 'COMPLETED'")
    Double calculateRevenueByPlanForPeriod(
            @Param("plan") SubscriptionPlan plan,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.plan = :plan AND t.status = 'COMPLETED'")
    long countCompletedByPlan(@Param("plan") SubscriptionPlan plan);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.status = 'COMPLETED'")
    Double calculateTotalRevenue();

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.plan = :plan AND t.status = 'COMPLETED'")
    Double calculateRevenueByPlan(@Param("plan") SubscriptionPlan plan);
}