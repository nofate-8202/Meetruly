package com.meetruly.admin.repository;

import com.meetruly.admin.model.AdminAction;
import com.meetruly.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AdminActionRepository extends JpaRepository<AdminAction, UUID> {

    List<AdminAction> findByAdmin(User admin);

    Page<AdminAction> findByAdmin(User admin, Pageable pageable);

    List<AdminAction> findByTargetUser(User targetUser);

    Page<AdminAction> findByTargetUser(User targetUser, Pageable pageable);

    @Query("SELECT a FROM AdminAction a WHERE a.performedAt BETWEEN :startDate AND :endDate")
    List<AdminAction> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT a FROM AdminAction a WHERE a.actionType = :actionType")
    List<AdminAction> findByActionType(@Param("actionType") AdminAction.ActionType actionType);

    @Query("SELECT COUNT(a) FROM AdminAction a WHERE a.performedAt >= :since")
    long countActionsSince(@Param("since") LocalDateTime since);

    @Query("SELECT a.actionType, COUNT(a) FROM AdminAction a WHERE a.performedAt BETWEEN :startDate AND :endDate GROUP BY a.actionType")
    List<Object[]> countActionsByTypeInDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}