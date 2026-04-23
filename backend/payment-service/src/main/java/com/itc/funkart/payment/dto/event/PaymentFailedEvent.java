package com.itc.funkart.payment.dto.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.itc.funkart.payment.entity.Payment;
import com.stripe.model.PaymentIntent;

/**
 * Kafka Event published when a payment fails or is declined.
 * <p>
 * FACTORY LOGIC: Safely extracts 'lastPaymentError' details from the PaymentIntent.
 * Includes specific 'stripe_error_code' (e.g., card_declined, expired_card)
 * so downstream services can provide specific user feedback.
 * </p>
 */
public record PaymentFailedEvent(
        @JsonProperty("payment_id") Long paymentId,
        @JsonProperty("user_id") Long userId,
        @JsonProperty("order_id") Long orderId,
        Long amount,
        String currency,
        @JsonProperty("error_message") String errorMessage,
        @JsonProperty("stripe_error_code") String stripeErrorCode,
        Long timestamp
) {
    // Factory
    public static PaymentFailedEvent from(Payment payment, PaymentIntent intent) {
        var error = intent.getLastPaymentError();

        return new PaymentFailedEvent(
                payment.getId(),
                payment.getUserId(),
                payment.getOrderId(),
                intent.getAmount(),
                payment.getCurrency(),
                (error != null) ? error.getMessage() : "Unknown Failure",
                (error != null) ? error.getCode() : "generic_error",
                System.currentTimeMillis()
        );
    }

}