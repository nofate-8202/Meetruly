package com.meetruly.user.repository;

import com.meetruly.user.model.User;
import com.meetruly.user.model.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    Optional<UserSession> findBySessionId(String sessionId);

    List<UserSession> findByUser(User user);

    List<UserSession> findByUserAndActive(User user, boolean active);

    @Query("SELECT s FROM UserSession s WHERE s.expiryDate < :now AND s.active = true")
    List<UserSession> findAllExpiredActiveSessions(@Param("now") LocalDateTime now);

    @Query("SELECT s FROM UserSession s WHERE s.lastActive < :time AND s.active = true")
    List<UserSession> findInactiveSessions(@Param("time") LocalDateTime time);

    @Query("SELECT COUNT(s) FROM UserSession s WHERE s.createdAt >= :since")
    long countSessionsCreatedSince(@Param("since") LocalDateTime since);

    void deleteByUser(User user);
}