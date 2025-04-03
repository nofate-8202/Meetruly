package com.meetruly.user.repository;

import com.meetruly.core.constant.Gender;
import com.meetruly.core.constant.UserRole;
import com.meetruly.user.model.User;
import com.meetruly.user.model.UserSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class UserSessionRepositoryTest {

    @Autowired
    private UserSessionRepository userSessionRepository;

    @Autowired
    private UserRepository userRepository;

    private User activeUser;
    private User inactiveUser;
    private UserSession activeSession;
    private UserSession expiredSession;
    private UserSession inactiveSession;

    @BeforeEach
    void setUp() {
        
        userSessionRepository.deleteAll();
        userRepository.deleteAll();

        LocalDateTime now = LocalDateTime.now();

        
        activeUser = new User();
        activeUser.setUsername("activeuser");
        activeUser.setEmail("active@example.com");
        activeUser.setPassword("password");
        activeUser.setGender(Gender.MALE);
        activeUser.setRole(UserRole.USER);
        activeUser.setCreatedAt(now);
        activeUser.setUpdatedAt(now);

        inactiveUser = new User();
        inactiveUser.setUsername("inactiveuser");
        inactiveUser.setEmail("inactive@example.com");
        inactiveUser.setPassword("password");
        inactiveUser.setGender(Gender.FEMALE);
        inactiveUser.setRole(UserRole.USER);
        inactiveUser.setCreatedAt(now);
        inactiveUser.setUpdatedAt(now);

        
        activeUser = userRepository.save(activeUser);
        inactiveUser = userRepository.save(inactiveUser);

        
        
        activeSession = new UserSession();
        activeSession.setUser(activeUser);
        activeSession.setSessionId(UUID.randomUUID().toString());
        activeSession.setIpAddress("192.168.1.1");
        activeSession.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
        activeSession.setCreatedAt(now.minusHours(1)); 
        activeSession.setLastActive(now);
        activeSession.setExpiryDate(now.plusDays(30));
        activeSession.setActive(true);

        
        expiredSession = new UserSession();
        expiredSession.setUser(activeUser);
        expiredSession.setSessionId(UUID.randomUUID().toString());
        expiredSession.setIpAddress("192.168.1.2");
        expiredSession.setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)");
        expiredSession.setCreatedAt(now.minusDays(31)); 
        expiredSession.setLastActive(now.minusDays(10)); 
        expiredSession.setExpiryDate(now.minusDays(1));
        expiredSession.setActive(true); 

        
        inactiveSession = new UserSession();
        inactiveSession.setUser(inactiveUser);
        inactiveSession.setSessionId(UUID.randomUUID().toString());
        inactiveSession.setIpAddress("192.168.1.3");
        inactiveSession.setUserAgent("Mozilla/5.0 (iPhone; CPU iPhone OS 15_0)");
        inactiveSession.setCreatedAt(now.minusDays(10)); 
        inactiveSession.setLastActive(now.minusDays(5));
        inactiveSession.setExpiryDate(now.plusDays(20));
        inactiveSession.setActive(false); 
        inactiveSession.setLogoutTime(now.minusDays(5));

        
        activeSession = userSessionRepository.save(activeSession);
        expiredSession = userSessionRepository.save(expiredSession);
        inactiveSession = userSessionRepository.save(inactiveSession);

        
        assertEquals(3, userSessionRepository.count(), "Should have 3 sessions saved in the test database");

        
        System.out.println("--- Sessions after setup ---");
        List<UserSession> allSessions = userSessionRepository.findAll();
        for (UserSession session : allSessions) {
            System.out.println("Session ID: " + session.getSessionId() +
                    ", User: " + session.getUser().getUsername() +
                    ", Created at: " + session.getCreatedAt() +
                    ", Last active: " + session.getLastActive() +
                    ", Is active: " + session.isActive());
        }
    }

    @Test
    void testBasicCrud() {
        
        UserSession newSession = new UserSession();
        newSession.setUser(activeUser);
        newSession.setSessionId(UUID.randomUUID().toString());
        newSession.setIpAddress("192.168.1.100");
        newSession.setUserAgent("Chrome/94.0.4606.71");
        newSession.setCreatedAt(LocalDateTime.now());
        newSession.setLastActive(LocalDateTime.now());
        newSession.setExpiryDate(LocalDateTime.now().plusDays(30));
        newSession.setActive(true);

        UserSession savedSession = userSessionRepository.save(newSession);
        assertNotNull(savedSession.getId());

        
        Optional<UserSession> foundSession = userSessionRepository.findById(savedSession.getId());
        assertTrue(foundSession.isPresent());
        assertEquals("192.168.1.100", foundSession.get().getIpAddress());

        
        LocalDateTime newLastActive = LocalDateTime.now().plusHours(1);
        foundSession.get().setLastActive(newLastActive);
        userSessionRepository.save(foundSession.get());

        UserSession updatedSession = userSessionRepository.findById(savedSession.getId()).get();
        assertEquals(newLastActive, updatedSession.getLastActive());

        
        userSessionRepository.delete(updatedSession);
        assertFalse(userSessionRepository.findById(savedSession.getId()).isPresent());
    }

    @Test
    void testFindBySessionId() {
        Optional<UserSession> foundSession = userSessionRepository.findBySessionId(activeSession.getSessionId());
        assertTrue(foundSession.isPresent());
        assertEquals(activeUser.getId(), foundSession.get().getUser().getId());
        assertEquals("192.168.1.1", foundSession.get().getIpAddress());

        
        Optional<UserSession> notFoundSession = userSessionRepository.findBySessionId("invalid-session-id");
        assertFalse(notFoundSession.isPresent());
    }

    @Test
    void testFindByUser() {
        List<UserSession> userSessions = userSessionRepository.findByUser(activeUser);
        assertEquals(2, userSessions.size());

        List<UserSession> inactiveUserSessions = userSessionRepository.findByUser(inactiveUser);
        assertEquals(1, inactiveUserSessions.size());
        assertEquals(inactiveSession.getId(), inactiveUserSessions.get(0).getId());
    }

    @Test
    void testFindByUserAndActive() {
        
        UserSession inactiveUserSession = userSessionRepository.findAll().stream()
                .filter(s -> s.getUser().getUsername().equals("inactiveuser"))
                .findFirst()
                .orElseThrow();
        inactiveUserSession.setActive(false);
        userSessionRepository.save(inactiveUserSession);

        
        List<UserSession> activeSessions = userSessionRepository.findByUserAndActive(activeUser, true);
        assertEquals(2, activeSessions.size(), "Should find 2 active sessions for activeUser");

        
        List<UserSession> inactiveSessions = userSessionRepository.findByUserAndActive(activeUser, false);
        assertEquals(0, inactiveSessions.size(), "Should find 0 inactive sessions for activeUser");

        
        List<UserSession> inactiveUserActiveSessions = userSessionRepository.findByUserAndActive(inactiveUser, true);
        assertEquals(0, inactiveUserActiveSessions.size(), "Should find 0 active sessions for inactiveUser");

        
        List<UserSession> inactiveUserInactiveSessions = userSessionRepository.findByUserAndActive(inactiveUser, false);
        assertEquals(1, inactiveUserInactiveSessions.size(), "Should find 1 inactive session for inactiveUser");
    }

    @Test
    void testFindAllExpiredActiveSessions() {
        LocalDateTime now = LocalDateTime.now();
        List<UserSession> expiredActiveSessions = userSessionRepository.findAllExpiredActiveSessions(now);
        assertEquals(1, expiredActiveSessions.size());
        assertEquals(expiredSession.getId(), expiredActiveSessions.get(0).getId());

        
        List<UserSession> noExpiredSessions = userSessionRepository.findAllExpiredActiveSessions(now.minusDays(32));
        assertEquals(0, noExpiredSessions.size());
    }

    @Test
    void testFindInactiveSessions() {
        LocalDateTime now = LocalDateTime.now();

        
        expiredSession.setLastActive(now.minusDays(10));
        userSessionRepository.save(expiredSession);

        
        List<UserSession> inactiveSessions = userSessionRepository.findInactiveSessions(now.minusDays(3));

        assertEquals(1, inactiveSessions.size(), "Should find one session inactive for 3+ days");

        
        if (!inactiveSessions.isEmpty()) {
            assertEquals(expiredSession.getId(), inactiveSessions.get(0).getId(),
                    "The found inactive session should be the expired session");
        }
    }

    @Test
    void testCountSessionsCreatedSince() {
        
        LocalDateTime now = LocalDateTime.now();

        
        activeSession.setCreatedAt(now.minusHours(1)); 
        expiredSession.setCreatedAt(now.minusDays(31)); 
        inactiveSession.setCreatedAt(now.minusDays(10)); 

        
        userSessionRepository.save(activeSession);
        userSessionRepository.save(expiredSession);
        userSessionRepository.save(inactiveSession);

        
        assertEquals(3, userSessionRepository.count(), "Should have 3 total sessions");

        
        long recentSessions = userSessionRepository.countSessionsCreatedSince(now.minusDays(2));

        assertEquals(1, recentSessions, "Should find only one session created in the last 2 days");

        
        long moreSessions = userSessionRepository.countSessionsCreatedSince(now.minusDays(15));
        assertEquals(2, moreSessions, "Should find two sessions created in the last 15 days");

        
        long totalSessions = userSessionRepository.countSessionsCreatedSince(now.minusDays(40));
        assertEquals(3, totalSessions, "Should find all three sessions created in the last 40 days");
    }

    @Test
    void testDeleteByUser() {
        
        assertEquals(2, userSessionRepository.findByUser(activeUser).size());
        assertEquals(1, userSessionRepository.findByUser(inactiveUser).size());

        
        userSessionRepository.deleteByUser(activeUser);

        
        assertEquals(0, userSessionRepository.findByUser(activeUser).size());
        assertEquals(1, userSessionRepository.findByUser(inactiveUser).size());

        
        userSessionRepository.deleteByUser(inactiveUser);

        
        assertEquals(0, userSessionRepository.count());
    }
}