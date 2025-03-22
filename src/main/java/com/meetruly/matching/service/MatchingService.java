package com.meetruly.matching.service;

import com.meetruly.matching.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MatchingService {

    LikeDto likeUser(UUID likerId, UUID likedId);

    void unlikeUser(UUID likerId, UUID likedId);

    boolean hasLiked(UUID likerId, UUID likedId);

    List<LikeDto> getLikesByUser(UUID userId, boolean asLiker);

    List<LikeDto> getUnviewedLikesByUser(UUID userId);

    void markLikeAsViewed(UUID likeId);

    long countLikesByUser(UUID userId);

    Optional<MatchDto> getMatchByUsers(UUID user1Id, UUID user2Id);

    List<MatchDto> getMatchesByUser(UUID userId);

    Page<MatchDto> getMatchesByUser(UUID userId, Pageable pageable);

    List<MatchDto> getMutualLikeMatchesByUser(UUID userId);

    List<MatchDto> getDailySuggestionMatchesByUser(UUID userId);

    List<MatchDto> getUnviewedMatchesByUser(UUID userId);

    void markMatchAsViewedByUser(UUID matchId, UUID userId);

    long countMatchesByUser(UUID userId);

    ProfileViewDto recordProfileView(UUID viewerId, UUID viewedId);

    List<ProfileViewDto> getProfileViewsByUser(UUID userId, boolean asViewer);

    Page<ProfileViewDto> getProfileViewsByViewed(UUID viewedId, Pageable pageable);

    long countProfileViewsByUser(UUID userId);

    List<ProfileCardDto> getDailySuggestionsForUser(UUID userId);

    void generateDailySuggestionsForAllUsers();

    MatchDto createDailySuggestionMatch(UUID user1Id, UUID user2Id, double compatibilityScore);


    List<ProfileCardDto> getTopLikedUsers(int limit);

    MatchingSummaryDto getMatchingSummary(UUID userId);

    double calculateCompatibilityScore(UUID user1Id, UUID user2Id);
}