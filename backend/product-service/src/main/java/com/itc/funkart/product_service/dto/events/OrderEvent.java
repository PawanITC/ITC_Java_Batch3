package com.itc.funkart.product_service.dto.events;

import com.itc.funkart.product_service.enums.OrderEventType;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

/**
 * Event received from the Order Service via Kafka.
 * Used to trigger stock updates and cart clearing.
 */
@Builder
public record OrderEvent(
        OrderEventType eventType,
        Long userId,
        BigDecimal totalAmount,
        List<Long> productIds
) {
}