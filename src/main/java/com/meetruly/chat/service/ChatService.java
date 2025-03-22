package com.meetruly.chat.service;

import com.meetruly.chat.dto.*;
import com.meetruly.chat.model.ChatRoom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ChatService {


    ChatRoom getChatRoomByUsers(UUID user1Id, UUID user2Id);

    List<ChatRoomDto> getChatRoomsByUser(UUID userId);

    Page<ChatRoomDto> getChatRoomsByUser(UUID userId, Pageable pageable);

    ChatRoomDto getChatRoomById(UUID chatRoomId, UUID currentUserId);


    MessageDto sendMessage(UUID senderId, ChatMessageRequest messageRequest);

    Page<MessageDto> getMessagesByChatRoom(UUID chatRoomId, UUID currentUserId, Pageable pageable);

    void markMessagesAsRead(UUID chatRoomId, UUID userId);

    void deleteMessage(UUID messageId, UUID userId);

    long countUnreadMessages(UUID userId);

    void updateTypingStatus(UUID userId, TypingStatusRequest typingStatusRequest);

    List<UUID> getTypingUsersInChatRoom(UUID chatRoomId, UUID currentUserId);

    boolean canSendMessage(UUID userId);

    int getRemainingMessages(UUID userId);

    ChatSummaryDto getChatSummary(UUID userId);
}