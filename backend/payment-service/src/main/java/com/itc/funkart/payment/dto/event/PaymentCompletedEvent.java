package com.itc.funkart.payment.dto.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.itc.funkart.payment.entity.Payment;
import com.stripe.model.PaymentIntent;

/**
 * Kafka Event published when a PaymentIntent successfully reaches 'succeeded'.
 * <p>
 * FACTORY LOGIC: Uses 'intent.getAmountReceived()' to ensure we report the
 * actual captured value from Stripe, which protects against partial capture discrepancies.
 * </p>
 */
public record PaymentCompletedEvent(
        @JsonProperty("payment_id") Long paymentId,
        @JsonProperty("user_id")Long userId,
        @JsonProperty("order_id")Long orderId,
        Long amount,
        String currency,
        Long timestamp
) {
    public static PaymentCompletedEvent from(Payment payment, PaymentIntent intent){
        return new PaymentCompletedEvent(
                payment.getId(),
                payment.getUserId(),
                payment.getOrderId(),
                intent.getAmountReceived(),
                intent.getCurrency().toUpperCase(),
                System.currentTimeMillis()
        );
    }
}