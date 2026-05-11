package com.itc.funkart.aggregator.dto;

import com.itc.funkart.common.enums.order.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <h2>OrderResponse</h2>
 * <p>Data returned to the client after an order has been successfully
 * processed or retrieved from the database.</p>
 */
@Data
@Builder
public class OrderResponse {

    private Long orderId;
    private Long customerId;
    private String customerEmail;
    private List<OrderItemResponse> items;
    private BigDecimal totalAmount;
    private OrderStatus orderStatus;

    /**
     * Indicates the status of the background Kafka message.
     * Values: PUBLISHED, PENDING_RETRY, FAILED
     */
    private String eventStatus;

    private LocalDateTime createdAt;
}