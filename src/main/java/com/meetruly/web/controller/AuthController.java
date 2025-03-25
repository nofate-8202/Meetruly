package com.meetruly.web.controller;

import com.meetruly.core.exception.MeetrulyException;
import com.meetruly.user.dto.*;
import com.meetruly.user.model.User;
import com.meetruly.user.model.UserSession;
import com.meetruly.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.UUID;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new UserRegistrationDto());
        return "auth/register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("user") UserRegistrationDto registrationDto,
                               BindingResult result, Model model, RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            return "auth/register";
        }

        if (!registrationDto.getPassword().equals(registrationDto.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "error.user", "Passwords do not match");
            return "auth/register";
        }

        try {

            User user = userService.registerUser(registrationDto);

            if (user.isApproved() && user.isEmailVerified()) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Registration successful! Please login.");
                return "redirect:/auth/login";
            } else {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Registration successful! Please verify your email and wait for admin approval.");
                return "redirect:/auth/verify-email";
            }
        } catch (MeetrulyException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/register";
        }
    }
    @GetMapping("/pending-approval")
    public String showPendingApprovalPage(Principal principal, RedirectAttributes redirectAttributes) {

        if (principal == null) {
            return "redirect:/auth/login";
        }

        try {

            String username = principal.getName();
            User user = userService.getUserByUsername(username)
                    .orElseThrow(() -> new MeetrulyException("User not found: " + username));

            if (user.isApproved()) {
                return "redirect:/";
            }

            if (!user.isEmailVerified()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Please verify your email first");
                return "redirect:/auth/verify-email";
            }

            return "auth/pending-approval";
        } catch (MeetrulyException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/auth/login";
        }
    }

    @GetMapping("/login")
    public String showLoginForm() {
        return "auth/login";
    }

    @GetMapping("/login-success")
    public String loginSuccess(Principal principal) {
        try {

            String username = principal.getName();
            User user = userService.getUserByUsername(username)
                    .orElseThrow(() -> new MeetrulyException("User not found: " + username));
            if (!user.isApproved() && user.isEmailVerified()) {
                return "redirect:/auth/pending-approval";
            }

            if (user.getRole().name().equals("ADMIN")) {
                return "redirect:/admin/dashboard";
            } else {
                return "redirect:/";
            }
        } catch (Exception e) {
            log.error("Error during login success handling", e);
            return "redirect:/auth/login?error=true";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response,
                         Authentication authentication, HttpSession session) {

        String sessionId = session.getId();
        try {
            userService.invalidateUserSession(sessionId);
        } catch (Exception e) {
            log.error("Error invalidating session", e);
        }

        if (authentication != null) {
            new SecurityContextLogoutHandler().logout(request, response, authentication);
        }

        return "redirect:/auth/login?logout";
    }

    @GetMapping("/verify-email")
    public String showVerifyEmailForm() {
        return "auth/verify-email";
    }

    @GetMapping("/verify-email/confirm")
    public String verifyEmail(@RequestParam("token") String token, RedirectAttributes redirectAttributes) {
        try {
            boolean verified = userService.verifyEmail(token);

            if (verified) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Email verified successfully! Please wait for admin approval before login.");
                return "redirect:/auth/login";
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Email verification failed.");
                return "redirect:/auth/verify-email";
            }
        } catch (Exception e) {

            log.error("Error verifying email: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error verifying email: " + e.getMessage());
            return "redirect:/auth/verify-email";
        }
    }

    @PostMapping("/verify-email/resend")
    public String resendVerificationEmail(@RequestParam("email") String email,
                                          RedirectAttributes redirectAttributes) {
        try {
            userService.resendVerificationEmail(email);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Verification email sent. Please check your inbox.");
        } catch (MeetrulyException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/auth/verify-email";
    }

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm(Model model) {
        model.addAttribute("passwordResetRequest", new PasswordResetRequest());
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@Valid @ModelAttribute("passwordResetRequest") PasswordResetRequest resetRequest,
                                        BindingResult result, RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            return "auth/forgot-password";
        }

        try {
            userService.initiatePasswordReset(resetRequest.getEmail());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Password reset email sent. Please check your inbox.");
            return "redirect:/auth/login";
        } catch (MeetrulyException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/auth/forgot-password";
        }
    }

    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam("token") String token, Model model) {
        try {

            boolean isValid = userService.validatePasswordResetToken(token);

            if (isValid) {
                PasswordResetConfirmRequest resetRequest = new PasswordResetConfirmRequest();
                resetRequest.setToken(token);
                model.addAttribute("passwordReset", resetRequest);
                return "auth/reset-password";
            } else {
                model.addAttribute("errorMessage", "Invalid or expired token.");
                return "auth/forgot-password";
            }
        } catch (MeetrulyException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/forgot-password";
        }
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@Valid @ModelAttribute("passwordReset") PasswordResetConfirmRequest resetRequest,
                                       BindingResult result, RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            return "auth/reset-password";
        }

        if (!resetRequest.getPassword().equals(resetRequest.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "error.passwordReset", "Passwords do not match");
            return "auth/reset-password";
        }

        try {
            userService.resetPassword(resetRequest);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Password has been reset successfully. Please login with your new password.");
            return "redirect:/auth/login";
        } catch (MeetrulyException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/auth/forgot-password";
        }
    }

    @GetMapping("/change-password")
    public String showChangePasswordForm() {
        return "auth/change-password";
    }

    @PostMapping("/change-password")
    public String processChangePassword(@RequestParam("currentPassword") String currentPassword,
                                        @RequestParam("newPassword") String newPassword,
                                        @RequestParam("confirmPassword") String confirmPassword,
                                        Principal principal,
                                        RedirectAttributes redirectAttributes) {

        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "New passwords do not match.");
            return "redirect:/auth/change-password";
        }

        try {

            String username = principal.getName();
            User user = userService.getUserByUsername(username)
                    .orElseThrow(() -> new MeetrulyException("User not found: " + username));

            userService.changePassword(user.getId(), currentPassword, newPassword);

            redirectAttributes.addFlashAttribute("successMessage", "Password changed successfully.");
            return "redirect:/profile";
        } catch (MeetrulyException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/auth/change-password";
        }
    }
}