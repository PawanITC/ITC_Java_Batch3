package com.itc.funkart.common.dto.event.order;

import com.itc.funkart.common.enums.order.OrderEventType;
import lombok.Builder;
import java.math.BigDecimal;
import java.util.List;

/**
 * <h2>OrderInitiatedEvent</h2>
 * <p>Triggered to start stock updates and cart clearing.
 * Note: Added 'orderId' to ensure traceability.</p>
 */
@Builder
public record OrderInitiatedEvent(
        OrderEventType eventType,
        Long orderId,
        Long userId,
        BigDecimal totalAmount,
        List<Long> productIds
) {
}