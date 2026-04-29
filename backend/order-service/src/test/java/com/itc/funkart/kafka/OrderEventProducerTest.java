package com.itc.funkart.kafka;

import com.itc.funkart.dto.OrderEvent;
import com.itc.funkart.entity.Order;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderEventProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private OrderEventProducer producer;

    @Captor
    private ArgumentCaptor<Message<?>> messageCaptor;

    private Order sampleOrder;

    @BeforeEach
    void setUp() {
        sampleOrder = Order.builder()
                .orderId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .productId(UUID.randomUUID())
                .quantity(2)
                .price(19.99)
                .orderStatus("CREATED")
                .createdAt(LocalDateTime.now().minusMinutes(1))
                .updatedAt(LocalDateTime.now().minusMinutes(1))
                .build();
    }

    @Test
    void publishOrderCreated_shouldSendMessageWithHeadersAndPayload() {
        SendResult<String, Object> result = mock(SendResult.class);
        RecordMetadata metadata = mock(RecordMetadata.class);
        when(result.getRecordMetadata()).thenReturn(metadata);
        when(metadata.partition()).thenReturn(0);
        when(metadata.offset()).thenReturn(0L);

        when(kafkaTemplate.send(any(Message.class))).thenReturn(CompletableFuture.completedFuture(result));

        String correlationId = UUID.randomUUID().toString();
        boolean published = producer.publishOrderCreated(sampleOrder, correlationId);

        assertEquals(true, published);
        verify(kafkaTemplate).send(messageCaptor.capture());

        Message<?> msg = messageCaptor.getValue();
        assertEquals("orders.events", msg.getHeaders().get("kafka_topic"));
        assertEquals(sampleOrder.getOrderId().toString(), msg.getHeaders().get("kafka_messageKey"));
        assertEquals(correlationId, msg.getHeaders().get("correlationId"));
        assertNotNull(msg.getHeaders().get("eventId"));

        OrderEvent event = (OrderEvent) msg.getPayload();
        assertEquals("order.created", event.getEventType());
        assertEquals(correlationId, event.getCorrelationId());
        assertEquals(sampleOrder.getOrderId(), event.getOrderId());
        assertEquals(sampleOrder.getCustomerId(), event.getCustomerId());
        assertEquals(sampleOrder.getProductId(), event.getProductId());
    }

    @Test
    void publishOrderUpdated_shouldSendUpdatedEvent() {
        SendResult<String, Object> result = mock(SendResult.class);
        RecordMetadata metadata = mock(RecordMetadata.class);
        when(result.getRecordMetadata()).thenReturn(metadata);
        when(metadata.partition()).thenReturn(0);
        when(metadata.offset()).thenReturn(0L);
        when(kafkaTemplate.send(any(Message.class))).thenReturn(CompletableFuture.completedFuture(result));

        String correlationId = UUID.randomUUID().toString();
        boolean published = producer.publishOrderUpdated(sampleOrder, correlationId);

        assertEquals(true, published);
        verify(kafkaTemplate).send(messageCaptor.capture());
        OrderEvent event = (OrderEvent) messageCaptor.getValue().getPayload();
        assertEquals("order.updated", event.getEventType());
        assertEquals(correlationId, event.getCorrelationId());
    }

    @Test
    void publishOrderCancelled_shouldSendCancelledEvent() {
        SendResult<String, Object> result = mock(SendResult.class);
        RecordMetadata metadata = mock(RecordMetadata.class);
        when(result.getRecordMetadata()).thenReturn(metadata);
        when(metadata.partition()).thenReturn(0);
        when(metadata.offset()).thenReturn(0L);
        when(kafkaTemplate.send(any(Message.class))).thenReturn(CompletableFuture.completedFuture(result));

        String correlationId = UUID.randomUUID().toString();
        UUID orderId = UUID.randomUUID();
        boolean published = producer.publishOrderCancelled(orderId, correlationId);

        assertEquals(true, published);
        verify(kafkaTemplate).send(messageCaptor.capture());
        OrderEvent event = (OrderEvent) messageCaptor.getValue().getPayload();
        assertEquals("order.cancelled", event.getEventType());
        assertEquals(orderId, event.getOrderId());
        assertEquals("CANCELLED", event.getOrderStatus());
    }

    @Test
    void publishOrderCreated_shouldThrowWhenKafkaTemplateThrows() {
        when(kafkaTemplate.send(any(Message.class))).thenThrow(new RuntimeException("no broker"));
        assertThrows(RuntimeException.class, () -> producer.publishOrderCreated(sampleOrder, "corr"));
    }
}

