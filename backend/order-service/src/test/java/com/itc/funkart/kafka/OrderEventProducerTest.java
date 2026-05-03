package com.itc.funkart.kafka;

import com.itc.funkart.common.constants.messaging.KafkaTopics;
import com.itc.funkart.common.dto.event.order.OrderCancelledEvent;
import com.itc.funkart.common.dto.event.order.OrderEvent;
import com.itc.funkart.common.dto.event.order.OrderInitiatedEvent;
import com.itc.funkart.common.enums.order.OrderEventType;
import com.itc.funkart.kafka.producer.OrderEventProducer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * <h2>OrderEventProducerTest</h2>
 * <p>
 * Verifies that domain events are correctly dispatched to the Kafka broker.
 * These tests ensure that the <b>Execution Engine</b> correctly maps Record fields
 * to Kafka message keys for partition affinity.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class OrderEventProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private OrderEventProducer producer;

    /**
     * Verifies that {@link OrderInitiatedEvent} uses the Order ID as the partition key.
     */
    @Test
    @DisplayName("Publish Order Created - Should use OrderId as key and send to orders topic")
    void publishOrderCreated_shouldSendExpectedPayload() {
        // Arrange: Using canonical constructor for the Record
        OrderInitiatedEvent event = new OrderInitiatedEvent(
                OrderEventType.ORDER_INITIATED,
                101L, // orderId (The Key)
                1L,   // userId
                BigDecimal.valueOf(250.00),
                List.of(50L, 51L)
        );

        // Mocking the async CompletableFuture returned by KafkaTemplate
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);

        // Act
        producer.publishOrderCreated(event);

        // Assert: Ensure the orderId (101) is converted to String key for Kafka partitioning
        verify(kafkaTemplate).send(eq(KafkaTopics.ORDERS), eq("101"), eq(event));
    }

    /**
     * Verifies that the full lifecycle {@link OrderEvent} is correctly broadcasted.
     */
    @Test
    @DisplayName("Publish Full Order Event - Should broadcast snapshot with items")
    void publishOrderEvent_shouldSendFullPayload() {
        // Arrange
        OrderEvent event = new OrderEvent(
                OrderEventType.PAYMENT_SUCCESS,
                101L,
                1L,
                BigDecimal.valueOf(250.00),
                LocalDateTime.now(),
                List.of() // Items payload
        );

        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);

        // Act
        producer.publishOrderEvent(event);

        // Assert
        verify(kafkaTemplate).send(eq(KafkaTopics.ORDERS), eq("101"), eq(event));
    }

    /**
     * Verifies that the specialized {@link OrderCancelledEvent} is dispatched correctly.
     */
    @Test
    @DisplayName("Publish Order Cancelled - Should send lightweight cancellation record")
    void publishOrderCancelled_shouldSendCancelEvent() {
        // Arrange: Testing the Builder pattern often used for cancellation reasons
        OrderCancelledEvent event = OrderCancelledEvent.builder()
                .orderId(101L)
                .userId(1L)
                .eventType(OrderEventType.ORDER_CANCELLED)
                .reason("Inventory Shortage")
                .timestamp(LocalDateTime.now())
                .build();

        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);

        // Act
        producer.publishOrderCancelled(event);

        // Assert
        verify(kafkaTemplate).send(eq(KafkaTopics.ORDERS), eq("101"), eq(event));
    }
}