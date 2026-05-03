package com.itc.funkart.service.impl;

import com.itc.funkart.common.dto.event.order.OrderEvent;
import com.itc.funkart.common.enums.order.OrderEventType;
import com.itc.funkart.entity.Order;
import com.itc.funkart.kafka.producer.OrderEventProducer;
import com.itc.funkart.mapper.OrderMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * <h2>KafkaEventServiceImplTest</h2>
 * <p>
 * Verifies the event orchestration and infrastructure resilience.
 * </p>
 *
 * <h3>JVM & Transaction Context:</h3>
 * <ul>
 *     <li><b>Stack Unwinding:</b> When Kafka is down during immediate dispatch,
 *      the <b>JVM Stack</b> unwinds the exception to the caller because
 *      there is no transaction boundary to buffer the failure.</li>
 *     <li><b>Memory Management:</b> Efficient mapping ensures {@link OrderEvent}
 *      objects in the <b>Heap</b> are short-lived and eligible for GC.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class KafkaEventServiceImplTest {

    @Mock
    private OrderEventProducer producer;

    @Mock
    private OrderMapper mapper;

    @InjectMocks
    private KafkaEventServiceImpl service;

    /**
     * <b>Test:</b> Non-Transactional Immediate Dispatch
     */
    @Test
    @DisplayName("sendOrderEvent - No Transaction - Immediate Dispatch")
    void sendOrderEvent_noTransaction_dispatchesImmediately() {
        Order order = new Order();
        order.setId(101L);
        OrderEvent mockEvent = new OrderEvent(OrderEventType.ORDER_INITIATED, 101L, 1L, null, null, null);

        when(mapper.toEvent(eq(order), any())).thenReturn(mockEvent);

        service.sendOrderEvent(order, OrderEventType.ORDER_INITIATED);

        verify(producer, times(1)).publishOrderEvent(mockEvent);
    }

    /**
     * <b>Test:</b> Mapping Failure Guard
     */
    @Test
    @DisplayName("sendOrderEvent - Mapping Failure")
    void sendOrderEvent_mappingFailure() {
        Order order = new Order();
        when(mapper.toEvent(any(), any())).thenThrow(new RuntimeException("Mapping Error"));

        assertThrows(RuntimeException.class, () ->
                service.sendOrderEvent(order, OrderEventType.ORDER_INITIATED)
        );

        verifyNoInteractions(producer);
    }

    /**
     * <b>Test:</b> Producer Failure (Kafka Down)
     * <p>
     * Confirms that during immediate dispatch (no transaction), a producer
     * failure bubbles up to the caller.
     * </p>
     */
    @Test
    @DisplayName("sendOrderEvent - Producer Exception Handling")
    void sendOrderEvent_producerFailure() {
        // Arrange
        Order order = new Order();
        order.setId(101L);
        OrderEvent mockEvent = new OrderEvent(OrderEventType.ORDER_INITIATED, 101L, 1L, null, null, null);

        when(mapper.toEvent(any(), any())).thenReturn(mockEvent);

        // Force the simulated "Kafka Down" error
        doThrow(new RuntimeException("Kafka Down")).when(producer).publishOrderEvent(any());

        // Act & Assert
        // We catch the exception here to prove we know it happens
        assertThrows(RuntimeException.class, () -> {
            service.sendOrderEvent(order, OrderEventType.ORDER_INITIATED);
        }, "Expected Kafka Down exception to bubble up during immediate dispatch");

        verify(producer).publishOrderEvent(any());
    }
}