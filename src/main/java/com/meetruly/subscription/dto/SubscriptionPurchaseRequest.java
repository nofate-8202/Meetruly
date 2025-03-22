package com.meetruly.subscription.dto;

import com.meetruly.core.constant.SubscriptionPlan;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPurchaseRequest {

    @NotNull(message = "Subscription plan is required")
    private SubscriptionPlan plan;

    private boolean autoRenew = false;

    @NotNull(message = "Payment method is required")
    private String paymentMethod;

    private String cardNumber;
    private String cardExpiryDate;
    private String cardCVV;
    private String cardHolderName;
}