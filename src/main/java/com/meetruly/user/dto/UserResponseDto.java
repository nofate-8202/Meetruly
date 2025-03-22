package com.meetruly.user.dto;

import com.meetruly.core.constant.Gender;
import com.meetruly.core.constant.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDto {

    private UUID id;
    private String username;
    private String email;
    private Gender gender;
    private UserRole role;
    private boolean enabled;
    private boolean accountNonLocked;
    private boolean emailVerified;
    private boolean profileCompleted;
    private boolean approved;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
}