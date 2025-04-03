package com.meetruly.user.repository;

import com.meetruly.core.constant.Gender;
import com.meetruly.core.constant.UserRole;
import com.meetruly.user.model.User;
import com.meetruly.user.model.VerificationToken;
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
class VerificationTokenRepositoryTest {

    @Autowired
    private VerificationTokenRepository verificationTokenRepository;

    @Autowired
    private UserRepository userRepository;

    private User user1;
    private User user2;
    private VerificationToken emailToken;
    private VerificationToken passwordToken;

    @BeforeEach
    void setUp() {
        
        verificationTokenRepository.deleteAll();
        userRepository.deleteAll();

        LocalDateTime now = LocalDateTime.now();

        
        user1 = new User();
        user1.setUsername("testuser1");
        user1.setEmail("test1@example.com");
        user1.setPassword("password");
        user1.setGender(Gender.MALE);
        user1.setRole(UserRole.USER);
        user1.setCreatedAt(now);
        user1.setUpdatedAt(now);

        user2 = new User();
        user2.setUsername("testuser2");
        user2.setEmail("test2@example.com");
        user2.setPassword("password");
        user2.setGender(Gender.FEMALE);
        user2.setRole(UserRole.USER);
        user2.setCreatedAt(now);
        user2.setUpdatedAt(now);

        
        user1 = userRepository.save(user1);
        user2 = userRepository.save(user2);

        
        emailToken = new VerificationToken();
        emailToken.setToken(UUID.randomUUID().toString());
        emailToken.setUser(user1);
        emailToken.setTokenType(VerificationToken.TokenType.EMAIL_VERIFICATION);
        emailToken.setCreatedAt(now);
        emailToken.setExpiryDate(now.plusDays(1)); 

        passwordToken = new VerificationToken();
        passwordToken.setToken(UUID.randomUUID().toString());
        passwordToken.setUser(user1);
        passwordToken.setTokenType(VerificationToken.TokenType.PASSWORD_RESET);
        passwordToken.setCreatedAt(now);
        passwordToken.setExpiryDate(now.plusHours(2)); 

        
        emailToken = verificationTokenRepository.save(emailToken);
        passwordToken = verificationTokenRepository.save(passwordToken);

        
        assertEquals(2, verificationTokenRepository.count(), "Should have 2 tokens saved in the test database");
    }

    @Test
    void testBasicCrud() {
        
        VerificationToken newToken = new VerificationToken();
        newToken.setToken(UUID.randomUUID().toString());
        newToken.setUser(user2);
        newToken.setTokenType(VerificationToken.TokenType.EMAIL_VERIFICATION);
        newToken.setCreatedAt(LocalDateTime.now());
        newToken.setExpiryDate(LocalDateTime.now().plusDays(1));

        VerificationToken savedToken = verificationTokenRepository.save(newToken);
        assertNotNull(savedToken.getId());

        
        Optional<VerificationToken> foundToken = verificationTokenRepository.findById(savedToken.getId());
        assertTrue(foundToken.isPresent());
        assertEquals(user2.getId(), foundToken.get().getUser().getId());

        
        LocalDateTime newExpiryDate = LocalDateTime.now().plusDays(7);
        foundToken.get().setExpiryDate(newExpiryDate);
        verificationTokenRepository.save(foundToken.get());

        VerificationToken updatedToken = verificationTokenRepository.findById(savedToken.getId()).get();
        assertEquals(newExpiryDate, updatedToken.getExpiryDate());

        
        verificationTokenRepository.delete(updatedToken);
        assertFalse(verificationTokenRepository.findById(savedToken.getId()).isPresent());
    }

    @Test
    void testFindByToken() {
        Optional<VerificationToken> foundToken = verificationTokenRepository.findByToken(emailToken.getToken());
        assertTrue(foundToken.isPresent());
        assertEquals(VerificationToken.TokenType.EMAIL_VERIFICATION, foundToken.get().getTokenType());
        assertEquals(user1.getId(), foundToken.get().getUser().getId());

        
        Optional<VerificationToken> notFoundToken = verificationTokenRepository.findByToken("invalid-token");
        assertFalse(notFoundToken.isPresent());
    }

    @Test
    void testFindByUser() {
        List<VerificationToken> userTokens = verificationTokenRepository.findByUser(user1);
        assertEquals(2, userTokens.size());

        
        List<VerificationToken> user2Tokens = verificationTokenRepository.findByUser(user2);
        assertTrue(user2Tokens.isEmpty());

        
        VerificationToken user2Token = new VerificationToken();
        user2Token.setToken(UUID.randomUUID().toString());
        user2Token.setUser(user2);
        user2Token.setTokenType(VerificationToken.TokenType.EMAIL_VERIFICATION);
        user2Token.setCreatedAt(LocalDateTime.now());
        user2Token.setExpiryDate(LocalDateTime.now().plusDays(1));
        verificationTokenRepository.save(user2Token);

        user2Tokens = verificationTokenRepository.findByUser(user2);
        assertEquals(1, user2Tokens.size());
    }

