package com.meetruly.web.controller;

import com.meetruly.core.exception.MeetrulyException;
import com.meetruly.core.constant.Gender;
import com.meetruly.core.constant.UserRole;
import com.meetruly.user.dto.PasswordResetConfirmRequest;
import com.meetruly.user.dto.UserRegistrationDto;
import com.meetruly.user.model.User;
import com.meetruly.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.Authentication;

import java.security.Principal;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private Principal principal;

    private MockHttpSession mockSession;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    public void setup() {
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".jsp");

        
        mockSession = new MockHttpSession();

        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setViewResolvers(viewResolver)
                .build();
    }

    @Test
    public void showRegistrationForm_ShouldReturnRegistrationPage() throws Exception {
        mockMvc.perform(get("/auth/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeExists("user"));
    }

    @Test
    public void registerUser_WithValidData_ShouldRedirectToLogin() throws Exception {
        
        UserRegistrationDto registrationDto = new UserRegistrationDto();
        registrationDto.setUsername("testUser");
        registrationDto.setEmail("test@example.com");
        registrationDto.setPassword("Password123");
        registrationDto.setConfirmPassword("Password123");
        registrationDto.setGender(Gender.MALE);

        
        User user = User.builder()
                .id(UUID.randomUUID())
                .username("testUser")
                .email("test@example.com")
                .gender(Gender.MALE)
                .role(UserRole.USER)
                .approved(true)
                .emailVerified(true)
                .enabled(true)
                .accountNonLocked(true)
                .build();

        
        when(userService.registerUser(any(UserRegistrationDto.class))).thenReturn(user);

        
        mockMvc.perform(post("/auth/register")
                        .param("username", "testUser")
                        .param("email", "test@example.com")
                        .param("password", "Password123")
                        .param("confirmPassword", "Password123")
                        .param("gender", "MALE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"));

        
        verify(userService).registerUser(any(UserRegistrationDto.class));
    }

    @Test
    public void registerUser_WithPasswordMismatch_ShouldReturnToRegistrationPage() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .param("username", "testUser")
                        .param("email", "test@example.com")
                        .param("password", "Password123")
                        .param("confirmPassword", "DifferentPassword")
                        .param("gender", "MALE"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeHasFieldErrors("user", "confirmPassword"));

        
        verify(userService, never()).registerUser(any(UserRegistrationDto.class));
    }

    @Test
    public void registerUser_WithExistingUsername_ShouldReturnToRegistrationPage() throws Exception {
        
        when(userService.registerUser(any(UserRegistrationDto.class)))
                .thenThrow(new MeetrulyException("Username is already taken!"));

        mockMvc.perform(post("/auth/register")
                        .param("username", "existingUser")
                        .param("email", "test@example.com")
                        .param("password", "Password123")
                        .param("confirmPassword", "Password123")
                        .param("gender", "MALE"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeExists("errorMessage"));

        
        verify(userService).registerUser(any(UserRegistrationDto.class));
    }

    @Test
    public void showLoginForm_ShouldReturnLoginPage() throws Exception {
        mockMvc.perform(get("/auth/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));
    }

    @Test
    public void loginSuccess_WithApprovedUser_ShouldRedirectToHome() throws Exception {
        
        when(principal.getName()).thenReturn("testUser");

        
        User user = User.builder()
                .id(UUID.randomUUID())
                .username("testUser")
                .email("test@example.com")
                .gender(Gender.MALE)
                .role(UserRole.USER)
                .approved(true)
                .emailVerified(true)
                .enabled(true)
                .accountNonLocked(true)
                .build();

        
        when(userService.getUserByUsername("testUser")).thenReturn(Optional.of(user));

        
        mockMvc.perform(get("/auth/login-success")
                        .principal(principal))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        
        verify(userService).getUserByUsername("testUser");
    }

    @Test
    public void loginSuccess_WithAdminUser_ShouldRedirectToAdminDashboard() throws Exception {
        
        when(principal.getName()).thenReturn("adminUser");

        
        User adminUser = User.builder()
                .id(UUID.randomUUID())
                .username("adminUser")
                .email("admin@example.com")
                .gender(Gender.MALE)
                .role(UserRole.ADMIN)
                .approved(true)
                .emailVerified(true)
                .enabled(true)
                .accountNonLocked(true)
                .build();

        
        when(userService.getUserByUsername("adminUser")).thenReturn(Optional.of(adminUser));

        
        mockMvc.perform(get("/auth/login-success")
                        .principal(principal))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"));

        
        verify(userService).getUserByUsername("adminUser");
    }

    @Test
    public void loginSuccess_WithPendingApprovalUser_ShouldRedirectToPendingApprovalPage() throws Exception {
        
        when(principal.getName()).thenReturn("pendingUser");

        
        User pendingUser = User.builder()
                .id(UUID.randomUUID())
                .username("pendingUser")
                .email("pending@example.com")
                .gender(Gender.MALE)
                .role(UserRole.USER)
                .approved(false)
                .emailVerified(true)
                .enabled(true)
                .accountNonLocked(true)
                .build();

        
        when(userService.getUserByUsername("pendingUser")).thenReturn(Optional.of(pendingUser));

        
        mockMvc.perform(get("/auth/login-success")
                        .principal(principal))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/pending-approval"));

        
        verify(userService).getUserByUsername("pendingUser");
    }

    @Test
    public void logout_ShouldInvalidateSessionAndRedirectToLogin() throws Exception {
        
        mockMvc.perform(get("/auth/logout")
                        .session(mockSession)
                        .principal(authentication))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login?logout"));

        
        verify(userService).invalidateUserSession("1");
    }

    @Test
    public void showVerifyEmailForm_ShouldReturnVerifyEmailPage() throws Exception {
        mockMvc.perform(get("/auth/verify-email"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/verify-email"));
    }

    @Test
    public void verifyEmail_WithValidToken_ShouldRedirectToLogin() throws Exception {
        
        when(userService.verifyEmail("validToken")).thenReturn(true);

        
        mockMvc.perform(get("/auth/verify-email/confirm")
                        .param("token", "validToken"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"))
                .andExpect(flash().attributeExists("successMessage"));

        
        verify(userService).verifyEmail("validToken");
    }

    @Test
    public void verifyEmail_WithInvalidToken_ShouldRedirectToVerifyEmailPage() throws Exception {
        
        when(userService.verifyEmail("invalidToken")).thenThrow(new MeetrulyException("Invalid token"));

        
        mockMvc.perform(get("/auth/verify-email/confirm")
                        .param("token", "invalidToken"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/verify-email"))
                .andExpect(flash().attributeExists("errorMessage"));

        
        verify(userService).verifyEmail("invalidToken");
    }

    @Test
    public void resendVerificationEmail_ShouldRedirectToVerifyEmailPage() throws Exception {
        
        mockMvc.perform(post("/auth/verify-email/resend")
                        .param("email", "test@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/verify-email"));

        
        verify(userService).resendVerificationEmail("test@example.com");
    }

    @Test
    public void showForgotPasswordForm_ShouldReturnForgotPasswordPage() throws Exception {
        mockMvc.perform(get("/auth/forgot-password"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/forgot-password"))
                .andExpect(model().attributeExists("passwordResetRequest"));
    }

    @Test
    public void processForgotPassword_WithValidEmail_ShouldRedirectToLogin() throws Exception {
        
        mockMvc.perform(post("/auth/forgot-password")
                        .param("email", "test@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"))
                .andExpect(flash().attributeExists("successMessage"));

        
        verify(userService).initiatePasswordReset("test@example.com");
    }

    @Test
    public void processForgotPassword_WithInvalidEmail_ShouldRedirectToForgotPasswordPage() throws Exception {
        
        doThrow(new MeetrulyException("User not found with email: invalid@example.com"))
                .when(userService).initiatePasswordReset("invalid@example.com");

        
        mockMvc.perform(post("/auth/forgot-password")
                        .param("email", "invalid@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/forgot-password"))
                .andExpect(flash().attributeExists("errorMessage"));

        
        verify(userService).initiatePasswordReset("invalid@example.com");
    }

    @Test
    public void showResetPasswordForm_WithValidToken_ShouldReturnResetPasswordPage() throws Exception {
        
        when(userService.validatePasswordResetToken("validToken")).thenReturn(true);

        
        mockMvc.perform(get("/auth/reset-password")
                        .param("token", "validToken"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/reset-password"))
                .andExpect(model().attributeExists("passwordReset"));

        
        verify(userService).validatePasswordResetToken("validToken");
    }

    @Test
    public void showResetPasswordForm_WithInvalidToken_ShouldReturnForgotPasswordPage() throws Exception {
        
        when(userService.validatePasswordResetToken("invalidToken"))
                .thenThrow(new MeetrulyException("Invalid reset token"));

        
        mockMvc.perform(get("/auth/reset-password")
                        .param("token", "invalidToken"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/forgot-password"))
                .andExpect(model().attributeExists("errorMessage"));

        
        verify(userService).validatePasswordResetToken("invalidToken");
    }

    @Test
    public void processResetPassword_WithValidData_ShouldRedirectToLogin() throws Exception {
        
        mockMvc.perform(post("/auth/reset-password")
                        .param("token", "validToken")
                        .param("password", "NewPassword123")
                        .param("confirmPassword", "NewPassword123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"))
                .andExpect(flash().attributeExists("successMessage"));

        
        verify(userService).resetPassword(any(PasswordResetConfirmRequest.class));
    }

    @Test
    public void processResetPassword_WithPasswordMismatch_ShouldReturnToResetPasswordPage() throws Exception {
        
        mockMvc.perform(post("/auth/reset-password")
                        .param("token", "validToken")
                        .param("password", "NewPassword123")
                        .param("confirmPassword", "DifferentPassword123"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/reset-password"))
                .andExpect(model().attributeHasFieldErrors("passwordReset", "confirmPassword"));

        
        verify(userService, never()).resetPassword(any(PasswordResetConfirmRequest.class));
    }

    @Test
    public void showChangePasswordForm_ShouldReturnChangePasswordPage() throws Exception {
        mockMvc.perform(get("/auth/change-password"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/change-password"));
    }

    @Test
    public void processChangePassword_WithValidData_ShouldRedirectToProfile() throws Exception {
        
        when(principal.getName()).thenReturn("testUser");

        
        User user = User.builder()
                .id(UUID.randomUUID())
                .username("testUser")
                .email("test@example.com")
                .build();

        
        when(userService.getUserByUsername("testUser")).thenReturn(Optional.of(user));

        
        mockMvc.perform(post("/auth/change-password")
                        .param("currentPassword", "CurrentPassword123")
                        .param("newPassword", "NewPassword123")
                        .param("confirmPassword", "NewPassword123")
                        .principal(principal))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attributeExists("successMessage"));

        
        verify(userService).getUserByUsername("testUser");
        verify(userService).changePassword(user.getId(), "CurrentPassword123", "NewPassword123");
    }

    @Test
    public void processChangePassword_WithPasswordMismatch_ShouldRedirectToChangePasswordPage() throws Exception {
        
        mockMvc.perform(post("/auth/change-password")
                        .param("currentPassword", "CurrentPassword123")
                        .param("newPassword", "NewPassword123")
                        .param("confirmPassword", "DifferentPassword123")
                        .principal(principal))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/change-password"))
                .andExpect(flash().attributeExists("errorMessage"));

        
        verify(userService, never()).getUserByUsername(anyString());
        verify(userService, never()).changePassword(any(UUID.class), anyString(), anyString());
    }

    @Test
    public void processChangePassword_WithIncorrectCurrentPassword_ShouldRedirectToChangePasswordPage() throws Exception {
        
        when(principal.getName()).thenReturn("testUser");

        
        User user = User.builder()
                .id(UUID.randomUUID())
                .username("testUser")
                .email("test@example.com")
                .build();

        
        when(userService.getUserByUsername("testUser")).thenReturn(Optional.of(user));

        
        doThrow(new MeetrulyException("Current password is incorrect"))
                .when(userService).changePassword(user.getId(), "WrongPassword", "NewPassword123");

        
        mockMvc.perform(post("/auth/change-password")
                        .param("currentPassword", "WrongPassword")
                        .param("newPassword", "NewPassword123")
                        .param("confirmPassword", "NewPassword123")
                        .principal(principal))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/change-password"))
                .andExpect(flash().attributeExists("errorMessage"));

        
        verify(userService).getUserByUsername("testUser");
        verify(userService).changePassword(user.getId(), "WrongPassword", "NewPassword123");
    }
}