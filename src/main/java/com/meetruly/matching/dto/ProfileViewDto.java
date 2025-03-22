package com.meetruly.matching.dto;

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
public class ProfileViewDto {

    private UUID id;
    private UUID viewerId;
    private String viewerUsername;
    private String viewerProfileImageUrl;
    private UUID viewedId;
    private String viewedUsername;
    private String viewedProfileImageUrl;
    private LocalDateTime viewedAt;
    private boolean notificationSent;
}