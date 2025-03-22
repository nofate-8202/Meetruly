package com.meetruly.subscription.dto;

import com.meetruly.core.constant.SubscriptionPlan;
import com.meetruly.subscription.model.Transaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionDto {

    private UUID id;
    private UUID userId;
    private String username;
    private String transactionReference;
    private SubscriptionPlan plan;
    private double amount;
    private String currency;
    private Transaction.TransactionStatus status;
    private Transaction.TransactionType type;
    private String paymentMethod;
    private String paymentDetails;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
}