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
 *
 * @param productId   The catalogue identifier of the purchased product.
 * @param productName The display name of the product at the time of checkout (snapshot).
 * @param quantity    The number of units ordered.
 * @param price       Unit price at the time of checkout (immutable snapshot).
 * @param subtotal    Pre-computed line total ({@code price × quantity}).
 */
public record CheckoutItemPayload(
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal price,
        BigDecimal subtotal
) {
    /**
     * Factory method that automatically computes the subtotal.
     *
     * @param id          The product identifier.
     * @param productName The product display name.
     * @param qty         The number of units.
     * @param price       Unit price; if {@code null}, subtotal defaults to zero.
     * @return A fully populated {@link CheckoutItemPayload}.
     */
    public static CheckoutItemPayload of(Long id, String productName, Integer qty, BigDecimal price) {

        BigDecimal subtotal = (price != null && qty != null)
                ? price.multiply(BigDecimal.valueOf(qty))
                : BigDecimal.ZERO;

        return new CheckoutItemPayload(id, productName, qty, price, subtotal);
    }
}