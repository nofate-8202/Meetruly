package com.meetruly.web.controller;

import com.meetruly.chat.dto.ChatSummaryDto;
import com.meetruly.chat.service.ChatService;
import com.meetruly.core.exception.MeetrulyException;
import com.meetruly.matching.dto.MatchingSummaryDto;
import com.meetruly.matching.dto.ProfileCardDto;
import com.meetruly.matching.service.MatchingService;
import com.meetruly.subscription.dto.SubscriptionDto;
import com.meetruly.subscription.dto.SubscriptionSummaryDto;
import com.meetruly.subscription.service.SubscriptionService;
import com.meetruly.user.model.User;
import com.meetruly.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.util.Collections;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class HomeController {

    private final UserService userService;
    private final MatchingService matchingService;
    private final ChatService chatService;
    private final SubscriptionService subscriptionService;

    @GetMapping({"/", "/home"})
    public String home(Model model, Principal principal) {

        if (principal != null) {
            try {
                // Зареждане на потребителя
                String username = principal.getName();
                User user = null;

                try {
                    user = userService.getUserByUsername(username)
                            .orElse(null);

                    if (user == null) {
                        log.error("User not found: {}", username);
                        model.addAttribute("errorMessage", "User not found. Please contact support.");
                        return "home";
                    }
                } catch (Exception e) {
                    log.error("Error retrieving user: {}", e.getMessage());
                    model.addAttribute("errorMessage", "Error retrieving user data. Please try again later.");
                    return "home";
                }

                // Зареждане на информация за съвпаденията
                MatchingSummaryDto matchingSummary = null;
                try {
                    matchingSummary = matchingService.getMatchingSummary(user.getId());

                    // Ако matchingSummary е null, създаваме празен обект
                    if (matchingSummary == null) {
                        matchingSummary = new MatchingSummaryDto();
                        matchingSummary.setDailySuggestions(Collections.emptyList());
                        matchingSummary.setTopLikedUsers(Collections.emptyList());
                    }

                    // Ако списъците са null, създаваме празни
                    if (matchingSummary.getDailySuggestions() == null) {
                        matchingSummary.setDailySuggestions(Collections.emptyList());
                    }
                    if (matchingSummary.getTopLikedUsers() == null) {
                        matchingSummary.setTopLikedUsers(Collections.emptyList());
                    }
                } catch (Exception e) {
                    log.error("Error retrieving matching summary: {}", e.getMessage());
                    matchingSummary = new MatchingSummaryDto();
                    matchingSummary.setDailySuggestions(Collections.emptyList());
                    matchingSummary.setTopLikedUsers(Collections.emptyList());
                }
                model.addAttribute("matchingSummary", matchingSummary);

                // Зареждане на информация за чат съобщенията
                ChatSummaryDto chatSummary = null;
                try {
                    chatSummary = chatService.getChatSummary(user.getId());
                    if (chatSummary == null) {
                        chatSummary = new ChatSummaryDto(); // Създаваме празен обект ако е null
                        chatSummary.setUnreadMessages(0); // Задаваме default стойност
                    }
                } catch (Exception e) {
                    log.error("Error retrieving chat summary: {}", e.getMessage());
                    chatSummary = new ChatSummaryDto();
                    chatSummary.setUnreadMessages(0);
                }
                model.addAttribute("chatSummary", chatSummary);

                // Зареждане на информация за абонамента
                SubscriptionSummaryDto subscriptionSummary = null;
                SubscriptionDto currentSubscription = null;
                try {
                    subscriptionSummary = subscriptionService.getSubscriptionSummary(user.getId());
                    currentSubscription = subscriptionService.getCurrentSubscription(user.getId());
                } catch (Exception e) {
                    log.error("Error retrieving subscription data: {}", e.getMessage());
                }
                model.addAttribute("subscriptionSummary", subscriptionSummary);
                model.addAttribute("currentSubscription", currentSubscription);

                // Добавяне на допълнителни атрибути на модела
                model.addAttribute("unreadMessages", chatSummary.getUnreadMessages());
                model.addAttribute("unviewedMatches", matchingSummary.getUnviewedMatches());

            } catch (Exception e) {
                log.error("Error retrieving user data for home page", e);
                model.addAttribute("errorMessage", "An error occurred while loading your data. Please try again later.");
            }
        }

        return "home";
    }
}