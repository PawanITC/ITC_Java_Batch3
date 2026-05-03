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
 */
@Builder
public record OrderCancelledEvent(
        OrderEventType eventType, // Always OrderEventType.ORDER_CANCELLED
        Long orderId,
        Long userId,
        String reason,
        LocalDateTime timestamp
) {}