package com.meetruly.user.model;

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
@Table(name = "user_sessions")
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true)
    private String sessionId;

    @Column(nullable = false)
    private String ipAddress;

    @Column
    private String userAgent;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime lastActive;

    @Column
    private LocalDateTime expiryDate;

    @Column
    private LocalDateTime logoutTime;

    @Column(nullable = false)
    private boolean active;

    @PrePersist
    public void prePersist() {

        this.createdAt = LocalDateTime.now();
        this.lastActive = LocalDateTime.now();
        this.active = true;

        if (this.expiryDate == null) {
            this.expiryDate = LocalDateTime.now().plusDays(30);
        }
    }
}