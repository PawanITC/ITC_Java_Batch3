package com.itc.funkart.kafka;

import com.itc.funkart.entity.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderEventProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private OrderEventProducer producer;

    @Captor
    private ArgumentCaptor<Map<String, Object>> payloadCaptor;

    private Order sampleOrder;

    @BeforeEach
    void setUp() {
        sampleOrder = Order.builder()
                .orderId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .productId(UUID.randomUUID())
                .orderStatus("CREATED")
                .build();
    }

    @Test
    void publishOrderCreated_shouldSendExpectedPayload() {
        producer.publishOrderCreated(sampleOrder);

        verify(kafkaTemplate).send(eq("order-events"), payloadCaptor.capture());

        Map<String, Object> payload = payloadCaptor.getValue();
        assertEquals("ORDER_CREATED", payload.get("eventType"));
        assertEquals(sampleOrder.getOrderId(), payload.get("orderId"));
        assertEquals(sampleOrder.getCustomerId(), payload.get("customerId"));
        assertInstanceOf(LocalDateTime.class, payload.get("timestamp"));
    }

    @Test
    void publishOrderUpdated_shouldSendUpdateEvent() {
        producer.publishOrderUpdated(sampleOrder);

        verify(kafkaTemplate).send(eq("order-events"), payloadCaptor.capture());

        Map<String, Object> payload = payloadCaptor.getValue();
        assertEquals("ORDER_UPDATED", payload.get("eventType"));
        assertEquals(sampleOrder.getOrderId(), payload.get("orderId"));
        assertNotNull(payload.get("orderId"));
    }

    @Test
    void publishOrderCancelled_shouldSendCancelEvent() {
        UUID orderId = UUID.randomUUID();
        producer.publishOrderCancelled(orderId);

        verify(kafkaTemplate).send(eq("order-events"), payloadCaptor.capture());

        Map<String, Object> payload = payloadCaptor.getValue();
        assertEquals("ORDER_CANCELLED", payload.get("eventType"));
        assertEquals(orderId, payload.get("orderId"));
    }
}
