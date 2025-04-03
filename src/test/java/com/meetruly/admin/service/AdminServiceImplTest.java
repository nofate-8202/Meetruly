package com.meetruly.admin.service;

import com.meetruly.admin.dto.*;
import com.meetruly.admin.model.AdminAction;
import com.meetruly.admin.model.UserBlock;
import com.meetruly.admin.model.UserReport;
import com.meetruly.admin.repository.AdminActionRepository;
import com.meetruly.admin.repository.UserBlockRepository;
import com.meetruly.admin.repository.UserReportRepository;
import com.meetruly.admin.service.impl.AdminServiceImpl;
import com.meetruly.chat.repository.MessageRepository;
import com.meetruly.core.constant.Gender;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock
    private AdminActionRepository adminActionRepository;

    @Mock
    private UserReportRepository userReportRepository;

    @Mock
    private UserBlockRepository userBlockRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private LikeRepository likeRepository;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ProfileViewRepository profileViewRepository;

    @InjectMocks
    private AdminServiceImpl adminService;

    
    private User adminUser;
    private User regularUser;
    private User reportedUser;
    private AdminAction adminAction;
    private UserReport userReport;
    private UserBlock userBlock;
    private UserProfile userProfile;

    @BeforeEach
    void setUp() {
        
        adminUser = new User();
        adminUser.setId(UUID.randomUUID());
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@example.com");
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setEnabled(true);
        adminUser.setApproved(true);

        regularUser = new User();
        regularUser.setId(UUID.randomUUID());
        regularUser.setUsername("user");
        regularUser.setEmail("user@example.com");
        regularUser.setRole(UserRole.USER);
        regularUser.setEnabled(true);
        regularUser.setApproved(true);

        reportedUser = new User();
        reportedUser.setId(UUID.randomUUID());
        reportedUser.setUsername("reported_user");
        reportedUser.setEmail("reported@example.com");
        reportedUser.setRole(UserRole.USER);
        reportedUser.setEnabled(true);
        reportedUser.setApproved(true);

        userProfile = new UserProfile();
        userProfile.setId(UUID.randomUUID());
        userProfile.setUser(regularUser);
        userProfile.setProfileImageUrl("/images/user.jpg");

        adminAction = new AdminAction();
        adminAction.setId(UUID.randomUUID());
        adminAction.setAdmin(adminUser);
        adminAction.setTargetUser(regularUser);
        adminAction.setActionType(AdminAction.ActionType.USER_APPROVAL);
        adminAction.setActionDetails("Approved user");
        adminAction.setPerformedAt(LocalDateTime.now());
        adminAction.setIpAddress("127.0.0.1");

        userReport = new UserReport();
        userReport.setId(UUID.randomUUID());
        userReport.setReporter(regularUser);
        userReport.setReportedUser(reportedUser);
        userReport.setReportType(UserReport.ReportType.INAPPROPRIATE_CONTENT);
        userReport.setReportReason("Inappropriate content in profile");
        userReport.setStatus(ReportStatus.PENDING);
        userReport.setCreatedAt(LocalDateTime.now());

        userBlock = new UserBlock();
        userBlock.setId(UUID.randomUUID());
        userBlock.setBlockedUser(reportedUser);
        userBlock.setBlockedBy(adminUser);
        userBlock.setReason("Violation of terms");
        userBlock.setStartDate(LocalDateTime.now());
        userBlock.setEndDate(LocalDateTime.now().plusDays(7));
        userBlock.setPermanent(false);
        userBlock.setActive(true);
    }

    @Nested
    class AdminActionTests {

        @Test
        void shouldLogAdminAction() {
            
            UUID adminId = adminUser.getId();
            UUID targetUserId = regularUser.getId();
            AdminAction.ActionType actionType = AdminAction.ActionType.USER_APPROVAL;
            String actionDetails = "Approved user: user";
            String ipAddress = "127.0.0.1";

            when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
            when(userRepository.findById(targetUserId)).thenReturn(Optional.of(regularUser));
            when(adminActionRepository.save(any(AdminAction.class))).thenReturn(adminAction);

            
            AdminActionDto result = adminService.logAdminAction(adminId, targetUserId, actionType, actionDetails, ipAddress);

            
            assertNotNull(result);
            assertEquals(adminAction.getId(), result.getId());
            assertEquals(adminUser.getId(), result.getAdminId());
            assertEquals(adminUser.getUsername(), result.getAdminUsername());
            assertEquals(regularUser.getId(), result.getTargetUserId());
            assertEquals(regularUser.getUsername(), result.getTargetUsername());
            assertEquals(adminAction.getActionType(), result.getActionType());
            assertEquals(adminAction.getActionDetails(), result.getActionDetails());
            assertEquals(adminAction.getPerformedAt(), result.getPerformedAt());
            assertEquals(adminAction.getIpAddress(), result.getIpAddress());

            
            verify(adminActionRepository, times(1)).save(any(AdminAction.class));
        }

        @Test
        void shouldThrowExceptionWhenAdminNotFound() {
            
            UUID adminId = UUID.randomUUID();
            UUID targetUserId = regularUser.getId();
            AdminAction.ActionType actionType = AdminAction.ActionType.USER_APPROVAL;
            String actionDetails = "Approved user: user";
            String ipAddress = "127.0.0.1";

            when(userRepository.findById(adminId)).thenReturn(Optional.empty());

            
            MeetrulyException exception = assertThrows(MeetrulyException.class, () -> {
                adminService.logAdminAction(adminId, targetUserId, actionType, actionDetails, ipAddress);
            });

            assertEquals("Admin not found with id: " + adminId, exception.getMessage());
            verify(adminActionRepository, never()).save(any(AdminAction.class));
        }

        @Test
        void shouldThrowExceptionWhenUserIsNotAdmin() {
            
            UUID userId = regularUser.getId();
            UUID targetUserId = reportedUser.getId();
            AdminAction.ActionType actionType = AdminAction.ActionType.USER_APPROVAL;
            String actionDetails = "Approved user: reported_user";
            String ipAddress = "127.0.0.1";

            when(userRepository.findById(userId)).thenReturn(Optional.of(regularUser));

            
            MeetrulyException exception = assertThrows(MeetrulyException.class, () -> {
                adminService.logAdminAction(userId, targetUserId, actionType, actionDetails, ipAddress);
            });

            assertEquals("User does not have administrator rights", exception.getMessage());
            verify(adminActionRepository, never()).save(any(AdminAction.class));
        }

        @Test
        void shouldReturnAdminActionsByAdmin() {
            
            UUID adminId = adminUser.getId();
            Pageable pageable = PageRequest.of(0, 10);
            List<AdminAction> actions = Collections.singletonList(adminAction);
            Page<AdminAction> actionPage = new PageImpl<>(actions, pageable, actions.size());

            when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
            when(adminActionRepository.findByAdmin(adminUser, pageable)).thenReturn(actionPage);

            
            Page<AdminActionDto> result = adminService.getAdminActionsByAdmin(adminId, pageable);

            
            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            assertEquals(adminAction.getId(), result.getContent().get(0).getId());
            assertEquals(adminUser.getId(), result.getContent().get(0).getAdminId());
            assertEquals(regularUser.getId(), result.getContent().get(0).getTargetUserId());
        }

        @Test
        void shouldReturnAdminActionsByTargetUser() {
            
            UUID targetUserId = regularUser.getId();
            Pageable pageable = PageRequest.of(0, 10);
            List<AdminAction> actions = Collections.singletonList(adminAction);
            Page<AdminAction> actionPage = new PageImpl<>(actions, pageable, actions.size());

            when(userRepository.findById(targetUserId)).thenReturn(Optional.of(regularUser));
            when(adminActionRepository.findByTargetUser(regularUser, pageable)).thenReturn(actionPage);

            
            Page<AdminActionDto> result = adminService.getAdminActionsByTargetUser(targetUserId, pageable);

            
            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            assertEquals(adminAction.getId(), result.getContent().get(0).getId());
            assertEquals(regularUser.getId(), result.getContent().get(0).getTargetUserId());
            verify(adminActionRepository, times(1)).findByTargetUser(regularUser, pageable);
        }

        @Test
        void shouldReturnAdminActionsByDateRange() {
            
            LocalDateTime startDate = LocalDateTime.now().minusDays(7);
            LocalDateTime endDate = LocalDateTime.now();
            List<AdminAction> actions = Collections.singletonList(adminAction);

            when(adminActionRepository.findByDateRange(startDate, endDate)).thenReturn(actions);

            
            List<AdminActionDto> result = adminService.getAdminActionsByDateRange(startDate, endDate);

            
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(adminAction.getId(), result.get(0).getId());
            verify(adminActionRepository, times(1)).findByDateRange(startDate, endDate);
        }
    }

    @Nested
    class UserManagementTests {

        @Test
        void shouldApproveUser() {
            
            UUID adminId = adminUser.getId();
            UUID userId = regularUser.getId();
            regularUser.setApproved(false);

            when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
            when(userRepository.findById(userId)).thenReturn(Optional.of(regularUser));
            when(userRepository.save(any(User.class))).thenReturn(regularUser);
            when(adminActionRepository.save(any(AdminAction.class))).thenReturn(adminAction);

            
            adminService.approveUser(adminId, userId);

            
            assertTrue(regularUser.isApproved());
            verify(userRepository, times(1)).save(regularUser);
            verify(adminActionRepository, times(1)).save(any(AdminAction.class));
        }

        @Test
        void shouldRejectUser() {
            
            UUID adminId = adminUser.getId();
            UUID userId = regularUser.getId();
            String reason = "Fake profile";

            when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
            when(userRepository.findById(userId)).thenReturn(Optional.of(regularUser));
            when(userRepository.save(any(User.class))).thenReturn(regularUser);
            when(adminActionRepository.save(any(AdminAction.class))).thenReturn(adminAction);

            
            adminService.rejectUser(adminId, userId, reason);

            
            assertFalse(regularUser.isApproved());
            assertFalse(regularUser.isEnabled());
            verify(userRepository, times(1)).save(regularUser);
            verify(adminActionRepository, times(1)).save(any(AdminAction.class));
        }

        @Test
        void shouldChangeUserRole() {
            
            UUID adminId = adminUser.getId();
            UUID userId = regularUser.getId();
            UserRole newRole = UserRole.ADMIN;

            when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
            when(userRepository.findById(userId)).thenReturn(Optional.of(regularUser));
            when(userRepository.save(any(User.class))).thenReturn(regularUser);
            when(adminActionRepository.save(any(AdminAction.class))).thenReturn(adminAction);

            
            adminService.changeUserRole(adminId, userId, newRole);

            
            assertEquals(UserRole.ADMIN, regularUser.getRole());
            verify(userRepository, times(1)).save(regularUser);
            verify(adminActionRepository, times(1)).save(any(AdminAction.class));
        }

        @Test
        void shouldReturnPendingApprovalUsers() {
            
            List<User> pendingUsers = Collections.singletonList(regularUser);
            when(userRepository.findByApproved(false)).thenReturn(pendingUsers);
            when(userProfileRepository.findByUserId(any(UUID.class))).thenReturn(Optional.of(userProfile));
            when(subscriptionRepository.findActiveSubscriptionByUserId(any(UUID.class))).thenReturn(Optional.empty());
            when(userBlockRepository.findActiveBlockByUser(any(User.class))).thenReturn(Optional.empty());

            
            List<UserDto> result = adminService.getPendingApprovalUsers();

            
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(regularUser.getId(), result.get(0).getId());
            assertEquals(regularUser.getUsername(), result.get(0).getUsername());
            assertEquals("FREE", result.get(0).getSubscriptionPlan());
            assertFalse(result.get(0).isBlocked());
        }

        @Test
        void shouldGetPendingApprovalUsersWithPagination() {
            
            Pageable pageable = PageRequest.of(0, 10);
            List<User> pendingUsers = Collections.singletonList(regularUser);
            Page<User> userPage = new PageImpl<>(pendingUsers, pageable, pendingUsers.size());

            when(userRepository.findPendingApprovalUsers(pageable)).thenReturn(userPage);
            when(userProfileRepository.findByUserId(any(UUID.class))).thenReturn(Optional.of(userProfile));
            when(subscriptionRepository.findActiveSubscriptionByUserId(any(UUID.class))).thenReturn(Optional.empty());
            when(userBlockRepository.findActiveBlockByUser(any(User.class))).thenReturn(Optional.empty());

            
            Page<UserDto> result = adminService.getPendingApprovalUsers(pageable);

            
            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            assertEquals(regularUser.getId(), result.getContent().get(0).getId());
            assertEquals(regularUser.getUsername(), result.getContent().get(0).getUsername());
            verify(userRepository, times(1)).findPendingApprovalUsers(pageable);
        }

        @Test
        void shouldThrowExceptionWhenApprovingAlreadyApprovedUser() {
            
            UUID adminId = adminUser.getId();
            UUID userId = regularUser.getId();
            regularUser.setApproved(true);

            when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
            when(userRepository.findById(userId)).thenReturn(Optional.of(regularUser));

            
            MeetrulyException exception = assertThrows(MeetrulyException.class, () -> {
                adminService.approveUser(adminId, userId);
            });

            assertEquals("User is already approved", exception.getMessage());
            verify(userRepository, never()).save(any(User.class));
            verify(adminActionRepository, never()).save(any(AdminAction.class));
        }
    }

    @Nested
    class ReportManagementTests {

        @Test
        void shouldGetReportsByStatus() {
            
            ReportStatus status = ReportStatus.PENDING;
            Pageable pageable = PageRequest.of(0, 10);
            List<UserReport> reports = Collections.singletonList(userReport);
            Page<UserReport> reportPage = new PageImpl<>(reports, pageable, reports.size());

            when(userReportRepository.findByStatus(status, pageable)).thenReturn(reportPage);
            when(userProfileRepository.findByUserId(any(UUID.class))).thenReturn(Optional.empty());

            
            Page<UserReportDto> result = adminService.getReportsByStatus(status, pageable);

            
            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            assertEquals(userReport.getId(), result.getContent().get(0).getId());
            assertEquals(regularUser.getId(), result.getContent().get(0).getReporterId());
            assertEquals(reportedUser.getId(), result.getContent().get(0).getReportedUserId());
            verify(userReportRepository, times(1)).findByStatus(status, pageable);
        }

        @Test
        void shouldGetPendingReports() {
            
            List<UserReport> pendingReports = Collections.singletonList(userReport);
            when(userReportRepository.findPendingReports()).thenReturn(pendingReports);
            when(userProfileRepository.findByUserId(any(UUID.class))).thenReturn(Optional.empty());

            
            List<UserReportDto> result = adminService.getPendingReports();

            
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(userReport.getId(), result.get(0).getId());
            verify(userReportRepository, times(1)).findPendingReports();
        }

        @Test
        void shouldGetReportById() {
            
            UUID reportId = userReport.getId();
            when(userReportRepository.findById(reportId)).thenReturn(Optional.of(userReport));
            when(userProfileRepository.findByUserId(any(UUID.class))).thenReturn(Optional.empty());

            
            UserReportDto result = adminService.getReportById(reportId);

            
            assertNotNull(result);
            assertEquals(reportId, result.getId());
            verify(userReportRepository, times(1)).findById(reportId);
        }

        @Test
        void shouldCreateUserReport() {
            
            UUID reporterId = regularUser.getId();
            UUID reportedUserId = reportedUser.getId();
            UserReport.ReportType reportType = UserReport.ReportType.INAPPROPRIATE_CONTENT;
            String reportReason = "Inappropriate content in profile";

            when(userRepository.findById(reporterId)).thenReturn(Optional.of(regularUser));
            when(userRepository.findById(reportedUserId)).thenReturn(Optional.of(reportedUser));
            when(userReportRepository.save(any(UserReport.class))).thenReturn(userReport);

            
            UserReportDto result = adminService.createUserReport(reporterId, reportedUserId, reportType, reportReason);

            
            assertNotNull(result);
            assertEquals(userReport.getId(), result.getId());
            assertEquals(regularUser.getId(), result.getReporterId());
            assertEquals(reportedUser.getId(), result.getReportedUserId());
            assertEquals(reportType, result.getReportType());
            assertEquals(reportReason, result.getReportReason());
            assertEquals(ReportStatus.PENDING, result.getStatus());
            verify(userReportRepository, times(1)).save(any(UserReport.class));
        }

        @Test
        void shouldThrowExceptionWhenSelfReporting() {
            
            UUID userId = regularUser.getId();
            UserReport.ReportType reportType = UserReport.ReportType.INAPPROPRIATE_CONTENT;
            String reportReason = "Test";

            when(userRepository.findById(userId)).thenReturn(Optional.of(regularUser));

            
            MeetrulyException exception = assertThrows(MeetrulyException.class, () -> {
                adminService.createUserReport(userId, userId, reportType, reportReason);
            });

            assertEquals("User cannot report themselves", exception.getMessage());
            verify(userReportRepository, never()).save(any(UserReport.class));
        }

        @Test
        void shouldHandleReport() {
            
            UUID adminId = adminUser.getId();
            UUID reportId = userReport.getId();

            ReportActionRequest actionRequest = new ReportActionRequest();
            actionRequest.setReportId(reportId);
            actionRequest.setStatus(ReportStatus.RESOLVED);
            actionRequest.setAdminNotes("Resolved - warning issued");
            actionRequest.setBlockUser(false);

            when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
            when(userRepository.findById(reportedUser.getId())).thenReturn(Optional.of(reportedUser));
            when(userReportRepository.findById(reportId)).thenReturn(Optional.of(userReport));
            when(userReportRepository.save(any(UserReport.class))).thenReturn(userReport);
            when(adminActionRepository.save(any(AdminAction.class))).thenReturn(adminAction);

            
            UserReportDto result = adminService.handleReport(adminId, actionRequest);

            
            assertNotNull(result);
            assertEquals(ReportStatus.RESOLVED, userReport.getStatus());
            assertEquals("Resolved - warning issued", userReport.getAdminNotes());
            assertEquals(adminUser, userReport.getHandledBy());
            assertNotNull(userReport.getHandledAt());
            verify(userReportRepository, times(1)).save(userReport);
            verify(adminActionRepository, times(1)).save(any(AdminAction.class));
        }

        @Test
        void shouldHandleReportAndBlockUser() {
            
            UUID adminId = adminUser.getId();
            UUID reportId = userReport.getId();

            ReportActionRequest actionRequest = new ReportActionRequest();
            actionRequest.setReportId(reportId);
            actionRequest.setStatus(ReportStatus.RESOLVED);
            actionRequest.setAdminNotes("Resolved - user blocked");
            actionRequest.setBlockUser(true);
            actionRequest.setBlockDurationDays(7);
            actionRequest.setPermanentBlock(false);
            actionRequest.setBlockReason("Violation of terms");

            when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
            when(userRepository.findById(reportedUser.getId())).thenReturn(Optional.of(reportedUser));
            when(userReportRepository.findById(reportId)).thenReturn(Optional.of(userReport));
            when(userReportRepository.save(any(UserReport.class))).thenReturn(userReport);
            when(userBlockRepository.findActiveBlockByUser(any(User.class))).thenReturn(Optional.empty());
            when(userBlockRepository.save(any(UserBlock.class))).thenReturn(userBlock);
            when(adminActionRepository.save(any(AdminAction.class))).thenReturn(adminAction);

            
            UserReportDto result = adminService.handleReport(adminId, actionRequest);

            
            assertNotNull(result);
            assertEquals(ReportStatus.RESOLVED, userReport.getStatus());
            verify(userReportRepository, times(1)).save(userReport);
            verify(userBlockRepository, times(1)).save(any(UserBlock.class));
            verify(adminActionRepository, times(2)).save(any(AdminAction.class)); 
        }

        @Test
        void shouldThrowExceptionWhenHandlingAlreadyHandledReport() {
            
            UUID adminId = adminUser.getId();
            UUID reportId = userReport.getId();
            userReport.setStatus(ReportStatus.RESOLVED);

            ReportActionRequest actionRequest = new ReportActionRequest();
            actionRequest.setReportId(reportId);
            actionRequest.setStatus(ReportStatus.DISMISSED);
            actionRequest.setAdminNotes("Test notes");

            when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
            when(userReportRepository.findById(reportId)).thenReturn(Optional.of(userReport));

            
            MeetrulyException exception = assertThrows(MeetrulyException.class, () -> {
                adminService.handleReport(adminId, actionRequest);
            });

            assertEquals("Report is already handled with status: " + userReport.getStatus(), exception.getMessage());
            verify(userReportRepository, never()).save(any(UserReport.class));
        }

        @Test
        void shouldThrowExceptionForInvalidReportId() {
            
            UUID adminId = adminUser.getId();
            UUID invalidReportId = UUID.randomUUID();

            ReportActionRequest actionRequest = new ReportActionRequest();
            actionRequest.setReportId(invalidReportId);
            actionRequest.setStatus(ReportStatus.RESOLVED);
            actionRequest.setAdminNotes("Test notes");

            when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
            when(userReportRepository.findById(invalidReportId)).thenReturn(Optional.empty());

            
            MeetrulyException exception = assertThrows(MeetrulyException.class, () -> {
                adminService.handleReport(adminId, actionRequest);
            });

            assertEquals("Report not found with id: " + invalidReportId, exception.getMessage());
        }
    }
    @Nested
    class BlockManagementTests {

        @Test
        void shouldBlockUser() {
            
            UUID adminId = adminUser.getId();
            UUID userId = reportedUser.getId();

            UserBlockRequest blockRequest = new UserBlockRequest();
            blockRequest.setUserId(userId);
            blockRequest.setReason("Violation of terms");
            blockRequest.setPermanent(false);
            blockRequest.setDurationDays(7);

            when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
            when(userRepository.findById(userId)).thenReturn(Optional.of(reportedUser));
            when(userBlockRepository.findActiveBlockByUser(any(User.class))).thenReturn(Optional.empty());
            when(userBlockRepository.save(any(UserBlock.class))).thenReturn(userBlock);
            when(userRepository.save(any(User.class))).thenReturn(reportedUser);
            when(adminActionRepository.save(any(AdminAction.class))).thenReturn(adminAction);

            
            UserBlockDto result = adminService.blockUser(adminId, blockRequest);

            
            assertNotNull(result);
            assertEquals(userBlock.getId(), result.getId());
            assertEquals(reportedUser.getId(), result.getBlockedUserId());
            assertEquals(adminUser.getId(), result.getBlockedById());
            assertEquals(blockRequest.getReason(), result.getReason());
            assertFalse(result.isPermanent());
            assertTrue(result.isActive());
            assertFalse(reportedUser.isEnabled());
            verify(userBlockRepository, times(1)).save(any(UserBlock.class));
            verify(userRepository, times(1)).save(reportedUser);
            verify(adminActionRepository, times(1)).save(any(AdminAction.class));
        }

        @Test
        void shouldThrowExceptionWhenUserAlreadyBlocked() {
            
            UUID adminId = adminUser.getId();
            UUID userId = reportedUser.getId();

            UserBlockRequest blockRequest = new UserBlockRequest();
            blockRequest.setUserId(userId);
            blockRequest.setReason("Violation of terms");
            blockRequest.setPermanent(false);
            blockRequest.setDurationDays(7);

            when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
            when(userRepository.findById(userId)).thenReturn(Optional.of(reportedUser));
            when(userBlockRepository.findActiveBlockByUser(any(User.class))).thenReturn(Optional.of(userBlock));

            
            MeetrulyException exception = assertThrows(MeetrulyException.class, () -> {
                adminService.blockUser(adminId, blockRequest);
            });

            assertEquals("User is already blocked", exception.getMessage());
            verify(userBlockRepository, never()).save(any(UserBlock.class));
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        void shouldUnblockUser() {
            
            UUID adminId = adminUser.getId();
            UUID blockId = userBlock.getId();
            String reason = "Unblocking after review";

            when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
            when(userRepository.findById(reportedUser.getId())).thenReturn(Optional.of(reportedUser));
            when(userBlockRepository.findById(blockId)).thenReturn(Optional.of(userBlock));
            when(userBlockRepository.save(any(UserBlock.class))).thenReturn(userBlock);
            when(userRepository.save(any(User.class))).thenReturn(reportedUser);
            when(adminActionRepository.save(any(AdminAction.class))).thenReturn(adminAction);

            
            adminService.unblockUser(adminId, blockId, reason);

            
            assertFalse(userBlock.isActive());
            assertNotNull(userBlock.getUnblockDate());
            assertEquals(adminUser, userBlock.getUnblockedBy());
            assertEquals(reason, userBlock.getUnblockReason());
            assertTrue(reportedUser.isEnabled());
            verify(userBlockRepository, times(1)).save(userBlock);
            verify(userRepository, times(1)).save(reportedUser);
            verify(adminActionRepository, times(1)).save(any(AdminAction.class));
        }

        @Test
        void shouldThrowExceptionWhenUnblockingInactiveBlock() {
            
            UUID adminId = adminUser.getId();
            UUID blockId = userBlock.getId();
            String reason = "Test unblock";
            userBlock.setActive(false);

            when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
            when(userBlockRepository.findById(blockId)).thenReturn(Optional.of(userBlock));

            
            MeetrulyException exception = assertThrows(MeetrulyException.class, () -> {
                adminService.unblockUser(adminId, blockId, reason);
            });

            assertEquals("Block is already inactive", exception.getMessage());
            verify(userBlockRepository, never()).save(any(UserBlock.class));
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        void shouldCheckIfUserIsBlocked() {
            
            UUID userId = reportedUser.getId();

            when(userRepository.findById(userId)).thenReturn(Optional.of(reportedUser));
            when(userBlockRepository.findActiveBlockByUser(reportedUser)).thenReturn(Optional.of(userBlock));

            
            boolean result = adminService.isUserBlocked(userId);

            
            assertTrue(result);
            verify(userBlockRepository, times(1)).findActiveBlockByUser(reportedUser);
        }

        @Test
        void shouldCheckIfUserIsNotBlocked() {
            
            UUID userId = regularUser.getId();

            when(userRepository.findById(userId)).thenReturn(Optional.of(regularUser));
            when(userBlockRepository.findActiveBlockByUser(regularUser)).thenReturn(Optional.empty());

            
            boolean result = adminService.isUserBlocked(userId);

            
            assertFalse(result);
            verify(userBlockRepository, times(1)).findActiveBlockByUser(regularUser);
        }

        @Test
        void shouldGetActiveBlocks() {
            
            List<UserBlock> activeBlocks = Collections.singletonList(userBlock);
            when(userBlockRepository.findActiveBlocks()).thenReturn(activeBlocks);
            when(userProfileRepository.findByUserId(any(UUID.class))).thenReturn(Optional.empty());

            
            List<UserBlockDto> result = adminService.getActiveBlocks();

            
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(userBlock.getId(), result.get(0).getId());
            assertEquals(reportedUser.getId(), result.get(0).getBlockedUserId());
            assertEquals(adminUser.getId(), result.get(0).getBlockedById());
            verify(userBlockRepository, times(1)).findActiveBlocks();
        }

        @Test
        void shouldGetActiveBlocksWithPagination() {
            
            Pageable pageable = PageRequest.of(0, 10);
            List<UserBlock> activeBlocks = Collections.singletonList(userBlock);
            Page<UserBlock> blockPage = new PageImpl<>(activeBlocks, pageable, activeBlocks.size());

            when(userBlockRepository.findActiveBlocks(pageable)).thenReturn(blockPage);
            when(userProfileRepository.findByUserId(any(UUID.class))).thenReturn(Optional.empty());

            
            Page<UserBlockDto> result = adminService.getActiveBlocks(pageable);

            
            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            assertEquals(userBlock.getId(), result.getContent().get(0).getId());
            verify(userBlockRepository, times(1)).findActiveBlocks(pageable);
        }

        @Test
        void shouldGetBlockById() {
            
            UUID blockId = userBlock.getId();
            when(userBlockRepository.findById(blockId)).thenReturn(Optional.of(userBlock));
            when(userProfileRepository.findByUserId(any(UUID.class))).thenReturn(Optional.empty());

            
            UserBlockDto result = adminService.getBlockById(blockId);

            
            assertNotNull(result);
            assertEquals(blockId, result.getId());
            verify(userBlockRepository, times(1)).findById(blockId);
        }

        @Test
        void shouldThrowExceptionForInvalidBlockId() {
            
            UUID adminId = adminUser.getId();
            UUID invalidBlockId = UUID.randomUUID();
            String reason = "Test unblock";

            when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
            when(userBlockRepository.findById(invalidBlockId)).thenReturn(Optional.empty());

            
            MeetrulyException exception = assertThrows(MeetrulyException.class, () -> {
                adminService.unblockUser(adminId, invalidBlockId, reason);
            });

            assertEquals("Block not found with id: " + invalidBlockId, exception.getMessage());
        }
    }

    @Nested
    class StatisticsTests {

        @Test
        void shouldProcessExpiredBlocks() {
            
            LocalDateTime now = LocalDateTime.now();
            List<UserBlock> expiredBlocks = new ArrayList<>();
            expiredBlocks.add(userBlock);

            when(userBlockRepository.findExpiredBlocks(any(LocalDateTime.class))).thenReturn(expiredBlocks);
            when(userBlockRepository.saveAll(anyList())).thenReturn(expiredBlocks);
            when(userRepository.save(any(User.class))).thenReturn(reportedUser);

            
            adminService.processExpiredBlocks();

            
            assertFalse(userBlock.isActive());
            assertNotNull(userBlock.getUnblockDate());
            assertEquals("Automatic unblock due to expiration", userBlock.getUnblockReason());
            assertTrue(reportedUser.isEnabled());
            verify(userBlockRepository, times(1)).findExpiredBlocks(any(LocalDateTime.class));
            verify(userBlockRepository, times(1)).saveAll(expiredBlocks);
            verify(userRepository, times(1)).save(reportedUser);
        }

        @Test
        void shouldGetDashboardStats() {
            
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime yesterday = now.minusDays(1);
            LocalDateTime firstDayOfMonth = now.withDayOfMonth(1);
            LocalDateTime firstDayOfPreviousMonth = firstDayOfMonth.minusMonths(1);

            when(userRepository.count()).thenReturn(100L);
            when(userRepository.countByApprovedAndProfileCompleted(false, true)).thenReturn(10L);
            when(userRepository.countByEnabledAndApproved(true, true)).thenReturn(80L);
            when(userBlockRepository.countActiveBlocks()).thenReturn(5L);
            when(userRepository.countByCreatedAtAfter(any(LocalDateTime.class))).thenReturn(3L);
            when(matchRepository.count()).thenReturn(50L);
            when(likeRepository.count()).thenReturn(200L);
            when(messageRepository.count()).thenReturn(500L);
            when(profileViewRepository.count()).thenReturn(1000L);
            when(messageRepository.countByCreatedAtAfter(any(LocalDateTime.class))).thenReturn(30L);
            when(likeRepository.countByCreatedAtAfter(any(LocalDateTime.class))).thenReturn(20L);
            when(transactionRepository.calculateTotalRevenue()).thenReturn(5000.0);
            when(transactionRepository.calculateRevenueForPeriod(any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(1000.0).thenReturn(800.0);
            when(subscriptionRepository.countActiveByPlan(any(SubscriptionPlan.class))).thenReturn(20L);
            when(userReportRepository.countByStatus(ReportStatus.PENDING)).thenReturn(15L);
            when(userReportRepository.count()).thenReturn(50L);

            List<Object[]> reportCounts = new ArrayList<>();
            reportCounts.add(new Object[] {UserReport.ReportType.INAPPROPRIATE_CONTENT, 10L});
            reportCounts.add(new Object[] {UserReport.ReportType.HARASSMENT, 15L});
            when(userReportRepository.countByReportType()).thenReturn(reportCounts);

            Page<UserReport> emptyReportsPage = new PageImpl<>(Collections.emptyList());
            when(userReportRepository.findByStatus(eq(ReportStatus.PENDING), any(Pageable.class)))
                    .thenReturn(emptyReportsPage);

            Page<User> emptyUsersPage = new PageImpl<>(Collections.emptyList());
            when(userRepository.findByApprovedAndProfileCompleted(eq(false), eq(true), any(Pageable.class)))
                    .thenReturn(emptyUsersPage);

            
            AdminDashboardDto result = adminService.getDashboardStats();

            
            assertNotNull(result);
            assertEquals(100L, result.getTotalUsers());
            assertEquals(10L, result.getPendingApprovalUsers());
            assertEquals(80L, result.getActiveUsers());
            assertEquals(5L, result.getBlockedUsers());
            assertEquals(50L, result.getTotalMatches());
            assertEquals(200L, result.getTotalLikes());
            assertEquals(500L, result.getTotalMessages());
            assertEquals(1000L, result.getTotalProfileViews());
            assertEquals(5000.0, result.getTotalRevenue());
            assertEquals(25.0, result.getGrowthPercentage(), 0.01);
            assertEquals(15L, result.getPendingReports());
            assertEquals(50L, result.getTotalReports());
            assertNotNull(result.getSubscriptionsByPlan());
            assertNotNull(result.getRevenueByPlan());
            assertNotNull(result.getReportsByType());
            verify(transactionRepository, times(1)).calculateTotalRevenue();
        }

        @Test
        void shouldGetRevenueStats() {
            
            LocalDateTime startDate = LocalDateTime.now().minusMonths(1);
            LocalDateTime endDate = LocalDateTime.now();

            when(transactionRepository.calculateRevenueForPeriod(eq(startDate), eq(endDate))).thenReturn(2000.0);
            when(transactionRepository.calculateRevenueForPeriod(any(), eq(startDate))).thenReturn(1800.0);
            when(transactionRepository.calculateRevenueByPlanForPeriod(any(SubscriptionPlan.class), eq(startDate), eq(endDate)))
                    .thenReturn(1200.0).thenReturn(800.0);
            when(subscriptionRepository.countActiveByPlan(any(SubscriptionPlan.class))).thenReturn(15L);
            when(subscriptionRepository.countActiveSubscriptions()).thenReturn(30L);
            when(userRepository.countByEnabledAndApproved(true, true)).thenReturn(50L);

            
            RevenueStatsDto result = adminService.getRevenueStats(startDate, endDate);

            
            assertNotNull(result);
            assertEquals(startDate, result.getStartDate());
            assertEquals(endDate, result.getEndDate());
            assertEquals(2000.0, result.getTotalRevenue());
            assertEquals(30L, result.getActiveSubscriptions());
            assertEquals(40.0, result.getAverageRevenuePerUser(), 0.01);
            assertNotNull(result.getRevenueByPlan());
            assertNotNull(result.getSubscriptionCountByPlan());
            assertNotNull(result.getRevenueByDay());
            assertNotNull(result.getRevenueByMonth());
            assertEquals(11.11, result.getGrowthPercentage(), 0.01); 
            verify(transactionRepository, times(1)).calculateRevenueForPeriod(startDate, endDate);
        }

        @Test
        void shouldHandleZeroRevenueValues() {
            
            LocalDateTime startDate = LocalDateTime.now().minusMonths(1);
            LocalDateTime endDate = LocalDateTime.now();

            when(transactionRepository.calculateRevenueForPeriod(eq(startDate), eq(endDate))).thenReturn(null);
            when(transactionRepository.calculateRevenueForPeriod(any(), eq(startDate))).thenReturn(null);
            when(transactionRepository.calculateRevenueByPlanForPeriod(any(SubscriptionPlan.class), eq(startDate), eq(endDate)))
                    .thenReturn(null);
            when(subscriptionRepository.countActiveByPlan(any(SubscriptionPlan.class))).thenReturn(0L);
            when(subscriptionRepository.countActiveSubscriptions()).thenReturn(0L);
            when(userRepository.countByEnabledAndApproved(true, true)).thenReturn(0L);

            
            RevenueStatsDto result = adminService.getRevenueStats(startDate, endDate);

            
            assertNotNull(result);
            assertEquals(0.0, result.getTotalRevenue());
            assertEquals(0L, result.getActiveSubscriptions());
            assertEquals(0.0, result.getAverageRevenuePerUser());
            assertEquals(0.0, result.getGrowthPercentage());
            assertNotNull(result.getRevenueByPlan());
            assertNotNull(result.getSubscriptionCountByPlan());
            assertNotNull(result.getRevenueByDay());
            assertNotNull(result.getRevenueByMonth());
        }
    }
}