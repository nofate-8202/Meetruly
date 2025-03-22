package com.meetruly.matching.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchingSummaryDto {

    private long totalLikesReceived;
    private long totalLikesGiven;
    private long totalMatches;
    private long unviewedLikes;
    private long unviewedMatches;
    private long totalProfileViews;

    private List<ProfileCardDto> dailySuggestions;
    private List<ProfileCardDto> topLikedUsers;
}