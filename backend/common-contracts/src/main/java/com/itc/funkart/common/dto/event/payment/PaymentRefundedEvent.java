package com.itc.funkart.common.dto.event.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.itc.funkart.common.enums.order.OrderEventType;
import lombok.Builder;

/**
 * <h2>PaymentRefundedEvent</h2>
 * <p>
 * Broadcast when a fund reversal is successfully processed.
 * This is used by the Order Service to update order status and the
 * Inventory Service to potentially restock items.
 * </p>
 * <p>
 * <b>Note:</b> Decoupled from Stripe SDK (Refund/Charge) and JPA (Payment)
 * to ensure microservice portability.
 * </p>
 *
 * @param paymentId      The internal primary key of the payment record.
 * @param orderId        The associated Order ID.
 * @param stripeRefundId The unique refund ID from Stripe (re_...).
 * @param amountRefunded The exact amount returned to the user (in cents).
 * @param currency       The ISO currency code (e.g., "USD").
 * @param timestamp      The epoch millisecond when the refund was finalized.
 */
@Builder
public record PaymentRefundedEvent(

        @JsonProperty("payment_id")
        Long paymentId,

        @JsonProperty("order_id")
        Long orderId,

        @JsonProperty("stripe_refund_id")
        String stripeRefundId,

        @JsonProperty("amount_refunded")
        Long amountRefunded,

        @JsonProperty("currency")
        String currency,

        @JsonProperty("timestamp")
        Long timestamp,

        @JsonProperty("event_type")
        String eventType
) {
    public PaymentRefundedEvent {
        if (timestamp == null) {
            timestamp = System.currentTimeMillis();
        }
        if (eventType == null) {
            eventType = "ORDER_REFUNDED";
        }
    }
}