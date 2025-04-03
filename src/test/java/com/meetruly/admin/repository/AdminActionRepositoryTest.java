package com.meetruly.admin.repository;

import com.meetruly.admin.model.AdminAction;
import com.meetruly.admin.model.AdminAction.ActionType;
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

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class AdminActionRepositoryTest {

    @Autowired
    private AdminActionRepository adminActionRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User admin;
    private User targetUser;
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

        targetUser = new User();
        targetUser.setUsername("user1");
        targetUser.setEmail("user1@example.com");
        targetUser.setPassword("password");
        targetUser.setGender(com.meetruly.core.constant.Gender.FEMALE);
        targetUser.setRole(com.meetruly.core.constant.UserRole.USER); 
        targetUser.setApproved(true);
        targetUser.setEnabled(true);
        targetUser.setEmailVerified(true);
        targetUser.setProfileCompleted(false);
        targetUser.setAccountNonLocked(true);
        targetUser.setCreatedAt(LocalDateTime.now());
        targetUser.setUpdatedAt(LocalDateTime.now());
        entityManager.persist(targetUser);

        baseTime = LocalDateTime.of(2023, 1, 1, 0, 0);

        
        AdminAction action1 = AdminAction.builder()
                .admin(admin)
                .targetUser(targetUser)
                .actionType(ActionType.USER_APPROVAL)
                .actionDetails("Approved user registration")
                .performedAt(baseTime.plusDays(5))
                .ipAddress("192.168.1.1")
                .build();
        entityManager.persist(action1);

        AdminAction action2 = AdminAction.builder()
                .admin(admin)
                .targetUser(targetUser)
                .actionType(ActionType.ROLE_CHANGE)
                .actionDetails("Changed role from USER to PREMIUM_USER")
                .performedAt(baseTime.plusDays(10))
                .ipAddress("192.168.1.1")
                .build();
        entityManager.persist(action2);

        AdminAction action3 = AdminAction.builder()
                .admin(admin)
                .targetUser(targetUser)
                .actionType(ActionType.USER_BLOCK)
                .actionDetails("Blocked user for inappropriate behavior")
                .performedAt(baseTime.plusDays(15))
                .ipAddress("192.168.1.1")
                .build();
        entityManager.persist(action3);

        AdminAction action4 = AdminAction.builder()
                .admin(admin)
                .actionType(ActionType.OTHER)
                .actionDetails("System configuration change")
                .performedAt(baseTime.plusDays(20))
                .ipAddress("192.168.1.1")
                .build();
        entityManager.persist(action4);

        entityManager.flush();
    }

    @Test
    public void testSaveAdminAction() {
        
        AdminAction adminAction = AdminAction.builder()
                .admin(admin)
                .targetUser(targetUser)
                .actionType(ActionType.USER_UNBLOCK)
                .actionDetails("Unblocked user after review")
                .performedAt(LocalDateTime.now())
                .ipAddress("192.168.1.1")
                .build();

        
        AdminAction savedAction = adminActionRepository.save(adminAction);

        
        assertNotNull(savedAction.getId());
        assertEquals(ActionType.USER_UNBLOCK, savedAction.getActionType());
        assertEquals("Unblocked user after review", savedAction.getActionDetails());
        assertEquals(admin.getId(), savedAction.getAdmin().getId());
        assertEquals(targetUser.getId(), savedAction.getTargetUser().getId());
    }

    @Test
    public void testFindByAdmin() {
        
        List<AdminAction> actions = adminActionRepository.findByAdmin(admin);

        
        assertEquals(4, actions.size());
        actions.forEach(action -> assertEquals(admin.getId(), action.getAdmin().getId()));
    }

    @Test
    public void testFindByAdminWithPagination() {
        
        PageRequest pageRequest = PageRequest.of(0, 2);

        
        Page<AdminAction> actionsPage = adminActionRepository.findByAdmin(admin, pageRequest);

        
        assertEquals(2, actionsPage.getContent().size());
        assertEquals(4, actionsPage.getTotalElements());
        actionsPage.forEach(action -> assertEquals(admin.getId(), action.getAdmin().getId()));
    }

    @Test
    public void testFindByTargetUser() {
        
        List<AdminAction> actions = adminActionRepository.findByTargetUser(targetUser);

        
        assertEquals(3, actions.size());
        actions.forEach(action -> assertEquals(targetUser.getId(), action.getTargetUser().getId()));
    }

    @Test
    public void testFindByTargetUserWithPagination() {
        
        PageRequest pageRequest = PageRequest.of(0, 2);

        
        Page<AdminAction> actionsPage = adminActionRepository.findByTargetUser(targetUser, pageRequest);

        
        assertEquals(2, actionsPage.getContent().size());
        assertEquals(3, actionsPage.getTotalElements());
        actionsPage.forEach(action -> assertEquals(targetUser.getId(), action.getTargetUser().getId()));
    }

    @Test
    public void testFindByDateRange() {
        
        LocalDateTime startDate = baseTime.plusDays(5);
        LocalDateTime endDate = baseTime.plusDays(15);

        
        List<AdminAction> actions = adminActionRepository.findByDateRange(startDate, endDate);

        
        assertEquals(3, actions.size());
        actions.forEach(action -> {
            assertTrue(action.getPerformedAt().isAfter(startDate.minusSeconds(1)));
            assertTrue(action.getPerformedAt().isBefore(endDate.plusSeconds(1)));
        });
    }

    @Test
    public void testFindByActionType() {
        
        ActionType actionType = ActionType.USER_BLOCK;

        
        List<AdminAction> actions = adminActionRepository.findByActionType(actionType);

        
        assertEquals(1, actions.size());
        actions.forEach(action -> assertEquals(actionType, action.getActionType()));
    }

    @Test
    public void testCountActionsSince() {
        
        LocalDateTime since = baseTime.plusDays(10);

        
        long count = adminActionRepository.countActionsSince(since);

        
        assertEquals(3, count);
    }

    @Test
    public void testCountActionsByTypeInDateRange() {
        
        LocalDateTime startDate = baseTime;
        LocalDateTime endDate = baseTime.plusDays(30);

        
        List<Object[]> results = adminActionRepository.countActionsByTypeInDateRange(startDate, endDate);

        
        assertEquals(3, results.size()); 
        for (Object[] result : results) {
            assertTrue(result[0] instanceof ActionType);
            assertTrue(result[1] instanceof Long);
            assertTrue((Long)result[1] > 0);
        }
    }
}