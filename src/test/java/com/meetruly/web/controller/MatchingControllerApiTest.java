package com.meetruly.web.controller;



import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetruly.core.constant.Gender;
import com.meetruly.core.constant.SubscriptionPlan;
import com.meetruly.matching.dto.*;
import com.meetruly.matching.model.Match;
import com.meetruly.matching.service.MatchingService;
import com.meetruly.subscription.dto.SubscriptionDto;
import com.meetruly.subscription.service.SubscriptionService;
import com.meetruly.user.model.User;
import com.meetruly.user.service.UserService;
import com.meetruly.web.config.ApprovalCheckFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(controllers = MatchingController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = ApprovalCheckFilter.class))
public class MatchingControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MatchingService matchingService;

    @MockBean
    private UserService userService;

    @MockBean
    private SubscriptionService subscriptionService;

    private UUID userId = UUID.randomUUID();
    private UUID otherUserId = UUID.randomUUID();

    
    @Test
    @WithMockUser(username = "testuser", authorities = {"ROLE_USER"})
    public void testShowMatchingDashboard() throws Exception {
        
        User testUser = new User();
        testUser.setId(userId);
        testUser.setUsername("testuser");
        testUser.setEnabled(true);
        testUser.setApproved(true);

        when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userService.isUserApproved("testuser")).thenReturn(true);

        
        List<ProfileCardDto> suggestions = new ArrayList<>();
        suggestions.add(ProfileCardDto.builder()
                .userId(otherUserId)
                .username("otheruser")
                .profileImageUrl("/images/default-profile.jpg")
                .age(25)
                .city("Sofia")
                .gender(Gender.FEMALE)
                .compatibilityScore(85.5)
                .isMatch(true)
                .isLiked(false)
                .build());

        MatchingSummaryDto summaryDto = MatchingSummaryDto.builder()
                .totalLikesReceived(5)
                .totalLikesGiven(10)
                .totalMatches(3)
                .unviewedLikes(2)
                .unviewedMatches(1)
                .totalProfileViews(20)
                .dailySuggestions(suggestions)
                .topLikedUsers(new ArrayList<>())
                .build();

        when(matchingService.getMatchingSummary(userId)).thenReturn(summaryDto);

        
        mockMvc.perform(get("/matching"))
                .andExpect(status().isOk())
                .andExpect(view().name("matching/dashboard"))
                .andExpect(model().attributeExists("summary"));
    }

    
    @Test
    @WithMockUser(username = "testuser", authorities = {"ROLE_USER"})
    public void testLikeUser_Success() throws Exception {
        
        User testUser = new User();
        testUser.setId(userId);
        testUser.setUsername("testuser");
        testUser.setEnabled(true);
        testUser.setApproved(true);

        when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userService.isUserApproved("testuser")).thenReturn(true);

        when(subscriptionService.canSendMessage(userId)).thenReturn(true);

        LikeDto likeDto = LikeDto.builder()
                .id(UUID.randomUUID())
                .likerId(userId)
                .likerUsername("testuser")
                .likedId(otherUserId)
                .likedUsername("otheruser")
                .viewed(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(matchingService.likeUser(userId, otherUserId)).thenReturn(likeDto);

        
        mockMvc.perform(post("/matching/like/" + otherUserId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.likerId").value(userId.toString()))
                .andExpect(jsonPath("$.likedId").value(otherUserId.toString()));

        verify(matchingService, times(1)).likeUser(userId, otherUserId);
    }

    
    @Test
    @WithMockUser(username = "testuser", authorities = {"ROLE_USER"})
    public void testLikeUser_ReachedLimit() throws Exception {
        
        User testUser = new User();
        testUser.setId(userId);
        testUser.setUsername("testuser");
        testUser.setEnabled(true);
        testUser.setApproved(true);

        when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userService.isUserApproved("testuser")).thenReturn(true);
        when(subscriptionService.canSendMessage(userId)).thenReturn(false);

        
        mockMvc.perform(post("/matching/like/" + otherUserId)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("upgrade your subscription plan")));

        verify(matchingService, never()).likeUser(any(), any());
    }

    
    @Test
    @WithMockUser(username = "testuser", authorities = {"ROLE_USER"})
    public void testUnlikeUser() throws Exception {
        
        User testUser = new User();
        testUser.setId(userId);
        testUser.setUsername("testuser");
        testUser.setEnabled(true);
        testUser.setApproved(true);

        when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userService.isUserApproved("testuser")).thenReturn(true);

        doNothing().when(matchingService).unlikeUser(userId, otherUserId);

        
        mockMvc.perform(delete("/matching/like/" + otherUserId)
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(matchingService, times(1)).unlikeUser(userId, otherUserId);
    }

    
    @Test
    @WithMockUser(username = "testuser", authorities = {"ROLE_USER"})
    public void testShowReceivedLikes_NotGoldSubscription() throws Exception {
        
        User testUser = new User();
        testUser.setId(userId);
        testUser.setUsername("testuser");
        testUser.setEnabled(true);
        testUser.setApproved(true);

        when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userService.isUserApproved("testuser")).thenReturn(true);

        SubscriptionDto subscriptionDto = new SubscriptionDto();
        subscriptionDto.setPlan(SubscriptionPlan.valueOf(SubscriptionPlan.SILVER.name()));
        when(subscriptionService.getCurrentSubscription(userId)).thenReturn(subscriptionDto);

        
        mockMvc.perform(get("/matching/likes/received"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/subscription/plans"));
    }

    
    @Test
    @WithMockUser(username = "testuser", authorities = {"ROLE_USER"})
    public void testShowSentLikes() throws Exception {
        
        
    }

    
    @Test
    @WithMockUser(username = "testuser", authorities = {"ROLE_USER"})
    public void testShowMatches() throws Exception {
        
        User testUser = new User();
        testUser.setId(userId);
        testUser.setUsername("testuser");
        testUser.setEnabled(true);
        testUser.setApproved(true);

        when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userService.isUserApproved("testuser")).thenReturn(true);

        MatchDto matchDto = MatchDto.builder()
                .id(UUID.randomUUID())
                .user1Id(userId)
                .user2Id(otherUserId)
                .otherUserId(otherUserId)
                .otherUsername("otheruser")
                .viewed(false)
                .matchType(Match.MatchType.MUTUAL_LIKE)
                .compatibilityScore(85.5)
                .build();

        List<MatchDto> matchesList = new ArrayList<>();
        matchesList.add(matchDto);

        Page<MatchDto> matchesPage = new PageImpl<>(matchesList);
        when(matchingService.getMatchesByUser(eq(userId), any(PageRequest.class))).thenReturn(matchesPage);

        
        mockMvc.perform(get("/matching/matches"))
                .andExpect(status().isOk())
                .andExpect(view().name("matching/matches"))
                .andExpect(model().attributeExists("matches"));

        verify(matchingService, times(1)).markMatchAsViewedByUser(matchDto.getId(), userId);
    }

    
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testGenerateDailySuggestions() throws Exception {
        
        User adminUser = new User();
        adminUser.setId(UUID.randomUUID());
        adminUser.setUsername("admin");
        adminUser.setEnabled(true);
        adminUser.setApproved(true);

        when(userService.getUserByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(userService.isUserApproved("admin")).thenReturn(true);

        doNothing().when(matchingService).generateDailySuggestionsForAllUsers();

        
        mockMvc.perform(post("/matching/generate-daily-suggestions")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(matchingService, times(1)).generateDailySuggestionsForAllUsers();
    }

    
    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    public void testGenerateDailySuggestions_Forbidden() throws Exception {
        
        User testUser = new User();
        testUser.setId(userId);
        testUser.setUsername("testuser");
        testUser.setEnabled(true);
        testUser.setApproved(true);

        when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userService.isUserApproved("testuser")).thenReturn(true);

        
        mockMvc.perform(post("/matching/generate-daily-suggestions")
                        .with(csrf()))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.junit.jupiter.api.Assertions.assertTrue(
                            status == 302 || status == 403,
                            "Expected status code 302 (redirect) or 403 (forbidden), but received: " + status
                    );
                });

        
    }
}