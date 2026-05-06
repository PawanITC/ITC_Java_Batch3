package com.itc.funkart.common.dto.event.order;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Specifically for the Payment Service.
 * Contains the secure Stripe identifiers and User context needed for receipting.
 */
public record OrderCreatedEvent(
        @JsonProperty("order_id") Long orderId,
        @JsonProperty("user_id") Long userId,
        @JsonProperty("user_name") String userName,
        @JsonProperty("user_email") String userEmail,
        @JsonProperty("user_role") String userRole,
        @JsonProperty("amount") Long amount,         // Cents
        @JsonProperty("currency") String currency,   // e.g. "usd"
        @JsonProperty("payment_intent_id") String paymentIntentId,
        @JsonProperty("payment_method_id") String paymentMethodId,
        @JsonProperty("return_url") String returnUrl
) {}