package com.meetruly.matching.model;

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
@Table(name = "matches", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user1_id", "user2_id"})
})
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user1_id", nullable = false)
    private User user1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user2_id", nullable = false)
    private User user2;

    @Column
    private double compatibilityScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchType matchType;

    @Column(nullable = false)
    private boolean user1Viewed;

    @Column(nullable = false)
    private boolean user2Viewed;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public enum MatchType {
        MUTUAL_LIKE,
        DAILY_SUGGESTION,
        ADMIN_CREATED
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.user1Viewed = false;
        this.user2Viewed = false;
    }

    public boolean involveUser(UUID userId) {
        return user1.getId().equals(userId) || user2.getId().equals(userId);
    }

    public User getOtherUser(UUID userId) {
        if (user1.getId().equals(userId)) {
            return user2;
        } else if (user2.getId().equals(userId)) {
            return user1;
        }
        return null;
    }

    public void markViewedByUser(UUID userId) {
        if (user1.getId().equals(userId)) {
            this.user1Viewed = true;
        } else if (user2.getId().equals(userId)) {
            this.user2Viewed = true;
        }
    }

    public boolean isViewedByUser(UUID userId) {
        if (user1.getId().equals(userId)) {
            return this.user1Viewed;
        } else if (user2.getId().equals(userId)) {
            return this.user2Viewed;
        }
        return false;
    }
}