package com.itc.funkart.mapper;

import com.itc.funkart.dto.OrderItemRequest;
import com.itc.funkart.dto.OrderItemResponse;
import com.itc.funkart.dto.OrderRequest;
import com.itc.funkart.dto.OrderResponse;
import com.itc.funkart.entity.Order;
import com.itc.funkart.entity.OrderItem;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <h2>OrderMapper (Manual Implementation)</h2>
 * <p>Explicitly handles the transformation between Entities and DTOs.</p>
 * <p>This approach is preferred for Junior Dev readability as it removes
 * MapStruct's annotation complexity and makes debugging straightforward.</p>
 */
@Component
public class OrderMapper {

    /**
     * Maps OrderRequest to a fresh Order entity.
     * Note: We don't map customerId or totalAmount here; the Service layer
     * handles those after JWT extraction and price fetching.
     */
    public Order toEntity(OrderRequest request) {
        if (request == null) return null;

        Order order = new Order();

        if (request.getItems() != null) {
            List<OrderItem> items = request.getItems().stream()
                    .map(this::toItemEntity)
                    .peek(item -> item.setOrder(order)) // Crucial: Link child to parent
                    .collect(Collectors.toList());
            order.setItems(items);
        }

        return order;
    }

    /**
     * Converts a saved Order entity into a Response DTO.
     */
    public OrderResponse toResponse(Order order) {
        if (order == null) return null;

        return OrderResponse.builder()
                .orderId(order.getId())
                .customerId(order.getCustomerId())
                .orderStatus(order.getStatus()) // Matches the DTO field name
                .totalAmount(order.getTotalAmount())
                .createdAt(order.getCreatedAt())
                .items(mapItemResponses(order.getItems()))
                .build();
    }

    // --- Private Helpers for Collection Mapping ---

    private OrderItem toItemEntity(OrderItemRequest itemRequest) {
        return OrderItem.builder()
                .productId(itemRequest.getProductId())
                .quantity(itemRequest.getQuantity())
                .build();
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
}