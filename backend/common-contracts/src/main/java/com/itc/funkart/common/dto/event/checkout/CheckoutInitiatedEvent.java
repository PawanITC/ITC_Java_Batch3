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
 * Note:
 * No orderId exists at this stage because the order has not been created yet.
 * </p>
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