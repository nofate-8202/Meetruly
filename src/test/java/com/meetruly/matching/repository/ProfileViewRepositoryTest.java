package com.meetruly.matching.repository;

import com.meetruly.core.constant.Gender;
import com.meetruly.core.constant.UserRole;
import com.meetruly.matching.model.ProfileView;
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

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class ProfileViewRepositoryTest {

    @Autowired
    private ProfileViewRepository profileViewRepository;

    @Autowired
    private UserRepository userRepository;

    private User user1;
    private User user2;
    private User user3;
    private ProfileView profileView1;
    private ProfileView profileView2;
    private ProfileView profileView3;

    @BeforeEach
    void setUp() {
        profileViewRepository.deleteAll();
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

        
        profileView1 = ProfileView.builder()
                .viewer(user1)
                .viewed(user2)
                .viewedAt(LocalDateTime.now())
                .notificationSent(false)
                .build();
        profileViewRepository.save(profileView1);

        profileView2 = ProfileView.builder()
                .viewer(user3)
                .viewed(user2)
                .viewedAt(LocalDateTime.now())
                .notificationSent(true)
                .build();
        profileViewRepository.save(profileView2);

        profileView3 = ProfileView.builder()
                .viewer(user1)
                .viewed(user3)
                .viewedAt(LocalDateTime.now())
                .notificationSent(false)
                .build();
        profileViewRepository.save(profileView3);
    }

    @Test
    void findByViewer_ShouldReturnAllViewsByViewer() {
        List<ProfileView> views = profileViewRepository.findByViewer(user1);

        assertEquals(2, views.size());
    }

    @Test
    void findByViewed_ShouldReturnAllViewsOfViewed() {
        List<ProfileView> views = profileViewRepository.findByViewed(user2);

        assertEquals(2, views.size());
    }

    @Test
    void findByViewedWithPagination_ShouldReturnPaginatedResults() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProfileView> viewsPage = profileViewRepository.findByViewed(user2, pageable);

        assertEquals(2, viewsPage.getTotalElements());
    }

    @Test
    void countViewsByUser_ShouldReturnCorrectCount() {
        long countUser2Views = profileViewRepository.countViewsByUser(user2);
        long countUser3Views = profileViewRepository.countViewsByUser(user3);

        assertEquals(2, countUser2Views);
        assertEquals(1, countUser3Views);
    }

    @Test
    void countProfileViewsSince_ShouldReturnCorrectCount() {
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        long count = profileViewRepository.countProfileViewsSince(oneDayAgo);

        assertEquals(3, count);
    }

    @Test
    void findTopViewersByUser_ShouldReturnViewersOrderedByViewCount() {
        Pageable pageable = PageRequest.of(0, 10);
        List<Object[]> topViewers = profileViewRepository.findTopViewersByUser(user2, pageable);

        assertNotNull(topViewers);
        assertFalse(topViewers.isEmpty());
        assertEquals(2, topViewers.size());

        
        
        boolean foundUser1 = false;
        boolean foundUser3 = false;

        for (Object[] result : topViewers) {
            User viewer = (User) result[0];
            Long count = (Long) result[1];

            if (viewer.getId().equals(user1.getId())) {
                foundUser1 = true;
                assertEquals(1L, count);
            } else if (viewer.getId().equals(user3.getId())) {
                foundUser3 = true;
                assertEquals(1L, count);
            }
        }

        assertTrue(foundUser1);
        assertTrue(foundUser3);
    }
}