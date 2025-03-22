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
public class LikeDto {

    private UUID id;
    private UUID likerId;
    private String likerUsername;
    private String likerProfileImageUrl;
    private UUID likedId;
    private String likedUsername;
    private String likedProfileImageUrl;
    private boolean viewed;
    private LocalDateTime createdAt;
}