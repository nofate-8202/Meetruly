package com.meetruly.admin.repository;

import com.meetruly.admin.model.UserReport;
import com.meetruly.admin.model.UserReport.ReportType;
import com.meetruly.core.constant.ReportStatus;
import com.meetruly.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class UserReportRepositoryTest {

    @Autowired
    private UserReportRepository userReportRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User admin;
    private User reporter;
    private User reportedUser;
    private LocalDateTime baseTime;

    @BeforeEach
    public void setup() {
        
        admin = new User();
        admin.setUsername("admin1");
        admin.setEmail("admin1@example.com");
        admin.setPassword("password");
        admin.setGender(com.meetruly.core.constant.Gender.MALE);
        admin.setRole(com.meetruly.core.constant.UserRole.ADMIN); 
        admin.setApproved(true);
        admin.setEnabled(true);
        admin.setEmailVerified(true);
        admin.setProfileCompleted(true);
        admin.setAccountNonLocked(true);
        admin.setCreatedAt(LocalDateTime.now());
        admin.setUpdatedAt(LocalDateTime.now());
        entityManager.persist(admin);

        reporter = new User();
        reporter.setUsername("reporter");
        reporter.setEmail("reporter@example.com");
        reporter.setPassword("password");
        reporter.setGender(com.meetruly.core.constant.Gender.FEMALE);
        reporter.setRole(com.meetruly.core.constant.UserRole.USER); 
        reporter.setApproved(true);
        reporter.setEnabled(true);
        reporter.setEmailVerified(true);
        reporter.setProfileCompleted(false);
        reporter.setAccountNonLocked(true);
        reporter.setCreatedAt(LocalDateTime.now());
        reporter.setUpdatedAt(LocalDateTime.now());
        entityManager.persist(reporter);

        reportedUser = new User();
        reportedUser.setUsername("reported");
        reportedUser.setEmail("reported@example.com");
        reportedUser.setPassword("password");
        reportedUser.setGender(com.meetruly.core.constant.Gender.OTHER);
        reportedUser.setRole(com.meetruly.core.constant.UserRole.USER); 
        reportedUser.setApproved(true);
        reportedUser.setEnabled(true);
        reportedUser.setEmailVerified(true);
        reportedUser.setProfileCompleted(false);
        reportedUser.setAccountNonLocked(true);
        reportedUser.setCreatedAt(LocalDateTime.now());
        reportedUser.setUpdatedAt(LocalDateTime.now());
        entityManager.persist(reportedUser);

        baseTime = LocalDateTime.of(2023, 1, 1, 0, 0);

        
        UserReport report1 = UserReport.builder()
                .reporter(reporter)
                .reportedUser(reportedUser)
                .reportType(ReportType.INAPPROPRIATE_CONTENT)
                .reportReason("Posting offensive content")
                .status(ReportStatus.PENDING)
                .createdAt(baseTime)
                .build();
        entityManager.persist(report1);

        
        UserReport report2 = UserReport.builder()
                .reporter(reporter)
                .reportedUser(reportedUser)
                .reportType(ReportType.HARASSMENT)
                .reportReason("Sending harassing messages")
                .status(ReportStatus.PENDING)
                .createdAt(baseTime.plusDays(5))
                .build();
        entityManager.persist(report2);

        
        UserReport report3 = UserReport.builder()
                .reporter(reporter)
                .reportedUser(reportedUser)
                .reportType(ReportType.FAKE_PROFILE)
                .reportReason("Fake identity and photos")
                .status(ReportStatus.RESOLVED)
                .adminNotes("Verified fake account")
                .handledBy(admin)
                .handledAt(baseTime.plusDays(2))
                .createdAt(baseTime.minusDays(3))
                .build();
        entityManager.persist(report3);

        
        UserReport report4 = UserReport.builder()
                .reporter(reporter)
                .reportedUser(reportedUser)
                .reportType(ReportType.OTHER)
                .reportReason("Suspicious behavior")
                .status(ReportStatus.DISMISSED)
                .adminNotes("No evidence found")
                .handledBy(admin)
                .handledAt(baseTime.plusDays(3))
                .createdAt(baseTime.minusDays(5))
                .build();
        entityManager.persist(report4);

        entityManager.flush();
    }

    @Test
    public void testSaveUserReport() {
        
        UserReport userReport = UserReport.builder()
                .reporter(reporter)
                .reportedUser(reportedUser)
                .reportType(ReportType.UNDERAGE_USER)
                .reportReason("User appears to be underage")
                .build();

        
        userReport.prePersist();

        
        UserReport savedReport = userReportRepository.save(userReport);

        
        assertNotNull(savedReport.getId());
        assertEquals(reporter.getId(), savedReport.getReporter().getId());
        assertEquals(reportedUser.getId(), savedReport.getReportedUser().getId());
        assertEquals(ReportType.UNDERAGE_USER, savedReport.getReportType());
        assertEquals("User appears to be underage", savedReport.getReportReason());
        assertEquals(ReportStatus.PENDING, savedReport.getStatus());
        assertNotNull(savedReport.getCreatedAt());
    }

    @Test
    public void testFindByReporter() {
        
        List<UserReport> reports = userReportRepository.findByReporter(reporter);

        
        assertEquals(4, reports.size());
        reports.forEach(report -> assertEquals(reporter.getId(), report.getReporter().getId()));
    }

    @Test
    public void testFindByReportedUser() {
        
        List<UserReport> reports = userReportRepository.findByReportedUser(reportedUser);

        
        assertEquals(4, reports.size());
        reports.forEach(report -> assertEquals(reportedUser.getId(), report.getReportedUser().getId()));
    }

    @Test
    public void testFindByStatus() {
        
        ReportStatus status = ReportStatus.PENDING;
        PageRequest pageRequest = PageRequest.of(0, 5);

        
        Page<UserReport> reportsPage = userReportRepository.findByStatus(status, pageRequest);

        
        assertEquals(2, reportsPage.getContent().size());
        reportsPage.forEach(report -> assertEquals(status, report.getStatus()));
    }

    @Test
    public void testFindPendingReports() {
        
        List<UserReport> pendingReports = userReportRepository.findPendingReports();

        
        assertEquals(2, pendingReports.size());
        pendingReports.forEach(report -> assertEquals(ReportStatus.PENDING, report.getStatus()));
    }

    @Test
    public void testFindPendingReportsWithPagination() {
        
        PageRequest pageRequest = PageRequest.of(0, 1);

        
        Page<UserReport> pendingReportsPage = userReportRepository.findPendingReports(pageRequest);

        
        assertEquals(1, pendingReportsPage.getContent().size());
        assertEquals(2, pendingReportsPage.getTotalElements());
        pendingReportsPage.forEach(report -> assertEquals(ReportStatus.PENDING, report.getStatus()));
    }

    @Test
    public void testFindByDateRange() {
        
        LocalDateTime startDate = baseTime.minusDays(1);
        LocalDateTime endDate = baseTime.plusDays(6);

        
        List<UserReport> reports = userReportRepository.findByDateRange(startDate, endDate);

        
        assertEquals(2, reports.size()); 
        reports.forEach(report -> {
            assertTrue(report.getCreatedAt().isAfter(startDate) || report.getCreatedAt().isEqual(startDate));
            assertTrue(report.getCreatedAt().isBefore(endDate) || report.getCreatedAt().isEqual(endDate));
        });
    }

    @Test
    public void testFindByReportType() {
        
        ReportType reportType = ReportType.INAPPROPRIATE_CONTENT;

        
        List<UserReport> reports = userReportRepository.findByReportType(reportType);

        
        assertEquals(1, reports.size());
        reports.forEach(report -> assertEquals(reportType, report.getReportType()));
    }

    @Test
    public void testCountReportsSince() {
        
        LocalDateTime since = baseTime.minusDays(2);

        
        long count = userReportRepository.countReportsSince(since);

        
        assertEquals(3, count); 
    }

    @Test
    public void testCountByStatus() {
        
        ReportStatus status = ReportStatus.PENDING;

        
        long count = userReportRepository.countByStatus(status);

        
        assertEquals(2, count);
    }

    @Test
    public void testCountByReportType() {
        
        List<Object[]> results = userReportRepository.countByReportType();

        
        assertEquals(4, results.size()); 
        for (Object[] result : results) {
            assertTrue(result[0] instanceof ReportType);
            assertTrue(result[1] instanceof Long);
            assertEquals(1L, (Long)result[1]);
        }
    }

    @Test
    public void testPrePersistMethod() {
        
        UserReport userReport = new UserReport();
        userReport.setReporter(reporter);
        userReport.setReportedUser(reportedUser);
        userReport.setReportType(ReportType.HARASSMENT);
        userReport.setReportReason("Harassing messages");

        
        userReport.prePersist();

        
        assertEquals(ReportStatus.PENDING, userReport.getStatus());
        assertNotNull(userReport.getCreatedAt());
    }
}
