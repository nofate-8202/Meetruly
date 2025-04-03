package com.meetruly.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetruly.core.constant.Gender;
import com.meetruly.core.constant.UserRole;
import com.meetruly.matching.service.MatchingService;
import com.meetruly.user.dto.ProfileCardDto;
import com.meetruly.user.dto.SearchProfileRequest;
import com.meetruly.user.dto.SearchProfileResponse;
import com.meetruly.user.dto.UserProfileDto;
import com.meetruly.user.model.User;
import com.meetruly.user.model.UserProfile;
import com.meetruly.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @MockBean
    private UserService userService;

    @MockBean
    private MatchingService matchingService;

    @Autowired
    private ObjectMapper objectMapper;

    
    @MockBean
    private com.meetruly.web.config.ApprovalCheckFilter approvalCheckFilter;

    private final UUID testUserId = UUID.randomUUID();
    private User testUser;
    private UserProfileDto testProfileDto;
    private UserProfile testProfile;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        
        testUser = User.builder()
                .id(testUserId)
                .username("testUser")
                .email("test@example.com")
                .gender(Gender.MALE)
                .role(UserRole.USER)
                .enabled(true)
                .accountNonLocked(true)
                .emailVerified(true)
                .profileCompleted(true)
                .approved(true)
                .build();

        
        testProfileDto = UserProfileDto.builder()
                .userId(testUserId)
                .firstName("Test")
                .lastName("User")
                .age(30)
                .profileImageUrl("/images/test-profile.jpg")
                .build();

        
        testProfile = new UserProfile();
        testProfile.setUser(testUser);
        testProfile.setFirstName("Test");
        testProfile.setLastName("User");
        testProfile.setAge(30);
        testProfile.setProfileImageUrl("/images/test-profile.jpg");
    }

    @Test
    @WithMockUser(username = "testUser")
    public void showUserProfile_ShouldReturnProfile_WhenUserIsAuthenticated() throws Exception {
        
        given(userService.getUserByUsername("testUser")).willReturn(Optional.of(testUser));
        given(userService.getUserProfile(testUserId)).willReturn(Optional.of(testProfileDto));

        
        mockMvc.perform(get("/profile"))
                .andExpect(status().isOk())
                .andExpect(view().name("user/profile"));

        verify(userService).getUserByUsername("testUser");
        verify(userService).getUserProfile(testUserId);
    }

    @Test
    @WithMockUser(username = "testUser")
    public void showUserProfile_ShouldCreateNewProfile_WhenProfileDoesNotExist() throws Exception {
        
        given(userService.getUserByUsername("testUser")).willReturn(Optional.of(testUser));
        given(userService.getUserProfile(testUserId)).willReturn(Optional.empty());

        
        mockMvc.perform(get("/profile"))
                .andExpect(status().isOk())
                .andExpect(view().name("user/profile"));

        verify(userService).getUserByUsername("testUser");
        verify(userService).getUserProfile(testUserId);
    }

    @Test
    @WithMockUser(username = "testUser")
    public void updateUserProfile_ShouldSucceed_WhenProfileIsValid() throws Exception {
        
        given(userService.getUserByUsername("testUser")).willReturn(Optional.of(testUser));
        given(userService.createOrUpdateProfile(eq(testUserId), any(UserProfileDto.class))).willReturn(testProfile);

        
        mockMvc.perform(post("/profile")
                        .with(csrf())
                        .param("firstName", "Test")
                        .param("lastName", "User")
                        .param("age", "30")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attribute("successMessage", "Profile updated successfully"));

        verify(userService).getUserByUsername("testUser");
        verify(userService).createOrUpdateProfile(eq(testUserId), any(UserProfileDto.class));
    }

    @Test
    @WithMockUser(username = "testUser")
    public void updateUserProfile_ShouldFail_WhenProfileIsInvalid() throws Exception {
        
        UserController standaloneController = new UserController(userService, matchingService);

        
        MockMvc standaloneMockMvc = MockMvcBuilders
                .standaloneSetup(standaloneController)
                .build();

        
        given(userService.getUserByUsername("testUser")).willReturn(Optional.of(testUser));

        
        
        standaloneMockMvc.perform(post("/profile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk());

        
        verify(userService, never()).createOrUpdateProfile(any(), any());
    }

    @Test
    @WithMockUser(username = "testUser")
    public void uploadProfileImage_ShouldSucceed_WhenImageIsValid() throws Exception {
        
        MockMultipartFile imageFile = new MockMultipartFile(
                "image",
                "test-image.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "test image content".getBytes()
        );

        given(userService.getUserByUsername("testUser")).willReturn(Optional.of(testUser));
        given(userService.getUserProfile(testUserId)).willReturn(Optional.of(testProfileDto));
        given(userService.createOrUpdateProfile(eq(testUserId), any(UserProfileDto.class))).willReturn(testProfile);

        
        mockMvc.perform(multipart("/profile/upload-image")
                        .file(imageFile)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attributeExists("successMessage"))
                .andExpect(flash().attributeExists("uploadedImageUrl"));

        verify(userService).getUserByUsername("testUser");
        verify(userService).createOrUpdateProfile(eq(testUserId), any(UserProfileDto.class));
    }

    @Test
    @WithMockUser(username = "testUser")
    public void uploadProfileImage_ShouldFail_WhenImageIsEmpty() throws Exception {
        
        MockMultipartFile emptyFile = new MockMultipartFile(
                "image",
                "",
                MediaType.IMAGE_JPEG_VALUE,
                new byte[0]
        );

        
        mockMvc.perform(multipart("/profile/upload-image")
                        .file(emptyFile)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attribute("errorMessage", "Please select an image to upload"));

        verify(userService, never()).getUserByUsername(anyString());
        verify(userService, never()).createOrUpdateProfile(any(), any());
    }

    @Test
    @WithMockUser(username = "testUser")
    public void showSearchForm_ShouldDisplaySearchForm_WhenUserIsAuthenticated() throws Exception {
        
        mockMvc.perform(get("/profile/search"))
                .andExpect(status().isOk())
                .andExpect(view().name("user/search"))
                .andExpect(model().attributeExists("searchRequest"));
    }

    @Test
    @WithMockUser(username = "testUser")
    public void searchProfiles_ShouldReturnResults_WhenSearchIsValid() throws Exception {
        
        List<ProfileCardDto> profileCards = new ArrayList<>();
        ProfileCardDto profileCard = new ProfileCardDto();
        profileCard.setUserId(UUID.randomUUID());
        profileCard.setUsername("otherUser");
        profileCard.setProfileImageUrl("/images/other-profile.jpg");
        profileCard.setAge(25);
        profileCard.setGender(Gender.FEMALE);
        profileCards.add(profileCard);

        SearchProfileResponse searchResponse = SearchProfileResponse.builder()
                .profiles(profileCards)
                .currentPage(0)
                .totalPages(1)
                .totalElements(1L)
                .build();

        given(userService.getUserByUsername("testUser")).willReturn(Optional.of(testUser));
        given(userService.hasReachedProfileViewLimit(testUserId)).willReturn(false);
        given(userService.searchProfiles(any(SearchProfileRequest.class))).willReturn(searchResponse);
        given(userService.canViewFullProfile(testUserId)).willReturn(true);

        
        mockMvc.perform(post("/profile/search")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("gender", "FEMALE")
                        .param("minAge", "18")
                        .param("maxAge", "40"))
                .andExpect(status().isOk())
                .andExpect(view().name("user/search-results"));

        verify(userService).getUserByUsername("testUser");
        verify(userService).hasReachedProfileViewLimit(testUserId);
        verify(userService).searchProfiles(any(SearchProfileRequest.class));
    }

    @Test
    @WithMockUser(username = "testUser")
    public void searchProfiles_ShouldShowError_WhenReachedProfileViewLimit() throws Exception {
        
        given(userService.getUserByUsername("testUser")).willReturn(Optional.of(testUser));
        given(userService.hasReachedProfileViewLimit(testUserId)).willReturn(true);

        
        mockMvc.perform(post("/profile/search")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(view().name("user/search"));

        verify(userService).getUserByUsername("testUser");
        verify(userService).hasReachedProfileViewLimit(testUserId);
        verify(userService, never()).searchProfiles(any(SearchProfileRequest.class));
    }

    @Test
    @WithMockUser(username = "testUser")
    public void viewUserProfile_ShouldShowProfile_WhenUserExists() throws Exception {
        
        UUID otherUserId = UUID.randomUUID();
        User otherUser = User.builder()
                .id(otherUserId)
                .username("otherUser")
                .gender(Gender.MALE)
                .build();

        UserProfileDto otherProfileDto = UserProfileDto.builder()
                .userId(otherUserId)
                .firstName("Other")
                .lastName("User")
                .age(25)
                .build();

        given(userService.getUserByUsername("testUser")).willReturn(Optional.of(testUser));
        given(userService.hasReachedProfileViewLimit(testUserId)).willReturn(false);
        given(userService.getUserById(otherUserId)).willReturn(otherUser);
        given(userService.getUserProfile(otherUserId)).willReturn(Optional.of(otherProfileDto));
        given(userService.canViewFullProfile(testUserId)).willReturn(true);

        
        mockMvc.perform(get("/profile/" + otherUserId))
                .andExpect(status().isOk())
                .andExpect(view().name("user/view-profile"));

        verify(userService).getUserByUsername("testUser");
        verify(userService).hasReachedProfileViewLimit(testUserId);
        verify(userService).getUserById(otherUserId);
        verify(userService).getUserProfile(otherUserId);
        verify(userService).incrementProfileViewCount(testUserId);
    }

    @Test
    @WithMockUser(username = "testUser")
    public void viewUserProfile_ShouldCreateEmptyProfile_WhenProfileDoesNotExist() throws Exception {
        
        UUID otherUserId = UUID.randomUUID();
        User otherUser = User.builder()
                .id(otherUserId)
                .username("otherUser")
                .gender(Gender.MALE)
                .build();

        given(userService.getUserByUsername("testUser")).willReturn(Optional.of(testUser));
        given(userService.hasReachedProfileViewLimit(testUserId)).willReturn(false);
        given(userService.getUserById(otherUserId)).willReturn(otherUser);
        given(userService.getUserProfile(otherUserId)).willReturn(Optional.empty());
        given(userService.canViewFullProfile(testUserId)).willReturn(true);

        
        mockMvc.perform(get("/profile/" + otherUserId))
                .andExpect(status().isOk())
                .andExpect(view().name("user/view-profile"));

        verify(userService).getUserByUsername("testUser");
        verify(userService).hasReachedProfileViewLimit(testUserId);
        verify(userService).getUserById(otherUserId);
        verify(userService).getUserProfile(otherUserId);
        verify(userService).incrementProfileViewCount(testUserId);
    }

    @Test
    @WithMockUser(username = "testUser")
    public void viewOwnProfile_ShouldNotIncrementViewCount() throws Exception {
        
        given(userService.getUserByUsername("testUser")).willReturn(Optional.of(testUser));
        given(userService.getUserById(testUserId)).willReturn(testUser);
        given(userService.getUserProfile(testUserId)).willReturn(Optional.of(testProfileDto));

        
        mockMvc.perform(get("/profile/" + testUserId))
                .andExpect(status().isOk())
                .andExpect(view().name("user/view-profile"));

        verify(userService).getUserByUsername("testUser");
        verify(userService, never()).hasReachedProfileViewLimit(testUserId);
        verify(userService).getUserById(testUserId);
        verify(userService).getUserProfile(testUserId);
        verify(userService, never()).incrementProfileViewCount(testUserId);
    }

    @Test
    @WithMockUser(username = "testUser")
    public void viewUserProfile_ShouldShowError_WhenReachedProfileViewLimit() throws Exception {
        
        UUID otherUserId = UUID.randomUUID();

        given(userService.getUserByUsername("testUser")).willReturn(Optional.of(testUser));
        given(userService.hasReachedProfileViewLimit(testUserId)).willReturn(true);

        
        mockMvc.perform(get("/profile/" + otherUserId))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile/search"));

        verify(userService).getUserByUsername("testUser");
        verify(userService).hasReachedProfileViewLimit(testUserId);
        verify(userService, never()).getUserById(otherUserId);
    }

    @Test
    @WithMockUser(username = "testUser")
    public void viewProfileGallery_ShouldShowGallery_ForValidGender() throws Exception {
        
        List<ProfileCardDto> profiles = new ArrayList<>();
        ProfileCardDto profile = new ProfileCardDto();
        profile.setUserId(UUID.randomUUID());
        profile.setUsername("femaleUser");
        profile.setGender(Gender.FEMALE);
        profiles.add(profile);

        given(userService.getUserByUsername("testUser")).willReturn(Optional.of(testUser));
        given(userService.getUserProfiles(eq(Gender.FEMALE), any(Pageable.class))).willReturn(profiles);
        given(matchingService.hasLiked(eq(testUserId), any(UUID.class))).willReturn(false);

        
        mockMvc.perform(get("/profile/gallery/female")
                        .param("page", "0")
                        .param("size", "12"))
                .andExpect(status().isOk())
                .andExpect(view().name("user/gallery"));

        verify(userService).getUserByUsername("testUser");
        verify(userService).getUserProfiles(eq(Gender.FEMALE), any(Pageable.class));
        verify(matchingService).hasLiked(eq(testUserId), any(UUID.class));
    }

    @Test
    @WithMockUser(username = "testUser")
    public void viewProfileGallery_ShouldRedirect_ForInvalidGender() throws Exception {
        
        mockMvc.perform(get("/profile/gallery/invalid"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"));

        verify(userService, never()).getUserProfiles(any(), any());
    }
}