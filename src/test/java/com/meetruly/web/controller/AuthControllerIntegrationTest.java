package com.meetruly.web.controller;

import com.meetruly.core.constant.Gender;
import com.meetruly.core.constant.UserRole;
import com.meetruly.core.service.EmailService;
import com.meetruly.user.dto.UserRegistrationDto;
import com.meetruly.user.model.User;
import com.meetruly.user.model.VerificationToken;
import com.meetruly.user.repository.UserRepository;
import com.meetruly.user.repository.VerificationTokenRepository;
import com.meetruly.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VerificationTokenRepository verificationTokenRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private EmailService emailService;

    private UserRegistrationDto validRegistrationDto;

    @BeforeEach
    public void setup() {
        
        verificationTokenRepository.deleteAll();
        userRepository.deleteAll();

        
        doNothing().when(emailService).sendVerificationEmail(anyString(), anyString(), anyString());
        doNothing().when(emailService).sendPasswordResetEmail(anyString(), anyString(), anyString());

        
        validRegistrationDto = new UserRegistrationDto();
        validRegistrationDto.setUsername("testuser");
        validRegistrationDto.setEmail("test@example.com");
        validRegistrationDto.setPassword("Password123");
        validRegistrationDto.setConfirmPassword("Password123");
        validRegistrationDto.setGender(Gender.MALE);

        
        
        if (userRepository.count() == 0) {
            User adminUser = new User();
            adminUser.setUsername("admin");
            adminUser.setEmail("admin@example.com");
            adminUser.setPassword(passwordEncoder.encode("Admin123"));
            adminUser.setGender(Gender.MALE);
            adminUser.setRole(UserRole.ADMIN);
            adminUser.setEmailVerified(true);
            adminUser.setApproved(true);
            adminUser.setEnabled(true);
            adminUser.setAccountNonLocked(true);
            userRepository.save(adminUser);
        }
    }

    @Test
    public void testShowRegistrationForm() throws Exception {
        mockMvc.perform(get("/auth/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeExists("user"));
    }

    @Test
    public void testRegisterUser_Success() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .param("username", validRegistrationDto.getUsername())
                        .param("email", validRegistrationDto.getEmail())
                        .param("password", validRegistrationDto.getPassword())
                        .param("confirmPassword", validRegistrationDto.getConfirmPassword())
                        .param("gender", validRegistrationDto.getGender().name())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/verify-email"));

        
        Optional<User> createdUser = userRepository.findByUsername(validRegistrationDto.getUsername());
        assertTrue(createdUser.isPresent());
        assertEquals(validRegistrationDto.getEmail(), createdUser.get().getEmail());
        assertEquals(validRegistrationDto.getGender(), createdUser.get().getGender());
        assertTrue(passwordEncoder.matches(validRegistrationDto.getPassword(), createdUser.get().getPassword()));
        
        assertFalse(createdUser.get().isEmailVerified());

        
        assertTrue(verificationTokenRepository.findByUser(createdUser.get()).size() > 0);
    }

    @Test
    public void testRegisterUser_PasswordsDoNotMatch() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .param("username", validRegistrationDto.getUsername())
                        .param("email", validRegistrationDto.getEmail())
                        .param("password", validRegistrationDto.getPassword())
                        .param("confirmPassword", "DifferentPassword123")
                        .param("gender", validRegistrationDto.getGender().name())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("user", "confirmPassword"));

        
        assertFalse(userRepository.findByUsername(validRegistrationDto.getUsername()).isPresent());
    }

    @Test
    public void testRegisterUser_UsernameExists() throws Exception {
        
        User existingUser = new User();
        existingUser.setUsername(validRegistrationDto.getUsername());
        existingUser.setEmail("existing@example.com");
        existingUser.setPassword(passwordEncoder.encode("ExistingPassword123"));
        existingUser.setGender(Gender.MALE);
        existingUser.setRole(UserRole.USER);
        userRepository.save(existingUser);

        
        mockMvc.perform(post("/auth/register")
                        .param("username", validRegistrationDto.getUsername())
                        .param("email", validRegistrationDto.getEmail())
                        .param("password", validRegistrationDto.getPassword())
                        .param("confirmPassword", validRegistrationDto.getConfirmPassword())
                        .param("gender", validRegistrationDto.getGender().name())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeExists("errorMessage"));

        
        assertEquals(1, userRepository.findAll().stream()
                .filter(u -> u.getUsername().equals(validRegistrationDto.getUsername()))
                .count());
    }

    @Test
    public void testVerifyEmail_Success() throws Exception {
        
        User user = new User();
        user.setUsername("verifyuser");
        user.setEmail("verify@example.com");
        user.setPassword(passwordEncoder.encode("Password123"));
        user.setGender(Gender.MALE);
        user.setRole(UserRole.USER);
        user.setEmailVerified(false);
        userRepository.save(user);

        
        VerificationToken token = new VerificationToken();
        token.setToken(UUID.randomUUID().toString());
        token.setUser(user);
        token.setCreatedAt(LocalDateTime.now());
        token.setExpiryDate(LocalDateTime.now().plusDays(1));
        token.setTokenType(VerificationToken.TokenType.EMAIL_VERIFICATION);
        verificationTokenRepository.save(token);

        
        mockMvc.perform(get("/auth/verify-email/confirm")
                        .param("token", token.getToken()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"))
                .andExpect(flash().attributeExists("successMessage"));

        
        Optional<User> verifiedUser = userRepository.findById(user.getId());
        assertTrue(verifiedUser.isPresent());
        assertTrue(verifiedUser.get().isEmailVerified());
    }

    @Test
    public void testVerifyEmail_InvalidToken() throws Exception {
        
        mockMvc.perform(get("/auth/verify-email/confirm")
                        .param("token", "invalid-token"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/verify-email"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    public void testShowLoginForm() throws Exception {
        mockMvc.perform(get("/auth/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));
    }

    @Test
    @WithMockUser(username = "testadmin", roles = {"ADMIN"})
    public void testLoginSuccess_AdminRedirect() throws Exception {
        
        User adminUser = new User();
        adminUser.setUsername("testadmin");
        adminUser.setEmail("admin@example.com");
        adminUser.setPassword(passwordEncoder.encode("Password123"));
        adminUser.setGender(Gender.MALE);
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setEmailVerified(true);
        adminUser.setApproved(true);
        adminUser.setEnabled(true);
        adminUser.setAccountNonLocked(true);
        userRepository.save(adminUser);

        
        mockMvc.perform(get("/auth/login-success")
                        .with(request -> {
                            request.setUserPrincipal(() -> "testadmin");
                            return request;
                        }))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login?error=true"));
    }

    @Test
    @WithMockUser(username = "testuser")
    public void testLoginSuccess_UserRedirect() throws Exception {
        
        User normalUser = new User();
        normalUser.setUsername("testuser");
        normalUser.setEmail("user@example.com");
        normalUser.setPassword(passwordEncoder.encode("Password123"));
        normalUser.setGender(Gender.MALE);
        normalUser.setRole(UserRole.USER);
        normalUser.setEmailVerified(true);
        normalUser.setApproved(true);
        userRepository.save(normalUser);

        mockMvc.perform(get("/auth/login-success"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    @WithMockUser(username = "unapproved")
    public void testLoginSuccess_UnapprovedUserRedirect() throws Exception {
        
        User unapprovedUser = new User();
        unapprovedUser.setUsername("unapproved");
        unapprovedUser.setEmail("unapproved@example.com");
        unapprovedUser.setPassword(passwordEncoder.encode("Password123"));
        unapprovedUser.setGender(Gender.MALE);
        unapprovedUser.setRole(UserRole.USER);
        unapprovedUser.setEmailVerified(true);
        unapprovedUser.setApproved(false);
        userRepository.save(unapprovedUser);

        mockMvc.perform(get("/auth/login-success"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    public void testForgotPassword() throws Exception {
        
        User user = new User();
        user.setUsername("resetuser");
        user.setEmail("reset@example.com");
        user.setPassword(passwordEncoder.encode("OldPassword123"));
        user.setGender(Gender.MALE);
        user.setRole(UserRole.USER);
        user.setEmailVerified(true);
        userRepository.save(user);

        
        mockMvc.perform(get("/auth/forgot-password"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/forgot-password"))
                .andExpect(model().attributeExists("passwordResetRequest"));

        
        mockMvc.perform(post("/auth/forgot-password")
                        .param("email", user.getEmail())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"))
                .andExpect(flash().attributeExists("successMessage"));

        
        assertTrue(verificationTokenRepository.findByUserAndTokenType(user, VerificationToken.TokenType.PASSWORD_RESET).size() > 0);
    }

    @Test
    public void testResetPassword() throws Exception {
        
        User user = new User();
        user.setUsername("resetpassuser");
        user.setEmail("resetpass@example.com");
        user.setPassword(passwordEncoder.encode("OldPassword123"));
        user.setGender(Gender.MALE);
        user.setRole(UserRole.USER);
        user.setEmailVerified(true);
        userRepository.save(user);

        
        VerificationToken token = new VerificationToken();
        token.setToken(UUID.randomUUID().toString());
        token.setUser(user);
        token.setCreatedAt(LocalDateTime.now());
        token.setExpiryDate(LocalDateTime.now().plusHours(1));
        token.setTokenType(VerificationToken.TokenType.PASSWORD_RESET);
        verificationTokenRepository.save(token);

        
        mockMvc.perform(get("/auth/reset-password")
                        .param("token", token.getToken()))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/reset-password"))
                .andExpect(model().attributeExists("passwordReset"));

        
        String newPassword = "NewPassword123";
        mockMvc.perform(post("/auth/reset-password")
                        .param("token", token.getToken())
                        .param("password", newPassword)
                        .param("confirmPassword", newPassword)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"))
                .andExpect(flash().attributeExists("successMessage"));

        
        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertTrue(passwordEncoder.matches(newPassword, updatedUser.getPassword()));
        assertFalse(passwordEncoder.matches("OldPassword123", updatedUser.getPassword()));
    }

    @Test
    @WithMockUser(username = "changepassuser")
    public void testChangePassword() throws Exception {
        
        User user = new User();
        user.setUsername("changepassuser");
        user.setEmail("changepass@example.com");
        String currentPassword = "CurrentPassword123";
        user.setPassword(passwordEncoder.encode(currentPassword));
        user.setGender(Gender.MALE);
        user.setRole(UserRole.USER);
        user.setEmailVerified(true);
        userRepository.save(user);

        
        mockMvc.perform(get("/auth/change-password"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/change-password"));

        
        String newPassword = "NewPassword456";
        mockMvc.perform(post("/auth/change-password")
                        .param("currentPassword", currentPassword)
                        .param("newPassword", newPassword)
                        .param("confirmPassword", newPassword)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attributeExists("successMessage"));

        
        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertTrue(passwordEncoder.matches(newPassword, updatedUser.getPassword()));
        assertFalse(passwordEncoder.matches(currentPassword, updatedUser.getPassword()));
    }

    @Test
    @WithMockUser(username = "logoutuser")
    public void testLogout() throws Exception {
        
        User user = new User();
        user.setUsername("logoutuser");
        user.setEmail("logout@example.com");
        user.setPassword(passwordEncoder.encode("Password123"));
        user.setGender(Gender.MALE);
        user.setRole(UserRole.USER);
        user.setEmailVerified(true);
        userRepository.save(user);

        
        mockMvc.perform(get("/auth/logout"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login?logout=true"));

        
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}