package com.itc.funkart.common.dto.event.checkout;

import com.itc.funkart.common.enums.order.OrderEventType;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

/**
 * <h2>CheckoutInitiatedEvent</h2>
 *
 * <p>
 * Emitted by Product Service when a user completes checkout.
 * This is the first step in the order saga.
 * </p>
 *
 * <p>
 * Consumed by:
 * - Order Service → creates persistent Order record
 * - Payment Service → prepares PaymentIntent (Stripe)
 * </p>
 *
 * <p>
 * Note: No orderId exists at this stage because the order has not been created yet.
 * </p>
 *
 * @param eventType       Always {@link OrderEventType#ORDER_INITIATED} for this event.
 * @param customerId      The authenticated user placing the order.
 * @param totalAmount     The basket total calculated by the Product Service.
 * @param items           Immutable snapshot of line items at checkout time.
 * @param currency        ISO 4217 currency code (e.g., {@code "usd"}).
 * @param paymentIntentId Stripe PaymentIntent ID created before this event (may be {@code null}).
 */
@Builder
public record CheckoutInitiatedEvent(

        OrderEventType eventType,

        Long customerId,

        BigDecimal totalAmount,

        List<CheckoutItemPayload> items,

        String currency,

        String paymentIntentId
) {
}