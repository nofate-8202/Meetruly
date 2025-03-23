package com.meetruly.admin.repository;

import com.meetruly.admin.model.UserBlock;
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
public interface UserBlockRepository extends JpaRepository<UserBlock, UUID> {

    List<UserBlock> findByBlockedUser(User blockedUser);

    @Query("SELECT b FROM UserBlock b WHERE b.blockedUser = :user AND b.active = true")
    Optional<UserBlock> findActiveBlockByUser(@Param("user") User user);

    @Query("SELECT b FROM UserBlock b WHERE b.active = true")
    List<UserBlock> findActiveBlocks();

    @Query("SELECT b FROM UserBlock b WHERE b.active = true")
    Page<UserBlock> findActiveBlocks(Pageable pageable);

    @Query("SELECT b FROM UserBlock b WHERE b.active = true AND b.permanent = false AND b.endDate <= :now")
    List<UserBlock> findExpiredBlocks(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(b) FROM UserBlock b WHERE b.active = true")
    long countActiveBlocks();

    @Query("SELECT COUNT(b) FROM UserBlock b WHERE b.active = true AND b.permanent = true")
    long countPermanentBlocks();

    @Query("SELECT COUNT(b) FROM UserBlock b WHERE b.startDate >= :since")
    long countBlocksSince(@Param("since") LocalDateTime since);
}