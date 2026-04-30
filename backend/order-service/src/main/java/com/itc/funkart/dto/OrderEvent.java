package com.itc.funkart.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <h2>OrderEvent</h2>
 * <p>This DTO is used for asynchronous communication via Kafka.</p>
 * <p>When an order is created, updated, or cancelled, this object is
 * broadcasted so other services (Inventory, Notification) can react.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent {

    private String eventType; // e.g., "ORDER_CREATED", "ORDER_CANCELLED"
    private Long orderId;
    private Long customerId;
    private BigDecimal totalAmount;
    private LocalDateTime timestamp;

    /**
     * Minimal item info needed for other services to process their logic
     * (e.g., Inventory Service needs to know which Product IDs to decrement).
     */
    private List<OrderItemEventPayload> items;
}