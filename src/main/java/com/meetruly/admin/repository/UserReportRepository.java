package com.meetruly.admin.repository;

import com.meetruly.admin.model.UserReport;
import com.meetruly.core.constant.ReportStatus;
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
public interface UserReportRepository extends JpaRepository<UserReport, UUID> {

    List<UserReport> findByReporter(User reporter);

    List<UserReport> findByReportedUser(User reportedUser);

    Page<UserReport> findByStatus(ReportStatus status, Pageable pageable);

    @Query("SELECT r FROM UserReport r WHERE r.status = 'PENDING'")
    List<UserReport> findPendingReports();

    @Query("SELECT r FROM UserReport r WHERE r.status = 'PENDING'")
    Page<UserReport> findPendingReports(Pageable pageable);

    @Query("SELECT r FROM UserReport r WHERE r.createdAt BETWEEN :startDate AND :endDate")
    List<UserReport> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT r FROM UserReport r WHERE r.reportType = :reportType")
    List<UserReport> findByReportType(@Param("reportType") UserReport.ReportType reportType);

    @Query("SELECT COUNT(r) FROM UserReport r WHERE r.createdAt >= :since")
    long countReportsSince(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(r) FROM UserReport r WHERE r.status = :status")
    long countByStatus(@Param("status") ReportStatus status);

    @Query("SELECT r.reportType, COUNT(r) FROM UserReport r GROUP BY r.reportType")
    List<Object[]> countByReportType();
}