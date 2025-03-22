package com.meetruly.chat.service.impl;

import com.meetruly.chat.dto.*;
import com.meetruly.chat.model.ChatRoom;
import com.meetruly.chat.model.Message;
import com.meetruly.chat.model.UserTypingStatus;
import com.meetruly.chat.repository.ChatRoomRepository;
import com.meetruly.chat.repository.MessageRepository;
import com.meetruly.chat.repository.UserTypingStatusRepository;
import com.meetruly.chat.service.ChatService;
import com.meetruly.core.exception.MeetrulyException;
import com.meetruly.subscription.service.SubscriptionService;
import com.meetruly.user.model.User;
import com.meetruly.user.model.UserProfile;
import com.meetruly.user.repository.UserProfileRepository;
import com.meetruly.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository;
    private final UserTypingStatusRepository typingStatusRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final SubscriptionService subscriptionService;

    @Override
    @Transactional
    public ChatRoom getChatRoomByUsers(UUID user1Id, UUID user2Id) {
        User user1 = userRepository.findById(user1Id)
                .orElseThrow(() -> new MeetrulyException("User 1 not found with id: " + user1Id));

        User user2 = userRepository.findById(user2Id)
                .orElseThrow(() -> new MeetrulyException("User 2 not found with id: " + user2Id));


        Optional<ChatRoom> existingChatRoom = chatRoomRepository.findByUsers(user1, user2);

        if (existingChatRoom.isPresent()) {
            return existingChatRoom.get();
        }

        ChatRoom chatRoom = ChatRoom.builder()
                .user1(user1)
                .user2(user2)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return chatRoomRepository.save(chatRoom);
    }

    @Override
    public List<ChatRoomDto> getChatRoomsByUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MeetrulyException("User not found with id: " + userId));

        List<ChatRoom> chatRooms = chatRoomRepository.findByUser(user);

        return chatRooms.stream()
                .map(chatRoom -> convertToChatRoomDto(chatRoom, userId))
                .collect(Collectors.toList());
    }

    @Override
    public Page<ChatRoomDto> getChatRoomsByUser(UUID userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MeetrulyException("User not found with id: " + userId));

        Page<ChatRoom> chatRooms = chatRoomRepository.findByUser(user, pageable);

        return chatRooms.map(chatRoom -> convertToChatRoomDto(chatRoom, userId));
    }

    @Override
    public ChatRoomDto getChatRoomById(UUID chatRoomId, UUID currentUserId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new MeetrulyException("Chat room not found with id: " + chatRoomId));

        if (!chatRoom.hasUser(currentUserId)) {
            throw new MeetrulyException("User is not part of this chat room");
        }
        return convertToChatRoomDto(chatRoom, currentUserId);
    }


    @Override
    @Transactional
    public MessageDto sendMessage(UUID senderId, ChatMessageRequest messageRequest) {

        if (!canSendMessage(senderId)) {
            throw new MeetrulyException("You have reached your daily message limit");
        }

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new MeetrulyException("Sender not found with id: " + senderId));

        User recipient = userRepository.findById(messageRequest.getRecipientId())
                .orElseThrow(() -> new MeetrulyException("Recipient not found with id: " + messageRequest.getRecipientId()));

        ChatRoom chatRoom;
        if (messageRequest.getChatRoomId() != null) {
            chatRoom = chatRoomRepository.findById(messageRequest.getChatRoomId())
                    .orElseThrow(() -> new MeetrulyException("Chat room not found with id: " + messageRequest.getChatRoomId()));


            if (!chatRoom.hasUser(senderId) || !chatRoom.hasUser(messageRequest.getRecipientId())) {
                throw new MeetrulyException("Users are not part of this chat room");
            }
        } else {
            chatRoom = getChatRoomByUsers(senderId, messageRequest.getRecipientId());
        }

        chatRoom.setUpdatedAt(LocalDateTime.now());
        chatRoomRepository.save(chatRoom);

        Message message = Message.builder()
                .chatRoom(chatRoom)
                .sender(sender)
                .recipient(recipient)
                .content(messageRequest.getContent())
                .isRead(false)
                .deleted(false)
                .sentAt(LocalDateTime.now())
                .build();

        Message savedMessage = messageRepository.save(message);

        subscriptionService.incrementMessageCount(senderId);

        return convertToMessageDto(savedMessage, senderId);
    }

    @Override
    public Page<MessageDto> getMessagesByChatRoom(UUID chatRoomId, UUID currentUserId, Pageable pageable) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new MeetrulyException("Chat room not found with id: " + chatRoomId));


        if (!chatRoom.hasUser(currentUserId)) {
            throw new MeetrulyException("User is not part of this chat room");
        }

        Page<Message> messages = messageRepository.findByChatRoom(chatRoom, pageable);

        return messages.map(message -> {
            MessageDto dto = convertToMessageDto(message, currentUserId);


            if (!message.isVisibleToUser(currentUserId)) {
                dto.setContent("This message has been deleted");
                dto.setDeleted(true);
            }

            return dto;
        });
    }

    @Override
    @Transactional
    public void markMessagesAsRead(UUID chatRoomId, UUID userId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new MeetrulyException("Chat room not found with id: " + chatRoomId));

        if (!chatRoom.hasUser(userId)) {
            throw new MeetrulyException("User is not part of this chat room");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MeetrulyException("User not found with id: " + userId));


        List<Message> unreadMessages = messageRepository.findUnreadMessagesByChatRoomAndUser(chatRoom, user);


        for (Message message : unreadMessages) {
            message.markAsRead();
        }

        messageRepository.saveAll(unreadMessages);
    }

    @Override
    @Transactional
    public void deleteMessage(UUID messageId, UUID userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new MeetrulyException("Message not found with id: " + messageId));

        if (!message.getSender().getId().equals(userId) && !message.getRecipient().getId().equals(userId)) {
            throw new MeetrulyException("User is not part of this message exchange");
        }

        message.markAsDeletedByUser(userId);
        messageRepository.save(message);
    }

    @Override
    public long countUnreadMessages(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MeetrulyException("User not found with id: " + userId));

        return messageRepository.countUnreadMessages(user);
    }

    @Override
    @Transactional
    public void updateTypingStatus(UUID userId, TypingStatusRequest typingStatusRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MeetrulyException("User not found with id: " + userId));

        ChatRoom chatRoom = chatRoomRepository.findById(typingStatusRequest.getChatRoomId())
                .orElseThrow(() -> new MeetrulyException("Chat room not found with id: " + typingStatusRequest.getChatRoomId()));


        if (!chatRoom.hasUser(userId)) {
            throw new MeetrulyException("User is not part of this chat room");
        }

        Optional<UserTypingStatus> existingStatus = typingStatusRepository.findByUserAndChatRoom(user, chatRoom);

        UserTypingStatus typingStatus;
        if (existingStatus.isPresent()) {
            typingStatus = existingStatus.get();
            typingStatus.setTyping(typingStatusRequest.isTyping());
            typingStatus.setLastTypingActivity(LocalDateTime.now());
        } else {
            typingStatus = UserTypingStatus.builder()
                    .user(user)
                    .chatRoom(chatRoom)
                    .typing(typingStatusRequest.isTyping())
                    .lastTypingActivity(LocalDateTime.now())
                    .build();
        }

        typingStatusRepository.save(typingStatus);
    }

    @Override
    public List<UUID> getTypingUsersInChatRoom(UUID chatRoomId, UUID currentUserId) {
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new MeetrulyException("User not found with id: " + currentUserId));

        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new MeetrulyException("Chat room not found with id: " + chatRoomId));


        if (!chatRoom.hasUser(currentUserId)) {
            throw new MeetrulyException("User is not part of this chat room");
        }

        LocalDateTime threshold = LocalDateTime.now().minusSeconds(5);
        List<UserTypingStatus> typingUsers = typingStatusRepository.findActiveTypingUsersByChatRoom(chatRoom, currentUser, threshold);

        return typingUsers.stream()
                .map(status -> status.getUser().getId())
                .collect(Collectors.toList());
    }

    @Override
    public boolean canSendMessage(UUID userId) {
        return subscriptionService.canSendMessage(userId);
    }

    @Override
    public int getRemainingMessages(UUID userId) {

        int dailyLimit = subscriptionService.getCurrentSubscription(userId)
                .getPlan().getDailyMessageLimit();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MeetrulyException("User not found with id: " + userId));

        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        long sentToday = messageRepository.countMessagesSentByUserSince(user, today);

        return (int) Math.max(0, dailyLimit - sentToday);
    }

    @Override
    public ChatSummaryDto getChatSummary(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MeetrulyException("User not found with id: " + userId));

        long totalChatRooms = chatRoomRepository.countByUser(user);
        long unreadMessages = messageRepository.countUnreadMessages(user);

        int dailyMessageLimit = subscriptionService.getCurrentSubscription(userId)
                .getPlan().getDailyMessageLimit();


        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        long messagesSentToday = messageRepository.countMessagesSentByUserSince(user, today);


        int remainingMessages = (int) Math.max(0, dailyMessageLimit - messagesSentToday);


        PageRequest pageRequest = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<ChatRoom> recentChatRooms = chatRoomRepository.findByUser(user, pageRequest);

        List<ChatRoomDto> recentChatRoomDtos = recentChatRooms.getContent().stream()
                .map(chatRoom -> convertToChatRoomDto(chatRoom, userId))
                .collect(Collectors.toList());

        return ChatSummaryDto.builder()
                .totalChatRooms(totalChatRooms)
                .unreadMessages(unreadMessages)
                .dailyMessageLimit(dailyMessageLimit)
                .messagesSentToday((int) messagesSentToday)
                .remainingMessages(remainingMessages)
                .recentChatRooms(recentChatRoomDtos)
                .build();
    }

    @Scheduled(fixedRate = 10000)
    @Transactional
    public void resetInactiveTypingStatuses() {

        LocalDateTime threshold = LocalDateTime.now().minusSeconds(10);
        List<UserTypingStatus> inactiveStatuses = typingStatusRepository.findInactiveTypingStatusesBefore(threshold);


        for (UserTypingStatus status : inactiveStatuses) {
            status.setTyping(false);
        }

        typingStatusRepository.saveAll(inactiveStatuses);
    }

    private ChatRoomDto convertToChatRoomDto(ChatRoom chatRoom, UUID currentUserId) {
        User user1 = chatRoom.getUser1();
        User user2 = chatRoom.getUser2();


        String user1ProfileImageUrl = getProfileImageUrl(user1.getId());
        String user2ProfileImageUrl = getProfileImageUrl(user2.getId());


        User currentUser = null;
        User otherUser = null;

        if (user1.getId().equals(currentUserId)) {
            currentUser = user1;
            otherUser = user2;
        } else if (user2.getId().equals(currentUserId)) {
            currentUser = user2;
            otherUser = user1;
        }

        if (currentUser == null) {
            throw new MeetrulyException("User is not part of this chat room");
        }


        long unreadMessagesCount = messageRepository.countUnreadMessagesByChatRoomAndUser(chatRoom, currentUser);
        boolean hasUnreadMessages = unreadMessagesCount > 0;


        PageRequest pageRequest = PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "sentAt"));
        Page<Message> latestMessagePage = messageRepository.findByChatRoom(chatRoom, pageRequest);
        MessageDto latestMessage = null;

        if (!latestMessagePage.isEmpty()) {
            Message message = latestMessagePage.getContent().get(0);
            if (message.isVisibleToUser(currentUserId)) {
                latestMessage = convertToMessageDto(message, currentUserId);
            }
        }


        LocalDateTime threshold = LocalDateTime.now().minusSeconds(5);
        List<UserTypingStatus> typingUsers = typingStatusRepository.findActiveTypingUsersByChatRoom(chatRoom, currentUser, threshold);
        boolean someoneTyping = !typingUsers.isEmpty();

        return ChatRoomDto.builder()
                .id(chatRoom.getId())
                .user1Id(user1.getId())
                .user1Username(user1.getUsername())
                .user1ProfileImageUrl(user1ProfileImageUrl)
                .user2Id(user2.getId())
                .user2Username(user2.getUsername())
                .user2ProfileImageUrl(user2ProfileImageUrl)
                .createdAt(chatRoom.getCreatedAt())
                .updatedAt(chatRoom.getUpdatedAt())
                .hasUnreadMessages(hasUnreadMessages)
                .unreadMessagesCount(unreadMessagesCount)
                .latestMessage(latestMessage)
                .someoneTyping(someoneTyping)
                .currentUserId(currentUserId)
                .build();
    }

    private MessageDto convertToMessageDto(Message message, UUID currentUserId) {


        String senderProfileImageUrl = getProfileImageUrl(message.getSender().getId());
        String recipientProfileImageUrl = getProfileImageUrl(message.getRecipient().getId());


        boolean fromCurrentUser = message.getSender().getId().equals(currentUserId);

        return MessageDto.messageBuilder()
                .id(message.getId())
                .chatRoomId(message.getChatRoom().getId())
                .senderId(message.getSender().getId())
                .senderUsername(message.getSender().getUsername())
                .senderProfileImageUrl(senderProfileImageUrl)
                .recipientId(message.getRecipient().getId())
                .recipientUsername(message.getRecipient().getUsername())
                .recipientProfileImageUrl(recipientProfileImageUrl)
                .content(message.getContent())
                .read(message.isRead())
                .deleted(message.isDeleted())
                .sentAt(message.getSentAt())
                .readAt(message.getReadAt())
                .deletedAt(message.getDeletedAt())
                .fromCurrentUser(fromCurrentUser)
                .build();
    }

    private String getProfileImageUrl(UUID userId) {
        return userProfileRepository.findByUserId(userId)
                .map(UserProfile::getProfileImageUrl)
                .orElse("/images/default-profile.jpg");
    }
}