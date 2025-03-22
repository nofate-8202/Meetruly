package com.meetruly.user.dto;

import com.meetruly.core.constant.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchProfileRequest {

    private Gender gender;

    private Integer minAge;

    private Integer maxAge;

    private EyeColor eyeColor;

    private HairColor hairColor;

    private Integer minHeight;

    private Integer maxHeight;

    private Integer minWeight;

    private Integer maxWeight;

    private Set<Interest> interests;

    private RelationshipType relationshipType;

    private RelationshipStatus relationshipStatus;

    private Country country;

    private City city;

    private boolean sortByInterestMatch;

    private int page = 0;
    private int size = 10;
}