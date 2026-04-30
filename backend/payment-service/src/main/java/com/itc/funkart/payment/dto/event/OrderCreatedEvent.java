package com.itc.funkart.payment.dto.event;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Received from the Order Service via Kafka.
 * This event triggers the Payment Service to finalize the Stripe transaction.
 */
public record OrderCreatedEvent(
        @JsonProperty("order_id") Long orderId,
        @JsonProperty("user_id") Long userId,
        @JsonProperty("user_name") String userName,
        @JsonProperty("user_email") String userEmail,
        @JsonProperty("user_role") String userRole,
        @JsonProperty("amount") Long amount,
        @JsonProperty("currency") String currency,
        @JsonProperty("payment_intent_id") String paymentIntentId,
        @JsonProperty("payment_method_id") String paymentMethodId,
        @JsonProperty("return_url") String returnUrl
) {}
