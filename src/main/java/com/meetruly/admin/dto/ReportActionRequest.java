package com.meetruly.admin.dto;

import com.meetruly.core.constant.ReportStatus;
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
public class ReportActionRequest {

    @NotNull(message = "Report ID is required")
    private UUID reportId;

    @NotNull(message = "Status is required")
    private ReportStatus status;

    @NotBlank(message = "Admin notes are required")
    private String adminNotes;

    private boolean blockUser;

    private int blockDurationDays;

    private boolean permanentBlock;

    private String blockReason;
}