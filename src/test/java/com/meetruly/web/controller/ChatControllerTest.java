package com.meetruly.web.controller;

import com.meetruly.chat.dto.*;
import com.meetruly.chat.model.ChatRoom;
import com.meetruly.chat.service.ChatService;
import com.meetruly.core.exception.MeetrulyException;
import com.meetruly.user.model.User;
import com.meetruly.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.ui.Model;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private ChatService chatService;

    @Mock
    private UserService userService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private Model model;

    @Mock
    private Principal principal;

    @InjectMocks
    private ChatController chatController;

    private User testUser;
    private User otherUser;
    private ChatRoom chatRoom;
    private UUID chatRoomId;
    private UUID userId;
    private UUID otherUserId;
    private ChatSummaryDto chatSummary;
    private ChatRoomDto chatRoomDto;
    private MessageDto messageDto;
    private List<MessageDto> messages;
    private Page<MessageDto> messagePage;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();
        chatRoomId = UUID.randomUUID();

        testUser = new User();
        testUser.setId(userId);
        testUser.setUsername("testuser");

        otherUser = new User();
        otherUser.setId(otherUserId);
        otherUser.setUsername("otheruser");

        chatRoom = new ChatRoom();
        chatRoom.setId(chatRoomId);
        chatRoom.setUser1(testUser);
        chatRoom.setUser2(otherUser);
        chatRoom.setCreatedAt(LocalDateTime.now());
        chatRoom.setUpdatedAt(LocalDateTime.now());

        chatRoomDto = ChatRoomDto.builder()
                .id(chatRoomId)
                .user1Id(userId)
                .user1Username("testuser")
                .user1ProfileImageUrl("/images/default-profile.jpg")
                .user2Id(otherUserId)
                .user2Username("otheruser")
                .user2ProfileImageUrl("/images/default-profile.jpg")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .hasUnreadMessages(false)
                .unreadMessagesCount(0)
                .someoneTyping(false)
                .currentUserId(userId)
                .build();

        messageDto = MessageDto.messageBuilder()
                .id(UUID.randomUUID())
                .chatRoomId(chatRoomId)
                .senderId(userId)
                .senderUsername("testuser")
                .senderProfileImageUrl("/images/default-profile.jpg")
                .recipientId(otherUserId)
                .recipientUsername("otheruser")
                .recipientProfileImageUrl("/images/default-profile.jpg")
                .content("Hello world")
                .read(false)
                .deleted(false)
                .sentAt(LocalDateTime.now())
                .fromCurrentUser(true)
                .build();

        messages = new ArrayList<>();
        messages.add(messageDto);

        messagePage = new PageImpl<>(messages);

        chatSummary = ChatSummaryDto.builder()
                .totalChatRooms(1)
                .unreadMessages(0)
                .dailyMessageLimit(15)
                .messagesSentToday(5)
                .remainingMessages(10)
                .recentChatRooms(List.of(chatRoomDto))
                .build();

        when(principal.getName()).thenReturn("testuser");
    }

    @Test
    void showChatDashboard_Success() {
        
        when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(chatService.getChatSummary(userId)).thenReturn(chatSummary);

        
        String viewName = chatController.showChatDashboard(model, principal);

        
        assertEquals("chat/dashboard", viewName);
        verify(model).addAttribute("chatSummary", chatSummary);
        verify(chatService).getChatSummary(userId);
    }

    @Test
    void showChatDashboard_UserNotFound() {
        
        when(userService.getUserByUsername("testuser")).thenReturn(Optional.empty());

        
        String viewName = chatController.showChatDashboard(model, principal);

        
        assertEquals("redirect:/home", viewName);
        verify(model).addAttribute(eq("errorMessage"), anyString());
    }

    @Test
    void showChatRoom_Success() {
        
        when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(chatService.getChatRoomById(chatRoomId, userId)).thenReturn(chatRoomDto);
        when(chatService.getMessagesByChatRoom(eq(chatRoomId), eq(userId), any(PageRequest.class))).thenReturn(messagePage);
        when(chatService.canSendMessage(userId)).thenReturn(true);
        when(chatService.getRemainingMessages(userId)).thenReturn(10);

        
        String viewName = chatController.showChatRoom(chatRoomId, 0, 20, model, principal);

        
        assertEquals("chat/chat-room", viewName);
        verify(chatService).markMessagesAsRead(chatRoomId, userId);
        verify(model).addAttribute("chatRoom", chatRoomDto);
        verify(model).addAttribute("messages", messages);
        verify(model).addAttribute("canSendMessage", true);
        verify(model).addAttribute("remainingMessages", 10);
        verify(model).addAttribute("currentUserId", userId);
    }

    @Test
    void showChatRoom_Exception() {
        
        when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(chatService.getChatRoomById(chatRoomId, userId)).thenThrow(new MeetrulyException("Error"));

        
        String viewName = chatController.showChatRoom(chatRoomId, 0, 20, model, principal);

        
        assertEquals("redirect:/chat", viewName);
        verify(model).addAttribute(eq("errorMessage"), anyString());
    }

    @Test
    void getMessages_Success() {
        
        when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(chatService.getMessagesByChatRoom(eq(chatRoomId), eq(userId), any(PageRequest.class))).thenReturn(messagePage);

        
        ResponseEntity<?> response = chatController.getMessages(chatRoomId, 0, 20, principal);

        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(messagePage, response.getBody());
    }

    @Test
    void getMessages_Error() {
        
        when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(chatService.getMessagesByChatRoom(eq(chatRoomId), eq(userId), any(PageRequest.class)))
                .thenThrow(new MeetrulyException("Error"));

        
        ResponseEntity<?> response = chatController.getMessages(chatRoomId, 0, 20, principal);

        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Error", response.getBody());
    }

    @Test
    void createNewChat_Success() {
        
        when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userService.getUserById(otherUserId)).thenReturn(otherUser);
        when(chatService.getChatRoomByUsers(userId, otherUserId)).thenReturn(chatRoom);

        
        String viewName = chatController.createNewChat(otherUserId, model, principal);

        
        assertEquals("redirect:/chat/" + chatRoomId, viewName);
    }

    @Test
    void createNewChat_WithSelf() {
        
        when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(testUser));

        
        String viewName = chatController.createNewChat(userId, model, principal);

        
        assertEquals("redirect:/chat", viewName);
        verify(model).addAttribute("errorMessage", "You cannot chat with yourself");
    }

    @Test
    void createNewChat_Error() {
        
        when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userService.getUserById(otherUserId)).thenReturn(otherUser);
        when(chatService.getChatRoomByUsers(userId, otherUserId)).thenReturn(null);

        
        String viewName = chatController.createNewChat(otherUserId, model, principal);

        
        assertEquals("redirect:/chat", viewName);
        verify(model).addAttribute(eq("errorMessage"), anyString());
    }

    @Test
    void sendMessage_Success() {
        
        ChatMessageRequest messageRequest = new ChatMessageRequest(otherUserId, "Hello", chatRoomId);

        when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(chatService.canSendMessage(userId)).thenReturn(true);
        when(chatService.sendMessage(userId, messageRequest)).thenReturn(messageDto);

        
        ResponseEntity<?> response = chatController.sendMessage(messageRequest, principal);

        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(messageDto, response.getBody());
        verify(messagingTemplate).convertAndSendToUser(
                messageDto.getRecipientUsername(),
                "/topic/messages",
                messageDto
        );
    }

    @Test
    void sendMessage_LimitReached() {
        
        ChatMessageRequest messageRequest = new ChatMessageRequest(otherUserId, "Hello", chatRoomId);

        when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(chatService.canSendMessage(userId)).thenReturn(false);

        
        ResponseEntity<?> response = chatController.sendMessage(messageRequest, principal);

        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("limit"));
        verify(chatService, never()).sendMessage(any(), any());
    }

    @Test
    void handleMessage_Success() {
        
        ChatMessageRequest messageRequest = new ChatMessageRequest(otherUserId, "Hello", chatRoomId);

        when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(chatService.canSendMessage(userId)).thenReturn(true);
        when(chatService.sendMessage(userId, messageRequest)).thenReturn(messageDto);

        
        chatController.handleMessage(messageRequest, principal);

        
        verify(messagingTemplate).convertAndSendToUser(
                messageDto.getRecipientUsername(),
                "/topic/messages",
                messageDto
        );
        verify(messagingTemplate).convertAndSendToUser(
                "testuser",
                "/topic/messages.sent",
                messageDto
        );
    }

    @Test
    void handleMessage_LimitReached() {
        
        ChatMessageRequest messageRequest = new ChatMessageRequest(otherUserId, "Hello", chatRoomId);

        when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(chatService.canSendMessage(userId)).thenReturn(false);

        
        chatController.handleMessage(messageRequest, principal);

        
        verify(messagingTemplate).convertAndSendToUser(
                eq("testuser"),
                eq("/topic/error"),
                anyString()
        );
        verify(chatService, never()).sendMessage(any(), any());
    }

    @Test
    void handleTyping_Success() {
        
        TypingStatusRequest typingRequest = new TypingStatusRequest(chatRoomId, true);

        when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(chatService.getChatRoomById(chatRoomId, userId)).thenReturn(chatRoomDto);

        
        chatController.handleTyping(typingRequest, principal);

        
        verify(chatService).updateTypingStatus(userId, typingRequest);
        verify(messagingTemplate).convertAndSendToUser(
                eq(chatRoomDto.getOtherUsername()),
                eq("/topic/typing"),
                any(TypingNotificationDto.class)
        );
    }

    @Test
    void markMessagesAsRead_Success() {
        
        when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(testUser));

        
        ResponseEntity<?> response = chatController.markMessagesAsRead(chatRoomId, principal);

        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(chatService).markMessagesAsRead(chatRoomId, userId);
    }

    @Test
    void markMessagesAsRead_Error() {
        
        when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(testUser));
        doThrow(new MeetrulyException("Error")).when(chatService).markMessagesAsRead(chatRoomId, userId);

        
        ResponseEntity<?> response = chatController.markMessagesAsRead(chatRoomId, principal);

        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Error", response.getBody());
    }

    @Test
    void deleteMessage_Success() {
        
        UUID messageId = UUID.randomUUID();
        when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(testUser));

        
        ResponseEntity<?> response = chatController.deleteMessage(messageId, principal);

        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(chatService).deleteMessage(messageId, userId);
    }

    @Test
    void deleteMessage_Error() {
        
        UUID messageId = UUID.randomUUID();
        when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(testUser));
        doThrow(new MeetrulyException("Error")).when(chatService).deleteMessage(messageId, userId);

        
        ResponseEntity<?> response = chatController.deleteMessage(messageId, principal);

        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Error", response.getBody());
    }

    @Test
    void getTypingUsers_Success() {
        
        List<UUID> typingUsers = List.of(otherUserId);
        when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(chatService.getTypingUsersInChatRoom(chatRoomId, userId)).thenReturn(typingUsers);

        
        ResponseEntity<?> response = chatController.getTypingUsers(chatRoomId, principal);

        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(typingUsers, response.getBody());
    }

    @Test
    void getTypingUsers_Error() {
        
        when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(chatService.getTypingUsersInChatRoom(chatRoomId, userId)).thenThrow(new MeetrulyException("Error"));

        
        ResponseEntity<?> response = chatController.getTypingUsers(chatRoomId, principal);

        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Error", response.getBody());
    }
}