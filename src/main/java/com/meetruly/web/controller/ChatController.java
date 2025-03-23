package com.meetruly.web.controller;

import com.meetruly.chat.dto.*;
import com.meetruly.chat.model.ChatRoom;
import com.meetruly.chat.service.ChatService;
import com.meetruly.core.exception.MeetrulyException;
import com.meetruly.user.model.User;
import com.meetruly.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public String showChatDashboard(Model model, Principal principal) {
        try {

            String username = principal.getName();
            User user = userService.getUserByUsername(username)
                    .orElseThrow(() -> new MeetrulyException("User not found: " + username));

            ChatSummaryDto chatSummary = chatService.getChatSummary(user.getId());

            model.addAttribute("chatSummary", chatSummary);

            return "chat/dashboard";
        } catch (MeetrulyException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "redirect:/home";
        }
    }

    @GetMapping("/{chatRoomId}")
    @PreAuthorize("isAuthenticated()")
    public String showChatRoom(@PathVariable("chatRoomId") UUID chatRoomId,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "20") int size,
                               Model model, Principal principal) {

        try {

            String username = principal.getName();
            log.info("Showing chat room {} for user {}", chatRoomId, username);

            User user = userService.getUserByUsername(username)
                    .orElseThrow(() -> new MeetrulyException("User not found: " + username));

            ChatRoomDto chatRoom = chatService.getChatRoomById(chatRoomId, user.getId());

            PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "sentAt"));

            Page<MessageDto> messagesPage = chatService.getMessagesByChatRoom(chatRoomId, user.getId(), pageRequest);

            chatService.markMessagesAsRead(chatRoomId, user.getId());

            boolean canSendMessage = chatService.canSendMessage(user.getId());
            int remainingMessages = chatService.getRemainingMessages(user.getId());

            model.addAttribute("chatRoom", chatRoom);
            model.addAttribute("messages", messagesPage.getContent());
            model.addAttribute("currentPage", messagesPage.getNumber());
            model.addAttribute("totalPages", messagesPage.getTotalPages());
            model.addAttribute("canSendMessage", canSendMessage);
            model.addAttribute("remainingMessages", remainingMessages);
            model.addAttribute("currentUserId", user.getId());

            return "chat/chat-room";
        } catch (Exception e) {
            log.error("Error showing chat room: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Error: " + e.getMessage());
            return "redirect:/chat";
        }
    }

    @GetMapping("/{chatRoomId}/messages")
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    public ResponseEntity<?> getMessages(
            @PathVariable("chatRoomId") UUID chatRoomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal) {

        try {

            String username = principal.getName();
            User user = userService.getUserByUsername(username)
                    .orElseThrow(() -> new MeetrulyException("User not found: " + username));

            PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "sentAt"));

            Page<MessageDto> messagesPage = chatService.getMessagesByChatRoom(chatRoomId, user.getId(), pageRequest);

            return ResponseEntity.ok(messagesPage);
        } catch (MeetrulyException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/new/{userId}")
    @PreAuthorize("isAuthenticated()")
    public String createNewChat(@PathVariable("userId") UUID userId, Model model, Principal principal) {
        try {

            String username = principal.getName();
            User currentUser = userService.getUserByUsername(username)
                    .orElseThrow(() -> new MeetrulyException("User not found: " + username));

            log.info("Creating chat: current user {} with user {}", currentUser.getId(), userId);

            if (currentUser.getId().equals(userId)) {
                log.warn("User attempted to chat with self");
                model.addAttribute("errorMessage", "You cannot chat with yourself");
                return "redirect:/chat";
            }

            User otherUser = userService.getUserById(userId);
            log.info("Other user found: {}", otherUser.getUsername());

            ChatRoom chatRoom = chatService.getChatRoomByUsers(currentUser.getId(), userId);

            if (chatRoom == null) {
                log.error("Failed to get chat room - null returned");
                model.addAttribute("errorMessage", "Failed to create chat room");
                return "redirect:/chat";
            }

            log.info("Chat room found/created with ID: {}", chatRoom.getId());
            return "redirect:/chat/" + chatRoom.getId();
        } catch (Exception e) {
            log.error("Error creating chat: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", e.getMessage());
            return "redirect:/chat";
        }
    }

    @PostMapping("/send")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> sendMessage(@Valid @RequestBody ChatMessageRequest messageRequest, Principal principal) {
        try {

            String username = principal.getName();
            User user = userService.getUserByUsername(username)
                    .orElseThrow(() -> new MeetrulyException("User not found: " + username));

            if (!chatService.canSendMessage(user.getId())) {
                return ResponseEntity.badRequest()
                        .body("You have reached your daily message limit. Please upgrade your subscription plan.");
            }

            MessageDto message = chatService.sendMessage(user.getId(), messageRequest);

            messagingTemplate.convertAndSendToUser(
                    message.getRecipientUsername(),
                    "/topic/messages",
                    message
            );

            return ResponseEntity.ok(message);
        } catch (MeetrulyException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @MessageMapping("/chat.sendMessage")
    @Transactional
    public void handleMessage(@Payload ChatMessageRequest messageRequest, Principal principal) {
        try {

            String username = principal.getName();
            User user = userService.getUserByUsername(username)
                    .orElseThrow(() -> new MeetrulyException("User not found: " + username));

            if (!chatService.canSendMessage(user.getId())) {

                messagingTemplate.convertAndSendToUser(
                        username,
                        "/topic/error",
                        "You have reached your daily message limit. Please upgrade your subscription plan."
                );
                return;
            }

            MessageDto message = chatService.sendMessage(user.getId(), messageRequest);

            messagingTemplate.convertAndSendToUser(
                    message.getRecipientUsername(),
                    "/topic/messages",
                    message
            );

            messagingTemplate.convertAndSendToUser(
                    username,
                    "/topic/messages.sent",
                    message
            );
        } catch (MeetrulyException e) {

            messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/topic/error",
                    e.getMessage()
            );
        }
    }

    @MessageMapping("/chat.typing")
    @Transactional
    public void handleTyping(@Payload TypingStatusRequest typingStatusRequest, Principal principal) {
        try {

            String username = principal.getName();
            User user = userService.getUserByUsername(username)
                    .orElseThrow(() -> new MeetrulyException("User not found: " + username));

            chatService.updateTypingStatus(user.getId(), typingStatusRequest);

            ChatRoomDto chatRoom = chatService.getChatRoomById(typingStatusRequest.getChatRoomId(), user.getId());

            messagingTemplate.convertAndSendToUser(
                    chatRoom.getOtherUsername(),
                    "/topic/typing",
                    new TypingNotificationDto(typingStatusRequest.getChatRoomId(), user.getId(), typingStatusRequest.isTyping())
            );
        } catch (MeetrulyException e) {

            messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/topic/error",
                    e.getMessage()
            );
        }
    }

    @PostMapping("/{chatRoomId}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> markMessagesAsRead(@PathVariable("chatRoomId") UUID chatRoomId, Principal principal) {
        try {

            String username = principal.getName();
            User user = userService.getUserByUsername(username)
                    .orElseThrow(() -> new MeetrulyException("User not found: " + username));

            chatService.markMessagesAsRead(chatRoomId, user.getId());

            return ResponseEntity.ok().build();
        } catch (MeetrulyException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/messages/{messageId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteMessage(@PathVariable("messageId") UUID messageId, Principal principal) {
        try {

            String username = principal.getName();
            User user = userService.getUserByUsername(username)
                    .orElseThrow(() -> new MeetrulyException("User not found: " + username));

            chatService.deleteMessage(messageId, user.getId());

            return ResponseEntity.ok().build();
        } catch (MeetrulyException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{chatRoomId}/typing")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getTypingUsers(@PathVariable("chatRoomId") UUID chatRoomId, Principal principal) {
        try {

            String username = principal.getName();
            User user = userService.getUserByUsername(username)
                    .orElseThrow(() -> new MeetrulyException("User not found: " + username));

            List<UUID> typingUsers = chatService.getTypingUsersInChatRoom(chatRoomId, user.getId());

            return ResponseEntity.ok(typingUsers);
        } catch (MeetrulyException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}