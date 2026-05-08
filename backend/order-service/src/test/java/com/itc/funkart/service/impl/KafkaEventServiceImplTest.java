package com.itc.funkart.service.impl;

import com.itc.funkart.common.dto.event.order.OrderCancelledEvent;
import com.itc.funkart.common.dto.event.order.OrderEvent;
import com.itc.funkart.common.enums.order.OrderEventType;
import com.itc.funkart.entity.Order;
import com.itc.funkart.kafka.producer.OrderEventProducer;
import com.itc.funkart.mapper.OrderMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * <h2>KafkaEventServiceImplTest</h2>
 *
 * <p>Unit tests run outside a Spring transaction, so
 * {@code TransactionSynchronizationManager.isActualTransactionActive()} returns
 * {@code false} — the {@code syncAndSend} fallback fires immediately.
 * This is the correct behaviour for unit tests; transactional integration tests
 * cover the post-commit path separately.</p>
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class KafkaEventServiceImplTest {

    @Mock private OrderEventProducer producer;
    @Mock private OrderMapper mapper;
    @InjectMocks private KafkaEventServiceImpl service;

    // =========================================================
    // sendOrderEvent
    // =========================================================

    @Nested
    @DisplayName("sendOrderEvent")
    class SendOrderEventTests {

        @Test
        @DisplayName("Success: maps and dispatches immediately (no active TX)")
        void success() {
            Order order = new Order();
            order.setId(101L);
            order.setCustomerId(1L);
            OrderEvent event = new OrderEvent(
                    OrderEventType.ORDER_INITIATED, 101L, 1L, null, null, null);
            when(mapper.toEvent(eq(order), eq(OrderEventType.ORDER_INITIATED))).thenReturn(event);

            service.sendOrderEvent(order, OrderEventType.ORDER_INITIATED);

            verify(producer, times(1)).publishOrderEvent(event);
        }

        @Test
        @DisplayName("Fault: mapper failure throws before producer is called")
        void mappingFailure() {
            Order order = new Order();
            order.setId(1L);
            order.setCustomerId(1L);
            when(mapper.toEvent(any(), any())).thenThrow(new RuntimeException("Mapping Error"));

            assertThrows(RuntimeException.class,
                    () -> service.sendOrderEvent(order, OrderEventType.ORDER_INITIATED));
            verifyNoInteractions(producer);
        }

        @Test
        @DisplayName("Fault: Kafka producer failure bubbles up (no TX to absorb it)")
        void producerFailure() {
            Order order = new Order();
            order.setId(101L);
            order.setCustomerId(1L);
            OrderEvent event = new OrderEvent(
                    OrderEventType.ORDER_INITIATED, 101L, 1L, null, null, null);
            when(mapper.toEvent(any(), any())).thenReturn(event);
            doThrow(new RuntimeException("Kafka Down")).when(producer).publishOrderEvent(any());

            assertThrows(RuntimeException.class,
                    () -> service.sendOrderEvent(order, OrderEventType.ORDER_INITIATED));
            verify(producer).publishOrderEvent(any());
        }
    }

    // =========================================================
    // sendOrderCreated (direct publish — bypasses syncAndSend)
    // =========================================================

    @Nested
    @DisplayName("sendOrderCreated")
    class SendOrderCreatedTests {

        @Test
        @DisplayName("Success: maps with ORDER_INITIATED type and publishes directly")
        void success() {
            Order order = new Order();
            order.setId(2001L);
            order.setCustomerId(100L);
            OrderEvent mappedEvent = new OrderEvent(
                    OrderEventType.ORDER_INITIATED, 2001L, 100L, null, LocalDateTime.now(), null);
            when(mapper.toEvent(order, OrderEventType.ORDER_INITIATED)).thenReturn(mappedEvent);

            service.sendOrderCreated(order);

            verify(mapper).toEvent(order, OrderEventType.ORDER_INITIATED);
            verify(producer).publishOrderEvent(mappedEvent);
        }

        @Test
        @DisplayName("Fault: producer exception propagates to caller")
        void producerFailure() {
            Order order = new Order();
            order.setId(1L);
            order.setCustomerId(1L);
            when(mapper.toEvent(any(), eq(OrderEventType.ORDER_INITIATED)))
                    .thenReturn(new OrderEvent(OrderEventType.ORDER_INITIATED, 1L, 1L, null, null, null));
            doThrow(new RuntimeException("Kafka Down")).when(producer).publishOrderEvent(any());

            assertThrows(RuntimeException.class, () -> service.sendOrderCreated(order));
        }
    }

    // =========================================================
    // sendOrderCancelled (uses syncAndSend — immediate in tests)
    // =========================================================

    @Nested
    @DisplayName("sendOrderCancelled")
    class SendOrderCancelledTests {

        @Test
        @DisplayName("Success: publishes cancellation event immediately (no active TX)")
        void success() {
            OrderCancelledEvent event = OrderCancelledEvent.builder()
                    .eventType(OrderEventType.ORDER_CANCELLED)
                    .orderId(55L).userId(10L)
                    .reason("Customer request")
                    .timestamp(LocalDateTime.now())
                    .build();

            service.sendOrderCancelled(event);

            verify(producer).publishOrderCancelled(event);
        }

        @Test
        @DisplayName("Fault: Kafka producer failure bubbles up")
        void producerFailure() {
            OrderCancelledEvent event = OrderCancelledEvent.builder()
                    .eventType(OrderEventType.ORDER_CANCELLED)
                    .orderId(55L).userId(10L)
                    .timestamp(LocalDateTime.now())
                    .build();
            doThrow(new RuntimeException("Kafka Down")).when(producer).publishOrderCancelled(any());

            assertThrows(RuntimeException.class, () -> service.sendOrderCancelled(event));
            verify(producer).publishOrderCancelled(event);
        }
    }
}
