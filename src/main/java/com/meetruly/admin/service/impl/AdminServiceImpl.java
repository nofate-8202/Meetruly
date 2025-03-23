package com.meetruly.admin.service.impl;

import com.meetruly.admin.dto.*;
import com.meetruly.admin.model.AdminAction;
import com.meetruly.admin.model.UserBlock;
import com.meetruly.admin.model.UserReport;
import com.meetruly.admin.repository.AdminActionRepository;
import com.meetruly.admin.repository.UserBlockRepository;
import com.meetruly.admin.repository.UserReportRepository;
import com.meetruly.admin.service.AdminService;
import com.meetruly.chat.repository.MessageRepository;
import com.meetruly.core.constant.ReportStatus;
import com.meetruly.core.constant.SubscriptionPlan;
import com.meetruly.core.constant.UserRole;
import com.meetruly.core.exception.MeetrulyException;
import com.meetruly.matching.repository.LikeRepository;
import com.meetruly.matching.repository.MatchRepository;
import com.meetruly.matching.repository.ProfileViewRepository;
import com.meetruly.subscription.repository.SubscriptionRepository;
import com.meetruly.subscription.repository.TransactionRepository;
import com.meetruly.user.model.User;
import com.meetruly.user.model.UserProfile;
import com.meetruly.user.repository.UserProfileRepository;
import com.meetruly.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements AdminService {

    private final AdminActionRepository adminActionRepository;
    private final UserReportRepository userReportRepository;
    private final UserBlockRepository userBlockRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final TransactionRepository transactionRepository;
    private final LikeRepository likeRepository;
    private final MatchRepository matchRepository;
    private final MessageRepository messageRepository;
    private final ProfileViewRepository profileViewRepository;

    @Override
    @Transactional
    public AdminActionDto logAdminAction(UUID adminId, UUID targetUserId, AdminAction.ActionType actionType, String actionDetails, String ipAddress) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new MeetrulyException("Admin not found with id: " + adminId));

        if (admin.getRole() != UserRole.ADMIN) {
            throw new MeetrulyException("User does not have administrator rights");
        }

        User targetUser = null;
        if (targetUserId != null) {
            targetUser = userRepository.findById(targetUserId)
                    .orElseThrow(() -> new MeetrulyException("Target user not found with id: " + targetUserId));
        }

        AdminAction adminAction = AdminAction.builder()
                .admin(admin)
                .targetUser(targetUser)
                .actionType(actionType)
                .actionDetails(actionDetails)
                .performedAt(LocalDateTime.now())
                .ipAddress(ipAddress)
                .build();

        AdminAction savedAction = adminActionRepository.save(adminAction);

        return convertToAdminActionDto(savedAction);
    }

    @Override
    public Page<AdminActionDto> getAdminActionsByAdmin(UUID adminId, Pageable pageable) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new MeetrulyException("Admin not found with id: " + adminId));

        return adminActionRepository.findByAdmin(admin, pageable)
                .map(this::convertToAdminActionDto);
    }

    @Override
    public Page<AdminActionDto> getAdminActionsByTargetUser(UUID targetUserId, Pageable pageable) {
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new MeetrulyException("Target user not found with id: " + targetUserId));

        return adminActionRepository.findByTargetUser(targetUser, pageable)
                .map(this::convertToAdminActionDto);
    }

    @Override
    public List<AdminActionDto> getAdminActionsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return adminActionRepository.findByDateRange(startDate, endDate).stream()
                .map(this::convertToAdminActionDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserDto> getPendingApprovalUsers() {
        return userRepository.findByApprovedAndProfileCompleted(false, true).stream()
                .map(this::convertToUserDto)
                .collect(Collectors.toList());
    }

    @Override
    public Page<UserDto> getPendingApprovalUsers(Pageable pageable) {
        return userRepository.findByApprovedAndProfileCompleted(false, true, pageable)
                .map(this::convertToUserDto);
    }

    @Override
    @Transactional
    public void approveUser(UUID adminId, UUID userId) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new MeetrulyException("Admin not found with id: " + adminId));

        if (admin.getRole() != UserRole.ADMIN) {
            throw new MeetrulyException("User does not have administrator rights");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MeetrulyException("User not found with id: " + userId));

        if (user.isApproved()) {
            throw new MeetrulyException("User is already approved");
        }

        user.setApproved(true);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        logAdminAction(
                adminId,
                userId,
                AdminAction.ActionType.USER_APPROVAL,
                "Approved user: " + user.getUsername(),
                null
        );
    }

    @Override
    @Transactional
    public void rejectUser(UUID adminId, UUID userId, String reason) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new MeetrulyException("Admin not found with id: " + adminId));

        if (admin.getRole() != UserRole.ADMIN) {
            throw new MeetrulyException("User does not have administrator rights");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MeetrulyException("User not found with id: " + userId));

        user.setApproved(false);
        user.setEnabled(false);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        logAdminAction(
                adminId,
                userId,
                AdminAction.ActionType.USER_REJECTION,
                "Rejected user: " + user.getUsername() + ". Reason: " + reason,
                null
        );
    }

    @Override
    @Transactional
    public void changeUserRole(UUID adminId, UUID userId, UserRole newRole) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new MeetrulyException("Admin not found with id: " + adminId));

        if (admin.getRole() != UserRole.ADMIN) {
            throw new MeetrulyException("User does not have administrator rights");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MeetrulyException("User not found with id: " + userId));

        UserRole oldRole = user.getRole();
        user.setRole(newRole);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        logAdminAction(
                adminId,
                userId,
                AdminAction.ActionType.ROLE_CHANGE,
                "Changed role for user: " + user.getUsername() + " from " + oldRole + " to " + newRole,
                null
        );
    }

    @Override
    @Transactional
    public UserReportDto createUserReport(UUID reporterId, UUID reportedUserId, UserReport.ReportType reportType, String reportReason) {
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new MeetrulyException("Reporter not found with id: " + reporterId));

        User reportedUser = userRepository.findById(reportedUserId)
                .orElseThrow(() -> new MeetrulyException("Reported user not found with id: " + reportedUserId));

        if (reporterId.equals(reportedUserId)) {
            throw new MeetrulyException("User cannot report themselves");
        }

        UserReport report = UserReport.builder()
                .reporter(reporter)
                .reportedUser(reportedUser)
                .reportType(reportType)
                .reportReason(reportReason)
                .status(ReportStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        UserReport savedReport = userReportRepository.save(report);

        return convertToUserReportDto(savedReport);
    }

    @Override
    public Page<UserReportDto> getReportsByStatus(ReportStatus status, Pageable pageable) {
        return userReportRepository.findByStatus(status, pageable)
                .map(this::convertToUserReportDto);
    }

    @Override
    public List<UserReportDto> getPendingReports() {
        return userReportRepository.findPendingReports().stream()
                .map(this::convertToUserReportDto)
                .collect(Collectors.toList());
    }

    @Override
    public UserReportDto getReportById(UUID reportId) {
        UserReport report = userReportRepository.findById(reportId)
                .orElseThrow(() -> new MeetrulyException("Report not found with id: " + reportId));

        return convertToUserReportDto(report);
    }

    @Override
    @Transactional
    public UserReportDto handleReport(UUID adminId, ReportActionRequest actionRequest) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new MeetrulyException("Admin not found with id: " + adminId));

        if (admin.getRole() != UserRole.ADMIN) {
            throw new MeetrulyException("User does not have administrator rights");
        }

        UserReport report = userReportRepository.findById(actionRequest.getReportId())
                .orElseThrow(() -> new MeetrulyException("Report not found with id: " + actionRequest.getReportId()));

        if (report.getStatus() != ReportStatus.PENDING) {
            throw new MeetrulyException("Report is already handled with status: " + report.getStatus());
        }

        report.setStatus(actionRequest.getStatus());
        report.setAdminNotes(actionRequest.getAdminNotes());
        report.setHandledBy(admin);
        report.setHandledAt(LocalDateTime.now());

        UserReport savedReport = userReportRepository.save(report);

        if (actionRequest.isBlockUser()) {
            UserBlockRequest blockRequest = UserBlockRequest.builder()
                    .userId(report.getReportedUser().getId())
                    .reason("Blocked due to report: " + actionRequest.getReportId() + ". " + actionRequest.getBlockReason())
                    .permanent(actionRequest.isPermanentBlock())
                    .durationDays(actionRequest.getBlockDurationDays())
                    .build();

            blockUser(adminId, blockRequest);
        }

        logAdminAction(
                adminId,
                report.getReportedUser().getId(),
                AdminAction.ActionType.REPORT_HANDLING,
                "Handled report: " + report.getId() + " with status: " + actionRequest.getStatus(),
                null
        );

        return convertToUserReportDto(savedReport);
    }

    @Override
    @Transactional
    public UserBlockDto blockUser(UUID adminId, UserBlockRequest blockRequest) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new MeetrulyException("Admin not found with id: " + adminId));

        if (admin.getRole() != UserRole.ADMIN) {
            throw new MeetrulyException("User does not have administrator rights");
        }

        User user = userRepository.findById(blockRequest.getUserId())
                .orElseThrow(() -> new MeetrulyException("User not found with id: " + blockRequest.getUserId()));

        Optional<UserBlock> existingBlock = userBlockRepository.findActiveBlockByUser(user);
        if (existingBlock.isPresent()) {
            throw new MeetrulyException("User is already blocked");
        }

        LocalDateTime endDate = null;
        if (!blockRequest.isPermanent()) {
            endDate = LocalDateTime.now().plusDays(blockRequest.getDurationDays());
        }

        UserBlock block = UserBlock.builder()
                .blockedUser(user)
                .blockedBy(admin)
                .reason(blockRequest.getReason())
                .startDate(LocalDateTime.now())
                .endDate(endDate)
                .permanent(blockRequest.isPermanent())
                .active(true)
                .build();

        UserBlock savedBlock = userBlockRepository.save(block);

        user.setEnabled(false);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        String actionDetails = "Blocked user: " + user.getUsername() + ". Reason: " + blockRequest.getReason();
        if (!blockRequest.isPermanent()) {
            actionDetails += " Duration: " + blockRequest.getDurationDays() + " days";
        } else {
            actionDetails += " Permanent block";
        }

        logAdminAction(
                adminId,
                user.getId(),
                AdminAction.ActionType.USER_BLOCK,
                actionDetails,
                null
        );

        return convertToUserBlockDto(savedBlock);
    }

    @Override
    @Transactional
    public void unblockUser(UUID adminId, UUID blockId, String reason) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new MeetrulyException("Admin not found with id: " + adminId));

        if (admin.getRole() != UserRole.ADMIN) {
            throw new MeetrulyException("User does not have administrator rights");
        }

        UserBlock block = userBlockRepository.findById(blockId)
                .orElseThrow(() -> new MeetrulyException("Block not found with id: " + blockId));

        if (!block.isActive()) {
            throw new MeetrulyException("Block is already inactive");
        }

        block.setActive(false);
        block.setUnblockDate(LocalDateTime.now());
        block.setUnblockedBy(admin);
        block.setUnblockReason(reason);
        userBlockRepository.save(block);

        User user = block.getBlockedUser();
        user.setEnabled(true);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        logAdminAction(
                adminId,
                user.getId(),
                AdminAction.ActionType.USER_UNBLOCK,
                "Unblocked user: " + user.getUsername() + ". Reason: " + reason,
                null
        );
    }

    @Override
    public List<UserBlockDto> getActiveBlocks() {
        return userBlockRepository.findActiveBlocks().stream()
                .map(this::convertToUserBlockDto)
                .collect(Collectors.toList());
    }

    @Override
    public Page<UserBlockDto> getActiveBlocks(Pageable pageable) {
        return userBlockRepository.findActiveBlocks(pageable)
                .map(this::convertToUserBlockDto);
    }

    @Override
    public UserBlockDto getBlockById(UUID blockId) {
        UserBlock block = userBlockRepository.findById(blockId)
                .orElseThrow(() -> new MeetrulyException("Block not found with id: " + blockId));

        return convertToUserBlockDto(block);
    }

    @Override
    public boolean isUserBlocked(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MeetrulyException("User not found with id: " + userId));

        return userBlockRepository.findActiveBlockByUser(user).isPresent();
    }

    @Override
    public AdminDashboardDto getDashboardStats() {

        long totalUsers = userRepository.count();
        long pendingApprovalUsers = userRepository.countByApprovedAndProfileCompleted(false, true);
        long activeUsers = userRepository.countByEnabledAndApproved(true, true);
        long blockedUsers = userBlockRepository.countActiveBlocks();

        LocalDateTime today = LocalDate.now().atStartOfDay();
        LocalDateTime firstDayOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        long registrationsToday = userRepository.countByCreatedAtAfter(today);
        long registrationsThisMonth = userRepository.countByCreatedAtAfter(firstDayOfMonth);

        long totalMatches = matchRepository.count();
        long totalLikes = likeRepository.count();
        long totalMessages = messageRepository.count();
        long totalProfileViews = profileViewRepository.count();

        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        long messagesLast24Hours = messageRepository.countByCreatedAtAfter(yesterday);
        long likesLast24Hours = likeRepository.countByCreatedAtAfter(yesterday);

        LocalDateTime firstDayOfPreviousMonth = firstDayOfMonth.minusMonths(1);
        Double totalRevenue = Optional.ofNullable(transactionRepository.calculateTotalRevenue())
                .orElse(0.0);

        Double revenueThisMonthResult = transactionRepository.calculateRevenueForPeriod(firstDayOfMonth, LocalDateTime.now());
        double revenueThisMonth = revenueThisMonthResult != null ? revenueThisMonthResult : 0.0;

        Double revenuePreviousMonthResult = transactionRepository.calculateRevenueForPeriod(firstDayOfPreviousMonth, firstDayOfMonth);
        double revenuePreviousMonth = revenuePreviousMonthResult != null ? revenuePreviousMonthResult : 0.0;

        double growthPercentage = 0.0;
        if (revenuePreviousMonth > 0) {
            growthPercentage = ((revenueThisMonth - revenuePreviousMonth) / revenuePreviousMonth) * 100;
        }

        Map<String, Integer> subscriptionsByPlan = new HashMap<>();
        for (SubscriptionPlan plan : SubscriptionPlan.values()) {
            long count = subscriptionRepository.countActiveByPlan(plan);
            subscriptionsByPlan.put(plan.name(), (int) count);
        }

        Map<String, Double> revenueByPlan = new HashMap<>();
        for (SubscriptionPlan plan : SubscriptionPlan.values()) {
            if (plan != SubscriptionPlan.FREE) {
                Double revenue = transactionRepository.calculateRevenueByPlan(plan);
                revenueByPlan.put(plan.name(), revenue != null ? revenue : 0.0);
            }
        }

        long pendingReports = userReportRepository.countByStatus(ReportStatus.PENDING);
        long totalReports = userReportRepository.count();

        Map<String, Long> reportsByType = new HashMap<>();
        List<Object[]> reportCounts = userReportRepository.countByReportType();
        for (Object[] count : reportCounts) {
            UserReport.ReportType type = (UserReport.ReportType) count[0];
            Long countValue = (Long) count[1];
            reportsByType.put(type.name(), countValue);
        }

        PageRequest recentReportsRequest = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<UserReportDto> recentReports = userReportRepository.findByStatus(ReportStatus.PENDING, recentReportsRequest)
                .stream()
                .map(this::convertToUserReportDto)
                .collect(Collectors.toList());

        PageRequest recentUsersRequest = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<UserDto> recentPendingUsers = userRepository.findByApprovedAndProfileCompleted(false, true, recentUsersRequest)
                .stream()
                .map(this::convertToUserDto)
                .collect(Collectors.toList());

        return AdminDashboardDto.builder()
                .totalUsers(totalUsers)
                .pendingApprovalUsers(pendingApprovalUsers)
                .activeUsers(activeUsers)
                .blockedUsers(blockedUsers)
                .registrationsToday(registrationsToday)
                .registrationsThisMonth(registrationsThisMonth)
                .totalMatches(totalMatches)
                .totalLikes(totalLikes)
                .totalMessages(totalMessages)
                .totalProfileViews(totalProfileViews)
                .messagesLast24Hours(messagesLast24Hours)
                .likesLast24Hours(likesLast24Hours)
                .totalRevenue(totalRevenue)
                .revenueThisMonth(revenueThisMonth)
                .revenuePreviousMonth(revenuePreviousMonth)
                .growthPercentage(growthPercentage)
                .subscriptionsByPlan(subscriptionsByPlan)
                .revenueByPlan(revenueByPlan)
                .pendingReports(pendingReports)
                .totalReports(totalReports)
                .reportsByType(reportsByType)
                .recentReports(recentReports)
                .recentPendingUsers(recentPendingUsers)
                .build();
    }

    @Override
    public RevenueStatsDto getRevenueStats(LocalDateTime startDate, LocalDateTime endDate) {

        Double totalRevenue = transactionRepository.calculateRevenueForPeriod(startDate, endDate);
        if (totalRevenue == null) {
            totalRevenue = 0.0;
        }

        Map<SubscriptionPlan, Double> revenueByPlan = new HashMap<>();
        for (SubscriptionPlan plan : SubscriptionPlan.values()) {
            if (plan != SubscriptionPlan.FREE) {
                Double planRevenue = transactionRepository.calculateRevenueByPlanForPeriod(plan, startDate, endDate);
                revenueByPlan.put(plan, planRevenue != null ? planRevenue : 0.0);
            }
        }

        Map<SubscriptionPlan, Long> subscriptionCountByPlan = new HashMap<>();
        for (SubscriptionPlan plan : SubscriptionPlan.values()) {
            long count = subscriptionRepository.countActiveByPlan(plan);
            subscriptionCountByPlan.put(plan, count);
        }

        long periodDays = ChronoUnit.DAYS.between(startDate, endDate);
        LocalDateTime previousPeriodStart = startDate.minusDays(periodDays);
        LocalDateTime previousPeriodEnd = startDate;

        Double previousRevenue = transactionRepository.calculateRevenueForPeriod(previousPeriodStart, previousPeriodEnd);
        if (previousRevenue == null) {
            previousRevenue = 0.0;
        }

        double growthPercentage = 0.0;
        if (previousRevenue > 0) {
            growthPercentage = ((totalRevenue - previousRevenue) / previousRevenue) * 100;
        }

        long activeSubscriptions = subscriptionRepository.countActiveSubscriptions();

        double averageRevenuePerUser = 0.0;
        long totalActiveUsers = userRepository.countByEnabledAndApproved(true, true);
        if (totalActiveUsers > 0) {
            averageRevenuePerUser = totalRevenue / totalActiveUsers;
        }

        double revenuePerDay = 0.0;
        if (periodDays > 0) {
            revenuePerDay = totalRevenue / periodDays;
        }

        Map<String, Double> revenueByDay = new HashMap<>();
        LocalDateTime currentDate = startDate;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        while (!currentDate.isAfter(endDate)) {
            LocalDateTime nextDate = currentDate.plusDays(1);
            Double dayRevenue = transactionRepository.calculateRevenueForPeriod(currentDate, nextDate);
            revenueByDay.put(currentDate.format(formatter), dayRevenue != null ? dayRevenue : 0.0);
            currentDate = nextDate;
        }

        Map<String, Double> revenueByMonth = new HashMap<>();
        LocalDateTime currentMonth = startDate.withDayOfMonth(1);
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");

        while (!currentMonth.isAfter(endDate)) {
            LocalDateTime nextMonth = currentMonth.plusMonths(1);
            Double monthRevenue = transactionRepository.calculateRevenueForPeriod(currentMonth, nextMonth);
            revenueByMonth.put(currentMonth.format(monthFormatter), monthRevenue != null ? monthRevenue : 0.0);
            currentMonth = nextMonth;
        }

        return RevenueStatsDto.builder()
                .startDate(startDate)
                .endDate(endDate)
                .totalRevenue(totalRevenue)
                .revenueByPlan(revenueByPlan)
                .subscriptionCountByPlan(subscriptionCountByPlan)
                .growthPercentage(growthPercentage)
                .activeSubscriptions(activeSubscriptions)
                .averageRevenuePerUser(averageRevenuePerUser)
                .revenuePerDay(revenuePerDay)
                .revenueByDay(revenueByDay)
                .revenueByMonth(revenueByMonth)
                .build();
    }

    @Override
    @Scheduled(cron = "0 0 * * * ?")
    @Transactional
    public void processExpiredBlocks() {
        LocalDateTime now = LocalDateTime.now();
        List<UserBlock> expiredBlocks = userBlockRepository.findExpiredBlocks(now);

        for (UserBlock block : expiredBlocks) {

            block.setActive(false);
            block.setUnblockDate(now);
            block.setUnblockReason("Automatic unblock due to expiration");

            User user = block.getBlockedUser();
            user.setEnabled(true);
            user.setUpdatedAt(now);
            userRepository.save(user);
        }

        userBlockRepository.saveAll(expiredBlocks);

        log.info("Processed {} expired blocks", expiredBlocks.size());
    }


    private AdminActionDto convertToAdminActionDto(AdminAction action) {
        return AdminActionDto.builder()
                .id(action.getId())
                .adminId(action.getAdmin().getId())
                .adminUsername(action.getAdmin().getUsername())
                .targetUserId(action.getTargetUser() != null ? action.getTargetUser().getId() : null)
                .targetUsername(action.getTargetUser() != null ? action.getTargetUser().getUsername() : null)
                .actionType(action.getActionType())
                .actionDetails(action.getActionDetails())
                .performedAt(action.getPerformedAt())
                .ipAddress(action.getIpAddress())
                .build();
    }

    private UserReportDto convertToUserReportDto(UserReport report) {
        return UserReportDto.builder()
                .id(report.getId())
                .reporterId(report.getReporter().getId())
                .reporterUsername(report.getReporter().getUsername())
                .reporterProfileImageUrl(getProfileImageUrl(report.getReporter().getId()))
                .reportedUserId(report.getReportedUser().getId())
                .reportedUsername(report.getReportedUser().getUsername())
                .reportedProfileImageUrl(getProfileImageUrl(report.getReportedUser().getId()))
                .reportType(report.getReportType())
                .reportReason(report.getReportReason())
                .status(report.getStatus())
                .adminNotes(report.getAdminNotes())
                .handledById(report.getHandledBy() != null ? report.getHandledBy().getId() : null)
                .handledByUsername(report.getHandledBy() != null ? report.getHandledBy().getUsername() : null)
                .handledAt(report.getHandledAt())
                .createdAt(report.getCreatedAt())
                .build();
    }

    private UserBlockDto convertToUserBlockDto(UserBlock block) {

        long remainingDays = 0;
        if (!block.isPermanent() && block.getEndDate() != null && block.isActive()) {
            remainingDays = ChronoUnit.DAYS.between(LocalDateTime.now(), block.getEndDate());
            if (remainingDays < 0) {
                remainingDays = 0;
            }
        }

        return UserBlockDto.builder()
                .id(block.getId())
                .blockedUserId(block.getBlockedUser().getId())
                .blockedUsername(block.getBlockedUser().getUsername())
                .blockedProfileImageUrl(getProfileImageUrl(block.getBlockedUser().getId()))
                .blockedById(block.getBlockedBy().getId())
                .blockedByUsername(block.getBlockedBy().getUsername())
                .reason(block.getReason())
                .startDate(block.getStartDate())
                .endDate(block.getEndDate())
                .permanent(block.isPermanent())
                .active(block.isActive())
                .unblockDate(block.getUnblockDate())
                .unblockedById(block.getUnblockedBy() != null ? block.getUnblockedBy().getId() : null)
                .unblockedByUsername(block.getUnblockedBy() != null ? block.getUnblockedBy().getUsername() : null)
                .unblockReason(block.getUnblockReason())
                .expired(block.isExpired())
                .remainingDays(remainingDays)
                .build();
    }

    private UserDto convertToUserDto(User user) {

        String subscriptionPlan = subscriptionRepository.findActiveSubscriptionByUserId(user.getId())
                .map(subscription -> subscription.getPlan().name())
                .orElse("FREE");

        boolean blocked = userBlockRepository.findActiveBlockByUser(user).isPresent();

        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .profileImageUrl(getProfileImageUrl(user.getId()))
                .gender(user.getGender())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .emailVerified(user.isEmailVerified())
                .profileCompleted(user.isProfileCompleted())
                .approved(user.isApproved())
                .subscriptionPlan(subscriptionPlan)
                .blocked(blocked)
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .build();
    }

    private String getProfileImageUrl(UUID userId) {
        return userProfileRepository.findByUserId(userId)
                .map(UserProfile::getProfileImageUrl)
                .orElse("/images/default-profile.jpg");
    }
}