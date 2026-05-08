package com.itc.funkart.common.dto.event.order;

import com.itc.funkart.common.enums.order.OrderEventType;
import lombok.Builder;
import java.time.LocalDateTime;

/**
 * <h2>Order Cancelled Event</h2>
 * <p>
 * Signals that an order has been voided.
 * Essential for inventory restock and payment refund flows.
 * </p>
 *
 * @param eventType Always {@link OrderEventType#ORDER_CANCELLED}.
 * @param orderId   The identifier of the cancelled order.
 * @param userId    The customer who owns the order.
 * @param reason    Optional human-readable cancellation reason.
 * @param timestamp The moment the cancellation was processed.
 */
@Builder
public record OrderCancelledEvent(
        OrderEventType eventType,
        Long orderId,
        Long userId,
        String reason,
        LocalDateTime timestamp
) {}