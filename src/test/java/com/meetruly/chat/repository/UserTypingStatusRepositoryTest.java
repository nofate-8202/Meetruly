package com.meetruly.chat.repository;

import com.meetruly.chat.model.ChatRoom;
import com.meetruly.chat.model.UserTypingStatus;
import com.meetruly.core.constant.Gender;
import com.meetruly.core.constant.UserRole;
import com.meetruly.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
public class UserTypingStatusRepositoryTest {

    @Autowired
    private UserTypingStatusRepository userTypingStatusRepository;

    @Autowired
    private EntityManager entityManager;

    private User user1;
    private User user2;
    private User user3;
    private ChatRoom chatRoom1;
    private ChatRoom chatRoom2;
    private UserTypingStatus typingStatus1;
    private UserTypingStatus typingStatus2;
    private UserTypingStatus typingStatus3;
    private LocalDateTime baseTime;

    @BeforeEach
    void setUp() {
        
        entityManager.clear();

        
        
        baseTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        
        user1 = createUser("user1");
        user2 = createUser("user2");
        user3 = createUser("user3");

        entityManager.persist(user1);
        entityManager.persist(user2);
        entityManager.persist(user3);
        entityManager.flush();

        
        chatRoom1 = ChatRoom.builder()
                .user1(user1)
                .user2(user2)
                .createdAt(baseTime)
                .updatedAt(baseTime)
                .build();

        chatRoom2 = ChatRoom.builder()
                .user1(user1)
                .user2(user3)
                .createdAt(baseTime)
                .updatedAt(baseTime)
                .build();

        entityManager.persist(chatRoom1);
        entityManager.persist(chatRoom2);
        entityManager.flush();

        
        
        typingStatus1 = UserTypingStatus.builder()
                .user(user1)
                .chatRoom(chatRoom1)
                .typing(true)
                .lastTypingActivity(baseTime)
                .build();

        
        typingStatus2 = UserTypingStatus.builder()
                .user(user2)
                .chatRoom(chatRoom1)
                .typing(true)
                .lastTypingActivity(baseTime.minusMinutes(5))
                .build();

        
        typingStatus3 = UserTypingStatus.builder()
                .user(user3)
                .chatRoom(chatRoom2)
                .typing(false)
                .lastTypingActivity(baseTime.minusMinutes(10))
                .build();

        
        
        entityManager.persist(typingStatus1);
        entityManager.persist(typingStatus2);
        entityManager.persist(typingStatus3);
        entityManager.flush();

        
        entityManager.createQuery(
                        "UPDATE UserTypingStatus uts SET uts.lastTypingActivity = :time WHERE uts.id = :id")
                .setParameter("time", baseTime)
                .setParameter("id", typingStatus1.getId())
                .executeUpdate();

        entityManager.createQuery(
                        "UPDATE UserTypingStatus uts SET uts.lastTypingActivity = :time WHERE uts.id = :id")
                .setParameter("time", baseTime.minusMinutes(5))
                .setParameter("id", typingStatus2.getId())
                .executeUpdate();

        entityManager.createQuery(
                        "UPDATE UserTypingStatus uts SET uts.lastTypingActivity = :time WHERE uts.id = :id")
                .setParameter("time", baseTime.minusMinutes(10))
                .setParameter("id", typingStatus3.getId())
                .executeUpdate();

        entityManager.flush();
        entityManager.clear();
    }

    private User createUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPassword("password");
        user.setGender(Gender.MALE);
        user.setEnabled(true);
        user.setEmailVerified(true);
        user.setApproved(true);
        user.setAccountNonLocked(true);
        user.setProfileCompleted(true);
        user.setRole(UserRole.USER);
        user.setCreatedAt(baseTime);
        user.setUpdatedAt(baseTime);
        return user;
    }

    @Test
    void findByUserAndChatRoom_ReturnsCorrectStatus() {
        
        Optional<UserTypingStatus> result = userTypingStatusRepository.findByUserAndChatRoom(user1, chatRoom1);

        
        assertTrue(result.isPresent(), "Should find status for user1 in chatRoom1");
        UserTypingStatus status = result.get();
        assertEquals(user1.getId(), status.getUser().getId(), "User should be user1");
        assertEquals(chatRoom1.getId(), status.getChatRoom().getId(), "Chat room should be chatRoom1");
        assertTrue(status.isTyping(), "Status should show the user is typing");

        
        assertEquals(
                baseTime,
                status.getLastTypingActivity().truncatedTo(ChronoUnit.SECONDS),
                "Last typing activity should be baseTime"
        );
    }

    @Test
    void findByUserAndChatRoom_ReturnsEmptyForNonExistentCombination() {
        
        Optional<UserTypingStatus> result = userTypingStatusRepository.findByUserAndChatRoom(user2, chatRoom2);

        
        assertFalse(result.isPresent(), "Should not find status for user2 in chatRoom2");
    }

    @Test
    void findActiveTypingUsersByChatRoom_ReturnsCorrectUsers() {
        
        LocalDateTime since = baseTime.minusMinutes(6);

        
        List<UserTypingStatus> activeStatuses = userTypingStatusRepository.findActiveTypingUsersByChatRoom(
                chatRoom1, user2, since);

        
        assertEquals(1, activeStatuses.size(), "Should have 1 active user (user1)");
        assertEquals(user1.getId(), activeStatuses.get(0).getUser().getId(), "Active user should be user1");
    }

    @Test
    void findActiveTypingUsersByChatRoom_FiltersNonActiveStatuses() {
        
        
        LocalDateTime since = baseTime.minusMinutes(2);

        
        List<UserTypingStatus> activeStatuses = userTypingStatusRepository.findActiveTypingUsersByChatRoom(
                chatRoom1, user1, since);

        
        
        assertEquals(0, activeStatuses.size(), "Should have no active users (user2 is inactive, user1 is excluded from query)");
    }

    @Test
    void findInactiveTypingStatusesBefore_ReturnsInactiveStatuses() {
        
        LocalDateTime time = baseTime.minusMinutes(2);

        
        List<UserTypingStatus> inactiveStatuses = userTypingStatusRepository.findInactiveTypingStatusesBefore(time);

        
        assertEquals(2, inactiveStatuses.size(), "Should have 2 inactive statuses (user2 and user3)");

        
        boolean containsUser2 = inactiveStatuses.stream()
                .anyMatch(status -> status.getUser().getId().equals(user2.getId()));
        boolean containsUser3 = inactiveStatuses.stream()
                .anyMatch(status -> status.getUser().getId().equals(user3.getId()));

        assertTrue(containsUser2, "Should contain user2's status");
        assertTrue(containsUser3, "Should contain user3's status");
    }

    @Test
    void saveUpdatesTypingStatus() {
        
        UserTypingStatus status = userTypingStatusRepository.findByUserAndChatRoom(user1, chatRoom1).orElseThrow();

        
        status.setTyping(false);
        LocalDateTime newTime = baseTime.plusMinutes(5);
        status.setLastTypingActivity(newTime);
        userTypingStatusRepository.save(status);
        entityManager.flush();
        entityManager.clear();

        
        UserTypingStatus updatedStatus = userTypingStatusRepository.findByUserAndChatRoom(user1, chatRoom1).orElseThrow();
        assertFalse(updatedStatus.isTyping(), "Status should show the user is not typing");
        assertEquals(newTime.truncatedTo(ChronoUnit.SECONDS),
                updatedStatus.getLastTypingActivity().truncatedTo(ChronoUnit.SECONDS),
                "Last typing activity should be updated");
    }
}