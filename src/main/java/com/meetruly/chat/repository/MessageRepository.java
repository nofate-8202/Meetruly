package com.meetruly.chat.repository;

import com.meetruly.chat.model.ChatRoom;
import com.meetruly.chat.model.Message;
import com.meetruly.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    @Query("SELECT m FROM Message m WHERE m.chatRoom = :chatRoom AND m.deleted = false ORDER BY m.sentAt ASC")
    List<Message> findByChatRoom(@Param("chatRoom") ChatRoom chatRoom);

    @Query("SELECT m FROM Message m WHERE m.chatRoom = :chatRoom AND m.deleted = false ORDER BY m.sentAt DESC")
    Page<Message> findByChatRoom(@Param("chatRoom") ChatRoom chatRoom, Pageable pageable);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.recipient = :user AND m.isRead = false AND m.deleted = false")
    long countUnreadMessages(@Param("user") User user);

    @Query("SELECT m FROM Message m WHERE m.recipient = :user AND m.isRead = false AND m.deleted = false")
    List<Message> findUnreadMessagesByUser(@Param("user") User user);

    @Query("SELECT m FROM Message m WHERE m.chatRoom = :chatRoom AND m.recipient = :user AND m.isRead = false AND m.deleted = false")
    List<Message> findUnreadMessagesByChatRoomAndUser(@Param("chatRoom") ChatRoom chatRoom, @Param("user") User user);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.chatRoom = :chatRoom AND m.recipient = :user AND m.isRead = false AND m.deleted = false")
    long countUnreadMessagesByChatRoomAndUser(@Param("chatRoom") ChatRoom chatRoom, @Param("user") User user);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.sender = :user AND m.sentAt >= :since AND m.deleted = false")
    long countMessagesSentByUserSince(@Param("user") User user, @Param("since") LocalDateTime since);

    @Query("SELECT m FROM Message m WHERE m.sender = :user AND m.sentAt >= :since AND m.deleted = false")
    List<Message> findMessagesSentByUserSince(@Param("user") User user, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.createdAt >= :date")
    long countByCreatedAtAfter(@Param("date") LocalDateTime date);
}