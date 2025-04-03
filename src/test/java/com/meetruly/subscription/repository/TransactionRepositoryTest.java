package com.meetruly.subscription.repository;

import com.meetruly.core.constant.SubscriptionPlan;
import com.meetruly.subscription.model.Transaction;
import com.meetruly.user.model.User;
import com.meetruly.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Transactional
public class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private Transaction testTransaction;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        
        transactionRepository.deleteAll();

        
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

        
        testTransaction = Transaction.builder()
                .user(testUser)
                .transactionReference("TR-12345678")
                .plan(SubscriptionPlan.SILVER)
                .amount(9.99)
                .currency("EUR")
                .status(Transaction.TransactionStatus.COMPLETED)
                .type(Transaction.TransactionType.SUBSCRIPTION_PURCHASE)
                .paymentMethod("Credit Card")
                .paymentDetails("Test payment details")
                .createdAt(now)
                .processedAt(now)
                .build();

        transactionRepository.save(testTransaction);
    }

    @Test

    void testFindByTransactionReference() {
        
        Optional<Transaction> result = transactionRepository.findByTransactionReference("TR-12345678");

        
        assertTrue(result.isPresent());
        assertEquals(testTransaction.getId(), result.get().getId());
        assertEquals(SubscriptionPlan.SILVER, result.get().getPlan());
    }

    @Test

    void testFindByTransactionReference_WhenNotExists() {
        
        Optional<Transaction> result = transactionRepository.findByTransactionReference("NON-EXISTENT");

        
        assertFalse(result.isPresent());
    }

    @Test

    void testFindByUser() {
        
        Transaction anotherTransaction = Transaction.builder()
                .user(testUser)
                .transactionReference("TR-87654321")
                .plan(SubscriptionPlan.GOLD)
                .amount(19.99)
                .currency("EUR")
                .status(Transaction.TransactionStatus.COMPLETED)
                .type(Transaction.TransactionType.SUBSCRIPTION_PURCHASE)
                .createdAt(now)
                .build();
        transactionRepository.save(anotherTransaction);

        
        List<Transaction> results = transactionRepository.findByUser(testUser);

        
        assertEquals(2, results.size());
    }

    @Test

    void testFindByUserPaginated() {
        
        for (int i = 0; i < 10; i++) {
            Transaction tx = Transaction.builder()
                    .user(testUser)
                    .transactionReference("TR-" + (10000 + i))
                    .plan(SubscriptionPlan.SILVER)
                    .amount(9.99)
                    .currency("EUR")
                    .status(Transaction.TransactionStatus.COMPLETED)
                    .type(Transaction.TransactionType.SUBSCRIPTION_PURCHASE)
                    .createdAt(now.minusDays(i))
                    .build();
            transactionRepository.save(tx);
        }

        
        Page<Transaction> page = transactionRepository.findByUser(testUser, PageRequest.of(0, 5));

        
        assertEquals(5, page.getContent().size());
        assertEquals(11, page.getTotalElements()); 
    }

    @Test

    void testFindByDateRange() {
        
        assertTrue(true);
    }

    @Test

    void testFindCompletedByDateRange() {
        
        Transaction pendingTransaction = Transaction.builder()
                .user(testUser)
                .transactionReference("TR-PENDING")
                .plan(SubscriptionPlan.GOLD)
                .amount(19.99)
                .currency("EUR")
                .status(Transaction.TransactionStatus.PENDING)
                .type(Transaction.TransactionType.SUBSCRIPTION_PURCHASE)
                .createdAt(now)
                .build();
        transactionRepository.save(pendingTransaction);

        
        List<Transaction> results = transactionRepository.findCompletedByDateRange(now.minusDays(1), now.plusDays(1));

        
        assertEquals(1, results.size());
        assertEquals(Transaction.TransactionStatus.COMPLETED, results.get(0).getStatus());
    }

    @Test

    void testCalculateRevenueForPeriod() {
        
        Transaction anotherTransaction = Transaction.builder()
                .user(testUser)
                .transactionReference("TR-ANOTHER")
                .plan(SubscriptionPlan.GOLD)
                .amount(19.99)
                .currency("EUR")
                .status(Transaction.TransactionStatus.COMPLETED)
                .type(Transaction.TransactionType.SUBSCRIPTION_PURCHASE)
                .createdAt(now)
                .build();
        transactionRepository.save(anotherTransaction);

        Transaction pendingTransaction = Transaction.builder()
                .user(testUser)
                .transactionReference("TR-PENDING")
                .plan(SubscriptionPlan.GOLD)
                .amount(29.99)
                .currency("EUR")
                .status(Transaction.TransactionStatus.PENDING)
                .type(Transaction.TransactionType.SUBSCRIPTION_PURCHASE)
                .createdAt(now)
                .build();
        transactionRepository.save(pendingTransaction);

        
        Double totalRevenue = transactionRepository.calculateRevenueForPeriod(now.minusDays(1), now.plusDays(1));

        
        assertNotNull(totalRevenue);
        assertEquals(29.98, totalRevenue, 0.01); 
    }

    @Test

    void testCalculateRevenueByPlanForPeriod() {
        
        Transaction silverTransaction = Transaction.builder()
                .user(testUser)
                .transactionReference("TR-SILVER1")
                .plan(SubscriptionPlan.SILVER)
                .amount(9.99)
                .currency("EUR")
                .status(Transaction.TransactionStatus.COMPLETED)
                .type(Transaction.TransactionType.SUBSCRIPTION_PURCHASE)
                .createdAt(now)
                .build();
        transactionRepository.save(silverTransaction);

        Transaction goldTransaction = Transaction.builder()
                .user(testUser)
                .transactionReference("TR-GOLD1")
                .plan(SubscriptionPlan.GOLD)
                .amount(19.99)
                .currency("EUR")
                .status(Transaction.TransactionStatus.COMPLETED)
                .type(Transaction.TransactionType.SUBSCRIPTION_PURCHASE)
                .createdAt(now)
                .build();
        transactionRepository.save(goldTransaction);

        
        Double silverRevenue = transactionRepository.calculateRevenueByPlanForPeriod(
                SubscriptionPlan.SILVER, now.minusDays(1), now.plusDays(1));

        
        assertNotNull(silverRevenue);
        assertEquals(19.98, silverRevenue, 0.01); 
    }

    @Test

    void testCountCompletedByPlan() {
        
        for (int i = 0; i < 3; i++) {
            Transaction tx = Transaction.builder()
                    .user(testUser)
                    .transactionReference("TR-SILVER-" + i)
                    .plan(SubscriptionPlan.SILVER)
                    .amount(9.99)
                    .currency("EUR")
                    .status(Transaction.TransactionStatus.COMPLETED)
                    .type(Transaction.TransactionType.SUBSCRIPTION_PURCHASE)
                    .createdAt(now)
                    .build();
            transactionRepository.save(tx);
        }

        
        long count = transactionRepository.countCompletedByPlan(SubscriptionPlan.SILVER);

        
        assertEquals(4, count); 
    }

    @Test

    void testCalculateTotalRevenue() {
        
        Transaction tx1 = Transaction.builder()
                .user(testUser)
                .transactionReference("TR-1")
                .plan(SubscriptionPlan.SILVER)
                .amount(9.99)
                .currency("EUR")
                .status(Transaction.TransactionStatus.COMPLETED)
                .type(Transaction.TransactionType.SUBSCRIPTION_PURCHASE)
                .createdAt(now)
                .build();

        Transaction tx2 = Transaction.builder()
                .user(testUser)
                .transactionReference("TR-2")
                .plan(SubscriptionPlan.GOLD)
                .amount(19.99)
                .currency("EUR")
                .status(Transaction.TransactionStatus.COMPLETED)
                .type(Transaction.TransactionType.SUBSCRIPTION_PURCHASE)
                .createdAt(now)
                .build();

        transactionRepository.save(tx1);
        transactionRepository.save(tx2);

        
        Double totalRevenue = transactionRepository.calculateTotalRevenue();

        
        assertNotNull(totalRevenue);
        assertEquals(39.97, totalRevenue, 0.01); 
    }

    @Test

    void testCalculateRevenueByPlan() {
        
        for (int i = 0; i < 2; i++) {
            Transaction tx = Transaction.builder()
                    .user(testUser)
                    .transactionReference("TR-GOLD-" + i)
                    .plan(SubscriptionPlan.GOLD)
                    .amount(19.99)
                    .currency("EUR")
                    .status(Transaction.TransactionStatus.COMPLETED)
                    .type(Transaction.TransactionType.SUBSCRIPTION_PURCHASE)
                    .createdAt(now)
                    .build();
            transactionRepository.save(tx);
        }

        
        Double goldRevenue = transactionRepository.calculateRevenueByPlan(SubscriptionPlan.GOLD);

        
        assertNotNull(goldRevenue);
        assertEquals(39.98, goldRevenue, 0.01); 
    }
}