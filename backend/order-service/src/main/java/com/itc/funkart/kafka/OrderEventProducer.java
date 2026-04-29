
package com.itc.funkart.kafka;

import com.itc.funkart.dto.OrderEvent;
import com.itc.funkart.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String ORDER_SERVICE = "order-service";
    private static final String ORDERS_TOPIC = "orders.events";

    // ========== PUBLISH ORDER CREATED ==========
    public boolean publishOrderCreated(Order order, String correlationId) {
        try {
            OrderEvent event = OrderEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("order.created")
                    .eventVersion(1)
                    .eventTime(LocalDateTime.now())
                    .correlationId(correlationId)
                    .source(ORDER_SERVICE)
                    .orderId(order.getOrderId())
                    .customerId(order.getCustomerId())
                    .productId(order.getProductId())
                    .quantity(order.getQuantity())
                    .price(order.getPrice())
                    .orderStatus(order.getOrderStatus())
                    .createdAt(order.getCreatedAt())
                    .build();

            publishEvent(ORDERS_TOPIC, order.getOrderId().toString(), event, correlationId);
            log.info("[{}] Order created event published | Order: {} | Event: {}",
                    correlationId, order.getOrderId(), event.getEventId());
            return true;

        } catch (Exception e) {
            log.error("[{}] Failed to publish order.created event | Order: {}",
                    correlationId, order.getOrderId(), e);
            throw new RuntimeException("Kafka publish failed", e);
        }
    }

    // ========== PUBLISH ORDER UPDATED ==========
    public boolean publishOrderUpdated(Order order, String correlationId) {
        try {
            OrderEvent event = OrderEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("order.updated")
                    .eventVersion(1)
                    .eventTime(LocalDateTime.now())
                    .correlationId(correlationId)
                    .source(ORDER_SERVICE)
                    .orderId(order.getOrderId())
                    .customerId(order.getCustomerId())
                    .productId(order.getProductId())
                    .quantity(order.getQuantity())
                    .price(order.getPrice())
                    .orderStatus(order.getOrderStatus())
                    .updatedAt(order.getUpdatedAt())
                    .build();

            publishEvent(ORDERS_TOPIC, order.getOrderId().toString(), event, correlationId);
            log.info("[{}] Order updated event published | Order: {}", correlationId, order.getOrderId());
            return true;

        } catch (Exception e) {
            log.error("[{}] Failed to publish order.updated event", correlationId, e);
            throw new RuntimeException("Kafka publish failed", e);
        }
    }

    // ========== PUBLISH ORDER CANCELLED ==========
    public boolean publishOrderCancelled(UUID orderId, String correlationId) {
        try {
            OrderEvent event = OrderEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("order.cancelled")
                    .eventVersion(1)
                    .eventTime(LocalDateTime.now())
                    .correlationId(correlationId)
                    .source(ORDER_SERVICE)
                    .orderId(orderId)
                    .orderStatus("CANCELLED")
                    .build();

            publishEvent(ORDERS_TOPIC, orderId.toString(), event, correlationId);
            log.info("[{}] Order cancelled event published | Order: {}", correlationId, orderId);
            return true;

        } catch (Exception e) {
            log.error("[{}] Failed to publish order.cancelled event", correlationId, e);
            throw new RuntimeException("Kafka publish failed", e);
        }
    }

    // ========== HELPER: Generic Event Publisher ==========
    private void publishEvent(String topic, String key, OrderEvent event, String correlationId) {
        try {
            // Build message with headers for tracing
            Message<OrderEvent> message = MessageBuilder
                    .withPayload(event)
                    .setHeader(KafkaHeaders.TOPIC, topic)
                    .setHeader(KafkaHeaders.KEY, key)
                    .setHeader("correlationId", correlationId)
                    .setHeader("eventId", event.getEventId())
                    .build();

            kafkaTemplate.send(message).whenComplete((result, ex) -> {
                if (ex == null) {
                    var metadata = result.getRecordMetadata();
                    log.debug("[{}] Event sent to Kafka | Topic: {} | Partition: {} | Offset: {} | Key: {}",
                            correlationId, topic, metadata.partition(), metadata.offset(), key);
                } else {
                    log.error("[{}] Failed to send event to Kafka | Topic: {} | Error: {}",
                            correlationId, topic, ex.getMessage(), ex);
                }
            });

        } catch (Exception e) {
            log.error("[{}] Error publishing event", correlationId, e);
            throw new RuntimeException("Event publishing failed", e);
        }
    }
}