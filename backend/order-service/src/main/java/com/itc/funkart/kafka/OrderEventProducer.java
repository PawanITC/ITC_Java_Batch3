

package com.itc.funkart.kafka;

import com.itc.funkart.entity.Order;
import com.itc.funkart.dto.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "order-events3";

    private boolean sendEvent(OrderEvent event, UUID key) {
        try {
            kafkaTemplate.send(TOPIC, key.toString(), event).get();
            log.info("✅ Kafka event sent for key={}", key);
            return true;
        } catch (Exception e) {
            log.error("❌ Kafka send failed for key={}: {}", key, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public boolean publishOrderCreated(Order order) {
        OrderEvent event = new OrderEvent();
        event.setEventType("ORDER_CREATED");
        event.setOrderId(order.getOrderId());
        event.setCustomerId(order.getCustomerId());
        event.setProductId(order.getProductId());
        event.setQuantity(order.getQuantity());
        event.setPrice(order.getPrice());
        event.setTimestamp(LocalDateTime.now());

        return sendEvent(event, order.getOrderId());
    }

    public boolean publishOrderUpdated(Order order) {
        OrderEvent event = new OrderEvent();
        event.setEventType("ORDER_UPDATED");
        event.setOrderId(order.getOrderId());
        event.setTimestamp(LocalDateTime.now());

        return sendEvent(event, order.getOrderId());
    }

    public boolean publishOrderCancelled(UUID orderId) {
        OrderEvent event = new OrderEvent();
        event.setEventType("ORDER_CANCELLED");
        event.setOrderId(orderId);
        event.setTimestamp(LocalDateTime.now());

        return sendEvent(event, orderId);
    }
}