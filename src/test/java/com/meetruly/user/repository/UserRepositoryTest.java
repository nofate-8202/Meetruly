package com.meetruly.user.repository;
import java.util.UUID;

import com.meetruly.core.constant.Gender;
import com.meetruly.core.constant.UserRole;
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

import static org.junit.jupiter.api.Assertions.*;


@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User regularUser;
    private User adminUser;
    private User unapprovedUser;

    @BeforeEach
    void setUp() {
        
        userRepository.deleteAll();

        LocalDateTime now = LocalDateTime.now();

        
        regularUser = new User();
        regularUser.setUsername("regularuser");
        regularUser.setEmail("regular@example.com");
        regularUser.setPassword("password");
        regularUser.setGender(Gender.MALE);
        regularUser.setRole(UserRole.USER);
        regularUser.setCreatedAt(now);
        regularUser.setUpdatedAt(now);
        regularUser.setLastLogin(now.minusDays(1));
        

        
        adminUser = new User();
        adminUser.setUsername("adminuser");
        adminUser.setEmail("admin@example.com");
        adminUser.setPassword("password");
        adminUser.setGender(Gender.FEMALE);
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setCreatedAt(now);
        adminUser.setUpdatedAt(now);
        adminUser.setLastLogin(now);
        

        
        unapprovedUser = new User();
        unapprovedUser.setUsername("pendinguser");
        unapprovedUser.setEmail("pending@example.com");
        unapprovedUser.setPassword("password");
        unapprovedUser.setGender(Gender.OTHER);
        unapprovedUser.setRole(UserRole.USER);
        unapprovedUser.setCreatedAt(now);
        unapprovedUser.setUpdatedAt(now);
        

        
        regularUser = userRepository.save(regularUser);
        adminUser = userRepository.save(adminUser);
        unapprovedUser = userRepository.save(unapprovedUser);

        
        assertEquals(3, userRepository.count(), "Should have 3 users saved in the test database");

        
        
        regularUser.setApproved(true);
        adminUser.setApproved(true);

        
        regularUser.setProfileCompleted(true);
        adminUser.setProfileCompleted(true);
        unapprovedUser.setProfileCompleted(true); 

        
        regularUser = userRepository.save(regularUser);
        adminUser = userRepository.save(adminUser);
        unapprovedUser = userRepository.save(unapprovedUser);
    }

    @Test
    void testFindById() {
        
        Optional<User> foundUser = userRepository.findById(regularUser.getId());
        assertTrue(foundUser.isPresent());
        assertEquals("regularuser", foundUser.get().getUsername());
    }

    @Test
    void testBasicCrud() {
        
        User user = User.builder()
                .username("test")
                .email("test@example.com")
                .password("password")
                .gender(Gender.MALE)
                .role(UserRole.USER)
                .enabled(true)
                .accountNonLocked(true)
                .emailVerified(false)
                .profileCompleted(false)
                .approved(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);
        assertNotNull(savedUser.getId());
        assertEquals(4, userRepository.count(), "Should now have 4 users");

        
        User foundUser = userRepository.findById(savedUser.getId()).orElse(null);
        assertNotNull(foundUser);
        assertEquals("test", foundUser.getUsername());

        
        foundUser.setUsername("updated");
        userRepository.save(foundUser);
        User updatedUser = userRepository.findById(savedUser.getId()).orElse(null);
        assertNotNull(updatedUser);
        assertEquals("updated", updatedUser.getUsername());

        
        userRepository.delete(updatedUser);
        assertTrue(userRepository.findById(savedUser.getId()).isEmpty());
        assertEquals(3, userRepository.count(), "Should be back to 3 users");
    }

    @Test
    void testFindByUsername() {
        
        Optional<User> foundUser = userRepository.findByUsername("regularuser");
        assertTrue(foundUser.isPresent());
        assertEquals("regularuser", foundUser.get().getUsername());

        
        assertTrue(userRepository.findByUsername("nonexistent").isEmpty());
    }

    @Test
    void testFindByEmail() {
        
        Optional<User> foundUser = userRepository.findByEmail("regular@example.com");
        assertTrue(foundUser.isPresent());
        assertEquals("regular@example.com", foundUser.get().getEmail());

        
        assertTrue(userRepository.findByEmail("nonexistent@example.com").isEmpty());
    }

    @Test
    void testExistsByUsername() {
        
        assertTrue(userRepository.existsByUsername("regularuser"));
        assertFalse(userRepository.existsByUsername("nonexistent"));
    }

    @Test
    void testExistsByEmail() {
        
        assertTrue(userRepository.existsByEmail("regular@example.com"));
        assertFalse(userRepository.existsByEmail("nonexistent@example.com"));
    }

    @Test
    void testExistsByRole() {
        
        assertTrue(userRepository.existsByRole(UserRole.ADMIN));
        assertTrue(userRepository.existsByRole(UserRole.USER));
    }

    @Test
    void testFindByEnabled() {
        
        List<User> initialEnabledUsers = userRepository.findByEnabled(true);
        assertEquals(3, initialEnabledUsers.size(), "All users should be enabled initially");

        
        regularUser.setEnabled(false);
        userRepository.save(regularUser);

        
        List<User> enabledUsers = userRepository.findByEnabled(true);
        assertEquals(2, enabledUsers.size(), "Should now have 2 enabled users");

        
        List<User> disabledUsers = userRepository.findByEnabled(false);
        assertEquals(1, disabledUsers.size(), "Should have 1 disabled user");
        assertEquals("regularuser", disabledUsers.get(0).getUsername());
    }

    @Test
    void testFindByApproved() {
        
        List<User> approvedUsers = userRepository.findByApproved(true);
        assertEquals(2, approvedUsers.size(), "Should have 2 approved users");

        List<User> unapprovedUsers = userRepository.findByApproved(false);
        assertEquals(1, unapprovedUsers.size(), "Should have 1 unapproved user");
        assertEquals("pendinguser", unapprovedUsers.get(0).getUsername());
    }

    @Test
    void testFindByGender() {
        
        Pageable pageable = PageRequest.of(0, 10);

        Page<User> maleUsers = userRepository.findByGender(Gender.MALE, pageable);
        assertEquals(1, maleUsers.getContent().size(), "Should have 1 male user");
        assertEquals("regularuser", maleUsers.getContent().get(0).getUsername());

        Page<User> femaleUsers = userRepository.findByGender(Gender.FEMALE, pageable);
        assertEquals(1, femaleUsers.getContent().size(), "Should have 1 female user");
        assertEquals("adminuser", femaleUsers.getContent().get(0).getUsername());

        Page<User> otherUsers = userRepository.findByGender(Gender.OTHER, pageable);
        assertEquals(1, otherUsers.getContent().size(), "Should have 1 user with OTHER gender");
        assertEquals("pendinguser", otherUsers.getContent().get(0).getUsername());
    }

    @Test
    void testFindByApprovedAndEnabled() {
        
        List<User> allUsers = userRepository.findAll();
        System.out.println("Total users: " + allUsers.size());

        for (User u : allUsers) {
            System.out.println(String.format("User %s (ID: %s): enabled=%s, approved=%s",
                    u.getUsername(), u.getId(), u.isEnabled(), u.isApproved()));
        }

        
        assertEquals(3, userRepository.count(), "Should have 3 users in total");
        assertEquals(2, userRepository.findByApproved(true).size(), "Should have 2 approved users");
        assertEquals(3, userRepository.findByEnabled(true).size(), "Should have 3 enabled users");

        
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> approvedAndEnabledUsers = userRepository.findByApprovedAndEnabled(true, true, pageable);

        
        assertEquals(2, approvedAndEnabledUsers.getTotalElements(),
                "Should have 2 approved and enabled users");

        
        regularUser.setEnabled(false);
        userRepository.save(regularUser);

        
        approvedAndEnabledUsers = userRepository.findByApprovedAndEnabled(true, true, pageable);
        assertEquals(1, approvedAndEnabledUsers.getTotalElements(),
                "Should have 1 approved and enabled user after disabling regularUser");

        
        if (!approvedAndEnabledUsers.isEmpty()) {
            assertEquals("adminuser", approvedAndEnabledUsers.getContent().get(0).getUsername());
        }
    }

    @Test
    void testFindActiveUsersSince() {
        LocalDateTime twoDaysAgo = LocalDateTime.now().minusDays(2);

        
        List<User> activeUsers = userRepository.findActiveUsersSince(twoDaysAgo);
        assertEquals(2, activeUsers.size(), "Should have 2 users with lastLogin set");

        
        LocalDateTime hourAgo = LocalDateTime.now().minusHours(1);
        activeUsers = userRepository.findActiveUsersSince(hourAgo);
        assertEquals(1, activeUsers.size(), "Should have 1 user with very recent lastLogin");
        assertEquals("adminuser", activeUsers.get(0).getUsername());
    }

    @Test
    void testCountByRole() {
        
        long adminCount = userRepository.countByRole(UserRole.ADMIN);
        assertEquals(1, adminCount, "Should have 1 admin user");

        long userCount = userRepository.countByRole(UserRole.USER);
        assertEquals(2, userCount, "Should have 2 regular users");
    }

    @Test
    void testFindByEnabledAndApprovedAndProfileCompleted() {
        
        List<User> allUsers = userRepository.findAll();
        System.out.println("------- testFindByEnabledAndApprovedAndProfileCompleted ---------");
        for (User u : allUsers) {
            System.out.println(String.format("User %s: enabled=%s, approved=%s, profileCompleted=%s",
                    u.getUsername(), u.isEnabled(), u.isApproved(), u.isProfileCompleted()));
        }

        
        List<User> users = userRepository.findByEnabledAndApprovedAndProfileCompleted(true, true, true);
        assertEquals(2, users.size(), "Should have 2 enabled, approved users with complete profiles");

        
        User incompleteUser = new User();
        incompleteUser.setUsername("incomplete");
        incompleteUser.setEmail("incomplete@example.com");
        incompleteUser.setPassword("password");
        incompleteUser.setGender(Gender.MALE);
        incompleteUser.setRole(UserRole.USER);
        incompleteUser.setCreatedAt(LocalDateTime.now());
        incompleteUser.setUpdatedAt(LocalDateTime.now());

        User savedIncompleteUser = userRepository.save(incompleteUser);

        
        savedIncompleteUser.setApproved(true);
        savedIncompleteUser.setProfileCompleted(false);
        userRepository.save(savedIncompleteUser);

        
        users = userRepository.findByEnabledAndApprovedAndProfileCompleted(true, true, true);
        assertEquals(2, users.size(), "Should still have 2 users with complete profiles");

        
        users = userRepository.findByEnabledAndApprovedAndProfileCompleted(true, true, false);
        assertEquals(1, users.size(), "Should have 1 user with incomplete profile");
        assertEquals("incomplete", users.get(0).getUsername());
    }

    @Test
    void testFindPendingApprovalUsers() {
        
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> pendingUsers = userRepository.findPendingApprovalUsers(pageable);

        assertEquals(1, pendingUsers.getContent().size(), "Should have 1 pending approval user");
        assertEquals("pendinguser", pendingUsers.getContent().get(0).getUsername());

        
        unapprovedUser.setApproved(true);
        userRepository.save(unapprovedUser);

        
        pendingUsers = userRepository.findPendingApprovalUsers(pageable);
        assertTrue(pendingUsers.getContent().isEmpty(), "Should have no pending users after approval");
    }

    @Test
    void testCountByApprovedAndProfileCompleted() {
        
        List<User> allUsers = userRepository.findAll();
        System.out.println("------- testCountByApprovedAndProfileCompleted ---------");
        for (User u : allUsers) {
            System.out.println(String.format("User %s: approved=%s, profileCompleted=%s",
                    u.getUsername(), u.isApproved(), u.isProfileCompleted()));
        }

        
        long count = userRepository.countByApprovedAndProfileCompleted(true, true);
        assertEquals(2, count, "Should have 2 approved users with complete profiles");

        count = userRepository.countByApprovedAndProfileCompleted(false, true);
        assertEquals(1, count, "Should have 1 unapproved user with complete profile");
    }

    @Test
    void testCountByCreatedAtAfter() {
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);

        
        long recentUsers = userRepository.countByCreatedAtAfter(yesterday);
        assertEquals(3, recentUsers, "All users should be counted as recent");

        
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);
        recentUsers = userRepository.countByCreatedAtAfter(tomorrow);
        assertEquals(0, recentUsers, "No users should be created after tomorrow");
    }

    @Test
    void testCountByEnabledAndApproved() {
        
        long count = userRepository.countByEnabledAndApproved(true, true);
        assertEquals(2, count, "Should have 2 enabled and approved users");

        
        regularUser.setEnabled(false);
        userRepository.save(regularUser);

        
        count = userRepository.countByEnabledAndApproved(true, true);
        assertEquals(1, count, "Should have 1 enabled and approved user after disabling regularUser");

        count = userRepository.countByEnabledAndApproved(false, true);
        assertEquals(1, count, "Should have 1 disabled but approved user");
    }

    @Test
    void testFindByApprovedAndProfileCompleted() {
        
        List<User> allUsers = userRepository.findAll();
        System.out.println("------- testFindByApprovedAndProfileCompleted ---------");
        for (User u : allUsers) {
            System.out.println(String.format("User %s: approved=%s, profileCompleted=%s",
                    u.getUsername(), u.isApproved(), u.isProfileCompleted()));
        }

        
        List<User> users = userRepository.findByApprovedAndProfileCompleted(true, true);
        assertEquals(2, users.size(), "Should have 2 approved users with complete profiles");

        
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = userRepository.findByApprovedAndProfileCompleted(true, true, pageable);
        assertEquals(2, userPage.getContent().size(), "Pageable result should also have 2 users");
    }
}