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
@Table(name = "profile_views")
public class ProfileView {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "viewer_id", nullable = false)
    private User viewer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "viewed_id", nullable = false)
    private User viewed;

    @Column(nullable = false)
    private LocalDateTime viewedAt;

    @Column(nullable = false)
    private boolean notificationSent;

    @PrePersist
    public void prePersist() {

        this.viewedAt = LocalDateTime.now();
        this.notificationSent = false;
    }
}