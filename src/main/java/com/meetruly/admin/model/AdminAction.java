package com.meetruly.admin.model;

import com.meetruly.user.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "admin_actions")
public class AdminAction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private User admin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id")
    private User targetUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionType actionType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String actionDetails;

    @Column(nullable = false)
    private LocalDateTime performedAt;

    @Column
    private String ipAddress;

    public enum ActionType {
        USER_APPROVAL,
        USER_REJECTION,
        USER_BLOCK,
        USER_UNBLOCK,
        ROLE_CHANGE,
        REPORT_HANDLING,
        MANUAL_MATCH_CREATION,
        SUBSCRIPTION_CHANGE,
        OTHER
    }

    @PrePersist
    public void prePersist() {

        this.performedAt = LocalDateTime.now();
    }
}