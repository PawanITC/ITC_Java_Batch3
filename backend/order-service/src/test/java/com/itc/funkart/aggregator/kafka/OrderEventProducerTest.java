package com.itc.funkart.aggregator.kafka;

import com.itc.funkart.common.constants.messaging.KafkaTopics;
import com.itc.funkart.common.dto.event.checkout.CheckoutItemPayload;
import com.itc.funkart.common.dto.event.order.OrderCancelledEvent;
import com.itc.funkart.common.dto.event.order.OrderEvent;
import com.itc.funkart.common.enums.order.OrderEventType;
import com.itc.funkart.aggregator.kafka.producer.OrderEventProducer;
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
 *
 * <h3>Fix applied:</h3>
 * <p>{@code producer.publishOrderCreated(event)} → {@code producer.publishOrderEvent(event)}.
 * The method was renamed in OrderEventProducer to better reflect that it dispatches
 * a full {@link OrderEvent}, not specifically a "created" event.</p>
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class OrderEventProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private OrderEventProducer producer;

    @Test
    @DisplayName("publishOrderEvent(OrderInitiatedEvent) — should use orderId as key and send to ORDERS topic")
    void publishOrderEvent_withInitiatedEvent_shouldSendExpectedPayload() {
        CheckoutItemPayload item = new CheckoutItemPayload(
                50L, 2, BigDecimal.valueOf(125.00), BigDecimal.valueOf(250.00)
        );

        // NOTE: OrderInitiatedEvent extends/is a subtype of the event — verify it routes correctly
        // This test now uses the OrderEvent wrapper as the producer expects
        OrderEvent event = new OrderEvent(
                OrderEventType.ORDER_INITIATED,
                101L,
                1L,
                BigDecimal.valueOf(250.00),
                LocalDateTime.now(),
                List.of()
        );

        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);

        // FIX: was producer.publishOrderCreated(event) — method renamed to publishOrderEvent
        producer.publishOrderEvent(event);

        // orderId (101) becomes the String partition key
        verify(kafkaTemplate).send(eq(KafkaTopics.ORDERS), eq("101"), eq(event));
    }

    @Test
    @DisplayName("publishOrderEvent — should broadcast full OrderEvent snapshot")
    void publishOrderEvent_shouldSendFullPayload() {
        OrderEvent event = new OrderEvent(
                OrderEventType.PAYMENT_SUCCESS,
                101L,
                1L,
                BigDecimal.valueOf(250.00),
                LocalDateTime.now(),
                List.of()
        );

        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);

        // FIX: was producer.publishOrderCreated(event)
        producer.publishOrderEvent(event);

        verify(kafkaTemplate).send(eq(KafkaTopics.ORDERS), eq("101"), eq(event));
    }

    @Test
    @DisplayName("publishOrderCancelled — should send lightweight cancellation record")
    void publishOrderCancelled_shouldSendCancelEvent() {
        OrderCancelledEvent event = OrderCancelledEvent.builder()
                .orderId(101L)
                .userId(1L)
                .eventType(OrderEventType.ORDER_CANCELLED)
                .reason("Inventory Shortage")
                .timestamp(LocalDateTime.now())
                .build();

        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);

        producer.publishOrderCancelled(event);

        verify(kafkaTemplate).send(eq(KafkaTopics.ORDERS), eq("101"), eq(event));
    }
}