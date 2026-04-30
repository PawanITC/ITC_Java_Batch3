package com.itc.funkart.kafka;

import com.itc.funkart.dto.OrderEvent;
import com.itc.funkart.entity.Order;
import com.itc.funkart.entity.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * <h2>OrderEventProducerTest</h2>
 */
@ExtendWith(MockitoExtension.class)
class OrderEventProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private OrderEventProducer producer;

    @Captor
    private ArgumentCaptor<OrderEvent> eventCaptor;

    private Order sampleOrder;

    @BeforeEach
    void setUp() {
        sampleOrder = new Order();
        sampleOrder.setId(101L);
        sampleOrder.setCustomerId(1L);
        sampleOrder.setStatus(OrderStatus.PENDING);
        sampleOrder.setTotalAmount(BigDecimal.valueOf(250.00));
    }

    @Test
    @DisplayName("Publish Order Created - Should send DTO to order-events topic")
    void publishOrderCreated_shouldSendExpectedPayload() {

        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        producer.publishOrderCreated(sampleOrder);

        verify(kafkaTemplate).send(eq("order-events"), anyString(), eventCaptor.capture());

        OrderEvent capturedEvent = eventCaptor.getValue();

        assertEquals("ORDER_CREATED", capturedEvent.getEventType());
        assertEquals(101L, capturedEvent.getOrderId());
        assertEquals(1L, capturedEvent.getCustomerId());
        assertEquals(BigDecimal.valueOf(250.00), capturedEvent.getTotalAmount());
        assertNotNull(capturedEvent.getTimestamp());
    }

    @Test
    @DisplayName("Publish Order Updated - Should broadcast status changes")
    void publishOrderUpdated_shouldSendUpdateEvent() {

        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        sampleOrder.setStatus(OrderStatus.SHIPPED);

        producer.publishOrderUpdated(sampleOrder);

        verify(kafkaTemplate).send(eq("order-events"), anyString(), eventCaptor.capture());

        OrderEvent capturedEvent = eventCaptor.getValue();

        assertEquals("ORDER_UPDATED", capturedEvent.getEventType());
        assertEquals(101L, capturedEvent.getOrderId());
    }

    @Test
    @DisplayName("Publish Order Cancelled - Should handle raw ID input")
    void publishOrderCancelled_shouldSendCancelEvent() {

        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        Long orderId = 101L;

        producer.publishOrderCancelled(orderId);

        verify(kafkaTemplate).send(eq("order-events"), anyString(), eventCaptor.capture());

        OrderEvent capturedEvent = eventCaptor.getValue();

        assertEquals("ORDER_CANCELLED", capturedEvent.getEventType());
        assertEquals(orderId, capturedEvent.getOrderId());
    }
}