


package com.itc.funkart.kafka;

import com.itc.funkart.dto.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    @KafkaListener(topics = "order-events3", groupId = "service-events")
    public void consume(OrderEvent event) {
        log.info("🔥 EVENT RECEIVED: {}", event);

        switch (event.getEventType()) {
            case "ORDER_CREATED" -> handleOrderCreated(event);
            case "ORDER_UPDATED" -> handleOrderUpdated(event);
            case "ORDER_CANCELLED" -> handleOrderCancelled(event);
            default -> log.warn("⚠️ Unknown event type: {}", event.getEventType());
        }
    }

    private void handleOrderCreated(OrderEvent event) {
        log.info("Processing ORDER_CREATED for orderId={}", event.getOrderId());
    }

    private void handleOrderUpdated(OrderEvent event) {
        log.info("Processing ORDER_UPDATED for orderId={}", event.getOrderId());
    }

    private void handleOrderCancelled(OrderEvent event) {
        log.info("Processing ORDER_CANCELLED for orderId={}", event.getOrderId());
    }
}