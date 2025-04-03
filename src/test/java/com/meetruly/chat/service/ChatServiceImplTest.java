package com.meetruly.chat.service;

import com.meetruly.chat.dto.*;
import com.meetruly.chat.model.ChatRoom;
import com.meetruly.chat.model.Message;
import com.meetruly.chat.model.UserTypingStatus;
import com.meetruly.chat.repository.ChatRoomRepository;
import com.meetruly.chat.repository.MessageRepository;
import com.meetruly.chat.repository.UserTypingStatusRepository;
import com.meetruly.chat.service.impl.ChatServiceImpl;
import com.meetruly.core.constant.SubscriptionPlan;
import com.meetruly.core.exception.MeetrulyException;
import com.meetruly.subscription.dto.SubscriptionDto;
import com.meetruly.subscription.service.SubscriptionService;
import com.meetruly.user.model.User;
import com.meetruly.user.model.UserProfile;
import com.meetruly.user.repository.UserProfileRepository;
import com.meetruly.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ChatServiceImplTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserTypingStatusRepository typingStatusRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private SubscriptionService subscriptionService;

    @InjectMocks
    private ChatServiceImpl chatService;

    private User user1;
    private User user2;
    private UserProfile userProfile1;
    private UserProfile userProfile2;
    private ChatRoom chatRoom;
    private Message message;
    private UserTypingStatus typingStatus;

    @BeforeEach
    void setUp() {
        // Create test users
        user1 = new User();
        user1.setId(UUID.randomUUID());
        user1.setUsername("user1");

        user2 = new User();
        user2.setId(UUID.randomUUID());
        user2.setUsername("user2");

        // Create user profiles
        userProfile1 = new UserProfile();
        userProfile1.setId(UUID.randomUUID());
        userProfile1.setUser(user1);
        userProfile1.setProfileImageUrl("/profiles/user1.jpg");

        userProfile2 = new UserProfile();
        userProfile2.setId(UUID.randomUUID());
        userProfile2.setUser(user2);
        userProfile2.setProfileImageUrl("/profiles/user2.jpg");

        // Create chat room
        chatRoom = new ChatRoom();
        chatRoom.setId(UUID.randomUUID());
        chatRoom.setUser1(user1);
        chatRoom.setUser2(user2);
        chatRoom.setCreatedAt(LocalDateTime.now().minusDays(1));
        chatRoom.setUpdatedAt(LocalDateTime.now().minusHours(1));

        // Create message
        message = new Message();
        message.setId(UUID.randomUUID());
        message.setChatRoom(chatRoom);
        message.setSender(user1);
        message.setRecipient(user2);
        message.setContent("Hello, user2!");
        message.setRead(false);
        message.setDeleted(false);
        message.setSentAt(LocalDateTime.now().minusHours(1));
        message.setCreatedAt(LocalDateTime.now().minusHours(1));

        // Create typing status
        typingStatus = new UserTypingStatus();
        typingStatus.setId(UUID.randomUUID());
        typingStatus.setUser(user1);
        typingStatus.setChatRoom(chatRoom);
        typingStatus.setTyping(true);
        typingStatus.setLastTypingActivity(LocalDateTime.now());
    }

    @Test
    void getChatRoomByUsers_ExistingChatRoom_ReturnsCorrectChatRoom() {
        // Arrange
        when(userRepository.findById(user1.getId())).thenReturn(Optional.of(user1));
        when(userRepository.findById(user2.getId())).thenReturn(Optional.of(user2));
        when(chatRoomRepository.findByUsers(user1, user2)).thenReturn(Optional.of(chatRoom));

        // Act
        ChatRoom result = chatService.getChatRoomByUsers(user1.getId(), user2.getId());

        // Assert
        assertNotNull(result);
        assertEquals(chatRoom.getId(), result.getId());
        verify(chatRoomRepository, times(1)).findByUsers(user1, user2);
        verify(chatRoomRepository, never()).save(any(ChatRoom.class));
    }

    @Test
    void getChatRoomByUsers_NonExistingChatRoom_CreatesAndReturnsNewChatRoom() {
        // Arrange
        when(userRepository.findById(user1.getId())).thenReturn(Optional.of(user1));
        when(userRepository.findById(user2.getId())).thenReturn(Optional.of(user2));
        when(chatRoomRepository.findByUsers(user1, user2)).thenReturn(Optional.empty());
        when(chatRoomRepository.save(any(ChatRoom.class))).thenAnswer(invocation -> {
            ChatRoom savedChatRoom = invocation.getArgument(0);
            savedChatRoom.setId(UUID.randomUUID());
            return savedChatRoom;
        });

        // Act
        ChatRoom result = chatService.getChatRoomByUsers(user1.getId(), user2.getId());

        // Assert
        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals(user1, result.getUser1());
        assertEquals(user2, result.getUser2());
        verify(chatRoomRepository, times(1)).findByUsers(user1, user2);
        verify(chatRoomRepository, times(1)).save(any(ChatRoom.class));
    }

    @Test
    void getChatRoomByUsers_UserNotFound_ThrowsMeetrulyException() {
        // Arrange
        when(userRepository.findById(user1.getId())).thenReturn(Optional.empty());

        // Act & Assert
        MeetrulyException exception = assertThrows(MeetrulyException.class, () -> {
            chatService.getChatRoomByUsers(user1.getId(), user2.getId());
        });
        assertTrue(exception.getMessage().contains("User 1 not found"));
        verify(chatRoomRepository, never()).findByUsers(any(), any());
        verify(chatRoomRepository, never()).save(any(ChatRoom.class));
    }

    @Test
    void getChatRoomsByUser_ReturnsCorrectList() {
        // Arrange
        List<ChatRoom> chatRooms = List.of(chatRoom);
        when(userRepository.findById(user1.getId())).thenReturn(Optional.of(user1));
        when(chatRoomRepository.findByUser(user1)).thenReturn(chatRooms);
        when(userProfileRepository.findByUserId(any(UUID.class))).thenReturn(Optional.of(userProfile1));
        when(messageRepository.countUnreadMessagesByChatRoomAndUser(any(ChatRoom.class), any(User.class))).thenReturn(0L);
        when(messageRepository.findByChatRoom(any(ChatRoom.class), any(PageRequest.class))).thenReturn(Page.empty());
        when(typingStatusRepository.findActiveTypingUsersByChatRoom(any(ChatRoom.class), any(User.class), any(LocalDateTime.class))).thenReturn(List.of());

        // Act
        List<ChatRoomDto> result = chatService.getChatRoomsByUser(user1.getId());

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(chatRoom.getId(), result.get(0).getId());
        verify(chatRoomRepository, times(1)).findByUser(user1);
    }

    @Test
    void getChatRoomsByUserPaginated_ReturnsCorrectPage() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<ChatRoom> chatRoomPage = new PageImpl<>(List.of(chatRoom));
        when(userRepository.findById(user1.getId())).thenReturn(Optional.of(user1));
        when(chatRoomRepository.findByUser(user1, pageable)).thenReturn(chatRoomPage);
        when(userProfileRepository.findByUserId(any(UUID.class))).thenReturn(Optional.of(userProfile1));
        when(messageRepository.countUnreadMessagesByChatRoomAndUser(any(ChatRoom.class), any(User.class))).thenReturn(0L);
        when(messageRepository.findByChatRoom(any(ChatRoom.class), any(PageRequest.class))).thenReturn(Page.empty());
        when(typingStatusRepository.findActiveTypingUsersByChatRoom(any(ChatRoom.class), any(User.class), any(LocalDateTime.class))).thenReturn(List.of());

        // Act
        Page<ChatRoomDto> result = chatService.getChatRoomsByUser(user1.getId(), pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(chatRoom.getId(), result.getContent().get(0).getId());
        verify(chatRoomRepository, times(1)).findByUser(user1, pageable);
    }

    @Test
    void getChatRoomById_ExistingChatRoom_ReturnsCorrectDto() {
        // Arrange
        when(chatRoomRepository.findById(chatRoom.getId())).thenReturn(Optional.of(chatRoom));
        when(userProfileRepository.findByUserId(any(UUID.class))).thenReturn(Optional.of(userProfile1));
        when(messageRepository.countUnreadMessagesByChatRoomAndUser(any(ChatRoom.class), any(User.class))).thenReturn(0L);
        when(messageRepository.findByChatRoom(any(ChatRoom.class), any(PageRequest.class))).thenReturn(Page.empty());
        when(typingStatusRepository.findActiveTypingUsersByChatRoom(any(ChatRoom.class), any(User.class), any(LocalDateTime.class))).thenReturn(List.of());

        // Act
        ChatRoomDto result = chatService.getChatRoomById(chatRoom.getId(), user1.getId());

        // Assert
        assertNotNull(result);
        assertEquals(chatRoom.getId(), result.getId());
        assertEquals(user1.getId(), result.getUser1Id());
        assertEquals(user2.getId(), result.getUser2Id());
        verify(chatRoomRepository, times(1)).findById(chatRoom.getId());
    }

    @Test
    void getChatRoomById_ChatRoomNotFound_ThrowsMeetrulyException() {
        // Arrange
        when(chatRoomRepository.findById(chatRoom.getId())).thenReturn(Optional.empty());

        // Act & Assert
        MeetrulyException exception = assertThrows(MeetrulyException.class, () -> {
            chatService.getChatRoomById(chatRoom.getId(), user1.getId());
        });
        assertTrue(exception.getMessage().contains("Chat room not found"));
        verify(chatRoomRepository, times(1)).findById(chatRoom.getId());
    }

    @Test
    void getChatRoomById_UserNotInChatRoom_ThrowsMeetrulyException() {
        // Arrange
        UUID nonMemberUserId = UUID.randomUUID();
        when(chatRoomRepository.findById(chatRoom.getId())).thenReturn(Optional.of(chatRoom));

        // Act & Assert
        MeetrulyException exception = assertThrows(MeetrulyException.class, () -> {
            chatService.getChatRoomById(chatRoom.getId(), nonMemberUserId);
        });
        assertTrue(exception.getMessage().contains("User is not part of this chat room"));
        verify(chatRoomRepository, times(1)).findById(chatRoom.getId());
    }

    @Test
    void sendMessage_ValidRequest_CreatesAndReturnsMessage() {
        // Arrange
        ChatMessageRequest request = ChatMessageRequest.builder()
                .recipientId(user2.getId())
                .content("Hello, user2!")
                .chatRoomId(chatRoom.getId())
                .build();

        when(userRepository.findById(user1.getId())).thenReturn(Optional.of(user1));
        when(userRepository.findById(user2.getId())).thenReturn(Optional.of(user2));
        when(chatRoomRepository.findById(chatRoom.getId())).thenReturn(Optional.of(chatRoom));
        when(messageRepository.save(any(Message.class))).thenReturn(message);
        when(subscriptionService.canSendMessage(user1.getId())).thenReturn(true);
        when(userProfileRepository.findByUserId(any(UUID.class))).thenReturn(Optional.of(userProfile1));

        // Act
        MessageDto result = chatService.sendMessage(user1.getId(), request);

        // Assert
        assertNotNull(result);
        assertEquals(message.getId(), result.getId());
        assertEquals(message.getContent(), result.getContent());
        assertEquals(user1.getId(), result.getSenderId());
        assertEquals(user2.getId(), result.getRecipientId());
        verify(messageRepository, times(1)).save(any(Message.class));
        verify(subscriptionService, times(1)).incrementMessageCount(user1.getId());
    }

    @Test
    void sendMessage_MessageLimitReached_ThrowsMeetrulyException() {
        // Arrange
        ChatMessageRequest request = ChatMessageRequest.builder()
                .recipientId(user2.getId())
                .content("Hello, user2!")
                .chatRoomId(chatRoom.getId())
                .build();

        when(subscriptionService.canSendMessage(user1.getId())).thenReturn(false);

        // Act & Assert
        MeetrulyException exception = assertThrows(MeetrulyException.class, () -> {
            chatService.sendMessage(user1.getId(), request);
        });
        assertTrue(exception.getMessage().contains("reached your daily message limit"));
        verify(messageRepository, never()).save(any(Message.class));
        verify(subscriptionService, never()).incrementMessageCount(any(UUID.class));
    }

    @Test
    void sendMessage_NoChatRoomId_CreatesNewChatRoomAndSendsMessage() {
        // Arrange
        ChatMessageRequest request = ChatMessageRequest.builder()
                .recipientId(user2.getId())
                .content("Hello, user2!")
                .build();

        when(userRepository.findById(user1.getId())).thenReturn(Optional.of(user1));
        when(userRepository.findById(user2.getId())).thenReturn(Optional.of(user2));
        when(chatRoomRepository.findByUsers(user1, user2)).thenReturn(Optional.of(chatRoom));
        when(messageRepository.save(any(Message.class))).thenReturn(message);
        when(subscriptionService.canSendMessage(user1.getId())).thenReturn(true);
        when(userProfileRepository.findByUserId(any(UUID.class))).thenReturn(Optional.of(userProfile1));

        // Act
        MessageDto result = chatService.sendMessage(user1.getId(), request);

        // Assert
        assertNotNull(result);
        assertEquals(message.getId(), result.getId());
        verify(chatRoomRepository, times(1)).save(chatRoom);
        verify(messageRepository, times(1)).save(any(Message.class));
    }

    @Test
    void getMessagesByChatRoom_ValidRequest_ReturnsMessages() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Message> messagePage = new PageImpl<>(List.of(message));

        when(chatRoomRepository.findById(chatRoom.getId())).thenReturn(Optional.of(chatRoom));
        when(messageRepository.findByChatRoom(chatRoom, pageable)).thenReturn(messagePage);
        when(userProfileRepository.findByUserId(any(UUID.class))).thenReturn(Optional.of(userProfile1));

        // Act
        Page<MessageDto> result = chatService.getMessagesByChatRoom(chatRoom.getId(), user1.getId(), pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(message.getId(), result.getContent().get(0).getId());
        assertEquals(message.getContent(), result.getContent().get(0).getContent());
        verify(messageRepository, times(1)).findByChatRoom(chatRoom, pageable);
    }

    @Test
    void getMessagesByChatRoom_DeletedMessage_SetsDeletedFlag() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        message.setDeletedBySender(true);
        message.markAsDeletedByUser(user1.getId());
        Page<Message> messagePage = new PageImpl<>(List.of(message));

        when(chatRoomRepository.findById(chatRoom.getId())).thenReturn(Optional.of(chatRoom));
        when(messageRepository.findByChatRoom(chatRoom, pageable)).thenReturn(messagePage);
        when(userProfileRepository.findByUserId(any(UUID.class))).thenReturn(Optional.of(userProfile1));

        // Act
        Page<MessageDto> result = chatService.getMessagesByChatRoom(chatRoom.getId(), user1.getId(), pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("This message has been deleted", result.getContent().get(0).getContent());
        assertTrue(result.getContent().get(0).isDeleted());
    }

    @Test
    void markMessagesAsRead_ValidRequest_MarksMessagesAsRead() {
        // Arrange
        List<Message> unreadMessages = List.of(message);

        when(chatRoomRepository.findById(chatRoom.getId())).thenReturn(Optional.of(chatRoom));
        when(userRepository.findById(user2.getId())).thenReturn(Optional.of(user2));
        when(messageRepository.findUnreadMessagesByChatRoomAndUser(chatRoom, user2)).thenReturn(unreadMessages);

        // Act
        chatService.markMessagesAsRead(chatRoom.getId(), user2.getId());

        // Assert
        assertTrue(message.isRead());
        assertNotNull(message.getReadAt());
        verify(messageRepository, times(1)).saveAll(unreadMessages);
    }

    @Test
    void deleteMessage_ValidRequest_MarksMessageAsDeleted() {
        // Arrange
        when(messageRepository.findById(message.getId())).thenReturn(Optional.of(message));

        // Act
        chatService.deleteMessage(message.getId(), user1.getId());

        // Assert
        assertTrue(message.isDeletedBySender());
        verify(messageRepository, times(1)).save(message);
    }

    @Test
    void deleteMessage_MessageNotFound_ThrowsMeetrulyException() {
        // Arrange
        when(messageRepository.findById(message.getId())).thenReturn(Optional.empty());

        // Act & Assert
        MeetrulyException exception = assertThrows(MeetrulyException.class, () -> {
            chatService.deleteMessage(message.getId(), user1.getId());
        });
        assertTrue(exception.getMessage().contains("Message not found"));
        verify(messageRepository, never()).save(any(Message.class));
    }

    @Test
    void deleteMessage_UserNotPartOfMessage_ThrowsMeetrulyException() {
        // Arrange
        UUID nonParticipantUserId = UUID.randomUUID();
        when(messageRepository.findById(message.getId())).thenReturn(Optional.of(message));

        // Act & Assert
        MeetrulyException exception = assertThrows(MeetrulyException.class, () -> {
            chatService.deleteMessage(message.getId(), nonParticipantUserId);
        });
        assertTrue(exception.getMessage().contains("User is not part of this message exchange"));
        verify(messageRepository, never()).save(any(Message.class));
    }

    @Test
    void countUnreadMessages_ReturnsCorrectCount() {
        // Arrange
        when(userRepository.findById(user1.getId())).thenReturn(Optional.of(user1));
        when(messageRepository.countUnreadMessages(user1)).thenReturn(5L);

        // Act
        long result = chatService.countUnreadMessages(user1.getId());

        // Assert
        assertEquals(5L, result);
        verify(messageRepository, times(1)).countUnreadMessages(user1);
    }

    @Test
    void updateTypingStatus_NewStatus_CreatesTypingStatus() {
        // Arrange
        TypingStatusRequest request = TypingStatusRequest.builder()
                .chatRoomId(chatRoom.getId())
                .typing(true)
                .build();

        when(userRepository.findById(user1.getId())).thenReturn(Optional.of(user1));
        when(chatRoomRepository.findById(chatRoom.getId())).thenReturn(Optional.of(chatRoom));
        when(typingStatusRepository.findByUserAndChatRoom(user1, chatRoom)).thenReturn(Optional.empty());
        when(typingStatusRepository.save(any(UserTypingStatus.class))).thenAnswer(invocation -> {
            UserTypingStatus savedStatus = invocation.getArgument(0);
            savedStatus.setId(UUID.randomUUID());
            return savedStatus;
        });

        // Act
        chatService.updateTypingStatus(user1.getId(), request);

        // Assert
        verify(typingStatusRepository, times(1)).save(any(UserTypingStatus.class));
    }

    @Test
    void updateTypingStatus_ExistingStatus_UpdatesTypingStatus() {
        // Arrange
        TypingStatusRequest request = TypingStatusRequest.builder()
                .chatRoomId(chatRoom.getId())
                .typing(false)
                .build();

        when(userRepository.findById(user1.getId())).thenReturn(Optional.of(user1));
        when(chatRoomRepository.findById(chatRoom.getId())).thenReturn(Optional.of(chatRoom));
        when(typingStatusRepository.findByUserAndChatRoom(user1, chatRoom)).thenReturn(Optional.of(typingStatus));

        // Act
        chatService.updateTypingStatus(user1.getId(), request);

        // Assert
        assertFalse(typingStatus.isTyping());
        verify(typingStatusRepository, times(1)).save(typingStatus);
    }

    @Test
    void getTypingUsersInChatRoom_ReturnsCorrectList() {
        // Arrange
        List<UserTypingStatus> typingStatuses = List.of(typingStatus);

        when(userRepository.findById(user2.getId())).thenReturn(Optional.of(user2));
        when(chatRoomRepository.findById(chatRoom.getId())).thenReturn(Optional.of(chatRoom));
        when(typingStatusRepository.findActiveTypingUsersByChatRoom(eq(chatRoom), eq(user2), any(LocalDateTime.class)))
                .thenReturn(typingStatuses);

        // Act
        List<UUID> result = chatService.getTypingUsersInChatRoom(chatRoom.getId(), user2.getId());

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(user1.getId(), result.get(0));
    }

    @Test
    void canSendMessage_DelegatesToSubscriptionService() {
        // Arrange
        when(subscriptionService.canSendMessage(user1.getId())).thenReturn(true);

        // Act
        boolean result = chatService.canSendMessage(user1.getId());

        // Assert
        assertTrue(result);
        verify(subscriptionService, times(1)).canSendMessage(user1.getId());
    }

    @Test
    void getRemainingMessages_CalculatesCorrectly() {
        // Arrange
        SubscriptionDto mockSubscriptionDto = new SubscriptionDto();
        mockSubscriptionDto.setPlan(SubscriptionPlan.SILVER);

        when(subscriptionService.getCurrentSubscription(user1.getId())).thenReturn(mockSubscriptionDto);
        when(userRepository.findById(user1.getId())).thenReturn(Optional.of(user1));
        when(messageRepository.countMessagesSentByUserSince(eq(user1), any(LocalDateTime.class))).thenReturn(5L);

        // Act
        int result = chatService.getRemainingMessages(user1.getId());

        // Assert
        assertEquals(10, result); // Silver has 15 messages, 5 used = 10 remaining
        verify(messageRepository, times(1)).countMessagesSentByUserSince(eq(user1), any(LocalDateTime.class));
    }

    @Test
    void getChatSummary_ReturnsCompleteSummary() {
        // Arrange
        SubscriptionDto mockSubscriptionDto = new SubscriptionDto();
        mockSubscriptionDto.setPlan(SubscriptionPlan.SILVER);
        List<ChatRoom> recentChatRooms = List.of(chatRoom);

        when(userRepository.findById(user1.getId())).thenReturn(Optional.of(user1));
        when(chatRoomRepository.countByUser(user1)).thenReturn(3L);
        when(messageRepository.countUnreadMessages(user1)).thenReturn(7L);
        when(subscriptionService.getCurrentSubscription(user1.getId())).thenReturn(mockSubscriptionDto);
        when(messageRepository.countMessagesSentByUserSince(eq(user1), any(LocalDateTime.class))).thenReturn(8L);
        when(chatRoomRepository.findByUser(eq(user1), any(PageRequest.class))).thenReturn(new PageImpl<>(recentChatRooms));
        when(userProfileRepository.findByUserId(any(UUID.class))).thenReturn(Optional.of(userProfile1));
        when(messageRepository.countUnreadMessagesByChatRoomAndUser(any(ChatRoom.class), any(User.class))).thenReturn(2L);
        when(messageRepository.findByChatRoom(any(ChatRoom.class), any(PageRequest.class))).thenReturn(Page.empty());
        when(typingStatusRepository.findActiveTypingUsersByChatRoom(any(ChatRoom.class), any(User.class), any(LocalDateTime.class))).thenReturn(List.of());

        // Act
        ChatSummaryDto result = chatService.getChatSummary(user1.getId());

        // Assert
        assertNotNull(result);
        assertEquals(3L, result.getTotalChatRooms());
        assertEquals(7L, result.getUnreadMessages());
        assertEquals(15, result.getDailyMessageLimit());
        assertEquals(8, result.getMessagesSentToday());
        assertEquals(7, result.getRemainingMessages());
        assertEquals(1, result.getRecentChatRooms().size());
    }

    @Test
    void resetInactiveTypingStatuses_UpdatesStatuses() {
        // Arrange
        List<UserTypingStatus> inactiveStatuses = List.of(typingStatus);
        when(typingStatusRepository.findInactiveTypingStatusesBefore(any(LocalDateTime.class))).thenReturn(inactiveStatuses);

        // Act
        chatService.resetInactiveTypingStatuses();

        // Assert
        assertFalse(typingStatus.isTyping());
        verify(typingStatusRepository, times(1)).saveAll(inactiveStatuses);
    }
}