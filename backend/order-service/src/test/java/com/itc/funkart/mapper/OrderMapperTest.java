//package com.itc.funkart.mapper;
//
//import com.itc.funkart.common.dto.event.checkout.CheckoutInitiatedEvent;
//import com.itc.funkart.common.dto.event.checkout.CheckoutItemPayload;
//import com.itc.funkart.common.dto.event.order.OrderCancelledEvent;
//import com.itc.funkart.common.dto.event.order.OrderEvent;
//import com.itc.funkart.common.enums.order.OrderEventType;
//import com.itc.funkart.common.enums.order.OrderStatus;
//import com.itc.funkart.dto.OrderResponse;
//import com.itc.funkart.entity.Order;
//import com.itc.funkart.entity.OrderItem;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//
//import java.math.BigDecimal;
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.*;
//
///**
// * <h2>OrderMapperTest</h2>
// * <p>
// * Verifies the integrity of manual transformations between JPA Entities and
// * immutable Records. This ensures the JVM handles data snapshots safely.
// * </p>
// */
//class OrderMapperTest {
//
//    private OrderMapper mapper;
//
//    @BeforeEach
//    void setUp() {
//        mapper = new OrderMapper();
//    }
//
//    @Test
//    @DisplayName("toEntity - Should map OrderInitiatedEvent to persistent Entity")
//    void toEntity_shouldMapFromInitiatedEvent() {
//        // 1. Create the Payload using the Record structure
//        CheckoutItemPayload item = new CheckoutItemPayload(
//                10L,
//                2,
//                new BigDecimal("125.00"),
//                new BigDecimal("250.00")
//        );
//
//        // 2. Create the Event with the 7 required fields
//        CheckoutInitiatedEvent event = new CheckoutInitiatedEvent(
//                OrderEventType.ORDER_INITIATED,
//                2001L,
//                100L,
//                new BigDecimal("250.00"),
//                "usd",
//                100L
//                );
//
//        // Act
//        Order result = mapper.toEntity(event);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(100L, result.getCustomerId()); // Checks customerId mapping
//        assertEquals(new BigDecimal("250.00"), result.getTotalAmount());
//        assertEquals(OrderStatus.PENDING, result.getStatus());
//    }
//
//    @Test
//    @DisplayName("toEvent - Should create full OrderEvent for Kafka broadcast")
//    void toEvent_shouldMapToLifecycleRecord() {
//        // Arrange
//        Order order = createSampleOrder(500L);
//
//        // Act
//        OrderEvent result = mapper.toEvent(order, OrderEventType.PAYMENT_SUCCESS);
//
//        // Assert
//        assertEquals(OrderEventType.PAYMENT_SUCCESS, result.eventType());
//        assertEquals(500L, result.orderId());
//        assertEquals(1, result.items().size());
//        assertEquals(new BigDecimal("200.00"), result.items().get(0).subtotal());
//        assertNotNull(result.timestamp());
//    }
//
//    @Test
//    @DisplayName("toCancelledEvent - Should create lightweight cancellation Record")
//    void toCancelledEvent_shouldMapCorrectFields() {
//        // Arrange
//        Order order = createSampleOrder(999L);
//        String reason = "Out of Stock";
//
//        // Act
//        OrderCancelledEvent result = mapper.toCancelledEvent(order, reason);
//
//        // Assert
//        assertEquals(OrderEventType.ORDER_CANCELLED, result.eventType());
//        assertEquals(999L, result.orderId());
//        assertEquals(reason, result.reason());
//        assertNotNull(result.timestamp());
//    }
//
//    @Test
//    @DisplayName("toResponse - Should map Entity to Response DTO")
//    void toResponse_shouldMapCorrectly() {
//        // Arrange
//        Order order = createSampleOrder(100L);
//
//        // Act
//        OrderResponse response = mapper.toResponse(order);
//
//        // Assert
//        assertEquals(100L, response.getOrderId());
//        assertEquals(OrderStatus.PENDING, response.getOrderStatus());
//        assertEquals(1, response.getItems().size());
//    }
//
//    @Test
//    @DisplayName("Null Safety - Should return null for all mapping methods")
//    void handleNulls() {
//        assertNull(mapper.toEntity(null));
//        assertNull(mapper.toResponse(null));
//        assertNull(mapper.toEvent(null, OrderEventType.ORDER_INITIATED));
//        assertNull(mapper.toCancelledEvent(null, "No reason"));
//    }
//
//    // Helper to build a consistent Order entity for tests
//    private Order createSampleOrder(Long id) {
//        Order order = new Order();
//        order.setId(id);
//        order.setCustomerId(1L);
//        order.setStatus(OrderStatus.PENDING);
//        order.setTotalAmount(new BigDecimal("200.00"));
//
//        OrderItem item = OrderItem.builder()
//                .productId(50L)
//                .quantity(2)
//                .priceAtPurchase(new BigDecimal("100.00"))
//                .build();
//        order.addOrderItem(item);
//
//        return order;
//    }
//}