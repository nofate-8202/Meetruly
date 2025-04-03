package com.meetruly.web.controller;

import com.meetruly.admin.dto.*;
import com.meetruly.admin.model.AdminAction;
import com.meetruly.admin.model.UserReport;
import com.meetruly.admin.service.AdminService;
import com.meetruly.core.constant.Gender;
import com.meetruly.core.constant.ReportStatus;
import com.meetruly.core.constant.SubscriptionPlan;
import com.meetruly.core.constant.UserRole;
import com.meetruly.core.exception.MeetrulyException;
import com.meetruly.subscription.dto.SubscriptionDto;
import com.meetruly.subscription.model.Subscription;
import com.meetruly.subscription.service.SubscriptionService;
import com.meetruly.user.model.User;
import com.meetruly.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private AdminService adminService;

    @Mock
    private UserService userService;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private Principal principal;

    @Mock
    private Model model;

    @Mock
    private RedirectAttributes redirectAttributes;

    @Mock
    private BindingResult bindingResult;

    @InjectMocks
    private AdminController adminController;

    private User adminUser;
    private UUID adminId;
    private UUID userId;
    private UUID reportId;
    private UUID blockId;

    @BeforeEach
    void setUp() {
        adminId = UUID.randomUUID();
        userId = UUID.randomUUID();
        reportId = UUID.randomUUID();
        blockId = UUID.randomUUID();

        adminUser = new User();
        adminUser.setId(adminId);
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@meetruly.com");
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setEnabled(true);
        adminUser.setEmailVerified(true);
        adminUser.setProfileCompleted(true);
        adminUser.setApproved(true);
    }

    @Test
    void showDashboard_ShouldReturnDashboardView() {
        
        AdminDashboardDto dashboardStats = new AdminDashboardDto();
        dashboardStats.setTotalUsers(100);
        dashboardStats.setActiveUsers(80);
        dashboardStats.setPendingApprovalUsers(5);

        when(principal.getName()).thenReturn("admin");
        when(userService.getUserByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(adminService.getDashboardStats()).thenReturn(dashboardStats);

        
        String viewName = adminController.showDashboard(model, principal);

        
        assertEquals("admin/dashboard", viewName);
        verify(model).addAttribute("stats", dashboardStats);
        verify(model).addAttribute("adminId", adminId);
    }

    @Test
    void showDashboard_WhenUserNotFound_ShouldRedirectToHome() {
        
        when(principal.getName()).thenReturn("nonexistent");
        when(userService.getUserByUsername("nonexistent")).thenReturn(Optional.empty());

        
        String viewName = adminController.showDashboard(model, principal);

        
        assertEquals("redirect:/home", viewName);
    }

    @Test
    void showDashboard_WhenUserNotAdmin_ShouldRedirectToHome() {
        
        User user = new User();
        user.setRole(UserRole.USER);

        when(principal.getName()).thenReturn("user");
        when(userService.getUserByUsername("user")).thenReturn(Optional.of(user));

        
        String viewName = adminController.showDashboard(model, principal);

        
        assertEquals("redirect:/home", viewName);
    }

    @Test
    void showPendingUsers_ShouldReturnPendingUsersView() {
        
        UserDto userDto = new UserDto();
        userDto.setId(userId);
        userDto.setUsername("testuser");
        userDto.setEmail("test@example.com");
        userDto.setApproved(false);

        List<UserDto> pendingUsersList = Collections.singletonList(userDto);
        Page<UserDto> pendingUsersPage = new PageImpl<>(pendingUsersList);

        when(principal.getName()).thenReturn("admin");
        when(userService.getUserByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(adminService.getPendingApprovalUsers(any(PageRequest.class))).thenReturn(pendingUsersPage);

        
        String viewName = adminController.showPendingUsers(0, 10, model, principal);

        
        assertEquals("admin/pending-users", viewName);
        verify(model).addAttribute("pendingUsers", pendingUsersList);
        verify(model).addAttribute("currentPage", 0);
        verify(model).addAttribute("totalPages", 1);
        verify(model).addAttribute("adminId", adminId);
    }

    @Test
    void approveUser_ShouldRedirectToPendingUsers() {
        
        when(principal.getName()).thenReturn("admin");
        when(userService.getUserByUsername("admin")).thenReturn(Optional.of(adminUser));
        doNothing().when(adminService).approveUser(adminId, userId);

        
        String viewName = adminController.approveUser(userId, principal, redirectAttributes);

        
        assertEquals("redirect:/admin/users/pending", viewName);
        verify(adminService).approveUser(adminId, userId);
        verify(redirectAttributes).addFlashAttribute("successMessage", "User has been approved successfully.");
    }

    @Test
    void approveUser_WhenExceptionThrown_ShouldAddErrorMessage() {
        
        when(principal.getName()).thenReturn("admin");
        when(userService.getUserByUsername("admin")).thenReturn(Optional.of(adminUser));
        doThrow(new MeetrulyException("Error approving user")).when(adminService).approveUser(adminId, userId);

        
        String viewName = adminController.approveUser(userId, principal, redirectAttributes);

        
        assertEquals("redirect:/admin/users/pending", viewName);
        verify(redirectAttributes).addFlashAttribute("errorMessage", "Error approving user");
    }

    @Test
    void rejectUser_ShouldRedirectToPendingUsers() {
        
        String reason = "Profile does not meet guidelines";

        when(principal.getName()).thenReturn("admin");
        when(userService.getUserByUsername("admin")).thenReturn(Optional.of(adminUser));
        doNothing().when(adminService).rejectUser(adminId, userId, reason);

        
        String viewName = adminController.rejectUser(userId, reason, principal, redirectAttributes);

        
        assertEquals("redirect:/admin/users/pending", viewName);
        verify(adminService).rejectUser(adminId, userId, reason);
        verify(redirectAttributes).addFlashAttribute("successMessage", "User has been rejected successfully.");
    }

    @Test
    void showAllUsers_ShouldReturnAllUsersView() {
        
        
        com.meetruly.user.dto.UserResponseDto userDto = new com.meetruly.user.dto.UserResponseDto();
        userDto.setId(userId);
        userDto.setUsername("testuser");
        userDto.setEmail("test@example.com");
        userDto.setGender(Gender.MALE);
        userDto.setRole(UserRole.USER);
        userDto.setEnabled(true);
        userDto.setEmailVerified(true);
        userDto.setProfileCompleted(true);
        userDto.setApproved(true);
        userDto.setCreatedAt(LocalDateTime.now());

        List<com.meetruly.user.dto.UserResponseDto> usersList = Collections.singletonList(userDto);
        Page<com.meetruly.user.dto.UserResponseDto> usersPage = new PageImpl<>(usersList);

        when(principal.getName()).thenReturn("admin");
        when(userService.getUserByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(userService.getUsers(any(PageRequest.class))).thenReturn(usersPage);
        when(adminService.isUserBlocked(userId)).thenReturn(false);

        
        SubscriptionDto subscriptionDto = new SubscriptionDto();
        subscriptionDto.setPlan(SubscriptionPlan.FREE);
        when(subscriptionService.getCurrentSubscription(userId)).thenReturn(subscriptionDto);

        
        String viewName = adminController.showAllUsers(0, 10, model, principal);

        
        assertEquals("admin/all-users", viewName);
        verify(model).addAttribute(eq("users"), any());
        verify(model).addAttribute("currentPage", 0);
        verify(model).addAttribute("totalPages", 1);
        verify(model).addAttribute("adminId", adminId);
    }

    @Test
    void changeUserRole_ShouldRedirectToUsers() {
        
        UserRole newRole = UserRole.ADMIN;

        when(principal.getName()).thenReturn("admin");
        when(userService.getUserByUsername("admin")).thenReturn(Optional.of(adminUser));
        doNothing().when(adminService).changeUserRole(adminId, userId, newRole);

        
        String viewName = adminController.changeUserRole(userId, newRole, principal, redirectAttributes);

        
        assertEquals("redirect:/admin/users", viewName);
        verify(adminService).changeUserRole(adminId, userId, newRole);
        verify(redirectAttributes).addFlashAttribute("successMessage", "User role has been changed successfully.");
    }

    @Test
    void showPendingReports_ShouldReturnPendingReportsView() {
        
        UserReportDto reportDto = new UserReportDto();
        reportDto.setId(reportId);
        reportDto.setReporterUsername("reporter");
        reportDto.setReportedUsername("reported");
        reportDto.setReportType(UserReport.ReportType.INAPPROPRIATE_CONTENT);
        reportDto.setReportReason("Inappropriate photo");
        reportDto.setStatus(ReportStatus.PENDING);

        List<UserReportDto> pendingReportsList = Collections.singletonList(reportDto);
        Page<UserReportDto> pendingReportsPage = new PageImpl<>(pendingReportsList);

        when(principal.getName()).thenReturn("admin");
        when(userService.getUserByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(adminService.getReportsByStatus(eq(ReportStatus.PENDING), any(PageRequest.class))).thenReturn(pendingReportsPage);

        
        String viewName = adminController.showPendingReports(0, 10, model, principal);

        
        assertEquals("admin/pending-reports", viewName);
        verify(model).addAttribute("reports", pendingReportsList);
        verify(model).addAttribute("currentPage", 0);
        verify(model).addAttribute("totalPages", 1);
        verify(model).addAttribute("adminId", adminId);
        verify(model).addAttribute("reportTypes", UserReport.ReportType.values());
    }

    @Test
    void showReportDetails_ShouldReturnReportDetailsView() {
        
        UserReportDto report = new UserReportDto();
        report.setId(reportId);
        report.setReporterUsername("reporter");
        report.setReportedUsername("reported");
        report.setReportType(UserReport.ReportType.INAPPROPRIATE_CONTENT);
        report.setReportReason("Inappropriate photo");
        report.setStatus(ReportStatus.PENDING);

        when(principal.getName()).thenReturn("admin");
        when(userService.getUserByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(adminService.getReportById(reportId)).thenReturn(report);

        
        String viewName = adminController.showReportDetails(reportId, model, principal);

        
        assertEquals("admin/report-details", viewName);
        verify(model).addAttribute("report", report);
        verify(model).addAttribute("adminId", adminId);
        verify(model).addAttribute("reportStatuses", ReportStatus.values());
    }

    @Test
    void handleReport_WithValidRequest_ShouldRedirectToPendingReports() {
        
        ReportActionRequest actionRequest = new ReportActionRequest();
        actionRequest.setStatus(ReportStatus.RESOLVED);
        actionRequest.setAdminNotes("Issue resolved");
        actionRequest.setBlockUser(false);

        when(principal.getName()).thenReturn("admin");
        when(userService.getUserByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(bindingResult.hasErrors()).thenReturn(false);

        
        String viewName = adminController.handleReport(reportId, actionRequest, bindingResult, principal, redirectAttributes);

        
        assertEquals("redirect:/admin/reports/pending", viewName);
        verify(adminService).handleReport(eq(adminId), any(ReportActionRequest.class));
        verify(redirectAttributes).addFlashAttribute("successMessage", "Report has been handled successfully.");
    }

    @Test
    void handleReport_WithBindingErrors_ShouldRedirectToReportDetails() {
        
        ReportActionRequest actionRequest = new ReportActionRequest();

        when(bindingResult.hasErrors()).thenReturn(true);

        
        String viewName = adminController.handleReport(reportId, actionRequest, bindingResult, principal, redirectAttributes);

        
        assertEquals("redirect:/admin/reports/" + reportId, viewName);
        verify(redirectAttributes).addFlashAttribute("errorMessage", "Invalid request. Please check the form.");
    }

    @Test
    void blockUser_WithValidRequest_ShouldRedirectToUsers() {
        
        UserBlockRequest blockRequest = new UserBlockRequest();
        blockRequest.setReason("Violated terms of service");
        blockRequest.setPermanent(false);
        blockRequest.setDurationDays(7);

        when(principal.getName()).thenReturn("admin");
        when(userService.getUserByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(bindingResult.hasErrors()).thenReturn(false);

        
        String viewName = adminController.blockUser(userId, blockRequest, bindingResult, principal, redirectAttributes);

        
        assertEquals("redirect:/admin/users", viewName);
        verify(adminService).blockUser(eq(adminId), any(UserBlockRequest.class));
        verify(redirectAttributes).addFlashAttribute("successMessage", "User has been blocked successfully.");
    }

    @Test
    void showBlocks_ShouldReturnBlocksView() {
        
        UserBlockDto blockDto = new UserBlockDto();
        blockDto.setId(blockId);
        blockDto.setBlockedUsername("blockeduser");
        blockDto.setReason("Violated terms of service");
        blockDto.setStartDate(LocalDateTime.now().minusDays(1));
        blockDto.setEndDate(LocalDateTime.now().plusDays(6));
        blockDto.setPermanent(false);
        blockDto.setActive(true);
        blockDto.setRemainingDays(6);

        List<UserBlockDto> blocksList = Collections.singletonList(blockDto);
        Page<UserBlockDto> blocksPage = new PageImpl<>(blocksList);

        when(principal.getName()).thenReturn("admin");
        when(userService.getUserByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(adminService.getActiveBlocks(any(PageRequest.class))).thenReturn(blocksPage);

        
        String viewName = adminController.showBlocks(0, 10, model, principal);

        
        assertEquals("admin/blocks", viewName);
        verify(model).addAttribute("blocks", blocksList);
        verify(model).addAttribute("currentPage", 0);
        verify(model).addAttribute("totalPages", 1);
        verify(model).addAttribute("adminId", adminId);
    }

    @Test
    void unblockUser_ShouldRedirectToBlocks() {
        
        String reason = "User appeal accepted";

        when(principal.getName()).thenReturn("admin");
        when(userService.getUserByUsername("admin")).thenReturn(Optional.of(adminUser));
        doNothing().when(adminService).unblockUser(adminId, blockId, reason);

        
        String viewName = adminController.unblockUser(blockId, reason, principal, redirectAttributes);

        
        assertEquals("redirect:/admin/blocks", viewName);
        verify(adminService).unblockUser(adminId, blockId, reason);
        verify(redirectAttributes).addFlashAttribute("successMessage", "User has been unblocked successfully.");
    }

    @Test
    void showRevenueStats_ShouldReturnRevenueView() {
        
        LocalDate startDate = LocalDate.now().withDayOfMonth(1);
        LocalDate endDate = LocalDate.now();
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        RevenueStatsDto revenueStats = new RevenueStatsDto();
        revenueStats.setStartDate(start);
        revenueStats.setEndDate(end);
        revenueStats.setTotalRevenue(1500.0);

        when(principal.getName()).thenReturn("admin");
        when(userService.getUserByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(adminService.getRevenueStats(start, end)).thenReturn(revenueStats);

        
        String viewName = adminController.showRevenueStats(startDate, endDate, model, principal);

        
        assertEquals("admin/revenue", viewName);
        verify(model).addAttribute("stats", revenueStats);
        verify(model).addAttribute("startDate", startDate);
        verify(model).addAttribute("endDate", endDate);
        verify(model).addAttribute("adminId", adminId);
    }

    @Test
    void showAuditLog_ShouldReturnAuditLogView() {
        
        AdminActionDto actionDto = new AdminActionDto();
        actionDto.setId(UUID.randomUUID());
        actionDto.setAdminId(adminId);
        actionDto.setAdminUsername("admin");
        actionDto.setTargetUserId(userId);
        actionDto.setTargetUsername("user");
        actionDto.setActionType(AdminAction.ActionType.USER_APPROVAL);
        actionDto.setActionDetails("Approved user: user");
        actionDto.setPerformedAt(LocalDateTime.now());

        List<AdminActionDto> actionsList = Collections.singletonList(actionDto);
        Page<AdminActionDto> actionsPage = new PageImpl<>(actionsList);

        when(principal.getName()).thenReturn("admin");
        when(userService.getUserByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(adminService.getAdminActionsByAdmin(eq(adminId), any(PageRequest.class))).thenReturn(actionsPage);

        
        String viewName = adminController.showAuditLog(0, 20, model, principal);

        
        assertEquals("admin/audit-log", viewName);
        verify(model).addAttribute("actions", actionsList);
        verify(model).addAttribute("currentPage", 0);
        verify(model).addAttribute("totalPages", 1);
        verify(model).addAttribute("adminId", adminId);
        verify(model).addAttribute("actionTypes", AdminAction.ActionType.values());
    }

    @Test
    void createReport_WithValidRequest_ShouldReturnSuccess() {
        
        UserReportRequest reportRequest = new UserReportRequest();
        reportRequest.setReportedUserId(userId);
        reportRequest.setReportType(UserReport.ReportType.FAKE_PROFILE);
        reportRequest.setReportReason("This profile uses fake photos");

        User reporter = new User();
        UUID reporterId = UUID.randomUUID();
        reporter.setId(reporterId);
        reporter.setUsername("reporter");

        UserReportDto createdReport = new UserReportDto();
        createdReport.setId(reportId);
        createdReport.setReporterId(reporterId);
        createdReport.setReporterUsername("reporter");
        createdReport.setReportedUserId(userId);
        createdReport.setReportedUsername("reported");
        createdReport.setReportType(UserReport.ReportType.FAKE_PROFILE);
        createdReport.setReportReason("This profile uses fake photos");
        createdReport.setStatus(ReportStatus.PENDING);

        when(principal.getName()).thenReturn("reporter");
        when(userService.getUserByUsername("reporter")).thenReturn(Optional.of(reporter));
        when(adminService.createUserReport(
                reporter.getId(),
                reportRequest.getReportedUserId(),
                reportRequest.getReportType(),
                reportRequest.getReportReason()
        )).thenReturn(createdReport);

        
        ResponseEntity<?> response = adminController.createReport(reportRequest, principal);

        
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(createdReport, response.getBody());
    }

    @Test
    void processExpiredBlocks_ShouldRedirectToBlocks() {
        
        doNothing().when(adminService).processExpiredBlocks();

        
        String viewName = adminController.processExpiredBlocks(principal, redirectAttributes);

        
        assertEquals("redirect:/admin/blocks", viewName);
        verify(adminService).processExpiredBlocks();
        verify(redirectAttributes).addFlashAttribute("successMessage", "Expired blocks have been processed successfully.");
    }

    @Test
    void processExpiredBlocks_WhenExceptionThrown_ShouldAddErrorMessage() {
        
        doThrow(new RuntimeException("Database error")).when(adminService).processExpiredBlocks();

        
        String viewName = adminController.processExpiredBlocks(principal, redirectAttributes);

        
        assertEquals("redirect:/admin/blocks", viewName);
        verify(redirectAttributes).addFlashAttribute(eq("errorMessage"), any(String.class));
    }
}