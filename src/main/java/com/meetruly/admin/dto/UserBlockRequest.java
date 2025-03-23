package com.meetruly.admin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBlockRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotBlank(message = "Reason is required")
    private String reason;

    private boolean permanent;

    @Min(value = 1, message = "Duration must be at least 1 day")
    private int durationDays;
}