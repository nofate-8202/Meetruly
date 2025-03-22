package com.meetruly.chat.dto;

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
public class TypingStatusRequest {

    @NotNull(message = "Chat room ID is required")
    private UUID chatRoomId;

    @NotNull(message = "Typing status is required")
    private boolean typing;
}