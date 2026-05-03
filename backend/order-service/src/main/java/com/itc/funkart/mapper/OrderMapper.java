package com.itc.funkart.mapper;

import com.itc.funkart.common.dto.event.order.OrderCancelledEvent;
import com.itc.funkart.common.dto.event.order.OrderEvent;
import com.itc.funkart.common.dto.event.order.OrderInitiatedEvent;
import com.itc.funkart.common.dto.event.order.OrderItemEventPayload;
import com.itc.funkart.common.enums.order.OrderEventType;
import com.itc.funkart.common.enums.order.OrderStatus;
import com.itc.funkart.dto.OrderItemRequest;
import com.itc.funkart.dto.OrderItemResponse;
import com.itc.funkart.dto.OrderResponse;
import com.itc.funkart.entity.Order;
import com.itc.funkart.entity.OrderItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <h2>OrderMapper (Manual Implementation)</h2>
 * <p>Explicitly handles the transformation between Entities, DTOs, and Kafka Events.</p>
 * <p>Manual mapping provides the best transparency for the JVM Execution Engine,
 * as it avoids the reflection overhead often associated with complex mapping libraries.</p>
 */
@Component
public class OrderMapper {

    /**
     * <h3>Map to Full Order Event</h3>
     * <p>
     * Uses the canonical constructor instead of a Builder to guarantee
     * that no field is left uninitialized during event creation.
     * </p>
     */
    public OrderEvent toEvent(Order order, OrderEventType type) {
        if (order == null) return null;

        // Direct constructor call enforces all @param requirements at compile time
        return new OrderEvent(
                type,
                order.getId(),
                order.getCustomerId(),
                order.getTotalAmount(),
                LocalDateTime.now(),
                mapItemEventPayloads(order.getItems())
        );
    }

    /**
     * <h3>Map to Cancelled Event</h3>
     * <p>
     * Specifically used for voiding orders. Includes the 'reason' to ensure
     * downstream services (Inventory/Payment) can log why stock is being returned.
     * </p>
     */
    public OrderCancelledEvent toCancelledEvent(Order order, String reason) {
        if (order == null) return null;

        // Assuming OrderCancelledEvent still uses Builder for flexibility with 'reason',
        // but can be switched to new OrderCancelledEvent(...) if preferred.
        return OrderCancelledEvent.builder()
                .eventType(OrderEventType.ORDER_CANCELLED)
                .orderId(order.getId())
                .userId(order.getCustomerId())
                .reason(reason)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Converts a saved Order entity into a Response DTO for the REST layer.
     */
    public OrderResponse toResponse(Order order) {
        if (order == null) return null;

        return OrderResponse.builder()
                .orderId(order.getId())
                .customerId(order.getCustomerId())
                .orderStatus(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .createdAt(order.getCreatedAt())
                .items(mapItemResponses(order.getItems()))
                .build();
    }


    /**
     * Maps the incoming Kafka "Initiated" event to a JPA Entity.
     */
    public Order toEntity(OrderInitiatedEvent event) {
        if (event == null) return null;

        Order order = new Order();
        // Use the newly added orderId for traceability if your DB supports manual ID assignment,
        // or keep it for logging/correlation context.
        order.setCustomerId(event.userId());
        order.setTotalAmount(event.totalAmount());
        order.setStatus(OrderStatus.PENDING);

        return order;
    }

    // --- Private Helpers for Collection Mapping ---

    /**
     * <h3>Map Item Event Payloads</h3>
     * <p>
     * Transforms internal OrderItems into the specific event payload record.
     * Calculates subtotals during mapping to minimize logic requirements for downstream consumers.
     * </p>
     */
    private List<OrderItemEventPayload> mapItemEventPayloads(List<OrderItem> items) {
        if (items == null) return List.of();

        return items.stream()
                .map(item -> new OrderItemEventPayload(
                        item.getProductId(),
                        item.getQuantity(),
                        item.getPriceAtPurchase(),
                        item.getPriceAtPurchase().multiply(BigDecimal.valueOf(item.getQuantity()))
                ))
                .toList(); // Cleaner and naturally immutable
    }

    private List<OrderItemResponse> mapItemResponses(List<OrderItem> items) {
        if (items == null) return List.of();

        return items.stream()
                .map(item -> OrderItemResponse.builder()
                        .productId(item.getProductId())
                        .quantity(item.getQuantity())
                        .priceAtPurchase(item.getPriceAtPurchase())
                        .build())
                .collect(Collectors.toList());
    }

    private OrderItem toItemEntity(OrderItemRequest itemRequest) {
        return OrderItem.builder()
                .productId(itemRequest.getProductId())
                .quantity(itemRequest.getQuantity())
                .build();
    }
}