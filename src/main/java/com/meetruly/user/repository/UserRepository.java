package com.meetruly.user.repository;

import com.meetruly.core.constant.Gender;
import com.meetruly.core.constant.UserRole;
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
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.role = :role")
    boolean existsByRole(@Param("role") UserRole role);

    List<User> findByEnabled(boolean enabled);

    List<User> findByApproved(boolean approved);

    Page<User> findByGender(Gender gender, Pageable pageable);

    Page<User> findByApprovedAndEnabled(boolean approved, boolean enabled, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.lastLogin >= :since")
    List<User> findActiveUsersSince(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role")
    long countByRole(@Param("role") UserRole role);

    @Query("SELECT u FROM User u WHERE u.enabled = :enabled AND u.approved = :approved AND u.profileCompleted = :profileCompleted")
    List<User> findByEnabledAndApprovedAndProfileCompleted(
            @Param("enabled") boolean enabled,
            @Param("approved") boolean approved,
            @Param("profileCompleted") boolean profileCompleted);
    @Query("SELECT COUNT(u) FROM User u WHERE u.approved = :approved AND u.profileCompleted = :profileCompleted")
    long countByApprovedAndProfileCompleted(
            @Param("approved") boolean approved,
            @Param("profileCompleted") boolean profileCompleted);

    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :date")
    long countByCreatedAtAfter(@Param("date") LocalDateTime date);

    @Query("SELECT COUNT(u) FROM User u WHERE u.enabled = :enabled AND u.approved = :approved")
    long countByEnabledAndApproved(
            @Param("enabled") boolean enabled,
            @Param("approved") boolean approved);
    List<User> findByApprovedAndProfileCompleted(boolean approved, boolean profileCompleted);

    Page<User> findByApprovedAndProfileCompleted(boolean approved, boolean profileCompleted, Pageable pageable);



}