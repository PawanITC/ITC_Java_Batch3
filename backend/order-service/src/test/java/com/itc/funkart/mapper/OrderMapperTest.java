package com.itc.funkart.mapper;

import com.itc.funkart.common.dto.event.checkout.CheckoutInitiatedEvent;
import com.itc.funkart.common.dto.event.checkout.CheckoutItemPayload;
import com.itc.funkart.common.dto.event.order.OrderCancelledEvent;
import com.itc.funkart.common.dto.event.order.OrderEvent;
import com.itc.funkart.common.enums.order.OrderEventType;
import com.itc.funkart.common.enums.order.OrderStatus;
import com.itc.funkart.dto.OrderResponse;
import com.itc.funkart.entity.Order;
import com.itc.funkart.entity.OrderItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h2>OrderMapperTest</h2>
 *
 * <p>Fixed from commented-out version: corrected {@code CheckoutInitiatedEvent}
 * constructor to 6 components — {@code (eventType, customerId, totalAmount, items,
 * currency, paymentIntentId)}. Old test had wrong arg types and a missing {@code items}
 * list. Also added null-safety coverage and {@code toInitiatedEvent} tests.</p>
 */
class OrderMapperTest {

    private OrderMapper mapper;

    @BeforeEach
    void setUp() { mapper = new OrderMapper(); }

    // =========================================================
    // toEntity
    // =========================================================

    @Nested
    @DisplayName("toEntity")
    class ToEntityTests {

        @Test
        @DisplayName("Maps customerId, totalAmount, defaults status to PENDING")
        void mapsFromInitiatedEvent() {
            CheckoutItemPayload item = new CheckoutItemPayload(
                    10L, 2, new BigDecimal("125.00"), new BigDecimal("250.00"));
            CheckoutInitiatedEvent event = new CheckoutInitiatedEvent(
                    OrderEventType.ORDER_INITIATED, 100L, new BigDecimal("250.00"),
                    List.of(item), "usd", null);

            Order result = mapper.toEntity(event);

            assertNotNull(result);
            assertEquals(100L, result.getCustomerId());
            assertEquals(new BigDecimal("250.00"), result.getTotalAmount());
            assertEquals(OrderStatus.PENDING, result.getStatus());
        }

        @Test
        @DisplayName("Returns null for null input")
        void nullInput() { assertNull(mapper.toEntity(null)); }
    }

    // =========================================================
    // toEvent
    // =========================================================

    @Nested
    @DisplayName("toEvent")
    class ToEventTests {

        @Test
        @DisplayName("Creates OrderEvent with correct type, IDs, amount, items, timestamp")
        void mapsAllFields() {
            Order order = sampleOrder(500L);
            OrderEvent result = mapper.toEvent(order, OrderEventType.PAYMENT_SUCCESS);

            assertEquals(OrderEventType.PAYMENT_SUCCESS, result.eventType());
            assertEquals(500L, result.orderId());
            assertEquals(1L, result.customerId());
            assertEquals(1, result.items().size());

            CheckoutItemPayload p = result.items().get(0);
            assertEquals(50L, p.productId());
            assertEquals(2, p.quantity());
            assertEquals(new BigDecimal("100.00"), p.price());
            assertEquals(new BigDecimal("200.00"), p.subtotal());
            assertNotNull(result.timestamp());
        }

        @Test
        @DisplayName("Returns null for null Order")
        void nullInput() { assertNull(mapper.toEvent(null, OrderEventType.ORDER_INITIATED)); }

        @Test
        @DisplayName("Empty items list when order has no items")
        void emptyItems() {
            Order order = new Order();
            order.setId(1L); order.setCustomerId(1L); order.setTotalAmount(BigDecimal.ZERO);
            assertTrue(mapper.toEvent(order, OrderEventType.ORDER_INITIATED).items().isEmpty());
        }
    }

    // =========================================================
    // toCancelledEvent
    // =========================================================

    @Nested
    @DisplayName("toCancelledEvent")
    class ToCancelledEventTests {

        @Test
        @DisplayName("Correct type, orderId, userId, reason, timestamp")
        void mapsCorrectFields() {
            Order order = sampleOrder(999L);
            OrderCancelledEvent result = mapper.toCancelledEvent(order, "Out of Stock");

            assertEquals(OrderEventType.ORDER_CANCELLED, result.eventType());
            assertEquals(999L, result.orderId());
            assertEquals(1L, result.userId());
            assertEquals("Out of Stock", result.reason());
            assertNotNull(result.timestamp());
        }

        @Test
        @DisplayName("Returns null for null Order")
        void nullInput() { assertNull(mapper.toCancelledEvent(null, "reason")); }

        @Test
        @DisplayName("Null reason does not throw")
        void nullReason() {
            assertDoesNotThrow(() -> mapper.toCancelledEvent(sampleOrder(1L), null));
        }
    }

    // =========================================================
    // toResponse
    // =========================================================

    @Nested
    @DisplayName("toResponse")
    class ToResponseTests {

        @Test
        @DisplayName("Maps orderId, customerId, status, and item list correctly")
        void mapsCorrectly() {
            Order order = sampleOrder(100L);
            OrderResponse response = mapper.toResponse(order);

            assertEquals(100L, response.getOrderId());
            assertEquals(1L, response.getCustomerId());
            assertEquals(OrderStatus.PENDING, response.getOrderStatus());
            assertEquals(1, response.getItems().size());
            assertEquals(50L, response.getItems().get(0).getProductId());
        }

        @Test
        @DisplayName("Returns null for null Order")
        void nullInput() { assertNull(mapper.toResponse(null)); }

        @Test
        @DisplayName("Empty items list when order has no items")
        void emptyItems() {
            Order order = new Order();
            order.setId(1L); order.setCustomerId(1L);
            order.setTotalAmount(BigDecimal.ZERO); order.setStatus(OrderStatus.PENDING);
            assertTrue(mapper.toResponse(order).getItems().isEmpty());
        }
    }

    // =========================================================
    // toInitiatedEvent
    // =========================================================

    @Nested
    @DisplayName("toInitiatedEvent")
    class ToInitiatedEventTests {

        @Test
        @DisplayName("Produces ORDER_INITIATED event with correct fields")
        void success() {
            Order order = sampleOrder(77L);
            CheckoutInitiatedEvent result = mapper.toInitiatedEvent(order);

            assertNotNull(result);
            assertEquals(OrderEventType.ORDER_INITIATED, result.eventType());
            assertEquals(1L, result.customerId());
            assertEquals(new BigDecimal("200.00"), result.totalAmount());
            assertEquals("usd", result.currency());
            assertEquals(1, result.items().size());
        }

        @Test
        @DisplayName("Returns null for null Order")
        void nullInput() { assertNull(mapper.toInitiatedEvent(null)); }
    }

    // =========================================================
    // Helper
    // =========================================================

    private Order sampleOrder(Long id) {
        Order order = new Order();
        order.setId(id);
        order.setCustomerId(1L);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(new BigDecimal("200.00"));
        order.addOrderItem(OrderItem.builder()
                .productId(50L).quantity(2).priceAtPurchase(new BigDecimal("100.00")).build());
        return order;
    }
}
