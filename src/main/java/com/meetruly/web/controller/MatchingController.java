package com.meetruly.web.controller;

import com.meetruly.core.exception.MeetrulyException;
import com.meetruly.matching.dto.*;
import com.meetruly.matching.service.MatchingService;
import com.meetruly.subscription.service.SubscriptionService;
import com.meetruly.user.model.User;
import com.meetruly.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/matching")
@RequiredArgsConstructor
@Slf4j
public class MatchingController {

    private final MatchingService matchingService;
    private final UserService userService;
    private final SubscriptionService subscriptionService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public String showMatchingDashboard(Model model, Principal principal) {
        try {

            String username = principal.getName();
            User user = userService.getUserByUsername(username)
                    .orElseThrow(() -> new MeetrulyException("User not found: " + username));

            MatchingSummaryDto summary = matchingService.getMatchingSummary(user.getId());

            model.addAttribute("summary", summary);

            return "matching/dashboard";
        } catch (MeetrulyException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "redirect:/home";
        }
    }

    @PostMapping("/like/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> likeUser(@PathVariable("userId") UUID userId, Principal principal) {
        try {

            String username = principal.getName();
            User currentUser = userService.getUserByUsername(username)
                    .orElseThrow(() -> new MeetrulyException("User not found: " + username));

            if (!subscriptionService.canSendMessage(currentUser.getId())) {
                return ResponseEntity.badRequest()
                        .body("You have reached your daily message limit. Please upgrade your subscription plan.");
            }

            LikeDto like = matchingService.likeUser(currentUser.getId(), userId);

            return ResponseEntity.ok(like);
        } catch (MeetrulyException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/like/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> unlikeUser(@PathVariable("userId") UUID userId, Principal principal) {
        try {

            String username = principal.getName();
            User currentUser = userService.getUserByUsername(username)
                    .orElseThrow(() -> new MeetrulyException("User not found: " + username));

            matchingService.unlikeUser(currentUser.getId(), userId);

            return ResponseEntity.ok().build();
        } catch (MeetrulyException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/likes/received")
    @PreAuthorize("isAuthenticated()")
    public String showReceivedLikes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model, Principal principal) {

        try {

            String username = principal.getName();
            User user = userService.getUserByUsername(username)
                    .orElseThrow(() -> new MeetrulyException("User not found: " + username));

            boolean isGoldSubscriber = subscriptionService.getCurrentSubscription(user.getId())
                    .getPlan().name().equals("GOLD");

            if (!isGoldSubscriber) {
                model.addAttribute("errorMessage",
                        "You need a GOLD subscription to see who liked you. Please upgrade your plan.");
                return "redirect:/subscription/plans";
            }

            List<LikeDto> likes = matchingService.getLikesByUser(user.getId(), false);

            for (LikeDto like : likes) {
                if (!like.isViewed()) {
                    matchingService.markLikeAsViewed(like.getId());
                }
            }

            model.addAttribute("likes", likes);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", (int) Math.ceil((double) likes.size() / size));

            return "matching/received-likes";
        } catch (MeetrulyException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "redirect:/matching";
        }
    }

    @GetMapping("/likes/sent")
    @PreAuthorize("isAuthenticated()")
    public String showSentLikes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model, Principal principal) {

        try {

            String username = principal.getName();
            User user = userService.getUserByUsername(username)
                    .orElseThrow(() -> new MeetrulyException("User not found: " + username));

            List<LikeDto> likes = matchingService.getLikesByUser(user.getId(), true);

            model.addAttribute("likes", likes);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", (int) Math.ceil((double) likes.size() / size));

            return "matching/sent-likes";
        } catch (MeetrulyException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "redirect:/matching";
        }
    }

    @GetMapping("/matches")
    @PreAuthorize("isAuthenticated()")
    public String showMatches(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model, Principal principal) {

        try {

            String username = principal.getName();
            User user = userService.getUserByUsername(username)
                    .orElseThrow(() -> new MeetrulyException("User not found: " + username));

            PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

            Page<MatchDto> matchesPage = matchingService.getMatchesByUser(user.getId(), pageRequest);

            for (MatchDto match : matchesPage.getContent()) {
                if (!match.isViewed()) {
                    matchingService.markMatchAsViewedByUser(match.getId(), user.getId());
                }
            }

            model.addAttribute("matches", matchesPage.getContent());
            model.addAttribute("currentPage", matchesPage.getNumber());
            model.addAttribute("totalPages", matchesPage.getTotalPages());

            return "matching/matches";
        } catch (MeetrulyException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "redirect:/matching";
        }
    }

    @GetMapping("/daily-suggestions")
    @PreAuthorize("isAuthenticated()")
    public String showDailySuggestions(Model model, Principal principal) {
        try {

            String username = principal.getName();
            User user = userService.getUserByUsername(username)
                    .orElseThrow(() -> new MeetrulyException("User not found: " + username));

            List<ProfileCardDto> suggestions = matchingService.getDailySuggestionsForUser(user.getId());

            model.addAttribute("suggestions", suggestions);

            return "matching/daily-suggestions";
        } catch (MeetrulyException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "redirect:/matching";
        }
    }

    @GetMapping("/profile-views")
    @PreAuthorize("isAuthenticated()")
    public String showProfileViews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model, Principal principal) {

        try {

            String username = principal.getName();
            User user = userService.getUserByUsername(username)
                    .orElseThrow(() -> new MeetrulyException("User not found: " + username));

            boolean isGoldSubscriber = subscriptionService.getCurrentSubscription(user.getId())
                    .getPlan().name().equals("GOLD");

            if (!isGoldSubscriber) {
                model.addAttribute("errorMessage",
                        "You need a GOLD subscription to see who viewed your profile. Please upgrade your plan.");
                return "redirect:/subscription/plans";
            }

            PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "viewedAt"));

            Page<ProfileViewDto> viewsPage = matchingService.getProfileViewsByViewed(user.getId(), pageRequest);

            model.addAttribute("profileViews", viewsPage.getContent());
            model.addAttribute("currentPage", viewsPage.getNumber());
            model.addAttribute("totalPages", viewsPage.getTotalPages());

            return "matching/profile-views";
        } catch (MeetrulyException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "redirect:/matching";
        }
    }

    @GetMapping("/top-users")
    @PreAuthorize("isAuthenticated()")
    public String showTopUsers(Model model, Principal principal) {
        try {

            List<ProfileCardDto> topUsers = matchingService.getTopLikedUsers(20);

            model.addAttribute("topUsers", topUsers);

            return "matching/top-users";
        } catch (MeetrulyException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "redirect:/matching";
        }
    }

    @PostMapping("/matches/{matchId}/viewed")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> markMatchAsViewed(@PathVariable("matchId") UUID matchId, Principal principal) {
        try {

            String username = principal.getName();
            User user = userService.getUserByUsername(username)
                    .orElseThrow(() -> new MeetrulyException("User not found: " + username));

            matchingService.markMatchAsViewedByUser(matchId, user.getId());

            return ResponseEntity.ok().build();
        } catch (MeetrulyException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/record-view/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> recordProfileView(@PathVariable("userId") UUID userId, Principal principal) {
        try {

            String username = principal.getName();
            User currentUser = userService.getUserByUsername(username)
                    .orElseThrow(() -> new MeetrulyException("User not found: " + username));

            ProfileViewDto profileView = matchingService.recordProfileView(currentUser.getId(), userId);

            return ResponseEntity.ok(profileView);
        } catch (MeetrulyException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/generate-daily-suggestions")
    @PreAuthorize("hasRole('ADMIN')")
    public String generateDailySuggestions(RedirectAttributes redirectAttributes) {
        try {
            matchingService.generateDailySuggestionsForAllUsers();

            redirectAttributes.addFlashAttribute("successMessage",
                    "Daily suggestions have been generated for all users.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Failed to generate daily suggestions: " + e.getMessage());
        }

        return "redirect:/admin/dashboard";
    }
}