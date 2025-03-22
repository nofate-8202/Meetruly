package com.meetruly.chat.repository;

import com.meetruly.chat.model.ChatRoom;
import com.meetruly.chat.model.UserTypingStatus;
import com.meetruly.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserTypingStatusRepository extends JpaRepository<UserTypingStatus, UUID> {

    Optional<UserTypingStatus> findByUserAndChatRoom(User user, ChatRoom chatRoom);

    @Query("SELECT uts FROM UserTypingStatus uts WHERE uts.chatRoom = :chatRoom AND uts.user != :currentUser AND uts.typing = true AND uts.lastTypingActivity >= :since")
    List<UserTypingStatus> findActiveTypingUsersByChatRoom(@Param("chatRoom") ChatRoom chatRoom, @Param("currentUser") User currentUser, @Param("since") LocalDateTime since);

    @Query("SELECT uts FROM UserTypingStatus uts WHERE uts.lastTypingActivity < :time")
    List<UserTypingStatus> findInactiveTypingStatusesBefore(@Param("time") LocalDateTime time);
}