package com.meetruly.matching.repository;

import com.meetruly.core.constant.Gender;
import com.meetruly.core.constant.UserRole;
import com.meetruly.matching.model.Match;
import com.meetruly.user.model.User;
import com.meetruly.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class MatchRepositoryTest {

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private UserRepository userRepository;

    private User user1;
    private User user2;
    private User user3;
    private Match match1;
    private Match match2;

    @BeforeEach
    void setUp() {
        matchRepository.deleteAll();
        userRepository.deleteAll();

        
        user1 = new User();
        user1.setUsername("user1");
        user1.setEmail("user1@example.com");
        user1.setPassword("password");
        user1.setGender(Gender.MALE);
        user1.setEnabled(true);
        user1.setApproved(true);
        user1.setProfileCompleted(true);
        user1.setRole(com.meetruly.core.constant.UserRole.USER);
        user1.setAccountNonLocked(true);
        user1.setEmailVerified(true);
        user1.setCreatedAt(LocalDateTime.now());
        user1.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user1);

        user2 = new User();
        user2.setUsername("user2");
        user2.setEmail("user2@example.com");
        user2.setPassword("password");
        user2.setGender(Gender.FEMALE);
        user2.setEnabled(true);
        user2.setApproved(true);
        user2.setProfileCompleted(true);
        user2.setRole(com.meetruly.core.constant.UserRole.USER);
        user2.setAccountNonLocked(true);
        user2.setEmailVerified(true);
        user2.setCreatedAt(LocalDateTime.now());
        user2.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user2);

        user3 = new User();
        user3.setUsername("user3");
        user3.setEmail("user3@example.com");
        user3.setPassword("password");
        user3.setGender(Gender.OTHER);
        user3.setEnabled(true);
        user3.setApproved(true);
        user3.setProfileCompleted(true);
        user3.setRole(com.meetruly.core.constant.UserRole.USER);
        user3.setAccountNonLocked(true);
        user3.setEmailVerified(true);
        user3.setCreatedAt(LocalDateTime.now());
        user3.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user3);

        
        match1 = Match.builder()
                .user1(user1)
                .user2(user2)
                .compatibilityScore(75.0)
                .matchType(Match.MatchType.MUTUAL_LIKE)
                .user1Viewed(true)
                .user2Viewed(false)
                .createdAt(LocalDateTime.now())
                .build();
        matchRepository.save(match1);

        match2 = Match.builder()
                .user1(user1)
                .user2(user3)
                .compatibilityScore(85.0)
                .matchType(Match.MatchType.DAILY_SUGGESTION)
                .user1Viewed(false)
                .user2Viewed(false)
                .createdAt(LocalDateTime.now())
                .build();
        matchRepository.save(match2);
    }

    @Test
    void findByUser_ShouldReturnAllMatchesByUser() {
        List<Match> matches = matchRepository.findByUser(user1);

        assertEquals(2, matches.size());
    }

    @Test
    void findByUserWithPagination_ShouldReturnPaginatedResults() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Match> matchesPage = matchRepository.findByUser(user1, pageable);

        assertEquals(2, matchesPage.getTotalElements());
    }

    @Test
    void findByUsers_ShouldReturnMatchBetweenUsers() {
        Optional<Match> match = matchRepository.findByUsers(user1, user2);

        assertTrue(match.isPresent());
        assertEquals(user1.getId(), match.get().getUser1().getId());
        assertEquals(user2.getId(), match.get().getUser2().getId());
    }

    @Test
    void findByUsers_ShouldReturnMatchRegardlessOfOrder() {
        Optional<Match> match = matchRepository.findByUsers(user2, user1);

        assertTrue(match.isPresent());
        assertEquals(match1.getId(), match.get().getId());
    }

    @Test
    void findMutualLikeMatchesByUser_ShouldReturnOnlyMutualLikeMatches() {
        List<Match> matches = matchRepository.findMutualLikeMatchesByUser(user1);

        assertEquals(1, matches.size());
        assertEquals(Match.MatchType.MUTUAL_LIKE, matches.get(0).getMatchType());
    }

    @Test
    void findDailySuggestionMatchesByUser_ShouldReturnOnlyDailySuggestionMatches() {
        List<Match> matches = matchRepository.findDailySuggestionMatchesByUser(user1);

        assertEquals(1, matches.size());
        assertEquals(Match.MatchType.DAILY_SUGGESTION, matches.get(0).getMatchType());
    }

    @Test
    void findUnviewedMatchesByUser_ShouldReturnUnviewedMatches() {
        
        List<Match> allMatches = matchRepository.findAll();
        System.out.println("All matches: " + allMatches.size());
        for (Match match : allMatches) {
            System.out.println("Match between " + match.getUser1().getUsername() +
                    " and " + match.getUser2().getUsername() +
                    ", user1Viewed: " + match.isUser1Viewed() +
                    ", user2Viewed: " + match.isUser2Viewed());
        }

        List<Match> unviewedMatchesUser1 = matchRepository.findUnviewedMatchesByUser(user1);
        List<Match> unviewedMatchesUser2 = matchRepository.findUnviewedMatchesByUser(user2);

        
        assertFalse(unviewedMatchesUser1.isEmpty(), "User1 should have at least one unviewed match");
        assertFalse(unviewedMatchesUser2.isEmpty(), "User2 should have at least one unviewed match");

        
        boolean match2InUser1Unviewed = unviewedMatchesUser1.stream()
                .anyMatch(m -> m.getId().equals(match2.getId()));
        assertTrue(match2InUser1Unviewed, "Match2 should be in user1's unviewed matches");

        
        boolean match1InUser2Unviewed = unviewedMatchesUser2.stream()
                .anyMatch(m -> m.getId().equals(match1.getId()));
        assertTrue(match1InUser2Unviewed, "Match1 should be in user2's unviewed matches");
    }

    @Test
    void countMatchesByUser_ShouldReturnCorrectCount() {
        long countUser1 = matchRepository.countMatchesByUser(user1);
        long countUser2 = matchRepository.countMatchesByUser(user2);

        assertEquals(2, countUser1);
        assertEquals(1, countUser2);
    }

    @Test
    void findDailySuggestionMatchesCreatedSince_ShouldReturnMatchesAfterDate() {
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        List<Match> recentMatches = matchRepository.findDailySuggestionMatchesCreatedSince(oneDayAgo);

        assertEquals(1, recentMatches.size());
        assertEquals(Match.MatchType.DAILY_SUGGESTION, recentMatches.get(0).getMatchType());
    }
}