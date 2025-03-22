package com.meetruly.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TypingNotificationDto {
    private UUID chatRoomId;
    private UUID userId;
    private boolean typing;
}