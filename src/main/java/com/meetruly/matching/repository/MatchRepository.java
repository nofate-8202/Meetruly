package com.meetruly.matching.repository;

import com.meetruly.matching.model.Match;
import com.meetruly.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MatchRepository extends JpaRepository<Match, UUID> {

    @Query("SELECT m FROM Match m WHERE (m.user1 = :user OR m.user2 = :user)")
    List<Match> findByUser(@Param("user") User user);

    @Query("SELECT m FROM Match m WHERE (m.user1 = :user OR m.user2 = :user)")
    Page<Match> findByUser(@Param("user") User user, Pageable pageable);

    @Query("SELECT m FROM Match m WHERE (m.user1 = :user1 AND m.user2 = :user2) OR (m.user1 = :user2 AND m.user2 = :user1)")
    Optional<Match> findByUsers(@Param("user1") User user1, @Param("user2") User user2);

    @Query("SELECT m FROM Match m WHERE (m.user1 = :user OR m.user2 = :user) AND m.matchType = 'MUTUAL_LIKE'")
    List<Match> findMutualLikeMatchesByUser(@Param("user") User user);

    @Query("SELECT m FROM Match m WHERE (m.user1 = :user OR m.user2 = :user) AND m.matchType = 'DAILY_SUGGESTION'")
    List<Match> findDailySuggestionMatchesByUser(@Param("user") User user);

    @Query("SELECT m FROM Match m WHERE ((m.user1 = :user AND m.user1Viewed = false) OR (m.user2 = :user AND m.user2Viewed = false))")
    List<Match> findUnviewedMatchesByUser(@Param("user") User user);

    @Query("SELECT COUNT(m) FROM Match m WHERE (m.user1 = :user OR m.user2 = :user)")
    long countMatchesByUser(@Param("user") User user);

    @Query("SELECT COUNT(m) FROM Match m WHERE m.createdAt >= :since")
    long countMatchesSince(@Param("since") LocalDateTime since);

    @Query("SELECT m FROM Match m WHERE m.matchType = 'DAILY_SUGGESTION' AND m.createdAt >= :date")
    List<Match> findDailySuggestionMatchesCreatedSince(@Param("date") LocalDateTime date);

    @Query("SELECT u FROM User u WHERE u.id NOT IN (SELECT m.user2.id FROM Match m WHERE m.user1.id = :userId AND m.matchType = 'DAILY_SUGGESTION') " +
            "AND u.id NOT IN (SELECT m.user1.id FROM Match m WHERE m.user2.id = :userId AND m.matchType = 'DAILY_SUGGESTION') " +
            "AND u.id != :userId AND u.enabled = true AND u.approved = true AND u.profileCompleted = true")
    List<User> findUsersNotMatchedWithUser(@Param("userId") UUID userId);

}