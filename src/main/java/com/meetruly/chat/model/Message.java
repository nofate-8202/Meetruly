package com.meetruly.chat.model;

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
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private boolean isRead;

    @Column(nullable = false)
    private boolean deleted;

    @Column
    private boolean deletedBySender;

    @Column
    private boolean deletedByRecipient;

    @Column(nullable = false)
    private LocalDateTime sentAt;

    @Column
    private LocalDateTime readAt;

    @Column
    private LocalDateTime deletedAt;
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.sentAt = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
        this.isRead = false;
        this.deleted = false;
        this.deletedBySender = false;
        this.deletedByRecipient = false;
    }

    public void markAsRead() {
        if (!this.isRead) {
            this.isRead = true;
            this.readAt = LocalDateTime.now();
        }
    }

    public void markAsDeletedByUser(UUID userId) {
        if (sender.getId().equals(userId)) {
            this.deletedBySender = true;
        } else if (recipient.getId().equals(userId)) {
            this.deletedByRecipient = true;
        }

        if (this.deletedBySender && this.deletedByRecipient) {
            this.deleted = true;
            this.deletedAt = LocalDateTime.now();
        }
    }

    public boolean isVisibleToUser(UUID userId) {
        if (sender.getId().equals(userId)) {
            return !this.deletedBySender;
        } else if (recipient.getId().equals(userId)) {
            return !this.deletedByRecipient;
        }
        return false;
    }
}