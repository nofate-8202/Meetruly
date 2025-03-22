package com.meetruly.subscription.model;

import com.meetruly.core.constant.SubscriptionPlan;
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
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionPlan plan;

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column(nullable = false)
    private LocalDateTime endDate;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private boolean autoRenew;

    @Column
    private int dailyMessageCount;

    @Column
    private int profileViewsCount;

    @Column
    private LocalDateTime messageCountResetDate;

    @Column
    private LocalDateTime profileViewsResetDate;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {

        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        this.dailyMessageCount = 0;
        this.profileViewsCount = 0;

        this.messageCountResetDate = LocalDateTime.now().plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        this.profileViewsResetDate = LocalDateTime.now().plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return endDate.isBefore(LocalDateTime.now());
    }

    public boolean hasReachedMessageLimit() {
        return dailyMessageCount >= plan.getDailyMessageLimit();
    }

    public boolean hasReachedProfileViewLimit() {
        return profileViewsCount >= plan.getProfileViewsLimit();
    }
}