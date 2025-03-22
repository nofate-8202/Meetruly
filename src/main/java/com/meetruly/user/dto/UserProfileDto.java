package com.meetruly.user.dto;

import com.meetruly.core.constant.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileDto {

    private UUID userId;

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;

    @NotNull(message = "Age is required")
    @Min(value = 18, message = "Age must be at least 18")
    @Max(value = 120, message = "Age must be at most 120")
    private Integer age;

    private EyeColor eyeColor;

    private HairColor hairColor;

    @Min(value = 50, message = "Height must be at least 50 cm")
    @Max(value = 250, message = "Height must be at most 250 cm")
    private Integer height;

    @Min(value = 30, message = "Weight must be at least 30 kg")
    @Max(value = 300, message = "Weight must be at most 300 kg")
    private Integer weight;

    private Set<Interest> interests;

    @Size(max = 1000, message = "Partner preferences must be at most 1000 characters")
    private String partnerPreferences;

    private RelationshipType relationshipType;

    private RelationshipStatus relationshipStatus;

    private Country country;

    private City city;

    private String profileImageUrl;
}