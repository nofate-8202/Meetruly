package com.meetruly.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class NotificationIntegrationService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${notification.service.url:http://localhost:8081/api/notifications}")
    private String notificationServiceUrl;

    @Value("${notification.service.api-key:meetruly-notifications-api-key-secret}")
    private String apiKey;

    public NotificationIntegrationService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Async
    public void sendNewMessageNotification(UUID recipientId, String recipientEmail, String recipientName,
                                           UUID senderId, String senderName, String messagePreview) {
        log.debug("Sending new message notification to: {}", recipientEmail);

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("recipientId", recipientId);
            requestBody.put("recipientEmail", recipientEmail);
            requestBody.put("recipientName", recipientName);
            requestBody.put("senderId", senderId);
            requestBody.put("senderName", senderName);
            requestBody.put("type", "NEW_MESSAGE");

            Map<String, String> parameters = new HashMap<>();
            parameters.put("senderName", senderName);
            parameters.put("messagePreview", messagePreview);
            requestBody.put("parameters", parameters);

            sendNotification(requestBody);
        } catch (Exception e) {
            log.error("Failed to send new message notification", e);
        }
    }

    @Async
    public void sendProfileApprovedNotification(UUID recipientId, String recipientEmail, String recipientName) {
        log.debug("Sending profile approved notification to: {}", recipientEmail);

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("recipientId", recipientId);
            requestBody.put("recipientEmail", recipientEmail);
            requestBody.put("recipientName", recipientName);
            requestBody.put("type", "PROFILE_APPROVED");

            sendNotification(requestBody);
        } catch (Exception e) {
            log.error("Failed to send profile approved notification", e);
        }
    }

    @Async
    public void sendAccountBlockedNotification(UUID recipientId, String recipientEmail, String recipientName,
                                               String duration, String reason) {
        log.debug("Sending account blocked notification to: {}", recipientEmail);

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("recipientId", recipientId);
            requestBody.put("recipientEmail", recipientEmail);
            requestBody.put("recipientName", recipientName);
            requestBody.put("type", "ACCOUNT_BLOCKED");

            Map<String, String> parameters = new HashMap<>();
            parameters.put("duration", duration);
            parameters.put("reason", reason);
            requestBody.put("parameters", parameters);

            sendNotification(requestBody);
        } catch (Exception e) {
            log.error("Failed to send account blocked notification", e);
        }
    }

    /**
     * Generic method to send any type of notification.
     */
    private void sendNotification(Map<String, Object> requestBody) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-API-KEY", apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    notificationServiceUrl + "/send",
                    HttpMethod.POST,
                    entity,
                    String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.debug("Notification sent successfully");
            } else {
                log.error("Failed to send notification. Status: {}", response.getStatusCode().value());
            }
        } catch (Exception e) {
            log.error("Exception while sending notification", e);
        }
    }
}
