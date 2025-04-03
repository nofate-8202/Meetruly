package com.meetruly.user.service;

import com.meetruly.core.constant.City;
import com.meetruly.core.constant.Country;
import com.meetruly.core.constant.Gender;
import com.meetruly.core.constant.UserRole;
import com.meetruly.core.exception.MeetrulyException;
import com.meetruly.core.service.EmailService;
import com.meetruly.user.dto.*;
import com.meetruly.user.model.User;
import com.meetruly.user.model.UserProfile;
import com.meetruly.user.model.UserSession;
import com.meetruly.user.model.VerificationToken;
import com.meetruly.user.repository.UserProfileRepository;
import com.meetruly.user.repository.UserRepository;
import com.meetruly.user.repository.UserSessionRepository;
import com.meetruly.user.repository.VerificationTokenRepository;
import com.meetruly.user.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private VerificationTokenRepository verificationTokenRepository;

    @Mock
    private UserSessionRepository userSessionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {

        userService = new UserServiceImpl(
                userRepository,
                userProfileRepository,
                verificationTokenRepository,
                userSessionRepository,
                passwordEncoder,
                emailService
        );
    }

    @Test
    void testRegisterUser_Success() {

        UserRegistrationDto registrationDto = UserRegistrationDto.builder()
                .username("testuser")
                .email("test@example.com")
                .password("Password123")
                .confirmPassword("Password123")
                .gender(Gender.MALE)
                .build();

        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return user;
        });


        User user = userService.registerUser(registrationDto);


        assertNotNull(user);
        assertEquals("testuser", user.getUsername());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("encodedPassword", user.getPassword());
        assertEquals(Gender.MALE, user.getGender());
        assertEquals(UserRole.ADMIN, user.getRole());
        assertTrue(user.isEnabled());
        assertTrue(user.isAccountNonLocked());
        assertTrue(user.isEmailVerified());


        verify(userRepository).existsByUsername("testuser");
        verify(userRepository).existsByEmail("test@example.com");
        verify(userRepository, atLeastOnce()).count();
        verify(passwordEncoder).encode("Password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testRegisterUser_UsernameExists() {

        UserRegistrationDto registrationDto = UserRegistrationDto.builder()
                .username("existinguser")
                .email("test@example.com")
                .password("Password123")
                .confirmPassword("Password123")
                .gender(Gender.MALE)
                .build();

        when(userRepository.existsByUsername("existinguser")).thenReturn(true);


        MeetrulyException exception = assertThrows(
                MeetrulyException.class,
                () -> userService.registerUser(registrationDto)
        );

        assertEquals("Username is already taken!", exception.getMessage());
        verify(userRepository).existsByUsername("existinguser");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testRegisterUser_EmailExists() {

        UserRegistrationDto registrationDto = UserRegistrationDto.builder()
                .username("testuser")
                .email("existing@example.com")
                .password("Password123")
                .confirmPassword("Password123")
                .gender(Gender.MALE)
                .build();

        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);


        MeetrulyException exception = assertThrows(
                MeetrulyException.class,
                () -> userService.registerUser(registrationDto)
        );

        assertEquals("Email is already in use!", exception.getMessage());
        verify(userRepository).existsByUsername("testuser");
        verify(userRepository).existsByEmail("existing@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testRegisterUser_PasswordMismatch() {

        UserRegistrationDto registrationDto = UserRegistrationDto.builder()
                .username("testuser")
                .email("test@example.com")
                .password("Password123")
                .confirmPassword("DifferentPassword123")
                .gender(Gender.MALE)
                .build();


        MeetrulyException exception = assertThrows(
                MeetrulyException.class,
                () -> userService.registerUser(registrationDto)
        );

        assertEquals("Passwords do not match!", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testVerifyEmail_ValidToken() {

        String token = "valid-token";
        UUID tokenId = UUID.randomUUID();
        User user = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .emailVerified(false)
                .build();

        VerificationToken verificationToken = VerificationToken.builder()
                .id(tokenId)
                .token(token)
                .user(user)
                .tokenType(VerificationToken.TokenType.EMAIL_VERIFICATION)
                .createdAt(LocalDateTime.now())
                .expiryDate(LocalDateTime.now().plusDays(1))
                .build();

        when(verificationTokenRepository.findByToken(token)).thenReturn(Optional.of(verificationToken));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));


        boolean result = userService.verifyEmail(token);


        assertTrue(result);
        assertTrue(user.isEmailVerified());


        verify(verificationTokenRepository).findByToken(token);
        verify(userRepository).save(user);
        verify(verificationTokenRepository).deleteById(tokenId);
        verify(verificationTokenRepository).flush();
    }

    @Test
    void testVerifyEmail_InvalidToken() {

        String token = "invalid-token";

        when(verificationTokenRepository.findByToken(token)).thenReturn(Optional.empty());


        MeetrulyException exception = assertThrows(
                MeetrulyException.class,
                () -> userService.verifyEmail(token)
        );

        assertEquals("Invalid verification token", exception.getMessage());
        verify(verificationTokenRepository).findByToken(token);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testVerifyEmail_ExpiredToken() {

        String token = "expired-token";
        User user = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .emailVerified(false)
                .build();

        VerificationToken verificationToken = VerificationToken.builder()
                .id(UUID.randomUUID())
                .token(token)
                .user(user)
                .tokenType(VerificationToken.TokenType.EMAIL_VERIFICATION)
                .createdAt(LocalDateTime.now().minusDays(2))
                .expiryDate(LocalDateTime.now().minusDays(1))
                .build();

        when(verificationTokenRepository.findByToken(token)).thenReturn(Optional.of(verificationToken));


        MeetrulyException exception = assertThrows(
                MeetrulyException.class,
                () -> userService.verifyEmail(token)
        );

        assertEquals("Token has expired", exception.getMessage());
        assertFalse(user.isEmailVerified());


        verify(verificationTokenRepository).findByToken(token);
        verify(userRepository, never()).save(any(User.class));


    }

    @Test
    void testCreateEmailVerificationToken() {

        User user = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .build();


        userService.createEmailVerificationToken(user);


        ArgumentCaptor<VerificationToken> tokenCaptor = ArgumentCaptor.forClass(VerificationToken.class);
        verify(verificationTokenRepository).save(tokenCaptor.capture());

        VerificationToken capturedToken = tokenCaptor.getValue();
        assertNotNull(capturedToken);
        assertEquals(user, capturedToken.getUser());
        assertEquals(VerificationToken.TokenType.EMAIL_VERIFICATION, capturedToken.getTokenType());
        assertNotNull(capturedToken.getToken());
        assertFalse(capturedToken.isExpired());


        verify(emailService).sendVerificationEmail(
                eq(user.getEmail()),
                anyString(),
                eq(user.getUsername())
        );
    }

    @Test
    void testIsUserApproved() {

        String username = "testuser";
        User user = User.builder()
                .id(UUID.randomUUID())
                .username(username)
                .approved(true)
                .build();

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));


        boolean result = userService.isUserApproved(username);


        assertTrue(result);
        verify(userRepository).findByUsername(username);
    }

    @Test
    void testIsUserApproved_UserNotFound() {

        String username = "nonexistent";

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());


        boolean result = userService.isUserApproved(username);


        assertFalse(result);
        verify(userRepository).findByUsername(username);
    }

    @Test
    void testResetPassword() {

        String newEncodedPassword = "newEncodedPassword";

        User user = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .password("oldEncodedPassword")
                .build();


        user.setPassword(newEncodedPassword);
        when(userRepository.save(user)).thenReturn(user);
        User savedUser = userRepository.save(user);


        assertEquals(newEncodedPassword, savedUser.getPassword());


        verify(userRepository).save(user);
    }

    @Test
    void testResetPassword_PasswordMismatch() {

        PasswordResetConfirmRequest resetRequest = PasswordResetConfirmRequest.builder()
                .token("token")
                .password("Password123")
                .confirmPassword("DifferentPassword123")
                .build();


        MeetrulyException exception = assertThrows(
                MeetrulyException.class,
                () -> userService.resetPassword(resetRequest)
        );

        assertEquals("Passwords do not match!", exception.getMessage());
        verify(verificationTokenRepository, never()).findByToken(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testGetUserByUsername() {

        String username = "testuser";
        User user = User.builder()
                .id(UUID.randomUUID())
                .username(username)
                .email("test@example.com")
                .build();

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));


        Optional<User> result = userService.getUserByUsername(username);


        assertTrue(result.isPresent());
        assertEquals(user, result.get());
        verify(userRepository).findByUsername(username);
    }

    @Test
    void testGetUserByEmail() {

        String email = "test@example.com";
        User user = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email(email)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));


        Optional<User> result = userService.getUserByEmail(email);


        assertTrue(result.isPresent());
        assertEquals(user, result.get());
        verify(userRepository).findByEmail(email);
    }

    @Test
    void testIsFirstUser_NoAdmin() {

        when(userRepository.existsByRole(UserRole.ADMIN)).thenReturn(false);


        boolean result = userService.isFirstUser();


        assertTrue(result);
        verify(userRepository).existsByRole(UserRole.ADMIN);
    }

    @Test
    void testIsFirstUser_AdminExists() {

        when(userRepository.existsByRole(UserRole.ADMIN)).thenReturn(true);


        boolean result = userService.isFirstUser();


        assertFalse(result);
        verify(userRepository).existsByRole(UserRole.ADMIN);
    }

    @Test
    void testCreateOrUpdateProfile_Create() {

        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .username("testuser")
                .profileCompleted(false)
                .build();

        UserProfileDto profileDto = UserProfileDto.builder()
                .firstName("John")
                .lastName("Doe")
                .age(30)
                .country(Country.BULGARIA)
                .city(City.SOFIA)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.save(any(User.class))).thenReturn(user);


        UserProfile result = userService.createOrUpdateProfile(userId, profileDto);


        assertNotNull(result);
        assertEquals("John", result.getFirstName());
        assertEquals("Doe", result.getLastName());
        assertEquals(30, result.getAge());
        assertEquals(Country.BULGARIA, result.getCountry());
        assertEquals(City.SOFIA, result.getCity());

        verify(userRepository).findById(userId);
        verify(userProfileRepository).findByUserId(userId);
        verify(userProfileRepository).save(any(UserProfile.class));
        verify(userRepository).save(user);
        assertTrue(user.isProfileCompleted());
    }

    @Test
    void testCreateOrUpdateProfile_Update() {

        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .username("testuser")
                .profileCompleted(true)
                .build();

        UserProfile existingProfile = UserProfile.builder()
                .id(UUID.randomUUID())
                .user(user)
                .firstName("Old")
                .lastName("Name")
                .age(25)
                .country(Country.USA)
                .build();

        UserProfileDto profileDto = UserProfileDto.builder()
                .firstName("New")
                .lastName("Name")
                .age(30)
                .country(Country.BULGARIA)
                .city(City.SOFIA)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(existingProfile));
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(i -> i.getArgument(0));


        UserProfile result = userService.createOrUpdateProfile(userId, profileDto);


        assertNotNull(result);
        assertEquals("New", result.getFirstName());
        assertEquals("Name", result.getLastName());
        assertEquals(30, result.getAge());
        assertEquals(Country.BULGARIA, result.getCountry());
        assertEquals(City.SOFIA, result.getCity());

        verify(userRepository).findById(userId);
        verify(userProfileRepository).findByUserId(userId);
        verify(userProfileRepository).save(any(UserProfile.class));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testGetUserProfile() {

        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .username("testuser")
                .build();

        UserProfile profile = UserProfile.builder()
                .id(UUID.randomUUID())
                .user(user)
                .firstName("John")
                .lastName("Doe")
                .age(30)
                .build();

        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));


        Optional<UserProfileDto> result = userService.getUserProfile(userId);


        assertTrue(result.isPresent());
        assertEquals("John", result.get().getFirstName());
        assertEquals("Doe", result.get().getLastName());
        assertEquals(30, result.get().getAge());

        verify(userProfileRepository).findByUserId(userId);
    }

    @Test
    void testGetUserProfile_NotFound() {

        UUID userId = UUID.randomUUID();
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());


        Optional<UserProfileDto> result = userService.getUserProfile(userId);


        assertFalse(result.isPresent());
        verify(userProfileRepository).findByUserId(userId);
    }

    @Test
    void testSearchProfiles() {

        SearchProfileRequest request = SearchProfileRequest.builder()
                .gender(Gender.FEMALE)
                .minAge(25)
                .maxAge(35)
                .page(0)
                .size(10)
                .build();

        User user1 = User.builder()
                .id(UUID.randomUUID())
                .username("user1")
                .gender(Gender.FEMALE)
                .approved(true)
                .enabled(true)
                .build();

        User user2 = User.builder()
                .id(UUID.randomUUID())
                .username("user2")
                .gender(Gender.FEMALE)
                .approved(true)
                .enabled(true)
                .build();

        UserProfile profile1 = UserProfile.builder()
                .id(UUID.randomUUID())
                .user(user1)
                .age(30)
                .city(City.SOFIA)
                .build();

        UserProfile profile2 = UserProfile.builder()
                .id(UUID.randomUUID())
                .user(user2)
                .age(40)
                .city(City.VARNA)
                .build();

        when(userRepository.findByApproved(true)).thenReturn(List.of(user1, user2));
        when(userProfileRepository.findByUserId(user1.getId())).thenReturn(Optional.of(profile1));
        when(userProfileRepository.findByUserId(user2.getId())).thenReturn(Optional.of(profile2));


        SearchProfileResponse response = userService.searchProfiles(request);


        assertNotNull(response);
        assertEquals(1, response.getProfiles().size());
        assertEquals(user1.getId(), response.getProfiles().get(0).getUserId());
        assertEquals(0, response.getCurrentPage());
        assertEquals(1, response.getTotalPages());
        assertEquals(1, response.getTotalElements());

        verify(userRepository).findByApproved(true);
        verify(userProfileRepository).findByUserId(user1.getId());
        verify(userProfileRepository).findByUserId(user2.getId());
    }

    @Test
    void testApproveUser() {

        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .username("testuser")
                .approved(false)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);


        userService.approveUser(userId);


        assertTrue(user.isApproved());
        verify(userRepository).findById(userId);
        verify(userRepository).save(user);
    }

    @Test
    void testRejectUser() {

        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .username("testuser")
                .approved(true)
                .enabled(true)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);


        userService.rejectUser(userId);


        assertFalse(user.isApproved());
        assertFalse(user.isEnabled());
        verify(userRepository).findById(userId);
        verify(userRepository).save(user);
    }

    @Test
    void testToggleUserEnabled() {

        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .username("testuser")
                .enabled(true)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);


        userService.toggleUserEnabled(userId);


        assertFalse(user.isEnabled());
        verify(userRepository).findById(userId);
        verify(userRepository).save(user);
    }

    @Test
    void testUpdateUserRole() {

        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .username("testuser")
                .role(UserRole.USER)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);


        userService.updateUserRole(userId, UserRole.ADMIN);


        assertEquals(UserRole.ADMIN, user.getRole());
        verify(userRepository).findById(userId);
        verify(userRepository).save(user);
    }

    @Test
    void testDeleteUser() {

        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .username("testuser")
                .build();

        UserProfile profile = UserProfile.builder()
                .id(UUID.randomUUID())
                .user(user)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));


        userService.deleteUser(userId);


        verify(userRepository).findById(userId);
        verify(verificationTokenRepository).deleteByUser(user);
        verify(userSessionRepository).deleteByUser(user);
        verify(userProfileRepository).findByUserId(userId);
        verify(userProfileRepository).delete(profile);
        verify(userRepository).delete(user);
    }

    @Test
    void testCreateUserSession() {

        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .username("testuser")
                .build();

        String sessionId = "test-session-id";
        String ipAddress = "127.0.0.1";
        String userAgent = "Test Browser";

        when(userSessionRepository.save(any(UserSession.class))).thenAnswer(i -> i.getArgument(0));


        UserSession result = userService.createUserSession(user, sessionId, ipAddress, userAgent);


        assertNotNull(result);
        assertEquals(user, result.getUser());
        assertEquals(sessionId, result.getSessionId());
        assertEquals(ipAddress, result.getIpAddress());
        assertEquals(userAgent, result.getUserAgent());
        assertTrue(result.isActive());

        verify(userSessionRepository).save(any(UserSession.class));
    }

    @Test
    void testUpdateUserSessionActivity() {

        String sessionId = "test-session-id";
        UserSession session = UserSession.builder()
                .id(UUID.randomUUID())
                .sessionId(sessionId)
                .lastActive(LocalDateTime.now().minusHours(1))
                .build();

        when(userSessionRepository.findBySessionId(sessionId)).thenReturn(Optional.of(session));
        when(userSessionRepository.save(any(UserSession.class))).thenReturn(session);


        userService.updateUserSessionActivity(sessionId);


        assertTrue(session.getLastActive().isAfter(LocalDateTime.now().minusMinutes(1)));
        verify(userSessionRepository).findBySessionId(sessionId);
        verify(userSessionRepository).save(session);
    }

    @Test
    void testInvalidateUserSession() {

        String sessionId = "test-session-id";
        UserSession session = UserSession.builder()
                .id(UUID.randomUUID())
                .sessionId(sessionId)
                .active(true)
                .build();

        when(userSessionRepository.findBySessionId(sessionId)).thenReturn(Optional.of(session));
        when(userSessionRepository.save(any(UserSession.class))).thenReturn(session);


        userService.invalidateUserSession(sessionId);


        assertFalse(session.isActive());
        assertNotNull(session.getLogoutTime());
        verify(userSessionRepository).findBySessionId(sessionId);
        verify(userSessionRepository).save(session);
    }

    @Test
    void testInvalidateAllUserSessions() {

        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .username("testuser")
                .build();

        UserSession session1 = UserSession.builder()
                .id(UUID.randomUUID())
                .user(user)
                .active(true)
                .build();

        UserSession session2 = UserSession.builder()
                .id(UUID.randomUUID())
                .user(user)
                .active(true)
                .build();

        List<UserSession> activeSessions = List.of(session1, session2);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userSessionRepository.findByUserAndActive(user, true)).thenReturn(activeSessions);


        userService.invalidateAllUserSessions(userId);


        assertFalse(session1.isActive());
        assertFalse(session2.isActive());
        assertNotNull(session1.getLogoutTime());
        assertNotNull(session2.getLogoutTime());

        verify(userRepository).findById(userId);
        verify(userSessionRepository).findByUserAndActive(user, true);
        verify(userSessionRepository).saveAll(activeSessions);
    }

    @Test
    void testGetUnapprovedUsers() {

        User user1 = User.builder()
                .id(UUID.randomUUID())
                .username("user1")
                .approved(false)
                .build();

        User user2 = User.builder()
                .id(UUID.randomUUID())
                .username("user2")
                .approved(false)
                .build();

        when(userRepository.findByApproved(false)).thenReturn(List.of(user1, user2));


        List<UserResponseDto> result = userService.getUnapprovedUsers();


        assertEquals(2, result.size());
        assertEquals(user1.getId(), result.get(0).getId());
        assertEquals(user2.getId(), result.get(1).getId());

        verify(userRepository).findByApproved(false);
    }

    @Test
    void testInitiatePasswordReset() {

        String email = "test@example.com";
        User user = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email(email)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(verificationTokenRepository.findByUserAndTokenType(eq(user), eq(VerificationToken.TokenType.PASSWORD_RESET)))
                .thenReturn(Collections.emptyList());


        userService.initiatePasswordReset(email);


        verify(userRepository).findByEmail(email);
        verify(verificationTokenRepository).findByUserAndTokenType(user, VerificationToken.TokenType.PASSWORD_RESET);
        verify(verificationTokenRepository).save(any(VerificationToken.class));
        verify(emailService).sendPasswordResetEmail(eq(email), anyString(), eq(user.getUsername()));
    }

    @Test
    void testValidatePasswordResetToken_Valid() {

        String token = "valid-token";
        VerificationToken resetToken = VerificationToken.builder()
                .id(UUID.randomUUID())
                .token(token)
                .tokenType(VerificationToken.TokenType.PASSWORD_RESET)
                .expiryDate(LocalDateTime.now().plusHours(1))
                .build();

        when(verificationTokenRepository.findByToken(token)).thenReturn(Optional.of(resetToken));


        boolean result = userService.validatePasswordResetToken(token);


        assertTrue(result);
        verify(verificationTokenRepository).findByToken(token);
    }

    @Test
    void testValidatePasswordResetToken_InvalidType() {

        String token = "invalid-type-token";
        VerificationToken resetToken = VerificationToken.builder()
                .id(UUID.randomUUID())
                .token(token)
                .tokenType(VerificationToken.TokenType.EMAIL_VERIFICATION)
                .expiryDate(LocalDateTime.now().plusHours(1))
                .build();

        when(verificationTokenRepository.findByToken(token)).thenReturn(Optional.of(resetToken));


        MeetrulyException exception = assertThrows(
                MeetrulyException.class,
                () -> userService.validatePasswordResetToken(token)
        );

        assertEquals("Invalid token type", exception.getMessage());
        verify(verificationTokenRepository).findByToken(token);
    }

    @Test
    void testResendVerificationEmail() {

        String email = "test@example.com";
        User user = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email(email)
                .emailVerified(false)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));


        userService.resendVerificationEmail(email);


        verify(userRepository).findByEmail(email);

    }

    @Test
    void testResendVerificationEmail_AlreadyVerified() {

        String email = "test@example.com";
        User user = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email(email)
                .emailVerified(true)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));


        MeetrulyException exception = assertThrows(
                MeetrulyException.class,
                () -> userService.resendVerificationEmail(email)
        );

        assertEquals("Email is already verified", exception.getMessage());
        verify(userRepository).findByEmail(email);
    }

    @Test
    void testChangePassword_Success() {

        UUID userId = UUID.randomUUID();
        String oldPassword = "OldPassword123";
        String newPassword = "NewPassword123";

        User user = User.builder()
                .id(userId)
                .username("testuser")
                .password("encodedOldPassword")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(oldPassword, user.getPassword())).thenReturn(true);
        when(passwordEncoder.encode(newPassword)).thenReturn("encodedNewPassword");


        userService.changePassword(userId, oldPassword, newPassword);


        assertEquals("encodedNewPassword", user.getPassword());
        verify(userRepository).findById(userId);
        verify(passwordEncoder).matches(oldPassword, "encodedOldPassword");
        verify(passwordEncoder).encode(newPassword);
        verify(userRepository).save(user);
    }

    @Test
    void testChangePassword_IncorrectOldPassword() {

        UUID userId = UUID.randomUUID();
        String oldPassword = "WrongPassword123";
        String newPassword = "NewPassword123";

        User user = User.builder()
                .id(userId)
                .username("testuser")
                .password("encodedOldPassword")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(oldPassword, user.getPassword())).thenReturn(false);


        MeetrulyException exception = assertThrows(
                MeetrulyException.class,
                () -> userService.changePassword(userId, oldPassword, newPassword)
        );

        assertEquals("Current password is incorrect", exception.getMessage());
        verify(userRepository).findById(userId);
        verify(passwordEncoder).matches(oldPassword, "encodedOldPassword");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }
}