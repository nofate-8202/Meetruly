package com.meetruly.matching.repository;

import com.meetruly.core.constant.Gender;
import com.meetruly.core.constant.UserRole;
import com.meetruly.matching.model.Like;
import com.meetruly.user.model.User;
import com.meetruly.user.repository.UserRepository;
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

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class LikeRepositoryTest {

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private UserRepository userRepository;

    private User user1;
    private User user2;
    private User user3;
    private Like like1;
    private Like like2;

    @BeforeEach
    void setUp() {
        likeRepository.deleteAll();
        userRepository.deleteAll();

        
        user1 = new User();
        user1.setUsername("user1");
        user1.setEmail("user1@example.com");
        user1.setPassword("password");
        user1.setGender(Gender.MALE);
        user1.setEnabled(true);
        user1.setApproved(true);
        user1.setProfileCompleted(true);
        user1.setRole(UserRole.USER);
        user1.setAccountNonLocked(true);
        user1.setEmailVerified(true);
        user1.setCreatedAt(LocalDateTime.now());
        user1.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user1);

        user2 = new User();
        user2.setUsername("user2");
        user2.setEmail("user2@example.com");
        user2.setPassword("password");
        user2.setGender(Gender.FEMALE);
        user2.setEnabled(true);
        user2.setApproved(true);
        user2.setProfileCompleted(true);
        user2.setRole(UserRole.USER);
        user2.setAccountNonLocked(true);
        user2.setEmailVerified(true);
        user2.setCreatedAt(LocalDateTime.now());
        user2.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user2);

        user3 = new User();
        user3.setUsername("user3");
        user3.setEmail("user3@example.com");
        user3.setPassword("password");
        user3.setGender(Gender.OTHER);
        user3.setEnabled(true);
        user3.setApproved(true);
        user3.setProfileCompleted(true);
        user3.setRole(UserRole.USER);
        user3.setAccountNonLocked(true);
        user3.setEmailVerified(true);
        user3.setCreatedAt(LocalDateTime.now());
        user3.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user3);

        
        like1 = Like.builder()
                .liker(user1)
                .liked(user2)
                .viewed(false)
                .createdAt(LocalDateTime.now())
                .build();
        likeRepository.save(like1);

        like2 = Like.builder()
                .liker(user3)
                .liked(user2)
                .viewed(true)
                .createdAt(LocalDateTime.now())
                .build();
        likeRepository.save(like2);
    }

    @Test
    void findByLikerAndLiked_ShouldReturnLike() {
        Optional<Like> result = likeRepository.findByLikerAndLiked(user1, user2);

        assertTrue(result.isPresent());
        assertEquals(user1.getId(), result.get().getLiker().getId());
        assertEquals(user2.getId(), result.get().getLiked().getId());
    }

    @Test
    void existsByLikerAndLiked_ShouldReturnTrue_WhenLikeExists() {
        boolean exists = likeRepository.existsByLikerAndLiked(user1, user2);

        assertTrue(exists);
    }

    @Test
    void existsByLikerAndLiked_ShouldReturnFalse_WhenLikeDoesNotExist() {
        boolean exists = likeRepository.existsByLikerAndLiked(user2, user1);

        assertFalse(exists);
    }

    @Test
    void findByLiker_ShouldReturnAllLikesByLiker() {
        List<Like> likes = likeRepository.findByLiker(user1);

        assertEquals(1, likes.size());
        assertEquals(user2.getId(), likes.get(0).getLiked().getId());
    }

    @Test
    void findByLiked_ShouldReturnAllLikesToLiked() {
        List<Like> likes = likeRepository.findByLiked(user2);

        assertEquals(2, likes.size());
    }

    @Test
    void findUnviewedLikesByUser_ShouldReturnUnviewedLikes() {
        
        List<Like> allLikes = likeRepository.findAll();
        System.out.println("All likes: " + allLikes.size());
        for (Like like : allLikes) {
            System.out.println("Like from " + like.getLiker().getUsername() +
                    " to " + like.getLiked().getUsername() +
                    ", viewed: " + like.isViewed());
        }

        
        List<Like> unviewedLikes = likeRepository.findUnviewedLikesByUser(user2);
        assertFalse(unviewedLikes.isEmpty(), "There should be at least one unviewed like");

        
        boolean foundUser1Like = false;
        for (Like like : unviewedLikes) {
            if (like.getLiker().getId().equals(user1.getId())) {
                foundUser1Like = true;
                assertFalse(like.isViewed(), "The like from user1 should be marked as unviewed");
                break;
            }
        }
        assertTrue(foundUser1Like, "The list should contain a like from user1");
    }

    @Test
    void countLikesByUser_ShouldReturnCorrectCount() {
        long count = likeRepository.countLikesByUser(user2);

        assertEquals(2, count);
    }

    @Test
    void findLikedUsersByUser_ShouldReturnUsersLikedByUser() {
        List<User> likedUsers = likeRepository.findLikedUsersByUser(user1);

        assertEquals(1, likedUsers.size());
        assertEquals(user2.getId(), likedUsers.get(0).getId());
    }

    @Test
    void findLikersByUser_ShouldReturnUsersWhoLikedUser() {
        List<User> likers = likeRepository.findLikersByUser(user2);

        assertEquals(2, likers.size());
    }

    @Test
    void findTopLikedUsers_ShouldReturnUsersOrderedByLikeCount() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Object[]> topLikedUsers = likeRepository.findTopLikedUsers(pageable);

        
        System.out.println("Number of results: " + topLikedUsers.getTotalElements());
        for (Object[] result : topLikedUsers.getContent()) {
            System.out.println("ID: " + result[0] + ", like count: " + result[1]);
            System.out.println("ID class: " + result[0].getClass().getName());
        }

        
        assertFalse(topLikedUsers.isEmpty(), "There should be at least one result");

        
        Object[] firstResult = topLikedUsers.getContent().get(0);
        assertEquals(2, firstResult.length, "The result should have two columns (ID and like count)");
        assertNotNull(firstResult[0], "ID should not be null");
        assertNotNull(firstResult[1], "Like count should not be null");
    }
}