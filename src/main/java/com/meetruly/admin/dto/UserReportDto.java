package com.meetruly.admin.dto;

import com.meetruly.admin.model.UserReport;
import com.meetruly.core.constant.ReportStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserReportDto {

    private UUID id;
    private UUID reporterId;
    private String reporterUsername;
    private String reporterProfileImageUrl;
    private UUID reportedUserId;
    private String reportedUsername;
    private String reportedProfileImageUrl;
    private UserReport.ReportType reportType;
    private String reportReason;
    private ReportStatus status;
    private String adminNotes;
    private UUID handledById;
    private String handledByUsername;
    private LocalDateTime handledAt;
    private LocalDateTime createdAt;
}