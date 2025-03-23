package com.meetruly.admin.service;

import com.meetruly.admin.dto.*;
import com.meetruly.admin.model.AdminAction;
import com.meetruly.admin.model.UserReport;
import com.meetruly.core.constant.ReportStatus;
import com.meetruly.core.constant.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AdminService {

    AdminActionDto logAdminAction(UUID adminId, UUID targetUserId, AdminAction.ActionType actionType, String actionDetails, String ipAddress);

    Page<AdminActionDto> getAdminActionsByAdmin(UUID adminId, Pageable pageable);

    Page<AdminActionDto> getAdminActionsByTargetUser(UUID targetUserId, Pageable pageable);

    List<AdminActionDto> getAdminActionsByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    List<UserDto> getPendingApprovalUsers();

    Page<UserDto> getPendingApprovalUsers(Pageable pageable);

    void approveUser(UUID adminId, UUID userId);

    void rejectUser(UUID adminId, UUID userId, String reason);

    void changeUserRole(UUID adminId, UUID userId, UserRole newRole);

    UserReportDto createUserReport(UUID reporterId, UUID reportedUserId, UserReport.ReportType reportType, String reportReason);

    Page<UserReportDto> getReportsByStatus(ReportStatus status, Pageable pageable);

    List<UserReportDto> getPendingReports();

    UserReportDto getReportById(UUID reportId);

    UserReportDto handleReport(UUID adminId, ReportActionRequest actionRequest);

    UserBlockDto blockUser(UUID adminId, UserBlockRequest blockRequest);

    void unblockUser(UUID adminId, UUID blockId, String reason);

    List<UserBlockDto> getActiveBlocks();

    Page<UserBlockDto> getActiveBlocks(Pageable pageable);

    UserBlockDto getBlockById(UUID blockId);

    boolean isUserBlocked(UUID userId);

    AdminDashboardDto getDashboardStats();

    RevenueStatsDto getRevenueStats(LocalDateTime startDate, LocalDateTime endDate);

    void processExpiredBlocks();
}