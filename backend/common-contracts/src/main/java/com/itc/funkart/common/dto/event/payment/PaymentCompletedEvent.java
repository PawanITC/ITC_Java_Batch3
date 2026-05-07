package com.itc.funkart.common.dto.event.payment;

import lombok.Builder;

/**
 * <h2>PaymentCompletedEvent</h2>
 * <p>
 * Broadcast when a payment transaction is successfully finalized and funds are captured.
 * This event triggers the downstream Order fulfillment process.
 * </p>
 * <p>
 * <b>Note:</b> This DTO is a pure Data Transfer Object. Mapping from Stripe or
 * Entity objects should be handled by the producer service to keep this
 * library dependency-free.
 * </p>
 *
 * @param paymentId The internal primary key of the payment record.
 * @param orderId   The associated Order ID to be marked as 'PAID'.
 * @param stripeId  The external Stripe PaymentIntent ID (pi_...) for auditing.
 * @param amount    The actual amount captured (in the smallest currency unit).
 * @param timestamp The epoch millisecond when the event was generated.
 */
@Builder
public record PaymentCompletedEvent(
        Long paymentId,
        Long orderId,
        Long userId,
        String stripeId,
        Long amount,
        Long timestamp
) {
    public PaymentCompletedEvent {
        if (timestamp == null) {
            timestamp = System.currentTimeMillis();
        }
    }
}