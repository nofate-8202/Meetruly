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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/subscription")
@RequiredArgsConstructor
@Slf4j
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final UserService userService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public String showSubscriptionInfo(Model model, Principal principal) {
        try {

            String username = principal.getName();
            User user = userService.getUserByUsername(username)
                    .orElseThrow(() -> new MeetrulyException("User not found: " + username));

            SubscriptionDto subscription = subscriptionService.getCurrentSubscription(user.getId());

            SubscriptionSummaryDto summary = subscriptionService.getSubscriptionSummary(user.getId());

            model.addAttribute("subscription", subscription);
            model.addAttribute("summary", summary);

            return "subscription/info";
        } catch (MeetrulyException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "redirect:/home";
        }
    }

    @GetMapping("/plans")
    @PreAuthorize("isAuthenticated()")
    public String showSubscriptionPlans(Model model, Principal principal) {
        try {

            String username = principal.getName();
            User user = userService.getUserByUsername(username)
                    .orElseThrow(() -> new MeetrulyException("User not found: " + username));

            SubscriptionDto subscription = subscriptionService.getCurrentSubscription(user.getId());

            model.addAttribute("currentPlan", subscription.getPlan());
            model.addAttribute("freePlan", SubscriptionPlan.FREE);
            model.addAttribute("silverPlan", SubscriptionPlan.SILVER);
            model.addAttribute("goldPlan", SubscriptionPlan.GOLD);

            return "subscription/plans";
        } catch (MeetrulyException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "redirect:/home";
        }
    }

    @GetMapping("/purchase/{plan}")
    @PreAuthorize("isAuthenticated()")
    public String showPurchaseForm(@PathVariable("plan") String planStr, Model model, Principal principal) {
        try {

            SubscriptionPlan plan = SubscriptionPlan.valueOf(planStr.toUpperCase());

            if (plan == SubscriptionPlan.FREE) {
                return "redirect:/subscription/switch-to-free";
            }

            String username = principal.getName();
            User user = userService.getUserByUsername(username)
                    .orElseThrow(() -> new MeetrulyException("User not found: " + username));

            SubscriptionPurchaseRequest purchaseRequest = new SubscriptionPurchaseRequest();
            purchaseRequest.setPlan(plan);

            model.addAttribute("purchaseRequest", purchaseRequest);
            model.addAttribute("plan", plan);

            return "subscription/purchase";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", "Invalid subscription plan");
            return "redirect:/subscription/plans";
        } catch (MeetrulyException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "redirect:/subscription/plans";
        }
    }

    @PostMapping("/purchase")
    @PreAuthorize("isAuthenticated()")
    public String processPurchase(@Valid @ModelAttribute("purchaseRequest") SubscriptionPurchaseRequest purchaseRequest,
                                  BindingResult result, Principal principal, RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            return "subscription/purchase";
        }

        try {

            String username = principal.getName();
            User user = userService.getUserByUsername(username)
                    .orElseThrow(() -> new MeetrulyException("User not found: " + username));

            Transaction transaction = subscriptionService.processSubscriptionPurchase(user.getId(), purchaseRequest);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Subscription purchased successfully. Your transaction reference is: " + transaction.getTransactionReference());
            return "redirect:/subscription";
        } catch (MeetrulyException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/subscription/plans";
        }
    }

    @GetMapping("/switch-to-free")
    @PreAuthorize("isAuthenticated()")
    public String switchToFreePlan(Principal principal, RedirectAttributes redirectAttributes) {
        try {

            String username = principal.getName();
            User user = userService.getUserByUsername(username)
                    .orElseThrow(() -> new MeetrulyException("User not found: " + username));

            subscriptionService.cancelSubscription(user.getId());

            redirectAttributes.addFlashAttribute("successMessage", "Switched to FREE plan successfully");
            return "redirect:/subscription";
        } catch (MeetrulyException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/subscription";
        }
    }

    @PostMapping("/toggle-auto-renew")
    @PreAuthorize("isAuthenticated()")
    public String toggleAutoRenew(@RequestParam("autoRenew") boolean autoRenew,
                                  Principal principal, RedirectAttributes redirectAttributes) {
        try {

            String username = principal.getName();
            User user = userService.getUserByUsername(username)
                    .orElseThrow(() -> new MeetrulyException("User not found: " + username));

            if (autoRenew) {
                subscriptionService.enableAutoRenewal(user.getId());
                redirectAttributes.addFlashAttribute("successMessage", "Auto-renewal enabled successfully");
            } else {
                subscriptionService.cancelAutoRenewal(user.getId());
                redirectAttributes.addFlashAttribute("successMessage", "Auto-renewal disabled successfully");
            }

            return "redirect:/subscription";
        } catch (MeetrulyException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/subscription";
        }
    }

    @GetMapping("/transactions")
    @PreAuthorize("isAuthenticated()")
    public String showTransactionHistory(Model model, Principal principal) {
        try {

            String username = principal.getName();
            User user = userService.getUserByUsername(username)
                    .orElseThrow(() -> new MeetrulyException("User not found: " + username));

            List<TransactionDto> transactions = subscriptionService.getUserTransactions(user.getId());

            model.addAttribute("transactions", transactions);

            return "subscription/transactions";
        } catch (MeetrulyException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "redirect:/subscription";
        }
    }

    @GetMapping("/history")
    @PreAuthorize("isAuthenticated()")
    public String showSubscriptionHistory(Model model, Principal principal) {
        try {

            String username = principal.getName();
            User user = userService.getUserByUsername(username)
                    .orElseThrow(() -> new MeetrulyException("User not found: " + username));

            List<SubscriptionDto> history = subscriptionService.getUserSubscriptionHistory(user.getId());

            model.addAttribute("subscriptions", history);

            return "subscription/history";
        } catch (MeetrulyException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "redirect:/subscription";
        }
    }
}