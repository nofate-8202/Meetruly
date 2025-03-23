package com.meetruly.admin.dto;

import com.meetruly.admin.model.AdminAction;
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
public class AdminActionDto {

    private UUID id;
    private UUID adminId;
    private String adminUsername;
    private UUID targetUserId;
    private String targetUsername;
    private AdminAction.ActionType actionType;
    private String actionDetails;
    private LocalDateTime performedAt;
    private String ipAddress;
}