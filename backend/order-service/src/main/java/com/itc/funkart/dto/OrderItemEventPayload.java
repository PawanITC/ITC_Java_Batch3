package com.itc.funkart.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * <h2>OrderItemEventPayload</h2>
 *
 * <p>
 * Represents a minimal, serialized view of an order item used in asynchronous
 * communication between microservices.
 * </p>
 *
 * <p>
 * This payload is intentionally lightweight and contains only the fields required
 * by downstream systems:
 * </p>
 *
 * <ul>
 *   <li><b>Inventory Service</b> → needs productId and quantity</li>
 *   <li><b>Billing/Analytics</b> → may use price and subtotal</li>
 * </ul>
 *
 * <p>
 * This avoids tight coupling with the internal OrderItem entity.
 * </p>
 *
 * @author Abbas Gure
 * @version 1.0
 */
@Data
@Builder
public class OrderItemEventPayload {

    /**
     * Unique identifier of the product.
     */
    private Long productId;

    /**
     * Quantity of the product ordered.
     */
    private Integer quantity;

    /**
     * Price per unit at the time of order.
     */
    private BigDecimal price;

    /**
     * Total price for this item (price * quantity).
     */
    private BigDecimal subtotal;
}