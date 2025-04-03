package com.meetruly.web.controller;

import com.meetruly.core.constant.SubscriptionPlan;
import com.meetruly.core.exception.MeetrulyException;
import com.meetruly.subscription.dto.SubscriptionDto;
import com.meetruly.subscription.dto.SubscriptionPurchaseRequest;
import com.meetruly.subscription.dto.SubscriptionSummaryDto;
import com.meetruly.subscription.dto.TransactionDto;
import com.meetruly.subscription.model.Transaction;
import com.meetruly.subscription.service.SubscriptionService;
import com.meetruly.user.model.User;
import com.meetruly.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SubscriptionController.class)
public class SubscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SubscriptionService subscriptionService;

    @MockBean
    private UserService userService;

    private UUID userId;
    private User testUser;
    private SubscriptionDto testSubscription;
    private SubscriptionSummaryDto testSummary;
    private Transaction testTransaction;
    private List<TransactionDto> testTransactions;
    private List<SubscriptionDto> testSubscriptionHistory;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();


        testUser = User.builder()
                .id(userId)
                .username("testuser")
                .email("test@example.com")
                .approved(true)
                .enabled(true)
                .build();


        when(userService.isUserApproved("testuser")).thenReturn(true);
        when(userService.isUserApproved("nonexistentuser")).thenReturn(false);


        testSubscription = SubscriptionDto.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .username("testuser")
                .plan(SubscriptionPlan.SILVER)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusMonths(1))
                .active(true)
                .autoRenew(true)
                .dailyMessageCount(5)
                .profileViewsCount(3)
                .dailyMessageLimit(15)
                .profileViewsLimit(10)
                .expired(false)
                .hasReachedMessageLimit(false)
                .hasReachedProfileViewLimit(false)
                .build();


        testSummary = SubscriptionSummaryDto.builder()
                .currentPlan(SubscriptionPlan.SILVER)
                .remainingMessages(10)
                .remainingProfileViews(7)
                .validUntil(LocalDateTime.now().plusMonths(1).format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")))
                .autoRenew(true)
                .freeDailyMessageLimit(3)
                .silverDailyMessageLimit(15)
                .goldDailyMessageLimit(999)
                .freeProfileViewsLimit(3)
                .silverProfileViewsLimit(10)
                .goldProfileViewsLimit(999)
                .silverPrice(9.99)
                .goldPrice(19.99)
                .build();


        testTransaction = Transaction.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .transactionReference("TRX-12345678")
                .plan(SubscriptionPlan.SILVER)
                .amount(9.99)
                .currency("EUR")
                .status(Transaction.TransactionStatus.COMPLETED)
                .type(Transaction.TransactionType.SUBSCRIPTION_PURCHASE)
                .paymentMethod("credit_card")
                .createdAt(LocalDateTime.now())
                .processedAt(LocalDateTime.now())
                .build();


        testTransactions = Arrays.asList(
                TransactionDto.builder()
                        .id(UUID.randomUUID())
                        .userId(userId)
                        .username("testuser")
                        .transactionReference("TRX-12345678")
                        .plan(SubscriptionPlan.SILVER)
                        .amount(9.99)
                        .currency("EUR")
                        .status(Transaction.TransactionStatus.COMPLETED)
                        .type(Transaction.TransactionType.SUBSCRIPTION_PURCHASE)
                        .paymentMethod("credit_card")
                        .createdAt(LocalDateTime.now().minusDays(30))
                        .processedAt(LocalDateTime.now().minusDays(30))
                        .build(),
                TransactionDto.builder()
                        .id(UUID.randomUUID())
                        .userId(userId)
                        .username("testuser")
                        .transactionReference("TRX-87654321")
                        .plan(SubscriptionPlan.GOLD)
                        .amount(19.99)
                        .currency("EUR")
                        .status(Transaction.TransactionStatus.COMPLETED)
                        .type(Transaction.TransactionType.SUBSCRIPTION_PURCHASE)
                        .paymentMethod("paypal")
                        .createdAt(LocalDateTime.now())
                        .processedAt(LocalDateTime.now())
                        .build()
        );


        testSubscriptionHistory = Arrays.asList(
                testSubscription,
                SubscriptionDto.builder()
                        .id(UUID.randomUUID())
                        .userId(userId)
                        .username("testuser")
                        .plan(SubscriptionPlan.FREE)
                        .startDate(LocalDateTime.now().minusMonths(2))
                        .endDate(LocalDateTime.now().minusMonths(1))
                        .active(false)
                        .build()
        );
    }

    @Test

    @WithMockUser(username = "testuser")
    void showSubscriptionInfo_WhenAuthenticated_ShouldReturnSubscriptionInfo() throws Exception {

        when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(subscriptionService.getCurrentSubscription(userId)).thenReturn(testSubscription);
        when(subscriptionService.getSubscriptionSummary(userId)).thenReturn(testSummary);


        mockMvc.perform(get("/subscription"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name("subscription/info"))
                .andExpect(model().attributeExists("subscription"))
                .andExpect(model().attributeExists("summary"))
                .andExpect(model().attribute("subscription", testSubscription))
                .andExpect(model().attribute("summary", testSummary));

        verify(userService).getUserByUsername("testuser");
        verify(subscriptionService).getCurrentSubscription(userId);
        verify(subscriptionService).getSubscriptionSummary(userId);
    }

    @Test
    @WithMockUser(username = "nonexistentuser")
    void showSubscriptionInfo_WhenUserNotFound_ShouldRedirectWithError() throws Exception {

        when(userService.isUserApproved("nonexistentuser")).thenReturn(true);
        when(userService.getUserByUsername("nonexistentuser")).thenReturn(Optional.empty());


        mockMvc.perform(get("/subscription"))
                .andDo(print())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"));


        verify(userService).getUserByUsername("nonexistentuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    void showSubscriptionPlans_WhenAuthenticated_ShouldShowPlans() throws Exception {

        when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(subscriptionService.getCurrentSubscription(userId)).thenReturn(testSubscription);


        mockMvc.perform(get("/subscription/plans"))
                .andExpect(status().isOk())
                .andExpect(view().name("subscription/plans"))
                .andExpect(model().attributeExists("currentPlan"))
                .andExpect(model().attribute("currentPlan", SubscriptionPlan.SILVER))
                .andExpect(model().attributeExists("freePlan"))
                .andExpect(model().attributeExists("silverPlan"))
                .andExpect(model().attributeExists("goldPlan"));

        verify(userService).getUserByUsername("testuser");
        verify(subscriptionService).getCurrentSubscription(userId);
    }

    @Test

    @WithMockUser(username = "testuser")
    void showPurchaseForm_WithValidPlan_ShouldShowForm() throws Exception {

        when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(testUser));


        mockMvc.perform(get("/subscription/purchase/silver"))
                .andExpect(status().isOk())
                .andExpect(view().name("subscription/purchase"))
                .andExpect(model().attributeExists("purchaseRequest"))
                .andExpect(model().attributeExists("plan"))
                .andExpect(model().attribute("plan", SubscriptionPlan.SILVER));

        verify(userService).getUserByUsername("testuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    void showPurchaseForm_WithFreePlan_ShouldRedirectToSwitchToFree() throws Exception {

        mockMvc.perform(get("/subscription/purchase/free"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/subscription/switch-to-free"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void processPurchase_WithValidData_ShouldProcessAndRedirect() throws Exception {

        when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(subscriptionService.processSubscriptionPurchase(eq(userId), any(SubscriptionPurchaseRequest.class)))
                .thenReturn(testTransaction);


        mockMvc.perform(post("/subscription/purchase")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("plan", "SILVER")
                        .param("autoRenew", "true")
                        .param("paymentMethod", "credit_card")
                        .param("cardNumber", "4111111111111111")
                        .param("cardExpiryDate", "12/25")
                        .param("cardCVV", "123")
                        .param("cardHolderName", "Test User"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/subscription"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(userService).getUserByUsername("testuser");
        verify(subscriptionService).processSubscriptionPurchase(eq(userId), any(SubscriptionPurchaseRequest.class));
    }

    @Test

    @WithMockUser(username = "testuser")
    void switchToFreePlan_WhenAuthenticated_ShouldSwitchAndRedirect() throws Exception {

        when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(testUser));
        doNothing().when(subscriptionService).cancelSubscription(userId);


        mockMvc.perform(get("/subscription/switch-to-free"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/subscription"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(userService).getUserByUsername("testuser");
        verify(subscriptionService).cancelSubscription(userId);
    }

    @Test

    @WithMockUser(username = "testuser")
    void toggleAutoRenew_WhenEnabling_ShouldEnableAndRedirect() throws Exception {

        when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(testUser));
        doNothing().when(subscriptionService).enableAutoRenewal(userId);


        mockMvc.perform(post("/subscription/toggle-auto-renew")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("autoRenew", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/subscription"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(userService).getUserByUsername("testuser");
        verify(subscriptionService).enableAutoRenewal(userId);
    }

    @Test

    @WithMockUser(username = "testuser")
    void toggleAutoRenew_WhenDisabling_ShouldDisableAndRedirect() throws Exception {

        when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(testUser));
        doNothing().when(subscriptionService).cancelAutoRenewal(userId);


        mockMvc.perform(post("/subscription/toggle-auto-renew")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("autoRenew", "false"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/subscription"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(userService).getUserByUsername("testuser");
        verify(subscriptionService).cancelAutoRenewal(userId);
    }

    @Test

    @WithMockUser(username = "testuser")
    void showTransactionHistory_WhenAuthenticated_ShouldShowHistory() throws Exception {

        when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(subscriptionService.getUserTransactions(userId)).thenReturn(testTransactions);


        mockMvc.perform(get("/subscription/transactions"))
                .andExpect(status().isOk())
                .andExpect(view().name("subscription/transactions"))
                .andExpect(model().attributeExists("transactions"))
                .andExpect(model().attribute("transactions", testTransactions));

        verify(userService).getUserByUsername("testuser");
        verify(subscriptionService).getUserTransactions(userId);
    }

    @Test

    @WithMockUser(username = "testuser")
    void showSubscriptionHistory_WhenAuthenticated_ShouldShowHistory() throws Exception {

        when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(subscriptionService.getUserSubscriptionHistory(userId)).thenReturn(testSubscriptionHistory);


        mockMvc.perform(get("/subscription/history"))
                .andExpect(status().isOk())
                .andExpect(view().name("subscription/history"))
                .andExpect(model().attributeExists("subscriptions"))
                .andExpect(model().attribute("subscriptions", testSubscriptionHistory));

        verify(userService).getUserByUsername("testuser");
        verify(subscriptionService).getUserSubscriptionHistory(userId);
    }

    @Test

    @WithMockUser(username = "testuser")
    void processPurchase_WhenExceptionOccurs_ShouldRedirectWithError() throws Exception {

        when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(subscriptionService.processSubscriptionPurchase(eq(userId), any(SubscriptionPurchaseRequest.class)))
                .thenThrow(new MeetrulyException("Payment failed"));


        mockMvc.perform(post("/subscription/purchase")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("plan", "SILVER")
                        .param("autoRenew", "true")
                        .param("paymentMethod", "credit_card")
                        .param("cardNumber", "4111111111111111")
                        .param("cardExpiryDate", "12/25")
                        .param("cardCVV", "123")
                        .param("cardHolderName", "Test User"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/subscription/plans"))
                .andExpect(flash().attribute("errorMessage", "Payment failed"));

        verify(userService).getUserByUsername("testuser");
        verify(subscriptionService).processSubscriptionPurchase(eq(userId), any(SubscriptionPurchaseRequest.class));
    }
}