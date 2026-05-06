package com.itc.funkart.common.dto.event.order;

import com.itc.funkart.common.dto.event.checkout.CheckoutItemPayload;
import com.itc.funkart.common.enums.order.OrderEventType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <h2>OrderEvent</h2>
 * <p>
 * The definitive record of an order's lifecycle state change.
 * This event is published <b>only</b> by the Order Service after a
 * valid order ID has been generated in the RDBMS.
 * </p>
 *
 * <p>
 * From a JVM perspective, this is a <b>Record</b> to ensure immutability
 * and thread-safety when being processed by multithreaded Kafka listeners
 * in downstream services like Inventory or Shipping.
 * </p>
 *
 * @param eventType   The current state (e.g., CREATED, CANCELLED, PAID).
 * @param orderId     The "Source of Truth" identifier from the Order DB.
 * @param customerId  The customer associated with the transaction.
 * @param totalAmount The final calculated price at the time of order creation.
 * @param timestamp   The moment the event was finalized.
 * @param items       The collection of line items, including quantity and snapshot prices.
 * @author Gemini
 * @version 1.1
 */
public record OrderEvent(
        OrderEventType eventType,
        Long orderId,
        Long customerId,
        BigDecimal totalAmount,
        LocalDateTime timestamp,
        List<CheckoutItemPayload> items
) {
}