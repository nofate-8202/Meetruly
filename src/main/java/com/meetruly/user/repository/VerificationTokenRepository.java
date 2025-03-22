package com.meetruly.user.repository;

import com.meetruly.user.model.User;
import com.meetruly.user.model.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {

    Optional<VerificationToken> findByToken(String token);

    List<VerificationToken> findByUser(User user);

    @Query("SELECT v FROM VerificationToken v WHERE v.user = :user AND v.tokenType = :tokenType")
    List<VerificationToken> findByUserAndTokenType(@Param("user") User user, @Param("tokenType") VerificationToken.TokenType tokenType);

    @Query("SELECT v FROM VerificationToken v WHERE v.expiryDate < :now")
    List<VerificationToken> findAllExpiredTokens(@Param("now") LocalDateTime now);

    void deleteByUser(User user);

    void deleteByExpiryDateBefore(LocalDateTime date);
}