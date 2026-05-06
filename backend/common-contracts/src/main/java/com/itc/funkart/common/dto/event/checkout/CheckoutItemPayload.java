package com.itc.funkart.common.dto.event.checkout;

import java.math.BigDecimal;

/**
 * <h2>CheckoutItemPayload</h2>
 *
 * <p>
 * Represents a single line item in a checkout request.
 * This is a snapshot of cart state at the moment the user initiates checkout.
 * </p>
 *
 * <p>
 * This is NOT an Order domain object — it becomes one only after Order Service persists it.
 * </p>
 */
public record CheckoutItemPayload(
        Long productId,
        Integer quantity,
        BigDecimal price,
        BigDecimal subtotal
) {
    public static CheckoutItemPayload of(Long id, Integer qty, BigDecimal price) {

        BigDecimal subtotal = (price != null && qty != null)
                ? price.multiply(BigDecimal.valueOf(qty))
                : BigDecimal.ZERO;

        return new CheckoutItemPayload(id, qty, price, subtotal);
    }
}