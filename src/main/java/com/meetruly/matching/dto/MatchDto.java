package com.meetruly.matching.dto;

import com.meetruly.matching.model.Match;
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
public class MatchDto {

    private UUID id;
    private UUID user1Id;
    private String user1Username;
    private String user1ProfileImageUrl;
    private UUID user2Id;
    private String user2Username;
    private String user2ProfileImageUrl;
    private double compatibilityScore;
    private Match.MatchType matchType;
    private boolean user1Viewed;
    private boolean user2Viewed;
    private LocalDateTime createdAt;

    private UUID otherUserId;
    private String otherUsername;
    private String otherProfileImageUrl;
    private boolean viewed;
}