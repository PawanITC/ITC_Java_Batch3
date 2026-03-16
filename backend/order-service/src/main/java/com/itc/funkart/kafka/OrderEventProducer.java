package com.itc.funkart.kafka;

import com.itc.funkart.entity.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC = "order-events";

    public void publishOrderCreated(Order order) {

        Map<String, Object> event = Map.of(
                "eventType", "ORDER_CREATED",
                "orderId", order.getOrderId(),
                "customerId", order.getCustomerId(),
                "timestamp", LocalDateTime.now()
        );

        kafkaTemplate.send(TOPIC, event);
    }

    public void publishOrderUpdated(Order order) {

        Map<String, Object> event = Map.of(
                "eventType", "ORDER_UPDATED",
                "orderId", order.getOrderId()
        );

        kafkaTemplate.send(TOPIC, event);
    }

    public void publishOrderCancelled(UUID orderId) {

        Map<String, Object> event = Map.of(
                "eventType", "ORDER_CANCELLED",
                "orderId", orderId
        );

        kafkaTemplate.send(TOPIC, event);
    }
}
