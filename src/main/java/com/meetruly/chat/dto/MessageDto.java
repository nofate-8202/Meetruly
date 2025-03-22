package com.meetruly.chat.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor

@Builder
public class MessageDto {

    private UUID id;
    private UUID chatRoomId;
    private UUID senderId;
    private String senderUsername;
    private String senderProfileImageUrl;
    private UUID recipientId;
    private String recipientUsername;
    private String recipientProfileImageUrl;
    private String content;
    private boolean read;
    private boolean deleted;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;
    private LocalDateTime deletedAt;

    private boolean fromCurrentUser;

    @Builder(builderMethodName = "messageBuilder")
    public MessageDto(UUID id, UUID chatRoomId, UUID senderId, String senderUsername, String senderProfileImageUrl,
                      UUID recipientId, String recipientUsername, String recipientProfileImageUrl, String content,
                      boolean read, boolean deleted, LocalDateTime sentAt, LocalDateTime readAt, LocalDateTime deletedAt,
                      boolean fromCurrentUser) {
        this.id = id;
        this.chatRoomId = chatRoomId;
        this.senderId = senderId;
        this.senderUsername = senderUsername;
        this.senderProfileImageUrl = senderProfileImageUrl;
        this.recipientId = recipientId;
        this.recipientUsername = recipientUsername;
        this.recipientProfileImageUrl = recipientProfileImageUrl;
        this.content = content;
        this.read = read;
        this.deleted = deleted;
        this.sentAt = sentAt;
        this.readAt = readAt;
        this.deletedAt = deletedAt;
        this.fromCurrentUser = fromCurrentUser;
    }
}