package com.meetruly.chat.repository;

import com.meetruly.chat.model.ChatRoom;
import com.meetruly.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
public class ChatRoomRepositoryTest {

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager entityManager;

    private User user1;
    private User user2;
    private User user3;
    private ChatRoom chatRoom1;
    private ChatRoom chatRoom2;

    @BeforeEach
    void setUp() {
        
        user1 = createUser("user1");
        user2 = createUser("user2");
        user3 = createUser("user3");

        entityManager.persist(user1);
        entityManager.persist(user2);
        entityManager.persist(user3);

        
        chatRoom1 = ChatRoom.builder()
                .user1(user1)
                .user2(user2)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();

        chatRoom2 = ChatRoom.builder()
                .user1(user1)
                .user2(user3)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        entityManager.persist(chatRoom1);
        entityManager.persist(chatRoom2);
        entityManager.flush();
    }

    private User createUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPassword("password");
        user.setGender(com.meetruly.core.constant.Gender.MALE); 
        user.setEnabled(true);
        user.setEmailVerified(false);
        user.setApproved(true);
        user.setAccountNonLocked(true);
        user.setProfileCompleted(false);
        user.setRole(com.meetruly.core.constant.UserRole.USER);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    @Test
    void findByUsers_WhenChatRoomExists_ReturnsMatchingChatRoom() {
        

        
        Optional<ChatRoom> foundChatRoom = chatRoomRepository.findByUsers(user1, user2);

        
        assertTrue(foundChatRoom.isPresent());
        assertEquals(chatRoom1.getId(), foundChatRoom.get().getId());
    }

    @Test
    void findByUsers_WhenChatRoomExistsWithReversedUsers_ReturnsMatchingChatRoom() {
        

        
        Optional<ChatRoom> foundChatRoom = chatRoomRepository.findByUsers(user2, user1);

        
        assertTrue(foundChatRoom.isPresent());
        assertEquals(chatRoom1.getId(), foundChatRoom.get().getId());
    }

    @Test
    void findByUsers_WhenChatRoomDoesNotExist_ReturnsEmptyOptional() {
        
        User newUser = createUser("newUser");
        entityManager.persist(newUser);
        entityManager.flush();

        
        Optional<ChatRoom> foundChatRoom = chatRoomRepository.findByUsers(newUser, user3);

        
        assertFalse(foundChatRoom.isPresent());
    }

    @Test
    void findByUser_ReturnsAllUserChatRooms() {
        

        
        List<ChatRoom> userChatRooms = chatRoomRepository.findByUser(user1);

        
        assertEquals(2, userChatRooms.size());
        assertTrue(userChatRooms.stream().anyMatch(room -> room.getId().equals(chatRoom1.getId())));
        assertTrue(userChatRooms.stream().anyMatch(room -> room.getId().equals(chatRoom2.getId())));
    }

    @Test
    void findByUser_WhenUserHasNoChatRooms_ReturnsEmptyList() {
        
        User newUser = createUser("userWithoutChats");
        entityManager.persist(newUser);
        entityManager.flush();

        
        List<ChatRoom> userChatRooms = chatRoomRepository.findByUser(newUser);

        
        assertTrue(userChatRooms.isEmpty());
    }

    @Test
    void findByUser_WithPagination_ReturnsPagedResults() {
        
        Pageable pageable = PageRequest.of(0, 1);

        
        Page<ChatRoom> chatRoomsPage = chatRoomRepository.findByUser(user1, pageable);

        
        assertEquals(1, chatRoomsPage.getContent().size());
        assertEquals(2, chatRoomsPage.getTotalElements());

        
        assertEquals(chatRoom2.getId(), chatRoomsPage.getContent().get(0).getId());
    }

    @Test
    void countByUser_ReturnsCorrectCount() {
        

        
        long user1Count = chatRoomRepository.countByUser(user1);
        long user2Count = chatRoomRepository.countByUser(user2);
        long user3Count = chatRoomRepository.countByUser(user3);

        
        assertEquals(2, user1Count);
        assertEquals(1, user2Count);
        assertEquals(1, user3Count);
    }

    @Test
    void hasUser_ReturnsTrueForBothUsers() {
        
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoom1.getId()).orElseThrow();

        
        boolean user1InRoom = chatRoom.hasUser(user1.getId());
        boolean user2InRoom = chatRoom.hasUser(user2.getId());
        boolean user3InRoom = chatRoom.hasUser(user3.getId());

        
        assertTrue(user1InRoom);
        assertTrue(user2InRoom);
        assertFalse(user3InRoom);
    }

    @Test
    void getOtherUser_ReturnsCorrectOtherUser() {
        
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoom1.getId()).orElseThrow();

        
        User otherUserForUser1 = chatRoom.getOtherUser(user1.getId());
        User otherUserForUser2 = chatRoom.getOtherUser(user2.getId());
        User otherUserForUser3 = chatRoom.getOtherUser(user3.getId());

        
        assertEquals(user2.getId(), otherUserForUser1.getId());
        assertEquals(user1.getId(), otherUserForUser2.getId());
        assertNull(otherUserForUser3);
    }
}