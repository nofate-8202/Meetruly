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

                String username = principal.getName();
                User user = userService.getUserByUsername(username)
                        .orElseThrow(() -> new MeetrulyException("User not found: " + username));

                MatchingSummaryDto matchingSummary = matchingService.getMatchingSummary(user.getId());
                model.addAttribute("matchingSummary", matchingSummary);

                ChatSummaryDto chatSummary = chatService.getChatSummary(user.getId());
                model.addAttribute("chatSummary", chatSummary);

                SubscriptionSummaryDto subscriptionSummary = subscriptionService.getSubscriptionSummary(user.getId());
                model.addAttribute("subscriptionSummary", subscriptionSummary);

                SubscriptionDto currentSubscription = subscriptionService.getCurrentSubscription(user.getId());
                model.addAttribute("currentSubscription", currentSubscription);

                model.addAttribute("unreadMessages", chatSummary.getUnreadMessages());
                model.addAttribute("unviewedMatches", matchingSummary.getUnviewedMatches());

            } catch (Exception e) {
                log.error("Error retrieving user data for home page", e);

            }
        }

        return "home";
    }
}