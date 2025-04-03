package com.meetruly.chat.repository;

import com.meetruly.chat.model.ChatRoom;
import com.meetruly.chat.model.Message;
import com.meetruly.core.constant.Gender;
import com.meetruly.core.constant.UserRole;
import com.meetruly.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
public class MessageRepositoryTest {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager entityManager;

    private User user1;
    private User user2;
    private ChatRoom chatRoom;
    private Message message1;
    private Message message2;
    private Message message3;
    private Message deletedMessage;
    private LocalDateTime baseTime;

    @BeforeEach
    void setUp() {
        
        user1 = createUser("user1");
        user2 = createUser("user2");

        entityManager.persist(user1);
        entityManager.persist(user2);
        entityManager.flush();

        
        chatRoom = ChatRoom.builder()
                .user1(user1)
                .user2(user2)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        entityManager.persist(chatRoom);
        entityManager.flush();

        
        
        message1 = Message.builder()
                .chatRoom(chatRoom)
                .sender(user1)
                .recipient(user2)
                .content("Hello from user1")
                .isRead(false)
                .deleted(false)
                .deletedBySender(false)
                .deletedByRecipient(false)
                .sentAt(LocalDateTime.now().minusHours(2))
                .createdAt(LocalDateTime.now().minusHours(2))
                .build();

        entityManager.persist(message1);
        entityManager.flush();

        
        message2 = Message.builder()
                .chatRoom(chatRoom)
                .sender(user2)
                .recipient(user1)
                .content("Hello from user2")
                .isRead(true)
                .deleted(false)
                .deletedBySender(false)
                .deletedByRecipient(false)
                .sentAt(LocalDateTime.now().minusHours(1))
                .readAt(LocalDateTime.now().minusMinutes(30))
                .createdAt(LocalDateTime.now().minusHours(1))
                .build();

        entityManager.persist(message2);
        entityManager.flush();

        
        message3 = Message.builder()
                .chatRoom(chatRoom)
                .sender(user1)
                .recipient(user2)
                .content("Another message from user1")
                .isRead(false)
                .deleted(false)
                .deletedBySender(false)
                .deletedByRecipient(false)
                .sentAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        entityManager.persist(message3);
        entityManager.flush();

        
        deletedMessage = Message.builder()
                .chatRoom(chatRoom)
                .sender(user1)
                .recipient(user2)
                .content("This message is deleted")
                .isRead(true)
                .deleted(false) 
                .deletedBySender(true)
                .deletedByRecipient(true)
                .sentAt(LocalDateTime.now().minusHours(3))
                .readAt(LocalDateTime.now().minusHours(2))
                .deletedAt(LocalDateTime.now().minusHours(1))
                .createdAt(LocalDateTime.now().minusHours(3))
                .build();

        entityManager.persist(deletedMessage);
        entityManager.flush();

        
        entityManager.getEntityManager().createQuery(
                        "UPDATE Message m SET m.deleted = true WHERE m.id = :id")
                .setParameter("id", deletedMessage.getId())
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();

        
        Message persistedDeletedMessage = entityManager.find(Message.class, deletedMessage.getId());
        if (!persistedDeletedMessage.isDeleted()) {
            throw new RuntimeException("Failed to mark deletedMessage as deleted in database!");
        }

        
        printTestData();
    }

    private void printTestData() {
        
        
    }

