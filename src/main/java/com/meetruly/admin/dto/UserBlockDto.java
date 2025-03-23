package com.meetruly.admin.dto;

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
public class UserBlockDto {

    private UUID id;
    private UUID blockedUserId;
    private String blockedUsername;
    private String blockedProfileImageUrl;
    private UUID blockedById;
    private String blockedByUsername;
    private String reason;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private boolean permanent;
    private boolean active;
    private LocalDateTime unblockDate;
    private UUID unblockedById;
    private String unblockedByUsername;
    private String unblockReason;
    private boolean expired;
    private long remainingDays;
}