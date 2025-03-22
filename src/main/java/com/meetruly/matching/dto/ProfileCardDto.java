package com.meetruly.matching.dto;

import com.meetruly.core.constant.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileCardDto {

    private UUID userId;
    private String username;
    private String profileImageUrl;
    private Integer age;
    private String city;
    private Gender gender;
    private double compatibilityScore;
    private boolean isMatch;
    private boolean isLiked;
}