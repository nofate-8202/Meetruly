package com.meetruly.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchProfileResponse {

    private List<ProfileCardDto> profiles;
    private int currentPage;
    private int totalPages;
    private long totalElements;
}