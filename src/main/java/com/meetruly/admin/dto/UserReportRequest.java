package com.meetruly.admin.dto;

import com.meetruly.admin.model.UserReport;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserReportRequest {

    @NotNull(message = "Reported user ID is required")
    private UUID reportedUserId;

    @NotNull(message = "Report type is required")
    private UserReport.ReportType reportType;

    @NotBlank(message = "Report reason is required")
    private String reportReason;
}