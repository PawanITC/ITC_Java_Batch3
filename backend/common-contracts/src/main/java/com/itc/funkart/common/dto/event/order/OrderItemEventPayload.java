package com.itc.funkart.common.dto.event.order;

import java.math.BigDecimal;

/**
 * <h2>OrderItemEventPayload</h2>
 * <p>
 * Represents the "Line Item" details within an {@link OrderEvent}.
 * Includes the price snapshot to ensure downstream financial services
 * (like Billing) have the exact data from the moment of purchase.
 * </p>
 */
public record OrderItemEventPayload(
        Long productId,
        Integer quantity,
        BigDecimal price,
        BigDecimal subtotal
) {
}