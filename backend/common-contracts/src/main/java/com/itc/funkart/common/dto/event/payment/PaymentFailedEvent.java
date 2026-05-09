package com.itc.funkart.common.dto.event.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

/**
 * <h2>PaymentFailedEvent</h2>
 * <p>
 * Broadcast when a payment transaction is declined, cancelled, or fails due to technical errors.
 * This event allows downstream services (Order, Notification) to take corrective action,
 * such as releasing inventory or notifying the user.
 * </p>
 * <p>
 * <b>Note:</b> To maintain microservice isolation, this DTO is decoupled from the Stripe SDK.
 * Transformation logic must reside in the Producer service.
 * </p>
 *
 * @param paymentId The internal primary key of the payment record.
 * @param orderId   The associated Order ID.
 * @param stripeId  The external Stripe PaymentIntent ID (pi_...) for traceability.
 * @param timestamp The epoch millisecond when the failure occurred.
 */
@Builder
public record PaymentFailedEvent(

        @JsonProperty("payment_id")
        Long paymentId,

        @JsonProperty("order_id")
        Long orderId,

        @JsonProperty("user_id")
        Long userId,

        @JsonProperty("stripe_id")
        String stripeId,

        @JsonProperty("amount")
        Long amount,

        @JsonProperty("timestamp")
        Long timestamp,

        @JsonProperty("event_type")
        String eventType
) {
    public PaymentFailedEvent {
        if (timestamp == null) {
            timestamp = System.currentTimeMillis();
        }
        if (eventType == null) {
            eventType = "ORDER_CANCELLED";
        }
    }
}