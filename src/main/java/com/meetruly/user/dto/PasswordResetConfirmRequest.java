package com.meetruly.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetConfirmRequest {

    @NotBlank(message = "Token is required")
    private String token;

    @NotBlank(message = "Password is required")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[A-Z]).{6,}$", message = "Password must be at least 6 characters with at least one uppercase letter and one number")
    private String password;

    @NotBlank(message = "Password confirmation is required")
    private String confirmPassword;
}