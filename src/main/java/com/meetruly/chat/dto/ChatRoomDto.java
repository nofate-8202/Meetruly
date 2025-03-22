package com.meetruly.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomDto {

    private UUID id;
    private UUID user1Id;
    private String user1Username;
    private String user1ProfileImageUrl;
    private UUID user2Id;
    private String user2Username;
    private String user2ProfileImageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean hasUnreadMessages;
    private long unreadMessagesCount;
    private MessageDto latestMessage;
    private boolean someoneTyping;

    private UUID currentUserId;

    public UUID getOtherUserId() {
        if (user1Id.equals(currentUserId)) {
            return user2Id;
        } else {
            return user1Id;
        }
    }

    public String getOtherUsername() {
        if (user1Id.equals(currentUserId)) {
            return user2Username;
        } else {
            return user1Username;
        }
    }

    public String getOtherProfileImageUrl() {
        if (user1Id.equals(currentUserId)) {
            return user2ProfileImageUrl;
        } else {
            return user1ProfileImageUrl;
        }
    }
}