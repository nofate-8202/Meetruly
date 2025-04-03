package com.meetruly.admin.repository;

import com.meetruly.admin.model.UserBlock;
import com.meetruly.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class UserBlockRepositoryTest {

    @Autowired
    private UserBlockRepository userBlockRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User admin;
    private User blockedUser1;
    private User blockedUser2;
    private LocalDateTime baseTime;

    @BeforeEach
    public void setup() {

        admin = new User();
        admin.setUsername("admin1");
        admin.setEmail("admin1@example.com");
        admin.setPassword("password");
        admin.setGender(com.meetruly.core.constant.Gender.MALE);
        admin.setRole(com.meetruly.core.constant.UserRole.ADMIN);
        admin.setApproved(true);
        admin.setEnabled(true);
        admin.setEmailVerified(true);
        admin.setProfileCompleted(true);
        admin.setAccountNonLocked(true);
        admin.setCreatedAt(LocalDateTime.now());
        admin.setUpdatedAt(LocalDateTime.now());
        entityManager.persist(admin);

        blockedUser1 = new User();
        blockedUser1.setUsername("user1");
        blockedUser1.setEmail("user1@example.com");
        blockedUser1.setPassword("password");
        blockedUser1.setGender(com.meetruly.core.constant.Gender.FEMALE);
        blockedUser1.setRole(com.meetruly.core.constant.UserRole.USER);
        blockedUser1.setApproved(true);
        blockedUser1.setEnabled(true);
        blockedUser1.setEmailVerified(true);
        blockedUser1.setProfileCompleted(false);
        blockedUser1.setAccountNonLocked(true);
        blockedUser1.setCreatedAt(LocalDateTime.now());
        blockedUser1.setUpdatedAt(LocalDateTime.now());
        entityManager.persist(blockedUser1);

        blockedUser2 = new User();
        blockedUser2.setUsername("user2");
        blockedUser2.setEmail("user2@example.com");
        blockedUser2.setPassword("password");
        blockedUser2.setGender(com.meetruly.core.constant.Gender.OTHER);
        blockedUser2.setRole(com.meetruly.core.constant.UserRole.USER);
        blockedUser2.setApproved(true);
        blockedUser2.setEnabled(true);
        blockedUser2.setEmailVerified(true);
        blockedUser2.setProfileCompleted(false);
        blockedUser2.setAccountNonLocked(true);
        blockedUser2.setCreatedAt(LocalDateTime.now());
        blockedUser2.setUpdatedAt(LocalDateTime.now());
        entityManager.persist(blockedUser2);

        baseTime = LocalDateTime.of(2023, 1, 1, 0, 0);

        
        UserBlock block1 = UserBlock.builder()
                .blockedUser(blockedUser1)
                .blockedBy(admin)
                .reason("Inappropriate messages")
                .startDate(baseTime)
                .endDate(baseTime.plusMonths(6))
                .permanent(false)
                .active(true)
                .build();
        entityManager.persist(block1);

        
        UserBlock block2 = UserBlock.builder()
                .blockedUser(blockedUser2)
                .blockedBy(admin)
                .reason("Multiple violations")
                .startDate(baseTime.plusDays(5))
                .permanent(true)
                .active(true)
                .build();
        entityManager.persist(block2);

        
        UserBlock block3 = UserBlock.builder()
                .blockedUser(blockedUser1)
                .blockedBy(admin)
                .reason("Suspicious activity")
                .startDate(baseTime.minusDays(30))
                .endDate(baseTime.minusDays(23))
                .permanent(false)
                .active(false)
                .unblockDate(baseTime.minusDays(28))
                .unblockedBy(admin)
                .unblockReason("Appeal granted")
                .build();
        entityManager.persist(block3);

        
        UserBlock block4 = UserBlock.builder()
                .blockedUser(blockedUser2)
                .blockedBy(admin)
                .reason("Minor violation")
                .startDate(baseTime.minusDays(20))
                .endDate(baseTime.minusDays(13))
                .permanent(false)
                .active(true) 
                .build();
        entityManager.persist(block4);

        entityManager.flush();
    }

    @Test
    public void testSaveUserBlock() {
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endDate = now.plusDays(7);

        UserBlock userBlock = UserBlock.builder()
                .blockedUser(blockedUser2)
                .blockedBy(admin)
                .reason("New inappropriate behavior")
                .startDate(now)
                .endDate(endDate)
                .permanent(false)
                .active(true)
                .build();

        
        UserBlock savedBlock = userBlockRepository.save(userBlock);

        
        assertNotNull(savedBlock.getId());
        assertEquals(blockedUser2.getId(), savedBlock.getBlockedUser().getId());
        assertEquals(admin.getId(), savedBlock.getBlockedBy().getId());
        assertEquals("New inappropriate behavior", savedBlock.getReason());
        assertTrue(savedBlock.isActive());
        assertFalse(savedBlock.isPermanent());
        assertEquals(endDate, savedBlock.getEndDate());
    }

    @Test
    public void testFindByBlockedUser() {
        
        List<UserBlock> blocks = userBlockRepository.findByBlockedUser(blockedUser1);

        
        assertEquals(2, blocks.size()); 
        blocks.forEach(block -> assertEquals(blockedUser1.getId(), block.getBlockedUser().getId()));
    }

    @Test
    public void testFindActiveBlockByUser() {
        
        
        

        
        List<UserBlock> activeBlocks = userBlockRepository.findByBlockedUser(blockedUser2).stream()
                .filter(UserBlock::isActive)
                .toList();

        assertTrue(activeBlocks.size() > 0, "Should have at least one active block");
        activeBlocks.forEach(block -> {
            assertEquals(blockedUser2.getId(), block.getBlockedUser().getId());
            assertTrue(block.isActive());
        });
    }

    @Test
    public void testFindActiveBlocks() {
        
        List<UserBlock> activeBlocks = userBlockRepository.findActiveBlocks();

        
        assertEquals(4, activeBlocks.size()); 
        activeBlocks.forEach(block -> assertTrue(block.isActive()));
    }

    @Test
    public void testFindActiveBlocksWithPagination() {
        
        PageRequest pageRequest = PageRequest.of(0, 2);

        
        Page<UserBlock> activeBlocksPage = userBlockRepository.findActiveBlocks(pageRequest);

        
        assertEquals(2, activeBlocksPage.getContent().size());
        assertEquals(4, activeBlocksPage.getTotalElements()); 
        activeBlocksPage.forEach(block -> assertTrue(block.isActive()));
    }

    @Test
    public void testFindExpiredBlocks() {
        
        LocalDateTime now = baseTime.plusDays(1);

        
        List<UserBlock> expiredBlocks = userBlockRepository.findExpiredBlocks(now);

        
        assertEquals(2, expiredBlocks.size()); 
        expiredBlocks.forEach(block -> {
            assertTrue(block.isActive());
            assertFalse(block.isPermanent());
            assertTrue(block.getEndDate().isBefore(now));
        });
    }

    @Test
    public void testCountActiveBlocks() {
        
        long count = userBlockRepository.countActiveBlocks();

        
        assertEquals(4, count); 
    }

    @Test
    public void testCountPermanentBlocks() {
        
        long count = userBlockRepository.countPermanentBlocks();

        
        assertEquals(1, count);
    }

    @Test
    public void testCountBlocksSince() {
        
        LocalDateTime since = baseTime.minusDays(10);

        
        long count = userBlockRepository.countBlocksSince(since);

        
        assertEquals(4, count); 
    }

    @Test
    public void testIsExpiredMethod() {
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime pastDate = now.minusDays(1);
        LocalDateTime futureDate = now.plusDays(1);

        UserBlock permanentBlock = new UserBlock();
        permanentBlock.setPermanent(true);

        UserBlock expiredBlock = new UserBlock();
        expiredBlock.setPermanent(false);
        expiredBlock.setEndDate(pastDate);

        UserBlock activeBlock = new UserBlock();
        activeBlock.setPermanent(false);
        activeBlock.setEndDate(futureDate);

        
        assertFalse(permanentBlock.isExpired(), "Permanent block should never expire");
        assertTrue(expiredBlock.isExpired(), "Block with end date in the past should be expired");
        assertFalse(activeBlock.isExpired(), "Block with end date in the future should not be expired");
    }
}