    @Test
    void testFindByUserAndTokenType() {
        List<VerificationToken> emailTokens = verificationTokenRepository.findByUserAndTokenType(user1, VerificationToken.TokenType.EMAIL_VERIFICATION);
        assertEquals(1, emailTokens.size());
        assertEquals(emailToken.getId(), emailTokens.get(0).getId());

        List<VerificationToken> passwordTokens = verificationTokenRepository.findByUserAndTokenType(user1, VerificationToken.TokenType.PASSWORD_RESET);
        assertEquals(1, passwordTokens.size());
        assertEquals(passwordToken.getId(), passwordTokens.get(0).getId());

        
        List<VerificationToken> noTokens = verificationTokenRepository.findByUserAndTokenType(user2, VerificationToken.TokenType.PASSWORD_RESET);
        assertTrue(noTokens.isEmpty());
    }

    @Test
    void testFindAllExpiredTokens() {
        
        VerificationToken expiredToken = new VerificationToken();
        expiredToken.setToken(UUID.randomUUID().toString());
        expiredToken.setUser(user2);
        expiredToken.setTokenType(VerificationToken.TokenType.EMAIL_VERIFICATION);
        expiredToken.setCreatedAt(LocalDateTime.now().minusDays(2));
        expiredToken.setExpiryDate(LocalDateTime.now().minusDays(1)); 
        verificationTokenRepository.save(expiredToken);

        
        LocalDateTime now = LocalDateTime.now();
        List<VerificationToken> expiredTokens = verificationTokenRepository.findAllExpiredTokens(now);
        assertEquals(1, expiredTokens.size());
        assertEquals(expiredToken.getId(), expiredTokens.get(0).getId());

        
        assertTrue(expiredToken.isExpired());
        assertFalse(emailToken.isExpired());
        assertFalse(passwordToken.isExpired());
    }

    @Test
    void testDeleteByUser() {
        
        assertEquals(2, verificationTokenRepository.count());

        
        verificationTokenRepository.deleteByUser(user1);

        
        assertEquals(0, verificationTokenRepository.count());
        assertTrue(verificationTokenRepository.findByUser(user1).isEmpty());

        
        VerificationToken user2Token = new VerificationToken();
        user2Token.setToken(UUID.randomUUID().toString());
        user2Token.setUser(user2);
        user2Token.setTokenType(VerificationToken.TokenType.EMAIL_VERIFICATION);
        user2Token.setCreatedAt(LocalDateTime.now());
        user2Token.setExpiryDate(LocalDateTime.now().plusDays(1));
        verificationTokenRepository.save(user2Token);

        
        VerificationToken newUser1Token = new VerificationToken();
        newUser1Token.setToken(UUID.randomUUID().toString());
        newUser1Token.setUser(user1);
        newUser1Token.setTokenType(VerificationToken.TokenType.EMAIL_VERIFICATION);
        newUser1Token.setCreatedAt(LocalDateTime.now());
        newUser1Token.setExpiryDate(LocalDateTime.now().plusDays(1));
        verificationTokenRepository.save(newUser1Token);

        
        assertEquals(1, verificationTokenRepository.findByUser(user1).size());
        assertEquals(1, verificationTokenRepository.findByUser(user2).size());

        
        verificationTokenRepository.deleteByUser(user2);

        
        assertEquals(1, verificationTokenRepository.count());
        assertEquals(1, verificationTokenRepository.findByUser(user1).size());
        assertTrue(verificationTokenRepository.findByUser(user2).isEmpty());
    }

    @Test
    void testDeleteByExpiryDateBefore() {
        
        LocalDateTime now = LocalDateTime.now();

        
        VerificationToken oldToken1 = new VerificationToken();
        oldToken1.setToken(UUID.randomUUID().toString());
        oldToken1.setUser(user2);
        oldToken1.setTokenType(VerificationToken.TokenType.EMAIL_VERIFICATION);
        oldToken1.setCreatedAt(now.minusDays(4));
        oldToken1.setExpiryDate(now.minusDays(3));
        verificationTokenRepository.save(oldToken1);

        
        VerificationToken oldToken2 = new VerificationToken();
        oldToken2.setToken(UUID.randomUUID().toString());
        oldToken2.setUser(user2);
        oldToken2.setTokenType(VerificationToken.TokenType.PASSWORD_RESET);
        oldToken2.setCreatedAt(now.minusDays(2));
        oldToken2.setExpiryDate(now.minusDays(1));
        verificationTokenRepository.save(oldToken2);

        
        assertEquals(4, verificationTokenRepository.count());

        
        verificationTokenRepository.deleteByExpiryDateBefore(now.minusDays(2));

        
        assertEquals(3, verificationTokenRepository.count());

        
        verificationTokenRepository.deleteByExpiryDateBefore(now);

        
        assertEquals(2, verificationTokenRepository.count());

        
        List<VerificationToken> remainingTokens = verificationTokenRepository.findAll();
        for (VerificationToken token : remainingTokens) {
            assertTrue(token.getExpiryDate().isAfter(now));
        }
    }
}
