package com.meetruly.user.service;

import com.meetruly.core.constant.Gender;
import com.meetruly.core.constant.UserRole;
import com.meetruly.user.dto.*;
import com.meetruly.user.model.User;
import com.meetruly.user.model.UserProfile;
import com.meetruly.user.model.UserSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserService extends UserDetailsService {

    User registerUser(UserRegistrationDto registrationDto);

    boolean verifyEmail(String token);

    void createEmailVerificationToken(User user);

    void resendVerificationEmail(String email);

    boolean isUserApproved(String username);

    void initiatePasswordReset(String email);

    boolean validatePasswordResetToken(String token);

    void resetPassword(PasswordResetConfirmRequest resetRequest);

    void changePassword(UUID userId, String oldPassword, String newPassword);

    UserProfile createOrUpdateProfile(UUID userId, UserProfileDto profileDto);

    Optional<UserProfileDto> getUserProfile(UUID userId);

    List<ProfileCardDto> getUserProfiles(Gender gender, Pageable pageable);

    SearchProfileResponse searchProfiles(SearchProfileRequest searchRequest);

    User getUserById(UUID id);

    Optional<User> getUserByUsername(String username);

    Optional<User> getUserByEmail(String email);

    User updateUser(UUID id, UserResponseDto userDto);

    void deleteUser(UUID id);

    void approveUser(UUID id);

    void rejectUser(UUID id);

    void toggleUserEnabled(UUID id);

    void updateUserRole(UUID id, UserRole newRole);

    boolean isFirstUser();

    UserSession createUserSession(User user, String sessionId, String ipAddress, String userAgent);

    Optional<UserSession> getUserSession(String sessionId);

    void updateUserSessionActivity(String sessionId);

    void invalidateUserSession(String sessionId);

    void invalidateAllUserSessions(UUID userId);

    boolean canSendMessage(UUID userId);

    boolean canViewFullProfile(UUID userId);

    boolean hasReachedProfileViewLimit(UUID userId);

    void incrementMessageCount(UUID userId);

    void incrementProfileViewCount(UUID userId);

    List<UserResponseDto> getUnapprovedUsers();

    long countUsersByRole(UserRole role);

    Page<UserResponseDto> getUsers(Pageable pageable);
}
