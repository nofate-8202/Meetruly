package com.meetruly.matching.service;

import com.meetruly.core.constant.Gender;
import com.meetruly.core.constant.Interest;
import com.meetruly.core.exception.MeetrulyException;
import com.meetruly.matching.dto.*;
import com.meetruly.matching.model.Like;
import com.meetruly.matching.model.Match;
import com.meetruly.matching.model.ProfileView;
import com.meetruly.matching.repository.LikeRepository;
import com.meetruly.matching.repository.MatchRepository;
import com.meetruly.matching.repository.ProfileViewRepository;
import com.meetruly.matching.service.impl.MatchingServiceImpl;
import com.meetruly.user.model.User;
import com.meetruly.user.model.UserProfile;
import com.meetruly.user.repository.UserProfileRepository;
import com.meetruly.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchingServiceImplTest {

    @Mock
    private LikeRepository likeRepository;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private ProfileViewRepository profileViewRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @InjectMocks
    private MatchingServiceImpl matchingService;

    private User user1;
    private User user2;
    private UserProfile profile1;
    private UserProfile profile2;
    private Like like;
    private Match match;
    private ProfileView profileView;

    @BeforeEach
    void setUp() {
        user1 = new User();
        user1.setId(UUID.randomUUID());
        user1.setUsername("user1");
        user1.setGender(Gender.MALE);
        user1.setEnabled(true);
        user1.setApproved(true);
        user1.setProfileCompleted(true);

        user2 = new User();
        user2.setId(UUID.randomUUID());
        user2.setUsername("user2");
        user2.setGender(Gender.FEMALE);
        user2.setEnabled(true);
        user2.setApproved(true);
        user2.setProfileCompleted(true);

        profile1 = new UserProfile();
        profile1.setUser(user1);
        profile1.setProfileImageUrl("/images/user1.jpg");
        profile1.setAge(25);
        Set<Interest> interests1 = new HashSet<>();
        interests1.add(Interest.MUSIC);
        interests1.add(Interest.TRAVEL);
        profile1.setInterests(interests1);

        profile2 = new UserProfile();
        profile2.setUser(user2);
        profile2.setProfileImageUrl("/images/user2.jpg");
        profile2.setAge(28);
        Set<Interest> interests2 = new HashSet<>();
        interests2.add(Interest.MUSIC);
        interests2.add(Interest.SPORT);
        profile2.setInterests(interests2);

        like = Like.builder()
                .id(UUID.randomUUID())
                .liker(user1)
                .liked(user2)
                .viewed(false)
                .createdAt(LocalDateTime.now())
                .build();

        match = Match.builder()
                .id(UUID.randomUUID())
                .user1(user1)
                .user2(user2)
                .compatibilityScore(75.0)
                .matchType(Match.MatchType.MUTUAL_LIKE)
                .user1Viewed(false)
                .user2Viewed(false)
                .createdAt(LocalDateTime.now())
                .build();

        profileView = ProfileView.builder()
                .id(UUID.randomUUID())
                .viewer(user1)
                .viewed(user2)
                .viewedAt(LocalDateTime.now())
                .notificationSent(false)
                .build();
    }

    @Test
    void likeUser_ShouldCreateNewLike() {
        when(userRepository.findById(user1.getId())).thenReturn(Optional.of(user1));
        when(userRepository.findById(user2.getId())).thenReturn(Optional.of(user2));
        when(likeRepository.findByLikerAndLiked(user1, user2)).thenReturn(Optional.empty());
        when(likeRepository.save(any(Like.class))).thenReturn(like);
        when(likeRepository.existsByLikerAndLiked(user2, user1)).thenReturn(false);
        when(userProfileRepository.findByUserId(any(UUID.class))).thenReturn(Optional.empty());

        LikeDto result = matchingService.likeUser(user1.getId(), user2.getId());

        assertNotNull(result);
        assertEquals(user1.getId(), result.getLikerId());
        assertEquals(user2.getId(), result.getLikedId());
        verify(likeRepository, times(1)).save(any(Like.class));
    }

    @Test
    void likeUser_WithExistingLike_ShouldReturnExistingLike() {
        when(userRepository.findById(user1.getId())).thenReturn(Optional.of(user1));
        when(userRepository.findById(user2.getId())).thenReturn(Optional.of(user2));
        when(likeRepository.findByLikerAndLiked(user1, user2)).thenReturn(Optional.of(like));
        when(userProfileRepository.findByUserId(any(UUID.class))).thenReturn(Optional.empty());

        LikeDto result = matchingService.likeUser(user1.getId(), user2.getId());

        assertNotNull(result);
        assertEquals(like.getId(), result.getId());
        verify(likeRepository, never()).save(any(Like.class));
    }

    @Test
    void likeUser_WhenUserLikesSelf_ShouldThrowException() {
        when(userRepository.findById(user1.getId())).thenReturn(Optional.of(user1));

        assertThrows(MeetrulyException.class, () -> {
            matchingService.likeUser(user1.getId(), user1.getId());
        });
        verify(likeRepository, never()).save(any(Like.class));
    }

    @Test
    void likeUser_WithMutualLike_ShouldCreateMatch() {
        when(userRepository.findById(user1.getId())).thenReturn(Optional.of(user1));
        when(userRepository.findById(user2.getId())).thenReturn(Optional.of(user2));
        when(likeRepository.findByLikerAndLiked(user1, user2)).thenReturn(Optional.empty());
        when(likeRepository.save(any(Like.class))).thenReturn(like);
        when(likeRepository.existsByLikerAndLiked(user2, user1)).thenReturn(true);
        when(matchRepository.findByUsers(user1, user2)).thenReturn(Optional.empty());
        when(matchRepository.save(any(Match.class))).thenReturn(match);
        when(userProfileRepository.findByUserId(user1.getId())).thenReturn(Optional.of(profile1));
        when(userProfileRepository.findByUserId(user2.getId())).thenReturn(Optional.of(profile2));

        LikeDto result = matchingService.likeUser(user1.getId(), user2.getId());

        assertNotNull(result);
        verify(matchRepository, times(1)).save(any(Match.class));
    }

    @Test
    void unlikeUser_ShouldDeleteLike() {
        when(userRepository.findById(user1.getId())).thenReturn(Optional.of(user1));
        when(userRepository.findById(user2.getId())).thenReturn(Optional.of(user2));
        when(likeRepository.findByLikerAndLiked(user1, user2)).thenReturn(Optional.of(like));

        matchingService.unlikeUser(user1.getId(), user2.getId());

        verify(likeRepository, times(1)).delete(like);
    }

    @Test
    void hasLiked_ShouldReturnTrue_WhenLikeExists() {
        when(userRepository.findById(user1.getId())).thenReturn(Optional.of(user1));
        when(userRepository.findById(user2.getId())).thenReturn(Optional.of(user2));
        when(likeRepository.existsByLikerAndLiked(user1, user2)).thenReturn(true);

        boolean result = matchingService.hasLiked(user1.getId(), user2.getId());

        assertTrue(result);
    }

    @Test
    void getLikesByUser_AsLiker_ShouldReturnLikerLikes() {
        when(userRepository.findById(user1.getId())).thenReturn(Optional.of(user1));
        when(likeRepository.findByLiker(user1)).thenReturn(List.of(like));
        when(userProfileRepository.findByUserId(any(UUID.class))).thenReturn(Optional.empty());

        List<LikeDto> result = matchingService.getLikesByUser(user1.getId(), true);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(user1.getId(), result.get(0).getLikerId());
    }

    @Test
    void markLikeAsViewed_ShouldUpdateLikeViewedStatus() {
        UUID likeId = like.getId();
        when(likeRepository.findById(likeId)).thenReturn(Optional.of(like));
        when(likeRepository.save(any(Like.class))).thenReturn(like);

        matchingService.markLikeAsViewed(likeId);

        assertTrue(like.isViewed());
        verify(likeRepository, times(1)).save(like);
    }

    @Test
    void getMatchByUsers_ShouldReturnMatch() {
        when(userRepository.findById(user1.getId())).thenReturn(Optional.of(user1));
        when(userRepository.findById(user2.getId())).thenReturn(Optional.of(user2));
        when(matchRepository.findByUsers(user1, user2)).thenReturn(Optional.of(match));
        when(userProfileRepository.findByUserId(any(UUID.class))).thenReturn(Optional.empty());

        Optional<MatchDto> result = matchingService.getMatchByUsers(user1.getId(), user2.getId());

        assertTrue(result.isPresent());
        assertEquals(match.getId(), result.get().getId());
    }

    @Test
    void getMatchesByUser_ShouldReturnUserMatches() {
        when(userRepository.findById(user1.getId())).thenReturn(Optional.of(user1));
        when(matchRepository.findByUser(user1)).thenReturn(List.of(match));
        when(userProfileRepository.findByUserId(any(UUID.class))).thenReturn(Optional.empty());

        List<MatchDto> result = matchingService.getMatchesByUser(user1.getId());

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(match.getId(), result.get(0).getId());
    }

    @Test
    void markMatchAsViewedByUser_ShouldUpdateMatchViewedStatus() {
        UUID matchId = match.getId();
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchRepository.save(any(Match.class))).thenReturn(match);

        matchingService.markMatchAsViewedByUser(matchId, user1.getId());

        assertTrue(match.isUser1Viewed());
        verify(matchRepository, times(1)).save(match);
    }

    @Test
    void recordProfileView_ShouldCreateProfileView() {
        when(userRepository.findById(user1.getId())).thenReturn(Optional.of(user1));
        when(userRepository.findById(user2.getId())).thenReturn(Optional.of(user2));
        when(profileViewRepository.save(any(ProfileView.class))).thenReturn(profileView);
        when(userProfileRepository.findByUserId(any(UUID.class))).thenReturn(Optional.empty());

        ProfileViewDto result = matchingService.recordProfileView(user1.getId(), user2.getId());

        assertNotNull(result);
        assertEquals(user1.getId(), result.getViewerId());
        assertEquals(user2.getId(), result.getViewedId());
    }

    @Test
    void getProfileViewsByUser_AsViewer_ShouldReturnViewerViews() {
        when(userRepository.findById(user1.getId())).thenReturn(Optional.of(user1));
        when(profileViewRepository.findByViewer(user1)).thenReturn(List.of(profileView));
        when(userProfileRepository.findByUserId(any(UUID.class))).thenReturn(Optional.empty());

        List<ProfileViewDto> result = matchingService.getProfileViewsByUser(user1.getId(), true);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(user1.getId(), result.get(0).getViewerId());
    }

    @Test
    void calculateCompatibilityScore_ShouldReturnCorrectScore() {
        when(userProfileRepository.findByUserId(user1.getId())).thenReturn(Optional.of(profile1));
        when(userProfileRepository.findByUserId(user2.getId())).thenReturn(Optional.of(profile2));

        double result = matchingService.calculateCompatibilityScore(user1.getId(), user2.getId());

        assertEquals(50.0, result);
    }

    @Test
    void getDailySuggestionsForUser_WithExistingMatches_ShouldReturnSuggestions() {
        
        
        
    }

    @Test
    void createDailySuggestionMatch_ShouldReturnMatchDto() {
        when(userRepository.findById(user1.getId())).thenReturn(Optional.of(user1));
        when(userRepository.findById(user2.getId())).thenReturn(Optional.of(user2));
        when(matchRepository.findByUsers(user1, user2)).thenReturn(Optional.empty());
        when(matchRepository.save(any(Match.class))).thenReturn(match);
        when(userProfileRepository.findByUserId(any())).thenReturn(Optional.empty());

        MatchDto result = matchingService.createDailySuggestionMatch(user1.getId(), user2.getId(), 85.0);

        assertNotNull(result);
        assertEquals(match.getId(), result.getId());
    }

    @Test
    void createDailySuggestionMatch_ShouldCreateMatch() {
        when(userRepository.findById(user1.getId())).thenReturn(Optional.of(user1));
        when(userRepository.findById(user2.getId())).thenReturn(Optional.of(user2));
        when(matchRepository.findByUsers(user1, user2)).thenReturn(Optional.empty());
        when(matchRepository.save(any(Match.class))).thenReturn(match);
        when(userProfileRepository.findByUserId(any(UUID.class))).thenReturn(Optional.empty());

        MatchDto result = matchingService.createDailySuggestionMatch(user1.getId(), user2.getId(), 75.0);

        assertNotNull(result);
        assertEquals(match.getId(), result.getId());
        verify(matchRepository, times(1)).save(any(Match.class));
    }
}