    private User createUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPassword("password");
        user.setGender(Gender.MALE);
        user.setEnabled(true);
        user.setEmailVerified(false);
        user.setApproved(true);
        user.setAccountNonLocked(true);
        user.setProfileCompleted(false);
        user.setRole(UserRole.USER);
        user.setCreatedAt(baseTime);
        user.setUpdatedAt(baseTime);
        return user;
    }

    @Test
    void findByChatRoom_ReturnsNonDeletedMessages() {
        
        List<Message> messages = messageRepository.findByChatRoom(chatRoom);

        
        List<UUID> expectedIds = Arrays.asList(message1.getId(), message2.getId(), message3.getId());
        List<UUID> actualIds = messages.stream().map(Message::getId).collect(Collectors.toList());

        
        assertEquals(3, messages.size(), "Should return only non-deleted messages");
        assertTrue(messages.stream().noneMatch(Message::isDeleted), "No deleted messages should be returned");
        assertTrue(actualIds.containsAll(expectedIds), "Should contain all non-deleted messages");
        assertFalse(actualIds.contains(deletedMessage.getId()), "Should not contain deleted message");
    }

    @Test
    void findByChatRoomPaginated_ReturnsPagedMessages() {
        
        PageRequest pageRequest = PageRequest.of(0, 2);

        
        Page<Message> messagePage = messageRepository.findByChatRoom(chatRoom, pageRequest);

        
        assertEquals(2, messagePage.getContent().size(), "Page should contain 2 messages");
        assertEquals(3, messagePage.getTotalElements(), "Total should be 3 non-deleted messages");

        
        assertEquals(message3.getId(), messagePage.getContent().get(0).getId(), "First should be message3 (newest)");
        assertEquals(message2.getId(), messagePage.getContent().get(1).getId(), "Second should be message2");
    }

    @Test
    void countUnreadMessages_ReturnsCorrectCount() {
        
        long unreadCount = messageRepository.countUnreadMessages(user2);

        
        assertEquals(2, unreadCount);
    }

    @Test
    void findUnreadMessagesByUser_ReturnsUnreadMessages() {
        
        List<Message> unreadMessages = messageRepository.findUnreadMessagesByUser(user2);

        
        assertEquals(2, unreadMessages.size());
        assertTrue(unreadMessages.stream().allMatch(m -> !m.isRead() && !m.isDeleted()));
        assertTrue(unreadMessages.stream().anyMatch(m -> m.getId().equals(message1.getId())));
        assertTrue(unreadMessages.stream().anyMatch(m -> m.getId().equals(message3.getId())));
    }

    @Test
    void findUnreadMessagesByChatRoomAndUser_ReturnsUnreadMessages() {
        
        List<Message> unreadMessages = messageRepository.findUnreadMessagesByChatRoomAndUser(chatRoom, user2);

        
        assertEquals(2, unreadMessages.size());
        assertTrue(unreadMessages.stream().allMatch(m -> !m.isRead() && !m.isDeleted()));
    }

    @Test
    void countUnreadMessagesByChatRoomAndUser_ReturnsCorrectCount() {
        
        long unreadCount = messageRepository.countUnreadMessagesByChatRoomAndUser(chatRoom, user2);

        
        assertEquals(2, unreadCount);
    }

    @Test
    void countMessagesSentByUserSince_ReturnsCorrectCount() {
        
        List<Message> allMessages = messageRepository.findAll();
        System.out.println("Total messages in DB: " + allMessages.size());

        
        allMessages.forEach(m -> {
            System.out.println("Message ID: " + m.getId() +
                    ", Sender: " + m.getSender().getUsername() +
                    ", Sent At: " + m.getSentAt() +
                    ", Deleted: " + m.isDeleted());
        });

        
        LocalDateTime since = message2.getSentAt();
        System.out.println("Using time cutoff (since): " + since);

        
        long manualCount = allMessages.stream()
                .filter(m -> m.getSender().getId().equals(user1.getId()))
                .filter(m -> !m.isDeleted())
                .filter(m -> m.getSentAt().isAfter(since))
                .count();

        System.out.println("Expected count (manual): " + manualCount);

        
        long messageCount = messageRepository.countMessagesSentByUserSince(user1, since);
        System.out.println("Actual count (repository): " + messageCount);

        
        assertEquals(1, messageCount, "Should only count message3");
    }

    @Test
    void findMessagesSentByUserSince_ReturnsMatchingMessages() {
        
        LocalDateTime since = message2.getSentAt();

        
        List<Message> messages = messageRepository.findMessagesSentByUserSince(user1, since);

        
        assertEquals(1, messages.size(), "Should only include message3");
        if (!messages.isEmpty()) {
            assertEquals(message3.getId(), messages.get(0).getId(), "Should be message3");
        }
    }

    @Test
    void countByCreatedAtAfter_ReturnsCorrectCount() {
        
        List<Message> allMessages = messageRepository.findAll();

        
        LocalDateTime futureTime = LocalDateTime.now().plusMinutes(10);

        
        long countFuture = messageRepository.countByCreatedAtAfter(futureTime);

        
        assertEquals(0, countFuture, "No messages should be after futureTime");

        
        long totalCount = messageRepository.count();

        
        long nonDeletedCount = allMessages.stream()
                .filter(m -> !m.isDeleted())
                .count();

        
        assertEquals(4, totalCount, "Should have 4 total messages");
        assertEquals(3, nonDeletedCount, "Should have 3 non-deleted messages");
    }

    @Test
    void messageMarkAsRead_SetsReadStatus() {
        
        Message message = messageRepository.findById(message1.getId()).orElseThrow();
        assertFalse(message.isRead());
        assertNull(message.getReadAt());

        
        message.markAsRead();
        entityManager.persist(message);
        entityManager.flush();
        entityManager.clear();

        
        Message updatedMessage = messageRepository.findById(message1.getId()).orElseThrow();
        assertTrue(updatedMessage.isRead());
        assertNotNull(updatedMessage.getReadAt());
    }

    @Test
    void messageMarkAsDeletedByUser_MarksAsDeletedByUser() {
        
        Message message = messageRepository.findById(message1.getId()).orElseThrow();
        assertFalse(message.isDeleted());
        assertFalse(message.isDeletedBySender());
        assertFalse(message.isDeletedByRecipient());

        
        message.markAsDeletedByUser(user1.getId());
        entityManager.persist(message);
        entityManager.flush();
        entityManager.clear();

        
        Message updatedMessage = messageRepository.findById(message1.getId()).orElseThrow();
        assertTrue(updatedMessage.isDeletedBySender());
        assertFalse(updatedMessage.isDeletedByRecipient());
        assertFalse(updatedMessage.isDeleted());

        
        updatedMessage.markAsDeletedByUser(user2.getId());
        entityManager.persist(updatedMessage);
        entityManager.flush();
        entityManager.clear();

        
        Message fullyDeletedMessage = messageRepository.findById(message1.getId()).orElseThrow();
        assertTrue(fullyDeletedMessage.isDeletedBySender());
        assertTrue(fullyDeletedMessage.isDeletedByRecipient());
        assertTrue(fullyDeletedMessage.isDeleted());
        assertNotNull(fullyDeletedMessage.getDeletedAt());
    }

    @Test
    void messageIsVisibleToUser_ReturnsCorrectVisibility() {
        
        Message message = messageRepository.findById(message1.getId()).orElseThrow();

        
        boolean visibleToSender = message.isVisibleToUser(user1.getId());
        boolean visibleToRecipient = message.isVisibleToUser(user2.getId());

        
        assertTrue(visibleToSender);
        assertTrue(visibleToRecipient);

        
        message.markAsDeletedByUser(user1.getId());
        entityManager.persist(message);
        entityManager.flush();
        entityManager.clear();

        
        Message updatedMessage = messageRepository.findById(message1.getId()).orElseThrow();
        assertFalse(updatedMessage.isVisibleToUser(user1.getId()));
        assertTrue(updatedMessage.isVisibleToUser(user2.getId()));
    }
}