package com.meetruly.user.service.impl;

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
import com.meetruly.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service

@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final VerificationTokenRepository tokenRepository;
    private final UserSessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;




    public UserServiceImpl(UserRepository userRepository, UserProfileRepository profileRepository, VerificationTokenRepository tokenRepository, UserSessionRepository sessionRepository, PasswordEncoder passwordEncoder, EmailService emailService) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.tokenRepository = tokenRepository;
        this.sessionRepository = sessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }


    private void ensureFirstUserIsApproved(User user) {

        long userCount = userRepository.count();

        if (userCount == 1 && user.getRole() == UserRole.ADMIN &&
                (!user.isApproved() || !user.isEmailVerified())) {
            log.info("Automatically approving and verifying first admin user: {}", user.getUsername());
            user.setApproved(true);
            user.setEmailVerified(true);
            userRepository.save(user);
        }
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        if (!user.isEnabled()) {
            throw new UsernameNotFoundException("User account is disabled.");
        }

        if (!user.isEmailVerified() && user.getRole() != UserRole.ADMIN) {
            throw new UsernameNotFoundException("Email is not verified yet.");
        }

        if (!user.isEnabled()) {
            throw new UsernameNotFoundException("User account is disabled.");
        }

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.isEnabled(),
                true,
                true,
                user.isAccountNonLocked(),
                authorities
        );
    }

    @Override
    @Transactional
    public User registerUser(UserRegistrationDto registrationDto) {

        if (userRepository.existsByUsername(registrationDto.getUsername())) {
            throw new MeetrulyException("Username is already taken!");
        }

        if (userRepository.existsByEmail(registrationDto.getEmail())) {
            throw new MeetrulyException("Email is already in use!");
        }

        if (!registrationDto.getPassword().equals(registrationDto.getConfirmPassword())) {
            throw new MeetrulyException("Passwords do not match!");
        }


        boolean isFirst = userRepository.count() == 0;
        log.info("Registering new user: {}. Is first user: {}", registrationDto.getUsername(), isFirst);

        User user = User.builder()
                .username(registrationDto.getUsername())
                .email(registrationDto.getEmail())
                .password(passwordEncoder.encode(registrationDto.getPassword()))
                .gender(registrationDto.getGender())

                .role(isFirst ? UserRole.ADMIN : UserRole.USER)
                .enabled(true)
                .accountNonLocked(true)

                .emailVerified(isFirst)
                .profileCompleted(false)

                .approved(isFirst)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);

        ensureFirstUserIsApproved(savedUser);

        if (!isFirst) {
            try {
                createEmailVerificationTokenInNewTransaction(savedUser);
            } catch (Exception e) {
                log.error("Failed to create verification token", e);
            }
        }

        return savedUser;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createEmailVerificationTokenInNewTransaction(User user) {
        createEmailVerificationToken(user);
    }

    @Override
    public boolean isFirstUser() {

        return !userRepository.existsByRole(UserRole.ADMIN);
    }

    @Override
    @Transactional
    public void createEmailVerificationToken(User user) {

        deleteExistingVerificationTokens(user);

        createNewVerificationToken(user);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteExistingVerificationTokens(User user) {
        List<VerificationToken> existingTokens = tokenRepository.findByUserAndTokenType(
                user, VerificationToken.TokenType.EMAIL_VERIFICATION);

        if (!existingTokens.isEmpty()) {
            tokenRepository.deleteAll(existingTokens);
            tokenRepository.flush();
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createNewVerificationToken(User user) {
        String tokenValue = UUID.randomUUID().toString();
        VerificationToken verificationToken = VerificationToken.builder()
                .token(tokenValue)
                .user(user)
                .createdAt(LocalDateTime.now())
                .expiryDate(LocalDateTime.now().plusHours(24))
                .tokenType(VerificationToken.TokenType.EMAIL_VERIFICATION)
                .build();

        verificationToken = tokenRepository.save(verificationToken);
        tokenRepository.flush();

        emailService.sendVerificationEmail(user.getEmail(), tokenValue, user.getUsername());
    }

    @Override
    @Transactional
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new MeetrulyException("User not found with email: " + email));

        if (user.isEmailVerified()) {
            throw new MeetrulyException("Email is already verified");
        }

        try {
            createEmailVerificationTokenInNewTransaction(user);
        } catch (Exception e) {
            log.error("Failed to resend verification email", e);
            throw new MeetrulyException("Failed to resend verification email");
        }
    }

    @Override
    public boolean isUserApproved(String username) {
        return userRepository.findByUsername(username)
                .map(User::isApproved)
                .orElse(false);
    }

    @Override
    @Transactional
    public boolean verifyEmail(String token) {
        VerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new MeetrulyException("Invalid verification token"));

        if (verificationToken.isExpired()) {

            throw new MeetrulyException("Token has expired");
        }

        if (verificationToken.getTokenType() != VerificationToken.TokenType.EMAIL_VERIFICATION) {
            throw new MeetrulyException("Invalid token type");
        }

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        userRepository.flush();

        deleteVerificationTokenInNewTransaction(verificationToken.getId());

        return true;
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteVerificationTokenInNewTransaction(UUID tokenId) {
        try {
            tokenRepository.deleteById(tokenId);
            tokenRepository.flush();
        } catch (Exception e) {
            log.error("Error deleting verification token: {}", e.getMessage(), e);

        }
    }

    @Override
    @Transactional
    public void initiatePasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new MeetrulyException("User not found with email: " + email));

        List<VerificationToken> existingTokens = tokenRepository.findByUserAndTokenType(
                user, VerificationToken.TokenType.PASSWORD_RESET);

        tokenRepository.deleteAll(existingTokens);

        String token = UUID.randomUUID().toString();
        VerificationToken resetToken = VerificationToken.builder()
                .token(token)
                .user(user)
                .createdAt(LocalDateTime.now())
                .expiryDate(LocalDateTime.now().plusHours(1))
                .tokenType(VerificationToken.TokenType.PASSWORD_RESET)
                .build();

        tokenRepository.save(resetToken);

        emailService.sendPasswordResetEmail(user.getEmail(), token, user.getUsername());
    }

    @Override
    public boolean validatePasswordResetToken(String token) {
        VerificationToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new MeetrulyException("Invalid reset token"));

        if (resetToken.isExpired()) {
            tokenRepository.delete(resetToken);
            throw new MeetrulyException("Token has expired");
        }

        if (resetToken.getTokenType() != VerificationToken.TokenType.PASSWORD_RESET) {
            throw new MeetrulyException("Invalid token type");
        }

        return true;
    }

    @Override
    @Transactional
    public void resetPassword(PasswordResetConfirmRequest resetRequest) {

        if (!resetRequest.getPassword().equals(resetRequest.getConfirmPassword())) {
            throw new MeetrulyException("Passwords do not match!");
        }

        VerificationToken resetToken = tokenRepository.findByToken(resetRequest.getToken())
                .orElseThrow(() -> new MeetrulyException("Invalid reset token"));

        if (resetToken.isExpired()) {
            tokenRepository.delete(resetToken);
            throw new MeetrulyException("Token has expired");
        }

        if (resetToken.getTokenType() != VerificationToken.TokenType.PASSWORD_RESET) {
            throw new MeetrulyException("Invalid token type");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(resetRequest.getPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        tokenRepository.delete(resetToken);

        invalidateAllUserSessions(user.getId());
    }

    @Override
    @Transactional
    public void changePassword(UUID userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MeetrulyException("User not found with id: " + userId));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new MeetrulyException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

    }

    @Override
    @Transactional
    public UserProfile createOrUpdateProfile(UUID userId, UserProfileDto profileDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MeetrulyException("User not found with id: " + userId));

        UserProfile profile = profileRepository.findByUserId(userId).orElse(null);

        if (profile == null) {
            profile = new UserProfile();
            profile.setUser(user);
            profile.setCreatedAt(LocalDateTime.now());
        }

        profile.setFirstName(profileDto.getFirstName());
        profile.setLastName(profileDto.getLastName());
        profile.setAge(profileDto.getAge());
        profile.setEyeColor(profileDto.getEyeColor());
        profile.setHairColor(profileDto.getHairColor());
        profile.setHeight(profileDto.getHeight());
        profile.setWeight(profileDto.getWeight());

        if (profileDto.getInterests() != null) {
            profile.setInterests(profileDto.getInterests());
        }

        profile.setPartnerPreferences(profileDto.getPartnerPreferences());
        profile.setRelationshipType(profileDto.getRelationshipType());
        profile.setRelationshipStatus(profileDto.getRelationshipStatus());
        profile.setCountry(profileDto.getCountry());
        profile.setCity(profileDto.getCity());

        if (profileDto.getProfileImageUrl() != null) {
            profile.setProfileImageUrl(profileDto.getProfileImageUrl());
        }

        profile.setUpdatedAt(LocalDateTime.now());

        UserProfile savedProfile = profileRepository.save(profile);

        if (!user.isProfileCompleted()) {
            user.setProfileCompleted(true);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
        }

        return savedProfile;
    }

    @Override
    public Optional<UserProfileDto> getUserProfile(UUID userId) {
        return profileRepository.findByUserId(userId)
                .map(this::convertToProfileDto);
    }

    @Override
    public List<ProfileCardDto> getUserProfiles(Gender gender, Pageable pageable) {
        return userRepository.findByGender(gender, pageable)
                .stream()
                .filter(User::isApproved)
                .filter(User::isEnabled)
                .map(this::convertToProfileCardDto)
                .collect(Collectors.toList());
    }

    @Override
    public SearchProfileResponse searchProfiles(SearchProfileRequest searchRequest) {

        Pageable pageable = PageRequest.of(
                searchRequest.getPage(),
                searchRequest.getSize(),
                Sort.by(Sort.Direction.DESC, "updatedAt")
        );

        int minAge = searchRequest.getMinAge() != null ? searchRequest.getMinAge() : 18;
        int maxAge = searchRequest.getMaxAge() != null ? searchRequest.getMaxAge() : 120;

        List<User> users = userRepository.findByApproved(true);
        List<User> filteredUsers = users.stream()
                .filter(User::isEnabled)
                .filter(user -> {

                    return searchRequest.getGender() == null ||
                            user.getGender() == searchRequest.getGender();
                })
                .toList();

        List<ProfileCardDto> profileDtos = new ArrayList<>();

        for (User user : filteredUsers) {
            Optional<UserProfile> profileOpt = profileRepository.findByUserId(user.getId());

            if (profileOpt.isPresent()) {
                UserProfile profile = profileOpt.get();

                if (profile.getAge() != null &&
                        (profile.getAge() < minAge || profile.getAge() > maxAge)) {
                    continue;
                }

                if (isProfileMatchingCriteria(profile, searchRequest)) {
                    profileDtos.add(convertProfileToCardDto(profile));
                }
            } else {

                ProfileCardDto basicCard = new ProfileCardDto();
                basicCard.setUserId(user.getId());
                basicCard.setUsername(user.getUsername());
                basicCard.setGender(user.getGender());
                basicCard.setBlurredImage(false);
                profileDtos.add(basicCard);
            }
        }

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), profileDtos.size());

        List<ProfileCardDto> pageContent = start >= profileDtos.size() ?
                Collections.emptyList() : profileDtos.subList(start, end);

        return SearchProfileResponse.builder()
                .profiles(pageContent)
                .currentPage(pageable.getPageNumber())
                .totalPages((int) Math.ceil((double) profileDtos.size() / pageable.getPageSize()))
                .totalElements((long) profileDtos.size())
                .build();
    }
    private ProfileCardDto convertProfileToCardDto(UserProfile profile) {
        ProfileCardDto dto = new ProfileCardDto();
        dto.setUserId(profile.getUser().getId());
        dto.setUsername(profile.getUser().getUsername());
        dto.setGender(profile.getUser().getGender());

        dto.setAge(profile.getAge());
        dto.setProfileImageUrl(profile.getProfileImageUrl());

        if (profile.getCity() != null) {
            dto.setCity(profile.getCity().getDisplayName());
        }

        dto.setBlurredImage(false);

        return dto;
    }

    private boolean isProfileMatchingCriteria(UserProfile profile, SearchProfileRequest request) {

        if (request.getEyeColor() != null &&
                profile.getEyeColor() != null &&
                profile.getEyeColor() != request.getEyeColor()) {
            return false;
        }

        if (request.getHairColor() != null &&
                profile.getHairColor() != null &&
                profile.getHairColor() != request.getHairColor()) {
            return false;
        }

        if (request.getMinHeight() != null &&
                profile.getHeight() != null &&
                profile.getHeight() < request.getMinHeight()) {
            return false;
        }

        if (request.getMaxHeight() != null &&
                profile.getHeight() != null &&
                profile.getHeight() > request.getMaxHeight()) {
            return false;
        }

        if (request.getMinWeight() != null &&
                profile.getWeight() != null &&
                profile.getWeight() < request.getMinWeight()) {
            return false;
        }

        if (request.getMaxWeight() != null &&
                profile.getWeight() != null &&
                profile.getWeight() > request.getMaxWeight()) {
            return false;
        }

        if (request.getInterests() != null &&
                !request.getInterests().isEmpty() &&
                profile.getInterests() != null &&
                !profile.getInterests().isEmpty()) {

            boolean hasMatchingInterest = profile.getInterests().stream()
                    .anyMatch(request.getInterests()::contains);

            if (!hasMatchingInterest) {
                return false;
            }
        }

        if (request.getRelationshipType() != null &&
                profile.getRelationshipType() != null &&
                profile.getRelationshipType() != request.getRelationshipType()) {
            return false;
        }

        if (request.getRelationshipStatus() != null &&
                profile.getRelationshipStatus() != null &&
                profile.getRelationshipStatus() != request.getRelationshipStatus()) {
            return false;
        }


        if (request.getCountry() != null &&
                profile.getCountry() != null &&
                profile.getCountry() != request.getCountry()) {
            return false;
        }

        if (request.getCity() != null &&
                profile.getCity() != null &&
                profile.getCity() != request.getCity()) {
            return false;
        }

        
        return true;
    }

    @Override
    public User getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new MeetrulyException("User not found with id: " + id));
    }

    @Override
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    @Transactional
    public User updateUser(UUID id, UserResponseDto userDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new MeetrulyException("User not found with id: " + id));

        user.setEnabled(userDto.isEnabled());
        user.setAccountNonLocked(userDto.isAccountNonLocked());
        user.setRole(userDto.getRole());
        user.setApproved(userDto.isApproved());
        user.setUpdatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new MeetrulyException("User not found with id: " + id));

        tokenRepository.deleteByUser(user);

        sessionRepository.deleteByUser(user);

        profileRepository.findByUserId(id).ifPresent(profileRepository::delete);

        userRepository.delete(user);
    }

    @Override
    @Transactional
    public void approveUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new MeetrulyException("User not found with id: " + id));

        user.setApproved(true);
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);
    }

    @Override
    @Transactional
    public void rejectUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new MeetrulyException("User not found with id: " + id));

        user.setApproved(false);
        user.setEnabled(false);
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);
    }

    @Override
    @Transactional
    public void toggleUserEnabled(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new MeetrulyException("User not found with id: " + id));

        user.setEnabled(!user.isEnabled());
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);
    }

    @Override
    @Transactional
    public void updateUserRole(UUID id, UserRole newRole) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new MeetrulyException("User not found with id: " + id));

        user.setRole(newRole);
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);
    }

    @Override
    @Transactional
    public UserSession createUserSession(User user, String sessionId, String ipAddress, String userAgent) {
        UserSession session = UserSession.builder()
                .user(user)
                .sessionId(sessionId)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .createdAt(LocalDateTime.now())
                .lastActive(LocalDateTime.now())
                .expiryDate(LocalDateTime.now().plusDays(30))
                .active(true)
                .build();

        return sessionRepository.save(session);
    }

    @Override
    public Optional<UserSession> getUserSession(String sessionId) {
        return sessionRepository.findBySessionId(sessionId);
    }

    @Override
    @Transactional
    public void updateUserSessionActivity(String sessionId) {
        UserSession session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new MeetrulyException("Session not found with id: " + sessionId));

        session.setLastActive(LocalDateTime.now());
        sessionRepository.save(session);
    }

    @Override
    @Transactional
    public void invalidateUserSession(String sessionId) {
        UserSession session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new MeetrulyException("Session not found with id: " + sessionId));

        session.setActive(false);
        session.setLogoutTime(LocalDateTime.now());

        sessionRepository.save(session);
    }

    @Override
    @Transactional
    public void invalidateAllUserSessions(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MeetrulyException("User not found with id: " + userId));

        List<UserSession> activeSessions = sessionRepository.findByUserAndActive(user, true);

        activeSessions.forEach(session -> {
            session.setActive(false);
            session.setLogoutTime(LocalDateTime.now());
        });

        sessionRepository.saveAll(activeSessions);
    }

    @Override
    public boolean canSendMessage(UUID userId) {


        return true;
    }

    @Override
    public boolean canViewFullProfile(UUID userId) {


        return true;
    }

    @Override
    public boolean hasReachedProfileViewLimit(UUID userId) {


        return false;
    }

    @Override
    public void incrementMessageCount(UUID userId) {

    }

    @Override
    public void incrementProfileViewCount(UUID userId) {

    }

    @Override
    public List<UserResponseDto> getUnapprovedUsers() {
        return userRepository.findByApproved(false).stream()
                .map(this::convertToUserResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public long countUsersByRole(UserRole role) {
        return userRepository.countByRole(role);
    }

    @Override
    public Page<UserResponseDto> getUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::convertToUserResponseDto);
    }


    private UserProfileDto convertToProfileDto(UserProfile profile) {
        return UserProfileDto.builder()
                .userId(profile.getUser().getId())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .age(profile.getAge())
                .eyeColor(profile.getEyeColor())
                .hairColor(profile.getHairColor())
                .height(profile.getHeight())
                .weight(profile.getWeight())
                .interests(profile.getInterests())
                .partnerPreferences(profile.getPartnerPreferences())
                .relationshipType(profile.getRelationshipType())
                .relationshipStatus(profile.getRelationshipStatus())
                .country(profile.getCountry())
                .city(profile.getCity())
                .profileImageUrl(profile.getProfileImageUrl())
                .build();
    }

    private ProfileCardDto convertToProfileCardDto(User user) {
        ProfileCardDto dto = new ProfileCardDto();
        dto.setUserId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setGender(user.getGender());

        profileRepository.findByUserId(user.getId()).ifPresent(profile -> {
            dto.setAge(profile.getAge());
            dto.setProfileImageUrl(profile.getProfileImageUrl());

            if (profile.getCity() != null) {
                dto.setCity(profile.getCity().getDisplayName());
            }
        });

        dto.setBlurredImage(false);

        return dto;
    }

    private UserResponseDto convertToUserResponseDto(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .gender(user.getGender())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .accountNonLocked(user.isAccountNonLocked())
                .emailVerified(user.isEmailVerified())
                .profileCompleted(user.isProfileCompleted())
                .approved(user.isApproved())
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .build();
    }
}