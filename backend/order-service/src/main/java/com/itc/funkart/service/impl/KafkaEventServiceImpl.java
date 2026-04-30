package com.itc.funkart.service.impl;

import com.itc.funkart.entity.Order;
import com.itc.funkart.kafka.OrderEventProducer;
import com.itc.funkart.service.KafkaEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaEventServiceImpl implements KafkaEventService {

    private final OrderEventProducer producer;

    @Override
    public boolean sendOrderEvent(Order order) {
        try {
            log.debug("📤 Attempting to broadcast event for Order: {}", order.getId());
            producer.publishOrderCreated(order);
            log.info("✅ Event successfully streamed to Kafka for Order: {}", order.getId());
            return true;
        } catch (Exception e) {
            // We log the error but don't throw it,
            // allowing the DB transaction to remain successful.
            log.error("❌ Kafka Stream Failure [Order {}]: {}", order.getId(), e.getMessage());
            return false;
        }
    }
}