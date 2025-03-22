package com.meetruly.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatSummaryDto {

    private long totalChatRooms;
    private long unreadMessages;
    private int dailyMessageLimit;
    private int messagesSentToday;
    private int remainingMessages;
    private List<ChatRoomDto> recentChatRooms;
}