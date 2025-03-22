package com.meetruly.user.dto;

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
    private boolean blurredImage;

    public String getDisplayImageUrl() {
        if (blurredImage) {

            return "/images/blurred-profile.jpg";
        } else {
            return profileImageUrl != null ? profileImageUrl : "/images/default-profile.jpg";
        }
    }
}