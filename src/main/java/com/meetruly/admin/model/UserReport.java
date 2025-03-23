package com.meetruly.admin.model;

import com.meetruly.core.constant.ReportStatus;
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
@Table(name = "user_reports")
public class UserReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_user_id", nullable = false)
    private User reportedUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportType reportType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reportReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status;

    @Column(columnDefinition = "TEXT")
    private String adminNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "handled_by_id")
    private User handledBy;

    @Column
    private LocalDateTime handledAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public enum ReportType {
        INAPPROPRIATE_CONTENT,
        HARASSMENT,
        FAKE_PROFILE,
        UNDERAGE_USER,
        OTHER
    }

    @PrePersist
    public void prePersist() {

        this.createdAt = LocalDateTime.now();
        this.status = ReportStatus.PENDING;
    }
}