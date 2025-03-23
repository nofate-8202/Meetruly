package com.meetruly.web.controller;

import com.meetruly.admin.dto.*;
import com.meetruly.admin.model.AdminAction;
import com.meetruly.admin.model.UserReport;
import com.meetruly.admin.service.AdminService;
import com.meetruly.core.constant.ReportStatus;
import com.meetruly.core.constant.UserRole;
import com.meetruly.core.exception.MeetrulyException;
import com.meetruly.subscription.service.SubscriptionService;
import com.meetruly.user.model.User;
import com.meetruly.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final AdminService adminService;
    private final UserService userService;
    private final SubscriptionService subscriptionService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public String showDashboard(Model model, Principal principal) {
        try {

            String username = principal.getName();
            User admin = userService.getUserByUsername(username)
                    .orElseThrow(() -> new MeetrulyException("Admin not found: " + username));

            if (admin.getRole() != UserRole.ADMIN) {
                return "redirect:/home";
            }

            AdminDashboardDto dashboardStats = adminService.getDashboardStats();

            model.addAttribute("stats", dashboardStats);
            model.addAttribute("adminId", admin.getId());

            return "admin/dashboard";
        } catch (MeetrulyException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "redirect:/home";
        }
    }

    @GetMapping("/users/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public String showPendingUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model, Principal principal) {

        try {

            String username = principal.getName();
            User admin = userService.getUserByUsername(username)
                    .orElseThrow(() -> new MeetrulyException("Admin not found: " + username));

            PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

            Page<UserDto> pendingUsers = adminService.getPendingApprovalUsers(pageRequest);

            model.addAttribute("pendingUsers", pendingUsers.getContent());
            model.addAttribute("currentPage", pendingUsers.getNumber());
            model.addAttribute("totalPages", pendingUsers.getTotalPages());
            model.addAttribute("adminId", admin.getId());

            return "admin/pending-users";
        } catch (MeetrulyException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/dashboard";
        }
    }

    @PostMapping("/users/{userId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public String approveUser(@PathVariable("userId") UUID userId, Principal principal, RedirectAttributes redirectAttributes) {
        try {

            String username = principal.getName();
            User admin = userService.getUserByUsername(username)
                    .orElseThrow(() -> new MeetrulyException("Admin not found: " + username));

            adminService.approveUser(admin.getId(), userId);

            redirectAttributes.addFlashAttribute("successMessage", "User has been approved successfully.");
            return "redirect:/admin/users/pending";
        } catch (MeetrulyException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/users/pending";
        }
    }

    @PostMapping("/users/{userId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public String rejectUser(
            @PathVariable("userId") UUID userId,
            @RequestParam("reason") String reason,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        try {

            String username = principal.getName();
            User admin = userService.getUserByUsername(username)
                    .orElseThrow(() -> new MeetrulyException("Admin not found: " + username));

            adminService.rejectUser(admin.getId(), userId, reason);

            redirectAttributes.addFlashAttribute("successMessage", "User has been rejected successfully.");
            return "redirect:/admin/users/pending";
        } catch (MeetrulyException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/users/pending";
        }
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public String showAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model, Principal principal) {

        try {

            String username = principal.getName();
            User admin = userService.getUserByUsername(username)
                    .orElseThrow(() -> new MeetrulyException("Admin not found: " + username));

            PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

            Page<UserDto> users = userService.getUsers(pageRequest).map(user -> {

                UserDto userDto = new UserDto();
                userDto.setId(user.getId());
                userDto.setUsername(user.getUsername());
                userDto.setEmail(user.getEmail());
                userDto.setGender(user.getGender());
                userDto.setRole(user.getRole());
                userDto.setEnabled(user.isEnabled());
                userDto.setEmailVerified(user.isEmailVerified());
                userDto.setProfileCompleted(user.isProfileCompleted());
                userDto.setApproved(user.isApproved());
                userDto.setCreatedAt(user.getCreatedAt());
                userDto.setLastLogin(user.getLastLogin());

                userDto.setBlocked(adminService.isUserBlocked(user.getId()));

                userDto.setSubscriptionPlan(subscriptionService.getCurrentSubscription(user.getId()).getPlan().name());

                return userDto;
            });

            model.addAttribute("users", users.getContent());
            model.addAttribute("currentPage", users.getNumber());
            model.addAttribute("totalPages", users.getTotalPages());
            model.addAttribute("adminId", admin.getId());

            return "admin/all-users";
        } catch (MeetrulyException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/dashboard";
        }
    }

    @PostMapping("/users/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public String changeUserRole(
            @PathVariable("userId") UUID userId,
            @RequestParam("role") UserRole role,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        try {

            String username = principal.getName();
            User admin = userService.getUserByUsername(username)
                    .orElseThrow(() -> new MeetrulyException("Admin not found: " + username));

            adminService.changeUserRole(admin.getId(), userId, role);

            redirectAttributes.addFlashAttribute("successMessage", "User role has been changed successfully.");
            return "redirect:/admin/users";
        } catch (MeetrulyException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/users";
        }
    }

    @GetMapping("/reports/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public String showPendingReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model, Principal principal) {

        try {

            String username = principal.getName();
            User admin = userService.getUserByUsername(username)
                    .orElseThrow(() -> new MeetrulyException("Admin not found: " + username));

            PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

            Page<UserReportDto> pendingReports = adminService.getReportsByStatus(ReportStatus.PENDING, pageRequest);

            model.addAttribute("reports", pendingReports.getContent());
            model.addAttribute("currentPage", pendingReports.getNumber());
            model.addAttribute("totalPages", pendingReports.getTotalPages());
            model.addAttribute("adminId", admin.getId());
            model.addAttribute("reportTypes", UserReport.ReportType.values());

            return "admin/pending-reports";
        } catch (MeetrulyException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/dashboard";
        }
    }

    @GetMapping("/reports/{reportId}")
    @PreAuthorize("hasRole('ADMIN')")
    public String showReportDetails(@PathVariable("reportId") UUID reportId, Model model, Principal principal) {
        try {

            String username = principal.getName();
            User admin = userService.getUserByUsername(username)
                    .orElseThrow(() -> new MeetrulyException("Admin not found: " + username));

            UserReportDto report = adminService.getReportById(reportId);

            model.addAttribute("report", report);
            model.addAttribute("adminId", admin.getId());
            model.addAttribute("reportStatuses", ReportStatus.values());

            return "admin/report-details";
        } catch (MeetrulyException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/reports/pending";
        }
    }

    @PostMapping("/reports/{reportId}/handle")
    @PreAuthorize("hasRole('ADMIN')")
    public String handleReport(
            @PathVariable("reportId") UUID reportId,
            @Valid @ModelAttribute("reportAction") ReportActionRequest actionRequest,
            BindingResult result,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid request. Please check the form.");
            return "redirect:/admin/reports/" + reportId;
        }

        try {

            String username = principal.getName();
            User admin = userService.getUserByUsername(username)
                    .orElseThrow(() -> new MeetrulyException("Admin not found: " + username));

            actionRequest.setReportId(reportId);

            adminService.handleReport(admin.getId(), actionRequest);

            redirectAttributes.addFlashAttribute("successMessage", "Report has been handled successfully.");
            return "redirect:/admin/reports/pending";
        } catch (MeetrulyException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/reports/" + reportId;
        }
    }

    @PostMapping("/users/{userId}/block")
    @PreAuthorize("hasRole('ADMIN')")
    public String blockUser(
            @PathVariable("userId") UUID userId,
            @Valid @ModelAttribute("blockRequest") UserBlockRequest blockRequest,
            BindingResult result,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid request. Please check the form.");
            return "redirect:/admin/users";
        }

        try {

            String username = principal.getName();
            User admin = userService.getUserByUsername(username)
                    .orElseThrow(() -> new MeetrulyException("Admin not found: " + username));

            blockRequest.setUserId(userId);

            adminService.blockUser(admin.getId(), blockRequest);

            redirectAttributes.addFlashAttribute("successMessage", "User has been blocked successfully.");
            return "redirect:/admin/users";
        } catch (MeetrulyException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/users";
        }
    }

    @GetMapping("/blocks")
    @PreAuthorize("hasRole('ADMIN')")
    public String showBlocks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model, Principal principal) {

        try {

            String username = principal.getName();
            User admin = userService.getUserByUsername(username)
                    .orElseThrow(() -> new MeetrulyException("Admin not found: " + username));

            PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startDate"));

            Page<UserBlockDto> blocks = adminService.getActiveBlocks(pageRequest);

            model.addAttribute("blocks", blocks.getContent());
            model.addAttribute("currentPage", blocks.getNumber());
            model.addAttribute("totalPages", blocks.getTotalPages());
            model.addAttribute("adminId", admin.getId());

            return "admin/blocks";
        } catch (MeetrulyException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/dashboard";
        }
    }

    @PostMapping("/blocks/{blockId}/unblock")
    @PreAuthorize("hasRole('ADMIN')")
    public String unblockUser(
            @PathVariable("blockId") UUID blockId,
            @RequestParam("reason") String reason,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        try {

            String username = principal.getName();
            User admin = userService.getUserByUsername(username)
                    .orElseThrow(() -> new MeetrulyException("Admin not found: " + username));

            adminService.unblockUser(admin.getId(), blockId, reason);

            redirectAttributes.addFlashAttribute("successMessage", "User has been unblocked successfully.");
            return "redirect:/admin/blocks";
        } catch (MeetrulyException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/blocks";
        }
    }

    @GetMapping("/revenue")
    @PreAuthorize("hasRole('ADMIN')")
    public String showRevenueStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model, Principal principal) {

        try {

            String username = principal.getName();
            User admin = userService.getUserByUsername(username)
                    .orElseThrow(() -> new MeetrulyException("Admin not found: " + username));

            if (startDate == null) {
                startDate = LocalDate.now().withDayOfMonth(1);
            }

            if (endDate == null) {
                endDate = LocalDate.now();
            }

            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.atTime(LocalTime.MAX);

            RevenueStatsDto revenueStats = adminService.getRevenueStats(start, end);

            model.addAttribute("stats", revenueStats);
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);
            model.addAttribute("adminId", admin.getId());

            return "admin/revenue";
        } catch (MeetrulyException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/dashboard";
        }
    }

    @GetMapping("/audit-log")
    @PreAuthorize("hasRole('ADMIN')")
    public String showAuditLog(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model, Principal principal) {

        try {

            String username = principal.getName();
            User admin = userService.getUserByUsername(username)
                    .orElseThrow(() -> new MeetrulyException("Admin not found: " + username));

            PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "performedAt"));

            Page<AdminActionDto> actions = adminService.getAdminActionsByAdmin(admin.getId(), pageRequest);

            model.addAttribute("actions", actions.getContent());
            model.addAttribute("currentPage", actions.getNumber());
            model.addAttribute("totalPages", actions.getTotalPages());
            model.addAttribute("adminId", admin.getId());
            model.addAttribute("actionTypes", AdminAction.ActionType.values());

            return "admin/audit-log";
        } catch (MeetrulyException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/dashboard";
        }
    }

    @PostMapping("/api/reports")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createReport(
            @Valid @RequestBody UserReportRequest reportRequest,
            Principal principal) {

        try {

            String username = principal.getName();
            User reporter = userService.getUserByUsername(username)
                    .orElseThrow(() -> new MeetrulyException("User not found: " + username));

            UserReportDto report = adminService.createUserReport(
                    reporter.getId(),
                    reportRequest.getReportedUserId(),
                    reportRequest.getReportType(),
                    reportRequest.getReportReason()
            );

            return ResponseEntity.ok(report);
        } catch (MeetrulyException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/blocks/process-expired")
    @PreAuthorize("hasRole('ADMIN')")
    public String processExpiredBlocks(Principal principal, RedirectAttributes redirectAttributes) {
        try {

            adminService.processExpiredBlocks();

            redirectAttributes.addFlashAttribute("successMessage", "Expired blocks have been processed successfully.");
            return "redirect:/admin/blocks";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to process expired blocks: " + e.getMessage());
            return "redirect:/admin/blocks";
        }
    }

    private void logAdminAction(UUID adminId, UUID targetUserId, AdminAction.ActionType actionType, String actionDetails, HttpServletRequest request) {
        String ipAddress = request.getRemoteAddr();
        adminService.logAdminAction(adminId, targetUserId, actionType, actionDetails, ipAddress);
    }
}