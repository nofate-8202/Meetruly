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
@Table(name = "user_blocks")
public class UserBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_user_id", nullable = false)
    private User blockedUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_by_id", nullable = false)
    private User blockedBy;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column
    private LocalDateTime endDate;

    @Column(nullable = false)
    private boolean permanent;

    @Column(nullable = false)
    private boolean active;

    @Column
    private LocalDateTime unblockDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unblocked_by_id")
    private User unblockedBy;

    @Column(columnDefinition = "TEXT")
    private String unblockReason;

    @PrePersist
    public void prePersist() {

        this.startDate = LocalDateTime.now();
        this.active = true;
    }

    public boolean isExpired() {
        if (permanent) {
            return false;
        }

        return endDate != null && endDate.isBefore(LocalDateTime.now());
    }
}