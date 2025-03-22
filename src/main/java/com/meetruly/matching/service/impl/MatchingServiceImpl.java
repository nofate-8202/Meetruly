package com.meetruly.matching.service.impl;

import com.meetruly.core.constant.Interest;
import com.meetruly.core.exception.MeetrulyException;
import com.meetruly.matching.dto.*;
import com.meetruly.matching.model.Like;
import com.meetruly.matching.model.Match;
import com.meetruly.matching.model.ProfileView;
import com.meetruly.matching.repository.LikeRepository;
import com.meetruly.matching.repository.MatchRepository;
import com.meetruly.matching.repository.ProfileViewRepository;
import com.meetruly.matching.service.MatchingService;
import com.meetruly.user.model.User;
import com.meetruly.user.model.UserProfile;
import com.meetruly.user.repository.UserProfileRepository;
import com.meetruly.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchingServiceImpl implements MatchingService {

    private final LikeRepository likeRepository;
    private final MatchRepository matchRepository;
    private final ProfileViewRepository profileViewRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;


    @Override
    @Transactional
    public LikeDto likeUser(UUID likerId, UUID likedId) {


        User liker = userRepository.findById(likerId)
                .orElseThrow(() -> new MeetrulyException("Liker user not found with id: " + likerId));

        User liked = userRepository.findById(likedId)
                .orElseThrow(() -> new MeetrulyException("Liked user not found with id: " + likedId));


        if (likerId.equals(likedId)) {
            throw new MeetrulyException("User cannot like themselves");
        }


        Optional<Like> existingLike = likeRepository.findByLikerAndLiked(liker, liked);
        if (existingLike.isPresent()) {
            return convertToLikeDto(existingLike.get());
        }


        Like like = Like.builder()
                .liker(liker)
                .liked(liked)
                .viewed(false)
                .createdAt(LocalDateTime.now())
                .build();

        Like savedLike = likeRepository.save(like);


        checkAndCreateMatch(liker, liked);

        return convertToLikeDto(savedLike);
    }


    @Override
    @Transactional
    public void unlikeUser(UUID likerId, UUID likedId) {
        User liker = userRepository.findById(likerId)
                .orElseThrow(() -> new MeetrulyException("Liker user not found with id: " + likerId));

        User liked = userRepository.findById(likedId)
                .orElseThrow(() -> new MeetrulyException("Liked user not found with id: " + likedId));

        Optional<Like> existingLike = likeRepository.findByLikerAndLiked(liker, liked);

        existingLike.ifPresent(likeRepository::delete);
    }


    @Override
    public boolean hasLiked(UUID likerId, UUID likedId) {
        User liker = userRepository.findById(likerId)
                .orElseThrow(() -> new MeetrulyException("Liker user not found with id: " + likerId));

        User liked = userRepository.findById(likedId)
                .orElseThrow(() -> new MeetrulyException("Liked user not found with id: " + likedId));

        return likeRepository.existsByLikerAndLiked(liker, liked);
    }


    @Override
    public List<LikeDto> getLikesByUser(UUID userId, boolean asLiker) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MeetrulyException("User not found with id: " + userId));

        List<Like> likes;
        if (asLiker) {


            likes = likeRepository.findByLiker(user);
        } else {


            likes = likeRepository.findByLiked(user);
        }

        return likes.stream()
                .map(this::convertToLikeDto)
                .collect(Collectors.toList());
    }


    @Override
    public List<LikeDto> getUnviewedLikesByUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MeetrulyException("User not found with id: " + userId));

        List<Like> unviewedLikes = likeRepository.findUnviewedLikesByUser(user);

        return unviewedLikes.stream()
                .map(this::convertToLikeDto)
                .collect(Collectors.toList());
    }


    @Override
    @Transactional
    public void markLikeAsViewed(UUID likeId) {
        Like like = likeRepository.findById(likeId)
                .orElseThrow(() -> new MeetrulyException("Like not found with id: " + likeId));

        like.setViewed(true);
        likeRepository.save(like);
    }


    @Override
    public long countLikesByUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MeetrulyException("User not found with id: " + userId));

        return likeRepository.countLikesByUser(user);
    }


    @Override
    public Optional<MatchDto> getMatchByUsers(UUID user1Id, UUID user2Id) {
        User user1 = userRepository.findById(user1Id)
                .orElseThrow(() -> new MeetrulyException("User 1 not found with id: " + user1Id));

        User user2 = userRepository.findById(user2Id)
                .orElseThrow(() -> new MeetrulyException("User 2 not found with id: " + user2Id));

        return matchRepository.findByUsers(user1, user2)
                .map(match -> convertToMatchDto(match, user1Id));
    }


    @Override
    public List<MatchDto> getMatchesByUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MeetrulyException("User not found with id: " + userId));

        List<Match> matches = matchRepository.findByUser(user);

        return matches.stream()
                .map(match -> convertToMatchDto(match, userId))
                .collect(Collectors.toList());
    }


    @Override
    public Page<MatchDto> getMatchesByUser(UUID userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MeetrulyException("User not found with id: " + userId));

        return matchRepository.findByUser(user, pageable)
                .map(match -> convertToMatchDto(match, userId));
    }


    @Override
    public List<MatchDto> getMutualLikeMatchesByUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MeetrulyException("User not found with id: " + userId));

        List<Match> matches = matchRepository.findMutualLikeMatchesByUser(user);

        return matches.stream()
                .map(match -> convertToMatchDto(match, userId))
                .collect(Collectors.toList());
    }


    @Override
    public List<MatchDto> getDailySuggestionMatchesByUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MeetrulyException("User not found with id: " + userId));

        List<Match> matches = matchRepository.findDailySuggestionMatchesByUser(user);

        return matches.stream()
                .map(match -> convertToMatchDto(match, userId))
                .collect(Collectors.toList());
    }


    @Override
    public List<MatchDto> getUnviewedMatchesByUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MeetrulyException("User not found with id: " + userId));

        List<Match> matches = matchRepository.findUnviewedMatchesByUser(user);

        return matches.stream()
                .map(match -> convertToMatchDto(match, userId))
                .collect(Collectors.toList());
    }


    @Override
    @Transactional
    public void markMatchAsViewedByUser(UUID matchId, UUID userId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new MeetrulyException("Match not found with id: " + matchId));

        if (!match.involveUser(userId)) {
            throw new MeetrulyException("User is not part of this match");
        }

        match.markViewedByUser(userId);
        matchRepository.save(match);
    }


    @Override
    public long countMatchesByUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MeetrulyException("User not found with id: " + userId));

        return matchRepository.countMatchesByUser(user);
    }


    @Override
    @Transactional
    public ProfileViewDto recordProfileView(UUID viewerId, UUID viewedId) {


        User viewer = userRepository.findById(viewerId)
                .orElseThrow(() -> new MeetrulyException("Viewer user not found with id: " + viewerId));

        User viewed = userRepository.findById(viewedId)
                .orElseThrow(() -> new MeetrulyException("Viewed user not found with id: " + viewedId));


        if (viewerId.equals(viewedId)) {
            throw new MeetrulyException("User cannot view their own profile");
        }


        ProfileView profileView = ProfileView.builder()
                .viewer(viewer)
                .viewed(viewed)
                .viewedAt(LocalDateTime.now())
                .notificationSent(false)
                .build();

        ProfileView savedProfileView = profileViewRepository.save(profileView);

        return convertToProfileViewDto(savedProfileView);
    }


    @Override
    public List<ProfileViewDto> getProfileViewsByUser(UUID userId, boolean asViewer) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MeetrulyException("User not found with id: " + userId));

        List<ProfileView> profileViews;
        if (asViewer) {


            profileViews = profileViewRepository.findByViewer(user);
        } else {


            profileViews = profileViewRepository.findByViewed(user);
        }

        return profileViews.stream()
                .map(this::convertToProfileViewDto)
                .collect(Collectors.toList());
    }


    @Override
    public Page<ProfileViewDto> getProfileViewsByViewed(UUID viewedId, Pageable pageable) {
        User viewed = userRepository.findById(viewedId)
                .orElseThrow(() -> new MeetrulyException("User not found with id: " + viewedId));

        return profileViewRepository.findByViewed(viewed, pageable)
                .map(this::convertToProfileViewDto);
    }


    @Override
    public long countProfileViewsByUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MeetrulyException("User not found with id: " + userId));

        return profileViewRepository.countViewsByUser(user);
    }


    @Override
    public List<ProfileCardDto> getDailySuggestionsForUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MeetrulyException("User not found with id: " + userId));


        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        List<Match> todayMatches = matchRepository.findDailySuggestionMatchesCreatedSince(todayStart);

        List<ProfileCardDto> suggestions = new ArrayList<>();


        List<Match> userMatches = todayMatches.stream()
                .filter(match -> match.involveUser(userId))
                .collect(Collectors.toList());

        if (userMatches.isEmpty()) {


            suggestions = generateDailySuggestionsForUser(user);
        } else {


            suggestions = userMatches.stream()
                    .map(match -> {
                        User otherUser = match.getOtherUser(userId);
                        return createProfileCardDto(otherUser, match.getCompatibilityScore(), true, hasLiked(userId, otherUser.getId()));
                    })
                    .collect(Collectors.toList());
        }

        return suggestions;
    }


    @Override
    @Scheduled(cron = "0 0 0 * * ?") // Изпълнява се всеки ден в полунощ
    @Transactional
    public void generateDailySuggestionsForAllUsers() {
        log.info("Generating daily suggestions for all users");


        List<User> activeUsers = userRepository.findByEnabledAndApprovedAndProfileCompleted(true, true, true);

        for (User user : activeUsers) {
            try {
                generateDailySuggestionsForUser(user);
                log.debug("Generated daily suggestions for user: {}", user.getUsername());
            } catch (Exception e) {
                log.error("Failed to generate daily suggestions for user: {}", user.getUsername(), e);
            }
        }

        log.info("Completed generating daily suggestions for all users");
    }


    private List<ProfileCardDto> generateDailySuggestionsForUser(User user) {


        List<User> potentialMatches = matchRepository.findUsersNotMatchedWithUser(user.getId());






        List<Map.Entry<User, Double>> userScores = new ArrayList<>();

        for (User potentialMatch : potentialMatches) {
            double score = calculateCompatibilityScore(user.getId(), potentialMatch.getId());
            userScores.add(new AbstractMap.SimpleEntry<>(potentialMatch, score));
        }


        userScores.sort(Map.Entry.<User, Double>comparingByValue().reversed());


        int maxSuggestions = Math.min(5, userScores.size());
        List<Map.Entry<User, Double>> topMatches = userScores.subList(0, maxSuggestions);


        List<ProfileCardDto> suggestions = new ArrayList<>();

        for (Map.Entry<User, Double> entry : topMatches) {
            User matchUser = entry.getKey();
            double score = entry.getValue();


            createDailySuggestionMatch(user.getId(), matchUser.getId(), score);


            ProfileCardDto card = createProfileCardDto(matchUser, score, true, hasLiked(user.getId(), matchUser.getId()));
            suggestions.add(card);
        }

        return suggestions;
    }


    @Override
    @Transactional
    public MatchDto createDailySuggestionMatch(UUID user1Id, UUID user2Id, double compatibilityScore) {
        User user1 = userRepository.findById(user1Id)
                .orElseThrow(() -> new MeetrulyException("User 1 not found with id: " + user1Id));

        User user2 = userRepository.findById(user2Id)
                .orElseThrow(() -> new MeetrulyException("User 2 not found with id: " + user2Id));


        Optional<Match> existingMatch = matchRepository.findByUsers(user1, user2);
        if (existingMatch.isPresent()) {
            return convertToMatchDto(existingMatch.get(), user1Id);
        }


        Match match = Match.builder()
                .user1(user1)
                .user2(user2)
                .compatibilityScore(compatibilityScore)
                .matchType(Match.MatchType.DAILY_SUGGESTION)
                .user1Viewed(false)
                .user2Viewed(false)
                .createdAt(LocalDateTime.now())
                .build();

        Match savedMatch = matchRepository.save(match);

        return convertToMatchDto(savedMatch, user1Id);
    }


    @Override
    public List<ProfileCardDto> getTopLikedUsers(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        Page<Object[]> topLikedUsersPage = likeRepository.findTopLikedUsers(pageable);

        List<ProfileCardDto> topUsers = new ArrayList<>();

        for (Object[] result : topLikedUsersPage.getContent()) {
            UUID userId = UUID.fromString(result[0].toString());
            long likeCount = Long.parseLong(result[1].toString());

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new MeetrulyException("User not found with id: " + userId));


            ProfileCardDto card = createProfileCardDto(user, 0, false, false);
            topUsers.add(card);
        }

        return topUsers;
    }


    @Override
    public MatchingSummaryDto getMatchingSummary(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MeetrulyException("User not found with id: " + userId));


        long totalLikesReceived = likeRepository.findByLiked(user).size();
        long totalLikesGiven = likeRepository.findByLiker(user).size();
        long totalMatches = matchRepository.countMatchesByUser(user);
        long unviewedLikes = likeRepository.findUnviewedLikesByUser(user).size();
        long unviewedMatches = matchRepository.findUnviewedMatchesByUser(user).size();
        long totalProfileViews = profileViewRepository.countViewsByUser(user);


        List<ProfileCardDto> dailySuggestions = getDailySuggestionsForUser(userId);


        List<ProfileCardDto> topLikedUsers = getTopLikedUsers(5);

        return MatchingSummaryDto.builder()
                .totalLikesReceived(totalLikesReceived)
                .totalLikesGiven(totalLikesGiven)
                .totalMatches(totalMatches)
                .unviewedLikes(unviewedLikes)
                .unviewedMatches(unviewedMatches)
                .totalProfileViews(totalProfileViews)
                .dailySuggestions(dailySuggestions)
                .topLikedUsers(topLikedUsers)
                .build();
    }


    @Override
    public double calculateCompatibilityScore(UUID user1Id, UUID user2Id) {


        Optional<UserProfile> profile1Opt = userProfileRepository.findByUserId(user1Id);
        Optional<UserProfile> profile2Opt = userProfileRepository.findByUserId(user2Id);

        if (profile1Opt.isEmpty() || profile2Opt.isEmpty()) {
            return 0.0;
        }

        UserProfile profile1 = profile1Opt.get();
        UserProfile profile2 = profile2Opt.get();


        double interestScore = calculateInterestCompatibility(profile1, profile2);





        return interestScore;
    }


    private double calculateInterestCompatibility(UserProfile profile1, UserProfile profile2) {
        Set<Interest> interests1 = profile1.getInterests();
        Set<Interest> interests2 = profile2.getInterests();

        if (interests1.isEmpty() || interests2.isEmpty()) {
            return 0.0;
        }


        Set<Interest> commonInterests = new HashSet<>(interests1);
        commonInterests.retainAll(interests2);


        double totalInterests = Math.max(interests1.size(), interests2.size());
        return (commonInterests.size() / totalInterests) * 100.0;
    }



    private LikeDto convertToLikeDto(Like like) {
        return LikeDto.builder()
                .id(like.getId())
                .likerId(like.getLiker().getId())
                .likerUsername(like.getLiker().getUsername())
                .likerProfileImageUrl(getProfileImageUrl(like.getLiker().getId()))
                .likedId(like.getLiked().getId())
                .likedUsername(like.getLiked().getUsername())
                .likedProfileImageUrl(getProfileImageUrl(like.getLiked().getId()))
                .viewed(like.isViewed())
                .createdAt(like.getCreatedAt())
                .build();
    }

    private MatchDto convertToMatchDto(Match match, UUID currentUserId) {
        boolean isUser1 = match.getUser1().getId().equals(currentUserId);
        User otherUser = isUser1 ? match.getUser2() : match.getUser1();

        MatchDto dto = MatchDto.builder()
                .id(match.getId())
                .user1Id(match.getUser1().getId())
                .user1Username(match.getUser1().getUsername())
                .user1ProfileImageUrl(getProfileImageUrl(match.getUser1().getId()))
                .user2Id(match.getUser2().getId())
                .user2Username(match.getUser2().getUsername())
                .user2ProfileImageUrl(getProfileImageUrl(match.getUser2().getId()))
                .compatibilityScore(match.getCompatibilityScore())
                .matchType(match.getMatchType())
                .user1Viewed(match.isUser1Viewed())
                .user2Viewed(match.isUser2Viewed())
                .createdAt(match.getCreatedAt())
                .build();


        dto.setOtherUserId(otherUser.getId());
        dto.setOtherUsername(otherUser.getUsername());
        dto.setOtherProfileImageUrl(getProfileImageUrl(otherUser.getId()));
        dto.setViewed(isUser1 ? match.isUser1Viewed() : match.isUser2Viewed());

        return dto;
    }

    private ProfileViewDto convertToProfileViewDto(ProfileView profileView) {
        return ProfileViewDto.builder()
                .id(profileView.getId())
                .viewerId(profileView.getViewer().getId())
                .viewerUsername(profileView.getViewer().getUsername())
                .viewerProfileImageUrl(getProfileImageUrl(profileView.getViewer().getId()))
                .viewedId(profileView.getViewed().getId())
                .viewedUsername(profileView.getViewed().getUsername())
                .viewedProfileImageUrl(getProfileImageUrl(profileView.getViewed().getId()))
                .viewedAt(profileView.getViewedAt())
                .notificationSent(profileView.isNotificationSent())
                .build();
    }

    private ProfileCardDto createProfileCardDto(User user, double compatibilityScore, boolean isMatch, boolean isLiked) {
        ProfileCardDto dto = ProfileCardDto.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .gender(user.getGender())
                .compatibilityScore(compatibilityScore)
                .isMatch(isMatch)
                .isLiked(isLiked)
                .build();


        userProfileRepository.findByUserId(user.getId()).ifPresent(profile -> {
            dto.setProfileImageUrl(profile.getProfileImageUrl());
            dto.setAge(profile.getAge());

            if (profile.getCity() != null) {
                dto.setCity(profile.getCity().getDisplayName());
            }
        });

        return dto;
    }

    private String getProfileImageUrl(UUID userId) {
        return userProfileRepository.findByUserId(userId)
                .map(UserProfile::getProfileImageUrl)
                .orElse("/images/default-profile.jpg");
    }


    private void checkAndCreateMatch(User user1, User user2) {


        boolean mutualLike = likeRepository.existsByLikerAndLiked(user2, user1);

        if (mutualLike) {


            Optional<Match> existingMatch = matchRepository.findByUsers(user1, user2);

            if (existingMatch.isEmpty()) {


                double compatibilityScore = calculateCompatibilityScore(user1.getId(), user2.getId());


                Match match = Match.builder()
                        .user1(user1)
                        .user2(user2)
                        .compatibilityScore(compatibilityScore)
                        .matchType(Match.MatchType.MUTUAL_LIKE)
                        .user1Viewed(false)
                        .user2Viewed(false)
                        .createdAt(LocalDateTime.now())
                        .build();

                matchRepository.save(match);

                log.info("Created new mutual like match between users: {} and {}",
                        user1.getUsername(), user2.getUsername());
            }
        }
    }
}